# 전체 서비스 실행: ./run.ps1
# 전체 서비스 종료: ./run.ps1 -Down
# Docker 인프라를 제외하고 앱 서버만 시작/종료: ./run.ps1 -SkipDocker

param(
    [switch]$SkipDocker,
    [switch]$Down
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = "Stop"
# 네이티브 명령의 stderr를 PowerShell 오류로 승격하지 않도록 처리한다.
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $global:PSNativeCommandUseErrorActionPreference = $false
}

# 스크립트에서 공통으로 사용하는 경로
$projectRoot = $PSScriptRoot
$logDir = Join-Path $projectRoot "run-logs"
$pidDir = Join-Path $logDir "pids"
$isWindowsHost = $env:OS -eq "Windows_NT"

# 서비스 기동 순서:
# 1) Eureka를 먼저 띄워서 클라이언트 등록 가능 상태를 만든다.
# 2) 도메인 서비스들을 순차적으로 실행한다.
# 3) Gateway를 마지막에 실행한다.
$services = @(
    @{ Name = "eureka-server"; Port = 8761; TimeoutSec = 240 },
    @{ Name = "user-service"; Port = 8081; TimeoutSec = 300 },
    @{ Name = "product-service"; Port = 8082; TimeoutSec = 300 },
    @{ Name = "order-service"; Port = 8083; TimeoutSec = 300 },
    @{ Name = "wishlist-service"; Port = 8084; TimeoutSec = 300 },
    @{ Name = "payment-service"; Port = 8085; TimeoutSec = 300 },
    @{ Name = "gateway-server"; Port = 8080; TimeoutSec = 360 }
)

# 전체 서비스가 공통으로 의존하는 인프라 포트 목록.
$infraPorts = @(3312, 3311, 3308, 3309, 3310, 6379, 2181, 9092)

function Test-PortOpen {
    param(
        [string]$HostName = "localhost",
        [int]$Port
    )

    return (Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
}

function Wait-Port {
    param(
        [string]$HostName = "localhost",
        [int]$Port,
        [int]$TimeoutSec = 120
    )

    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        if (Test-PortOpen -HostName $HostName -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
    }

    return $false
}

function Invoke-Docker {
    param(
        [string[]]$Arguments,
        [switch]$Quiet
    )

    # Windows PowerShell에서는 네이티브 stderr가 error record로 잡힐 수 있다.
    # 여기서는 잠시 ErrorAction을 완화하고, 종료 코드를 기준으로 성공/실패를 판단한다.
    $originalPreference = $ErrorActionPreference
    $script:ErrorActionPreference = "Continue"
    try {
        if ($Quiet) {
            & docker @Arguments 1> $null 2> $null
        } else {
            & docker @Arguments
        }
        return $LASTEXITCODE
    } finally {
        $script:ErrorActionPreference = $originalPreference
    }
}

function Test-DockerReady {
    $dockerExitCode = Invoke-Docker -Arguments @("info") -Quiet
    return $dockerExitCode -eq 0
}

function Ensure-DockerReady {
    param([int]$TimeoutSec = 240)

    # Docker 데몬이 이미 준비되어 있으면 바로 진행한다.
    if (Test-DockerReady) {
        return $true
    }

    # 데몬이 준비되지 않았다면 Docker Desktop을 한 번 실행해본다.
    $dockerDesktop = Join-Path $env:ProgramFiles "Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path $dockerDesktop)) {
        throw "Docker Desktop 실행 파일을 찾을 수 없습니다: $dockerDesktop"
    }

    Write-Host "Docker 데몬이 준비되지 않아 Docker Desktop을 실행합니다..."
    Start-Process -FilePath $dockerDesktop | Out-Null

    $elapsed = 0
    while ($elapsed -lt $TimeoutSec) {
        if (Test-DockerReady) {
            return $true
        }

        Start-Sleep -Seconds 3
        $elapsed += 3
    }

    return $false
}

function Stop-ByPidFile {
    param(
        [string]$ServiceName,
        [string]$PidFilePath
    )

    if (-not (Test-Path $PidFilePath)) {
        return
    }

    $pidText = (Get-Content $PidFilePath -ErrorAction SilentlyContinue | Select-Object -First 1)
    Remove-Item -Path $PidFilePath -Force -ErrorAction SilentlyContinue

    [int]$targetPid = 0
    if (-not [int]::TryParse($pidText, [ref]$targetPid)) {
        return
    }

    $process = Get-Process -Id $targetPid -ErrorAction SilentlyContinue
    if ($process) {
        try {
            Stop-Process -Id $targetPid -Force -ErrorAction Stop
            Write-Host "  - PID 파일 기준으로 $ServiceName 프로세스를 종료했습니다. (PID: $targetPid)"
        } catch {
            Write-Warning "  - $ServiceName 프로세스 종료에 실패했습니다. (PID: $targetPid)"
        }
    }
}

function Stop-ByPort {
    param([int]$Port)

    $procIds = @(
        Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    )

    foreach ($procId in $procIds) {
        if (-not $procId) {
            continue
        }

        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "  - :$Port 포트에서 실행 중인 PID $procId 프로세스를 종료했습니다."
        } catch {
            Write-Warning "  - :$Port 포트의 PID $procId 프로세스 종료에 실패했습니다."
        }
    }
}

