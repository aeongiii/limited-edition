# Run all: ./run.ps1
# Stop all: ./run.ps1 -Down

param(
    [switch]$SkipDocker,
    [switch]$Down
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"

function Wait-Port {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSec = 120
    )

    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        $ok = (Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
        if ($ok) { return $true }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }
    return $false
}

function Ensure-DockerReady {
    param([int]$TimeoutSec = 180)

    try {
        docker info | Out-Null
        return $true
    } catch {
        $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
        if (-not (Test-Path $dockerDesktop)) {
            throw "Docker Desktop executable not found: $dockerDesktop"
        }
        Write-Host "Docker daemon is not ready. Launching Docker Desktop..."
        Start-Process $dockerDesktop | Out-Null
    }

    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        try {
            docker info | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 3
            $elapsed += 3
        }
    }
    return $false
}

function Stop-ByPort {
    param([int]$Port)

    $procIds = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)

    foreach ($procId in $procIds) {
        if ($procId) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction Stop
                Write-Host "  - Stopped PID $procId on :$Port"
            } catch {
                Write-Warning "  - Failed to stop PID $procId on :$Port"
            }
        }
    }
}

$projectRoot = $PSScriptRoot
$logDir = Join-Path $projectRoot "run-logs"

$services = @(
    @{ Name = "eureka-server"; Port = 8761 },
    @{ Name = "user-service"; Port = 8081 },
    @{ Name = "product-service"; Port = 8082 },
    @{ Name = "order-service"; Port = 8083 },
    @{ Name = "wishlist-service"; Port = 8084 },
    @{ Name = "payment-service"; Port = 8085 },
    @{ Name = "gateway-server"; Port = 8080 }
)

$infraPorts = @(3312, 3311, 3308, 3309, 3310, 6379, 2181, 9092)

Set-Location $projectRoot
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if ($Down) {
    Write-Host "[1/2] Stopping Spring Boot services..."
    foreach ($svc in $services) {
        Stop-ByPort -Port $svc.Port
    }

    if (-not $SkipDocker) {
        Write-Host "[2/2] Stopping Docker infrastructure..."
        docker compose down
    } else {
        Write-Host "[2/2] Skip Docker down."
    }

    Write-Host "Shutdown completed."
    return
}

if (-not $SkipDocker) {
    Write-Host "[1/4] Starting Docker infrastructure..."
    if (-not (Ensure-DockerReady)) {
        throw "Docker daemon is not ready."
    }
    docker compose up -d

    Write-Host "[2/4] Waiting infra ports..."
    foreach ($port in $infraPorts) {
        if (Wait-Port -HostName "localhost" -Port $port -TimeoutSec 180) {
            Write-Host "  - localhost:$port UP"
        } else {
            Write-Warning "  - localhost:$port not ready (timeout)"
        }
    }
} else {
    Write-Host "[1/4] Skip Docker start."
}

Write-Host "[3/4] Starting Spring Boot services..."

foreach ($svc in $services) {
    $serviceName = $svc.Name
    $servicePort = $svc.Port
    $logOut = Join-Path $logDir "$serviceName.out.log"
    $logErr = Join-Path $logDir "$serviceName.err.log"

    $alreadyUp = (Test-NetConnection -ComputerName localhost -Port $servicePort -WarningAction SilentlyContinue).TcpTestSucceeded
    if ($alreadyUp) {
        Write-Host "Already running: $serviceName (:$servicePort)"
        continue
    }

    Write-Host "Launching: $serviceName (:$servicePort)"

    if (Test-Path $logOut) { Remove-Item $logOut -Force }
    if (Test-Path $logErr) { Remove-Item $logErr -Force }

    $startCmd = "gradlew.bat :$($serviceName):bootRun --no-daemon 1> `"$logOut`" 2> `"$logErr`""
    $proc = Start-Process cmd.exe `
        -ArgumentList '/c', $startCmd `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -PassThru

    $isUp = $false
    $startupTimeoutSec = if ($serviceName -eq "gateway-server") { 360 } else { 240 }
    for ($elapsed = 0; $elapsed -lt $startupTimeoutSec; $elapsed += 2) {
        Start-Sleep -Seconds 2
        $isUp = (Test-NetConnection -ComputerName localhost -Port $servicePort -WarningAction SilentlyContinue).TcpTestSucceeded
        if ($isUp) { break }
        if ($proc.HasExited) { break }
    }

    if ($isUp) {
        Write-Host "  -> UP: $serviceName (:$servicePort)"
    } else {
        Write-Warning "  -> Failed to confirm startup: $serviceName (:$servicePort). Check logs: $logOut / $logErr"
    }
}

Write-Host "[4/4] Final port check..."
foreach ($svc in $services) {
    $ok = (Test-NetConnection -ComputerName localhost -Port $svc.Port -WarningAction SilentlyContinue).TcpTestSucceeded
    if ($ok) {
        Write-Host "  - $($svc.Name) :$($svc.Port) UP"
    } else {
        Write-Warning "  - $($svc.Name) :$($svc.Port) DOWN"
    }
}

Write-Host "Startup completed."
