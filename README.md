# **한정판 상품을 판매하는 E-Commerce 서비스**


- **프로젝트 진행 기간 : 2024.12 ~ 2025.01 (4주)**

<br>


## **📖 목차**

1. [❓프로젝트 소개](#-프로젝트-소개)
2. [📋기술 스택](#-기술-스택)
3. [🖥️실행 방법](#-실행-방법)
5. [🔍아키텍처](#-아키텍처)
3. [✨특징](#-특징)
6. [🚀성능 개선](#-성능-개선)
7. [⭐트러블 슈팅](#-트러블-슈팅)
8. [⚖️기술적 의사결정](#-기술적-의사결정)

## **🔗 LINK**

- [📝 API 명세서](https://documenter.getpostman.com/view/39288753/2sAYQakB2p)

<br>


## **❓ 프로젝트 소개**

한정 판매 상품은 정해진 시간에 구매를 열고, 일반 상품은 언제든 구매할 수 있도록 설계한 E-Commerce 서비스입니다. 특히, **정확성**을 최우선 목표로 삼아, 재고 수량을 초과하여 결제되지 않도록 제한하며, 남은 재고 수량을 정확히 보여주는 데 중점을 두었습니다.

대규모 트래픽 상황에서도 안정적으로 재고를 관리하기 위해 **MSA**와 **Redis 기반 캐싱**을 사용했습니다. 또한, 분산 환경에서 주문 프로세스가 일관성 있게 진행되도록 **Orchestration-Based Saga 패턴**을 사용했습니다.

<br>


## **📋 기술 스택**
### **1. 백엔드**

- Java 21, Spring Boot 3.4.0, Gradle 8.5
- Spring Boot Starter Web, JPA, Security
- Spring Security, JWT 0.11.5
- Quartz Job Scheduler

### **2. 배포 및 운영 환경**

- Docker / Docker Compose
- Spring Cloud Netflix Eureka 4.0.4
- Spring Cloud Gateway
- Spring Cloud OpenFeign 4.1.3

### **3. 데이터 처리 및 캐싱**

- MySQL 8.0, MySQL Connector
- Redis 7.4.1, Redisson 3.21.1

<br>


## 🖥️ 실행 방법

- Docker 및 Docker Compose를 설치한 뒤, 아래 명령어를 실행해주세요.

```bash
docker-compose up -d
```

- http://localhost:8080 에서 서비스에 접속할 수 있습니다.
- 각 모듈의 init.sql이 자동 실행되어 데이터베이스가 세팅됩니다.

<br>


## **🔍 아키텍처**

![title](https://github.com/user-attachments/assets/e5039d45-f7f7-4395-942b-95f12613ea51)


<br>


## **✨ 특징**

- MSA 구조로 **확장성 확보**, 트래픽 병목 시 특정 서비스에 대해 **독립적인 스케일 아웃 가능**
- API Gateway와 Eureka를 사용하여 **서비스 디스커버리 및 로드밸런싱을 구현**하고, 단일 포트만 외부에 노출하여 보안성 강화
- Redis 캐싱과 분산 락을 사용하여 한정판 구매 상황에서 **남은 재고 수량을 정확히 표시**
- Orchestration-Based Saga패턴을 적용하여 **분산 환경에서 안정적인 트랜잭션 관리**
- JWT 필터를 직접 구현하여 Access Token을 HTTP-only Cookie에 저장하고, Refresh Token은 만료시간 7일 설정하여 **인증 및 인가 처리**
- Docker와 Docker Compose를 사용해 Mysql, Redis, API Gateway, Eureka 등을 컨테이너화하여 **일관된 개발·배포 환경 구축**

<br>


## **🚀 성능 개선**

### **1. 주문 내역 조회 성능 개선**

- 최신 주문 데이터 5개 조회 시 DB 풀스캔이 발생하여 응답시간 지연 문제 발생
- **복합 인덱스**를 사용해 쿼리를 최적화하고 서버 부하를 감소시켜 응답 속도 및 처리량 개선
- **평균, 최소, 최대 응답시간** **최대 93.99% 감소**

|  | **개선 전** | **개선 후** | **개선율** |
| --- | --- | --- | --- |
| **평균 응답 시간 (ms)** | 713.69 | 42.91 | **93.99%** |
| **최대 응답 시간 (ms)** | 867.41 | 95.30 | **89.01%** |
| **최소 응답 시간 (ms)** | 153.43 | 20.63 | **86.55%** |


- **TPS 261.66% 증가**


|  | **개선 전** | **개선 후** | **개선율** |
| --- | --- | --- | --- |
| **TPS (초당 트랜잭션)** | 101.97 | 368.66 | **261.66%** |






<img src="https://github.com/user-attachments/assets/f4736ebf-70da-44f5-bd89-e1c747ef9f3f" width="100%">


### **2. 남은 수량 조회 성능 개선**

- **Redis 캐싱**을 사용하여 조회 성능을 향상시키고 캐시 히트율을 높임
- 재고 조회는 요청 빈도가 매우 높으므로 Look-Aside 방식으로 DB 부하 방지
- 데이터 불일치 문제를 방지하기 위해 Write-Through 방식으로 DB와 Redis를 동기화
- 캐시가 없는 경우에만 DB에서 조회 후 Redis에 저장, TTL 5분 설정하여 최신 재고 수량 유지
- **평균, 최소, 최대 응답시간 최대 82.45% 감소**

|  | 개선 전 | 개선 후 | 개선율 |
|---|---|---|---|
| **평균 응답 시간 (ms)** | 20.20 | 12.96 | **35.87%** |
| **최대 응답 시간 (ms)** | 487.75 | 85.62 | **82.45%** |
| **최소 응답 시간 (ms)** | 7.68 | 6.29 | **18.12%** |

- **TPS 43.26% 증가**

|  | **개선 전** | **개선 후** | **개선율** |
|---|---|---|---|
| **TPS (초당 트랜잭션)** | 24.28 | 34.79 | **43.26%** |

<img src="https://github.com/user-attachments/assets/610ec21f-3168-4c7f-8ee0-c8a4153be8bb" width="100%">


### **3. 대규모 트래픽 환경에서 동시 주문 시 데이터 일관성 문제 해결**

- 한정된 재고를 여러 사용자가 동시에 주문할 때 Race Condition 및 데이터 일관성 문제 발생
- 대규모 트래픽 환경에 적합한 **Redis 분산 락을** 사용하여 **Race Condition 100% 해결, 데이터 정합성과 동시성 문제를 완벽히 개선**
- TPS는 감소했으나, 한정판 구매에서는 정확성이 더 중요하다고 판단하여 Race Condition을 우선하여 개선
- **[1차 개선]**
  - 개별 상품에 **RLock을 적용하여 동일 상품 재고에 대한 동시 접근 차단**
  - 한계 : 하나의 주문에 여러 개의 상품이 포함되는 경우 **Race Condition 문제가 여전히 발생할 수 있고**, 여러 상품이 동시에 처리될 때 **DeadLock 발생 가능성 존재**
- **[2차 개선]**
  - 하나의 주문에 포함된 **전체 상품에 RMultiLock을 적용**하여 **한 번에 여러 개의 락을 획득하도록 변경, 동시 주문 환경에서도 재고 일관성을 100% 보장할 수 있게 됨**

|  | **개선 전** | **1차 개선 (RLock 적용)** | **2차 개선 (RMultiLock 적용)** |
| --- | --- | --- | --- |
| **Race Condition 발생률** | **15.5%** | **14.2%** | **0%** |

<img src="https://github.com/user-attachments/assets/f97f208f-32d2-4dae-ad0a-6bddfc86670b" width="50%">


### **4. 대규모 트래픽 환경에서 동시 주문 시 트랜잭션 안정성 개선**

- 트래픽이 집중되는 상황에서 예외 발생 시 롤백이 완벽히 수행되지 않는 문제 발생
- **Orchestration-Based SAGA 패턴**을 적용하여 분산 환경에서도 일관된 트랜잭션 처리,
  실패 시 **보상 트랜잭션** 수행하여 데이터 일관성 유지
- **개선 결과**
  - **평균 응답속도 18.1% 단축**
  - **TPS 17.8% 개선**
  - **데이터 일관성 100% 보장**

|  | **개선 전** | **개선 후** | 개선율 |
| --- | --- | --- | --- |
| **평균 응답 속도** | 8.05 sec | 6.59 sec | **18.1%** |
| **TPS** | 13.28 | 15.64 | **17.8%** |
| **트랜잭션 복구율** | 93.67% | 100% | **100% 보장** |


<img src="https://github.com/user-attachments/assets/e6c25a67-aeb1-42c0-8851-8b9e8575bde7" width="100%">

<img src="https://github.com/user-attachments/assets/ea341366-3666-48ef-86df-6224017e3310" width="50%">


<br>


## ⭐ **트러블 슈팅**

- **HS512 해시 알고리즘을 적용하여 비밀키 처리 문제 해결**
  - JWT의 길이를 HS512에서 요구하는 최소 바이트 수(64바이트)보다 짧게 설정하여, 토큰 생성 시 `WeakKeyException` 문제 발생
  - .env 파일에서 JWT를 Base64 인코딩된 문자열로 설정했지만, 길이가 충분하지 않아 `Keys.hmacShaKeyFor()` 호출 시 예외 발생
  - 비밀키 길이를 64바이트 이상의 문자열로 설정하고, .env에서 불러온 환경 변수 값을 검증하는 null이거나 너무 짧을 경우 예외를 발생시키는 로직을 추가하여 해결
- **재고 감소 시점을 주문 프로세스 진입 시점으로 설정하여 재고 불일치 문제 예방**
  - 기존에는 사용자가 결제를 완료할 때 재고를 차감하도록 구현하여, 상품 페이지에는 재고가 남아있다고 표시되지만 결제 단계에서 품절되는 문제가 발생해 사용자 경험 저하
  - 재고 감소 시점을 주문 프로세스 진입 시점으로 조정 → 주문 요청이 들어오면 즉시 재고를 차감하고 결제 실패 시 재고를 복구하는 방식으로 변경하여 재고 불일치 문제 해결
- **`socket hang up` 오류를 Postman Proxy 비활성화 및 Request Timeout 조정하여 해결**
  - API 요청을 Postman으로 테스트할 때 Error : socket hang up 오류 발생, 서버 로그에는 클라이언트 요청이 도착하지 않아 오류 로그가 없어 해결에 어려움을 겪음
  - Postman의 Proxy 설정을 끄고 Request Timeout을 0으로 설정하여 서버의 응답 시간을 확보하여 해결
- **`@EnableEurekaServer` 인식 불가 문제를 Spring Release 저장소 추가하여 해결**
  - Spring Cloud Netflix Eureka Server를 설정하기 위해 @EnableEurekaServer 애너테이션을 사용했으나 SpringBoot가 인식하지 못하는 문제 발생
  - build.gradle의 dependencyManagement 섹션에 Spring Cloud BOM을 추가하여 Eureka Server 관련 라이브러리가 올바른 버전으로 설정되도록 변경하여 해결
- **Quartz Job 관련 문제를 `SpringBeanJobFactory`, 동적 Quartz Job 생성하여 해결**
  - Quartz Job은 Spring에서 관리하지 않으므로 Service와 Repository를 주입받지 못하는 문제 발생

    →  `SpringBeanJobFactory`를 확장하여 Quartz Job에서도 `@Autowired`로 Spring 빈을 주입받을 수 있도록 변경

  - Quartz 정적 스케줄러를 활용하여 상태 변경을 시도했으나, 주문 상태가 정확히 24시간 후에 변경되지 않는 문제 발생

    → 동적 Quartz Job을 생성하여 정확히 24시간 후에 실행되도록 변경


<br>


## **⚖️ 기술적 의사결정**

- [**MSA 구조**를 사용하여 결합도가 낮고 확장성에 유리하도록 설계](https://aeongiii.tistory.com/94)
  - **Eureka**로 서비스 디스커버리를 구성하고 **API Gateway**를 활용해 하나의 포트만 공개
  - **FeignClient**를 사용하여 내부 서비스 간 통신 구현
- **JWT Token** 관리 시 보안성과 안정성을 고려
  - **Access Token**을 **HTTP-Only Cookie**에 저장하여 **XSS 공격 방지**
  - **Refresh Token**의 만료시간 7일로 설정
- **ERD 설계**에서 데이터 일관성 보장
  - 변경된 상품 정보를 주문 내역에 그대로 반영할 경우, 과거의 주문 기록과 불일치 가능성
  - product와 productSnapshot(상품 버전 스냅샷) 테이블을 분리하여 주문 당시의 상품 정보 보존
- **Docker**와 **Docker Compose**를 사용해 개발 및 배포 환경 일관성 유지
  - 환경 차이로 인한 오류 최소화, 일관된 환경에서 테스트 및 운영 가능