function Get-GradleStartConfig {
    if ($isWindowsHost) {
        $gradleBat = Join-Path $projectRoot "gradlew.bat"
        if (-not (Test-Path $gradleBat)) {
            throw "Gradle wrapper 파일을 찾을 수 없습니다: $gradleBat"
        }

        return @{
            FilePath = $gradleBat
            PrefixArgs = @()
        }
    }

    $gradleSh = Join-Path $projectRoot "gradlew"
    if (-not (Test-Path $gradleSh)) {
        throw "Gradle wrapper 파일을 찾을 수 없습니다: $gradleSh"
    }

    return @{
        FilePath = "bash"
        PrefixArgs = @("./gradlew")
    }
}

Set-Location $projectRoot
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
New-Item -ItemType Directory -Force -Path $pidDir | Out-Null

if ($Down) {
    Write-Host "[1/2] Spring Boot 서비스 종료 중..."
    foreach ($svc in $services) {
        $pidFile = Join-Path $pidDir "$($svc.Name).pid"
        Stop-ByPidFile -ServiceName $svc.Name -PidFilePath $pidFile
        Stop-ByPort -Port $svc.Port
    }

    if (-not $SkipDocker) {
        Write-Host "[2/2] Docker 인프라 종료 중..."
        $dockerDownExitCode = Invoke-Docker -Arguments @("compose", "down")
        if ($dockerDownExitCode -ne 0) {
            Write-Warning "docker compose down 실행에 실패했습니다. Docker 데몬이 이미 중지되었을 수 있습니다."
        }
    } else {
        Write-Host "[2/2] Docker 종료를 건너뜁니다."
    }

    Write-Host "종료가 완료되었습니다."
    return
}

if (-not $SkipDocker) {
    Write-Host "[1/4] Docker 인프라 시작 중..."
    if (-not (Ensure-DockerReady)) {
        throw "제한 시간 내에 Docker 데몬이 준비되지 않았습니다."
    }

    $dockerUpExitCode = Invoke-Docker -Arguments @("compose", "up", "-d")
    if ($dockerUpExitCode -ne 0) {
        throw "docker compose up -d 실행에 실패했습니다. Docker 인프라 상태를 먼저 확인한 뒤 다시 실행해 주세요."
    }

    Write-Host "[2/4] 인프라 포트 준비 대기 중..."
    foreach ($port in $infraPorts) {
        if (Wait-Port -HostName "localhost" -Port $port -TimeoutSec 180) {
            Write-Host "  - localhost:$port 준비 완료"
        } else {
            throw "인프라 포트 localhost:$port 가 준비되지 않았습니다."
        }
    }
} else {
    Write-Host "[1/4] Docker 시작을 건너뜁니다."
}

Write-Host "[3/4] Spring Boot 서비스 시작 중..."
$gradleStart = Get-GradleStartConfig

foreach ($svc in $services) {
    $serviceName = $svc.Name
    $servicePort = $svc.Port
    $startupTimeoutSec = $svc.TimeoutSec
    $logOut = Join-Path $logDir "$serviceName.out.log"
    $logErr = Join-Path $logDir "$serviceName.err.log"
    $pidFile = Join-Path $pidDir "$serviceName.pid"

    # 이미 떠 있는 서비스는 중복 bootRun을 피하기 위해 건너뛴다.
    if (Test-PortOpen -Port $servicePort) {
        Write-Host "이미 실행 중: $serviceName (:$servicePort)"
        continue
    }

    Write-Host "실행 시작: $serviceName (:$servicePort)"

    if (Test-Path $logOut) { Remove-Item $logOut -Force }
    if (Test-Path $logErr) { Remove-Item $logErr -Force }
    if (Test-Path $pidFile) { Remove-Item $pidFile -Force }

    $serviceTaskArg = ":${serviceName}:bootRun"
    $startArgs = @($gradleStart.PrefixArgs + @($serviceTaskArg, "--no-daemon"))
    $processParams = @{
        FilePath = $gradleStart.FilePath
        ArgumentList = $startArgs
        WorkingDirectory = $projectRoot
        RedirectStandardOutput = $logOut
        RedirectStandardError = $logErr
        PassThru = $true
    }
    if ($isWindowsHost) {
        $processParams["WindowStyle"] = "Hidden"
    }

    $proc = Start-Process @processParams
    Set-Content -Path $pidFile -Value $proc.Id -Encoding ASCII

    $isUp = $false
    for ($elapsed = 0; $elapsed -lt $startupTimeoutSec; $elapsed += 2) {
        Start-Sleep -Seconds 2
        if (Test-PortOpen -Port $servicePort) {
            $isUp = $true
            break
        }
        if ($proc.HasExited) {
            break
        }
    }

    if ($isUp) {
        Write-Host "  -> 실행 확인 완료: $serviceName (:$servicePort)"
    } else {
        Write-Warning "  -> 실행 확인 실패: $serviceName (:$servicePort). 로그 확인: $logOut / $logErr"
    }
}

Write-Host "[4/4] 최종 포트 상태 확인..."
$downServices = @()
foreach ($svc in $services) {
    $ok = Test-PortOpen -Port $svc.Port
    if ($ok) {
        Write-Host "  - $($svc.Name) :$($svc.Port) 실행 중"
    } else {
        Write-Warning "  - $($svc.Name) :$($svc.Port) 중지 상태"
        $downServices += $svc.Name
    }
}

if ($downServices.Count -gt 0) {
    Write-Warning "일부 서비스 기동에 실패했습니다. 중지 상태 서비스: $($downServices -join ', ')"
    Write-Warning "로그 확인 경로: $logDir"
} else {
    Write-Host "기동이 완료되었습니다. 모든 서비스가 정상 실행 중입니다."
}