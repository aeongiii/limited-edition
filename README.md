# 한정판 상품을 빠르고 정확하게 판매하는 E-Commerce 서비스
- 프로젝트 진행 기간 : 2024.12 ~ 2025.01 (4주)
<br>

## 📖 목차
1. ❓ [프로젝트 소개](#-프로젝트-소개)   
2. ✨ [특징](#-특징)   
3. ✅ [요구사항](#-요구사항)   
4. 📋 [개발 환경 및 기술 스택](#-개발-환경-및-기술-스택)   
5. 🔍 [아키텍처](#-아키텍처)  
6. 🚀 [성능 개선 & 트러블 슈팅](#-성능-개선--트러블-슈팅)  
7. ⚖️ [기술적 의사결정](#-기술적-의사결정)

<br>

## 🔗 LINK
- 🖥️ [설치 및 빌드 방법](https://muddy-roast-f94.notion.site/17d0370a1e8b802bae43e72101052a0a)   
  
- 📝 [API 명세서](https://documenter.getpostman.com/view/39288753/2sAYQakB2p)   
   
- ⚙️ [파일 구조도](https://muddy-roast-f94.notion.site/17e0370a1e8b8087b250e36074ba978c)   


<br>

## ❓ 프로젝트 소개
> 이 프로젝트는 회원가입, 이메일 인증, 위시리스트 관리, 주문, 결제, 반품 등 전자상거래에서 필요한 기능을 구현한  **E-Commerce 서비스**입니다.
> 한정 판매 상품은 정해진 시간에 구매를 열고, 일반 상품은 언제든 구매할 수 있도록 설계했습니다.
> 
> 특히, **정확성**을 최우선 목표로 삼아, 재고 수량을 초과하여 결제되지 않도록 제한하며, 남은 재고 수량을 정확히 보여주는 데 중점을 두었습니다.
>
> 대규모 트래픽 상황에서도 안정적으로 재고를 관리하기 위해 **MSA**와 **Redis 기반 캐싱**을 사용했습니다. 또한, 분산 환경에서 주문 프로세스가 일관성 있게 진행되도록 **Orchestration-Based Saga 패턴**을 사용했습니다.  

<br>

## ✨ 특징
- **Redis 캐싱**과 **분산 락**을 사용하여 한정판 구매 상황에서 남은 재고 수량을 정확히 표시
- **Orchestration-Based Saga패턴**을 적용하여 분산 환경에서 안정적인 트랜잭션 관리
- **API Gateway**와 **Eureka**를 사용하여 서비스 디스커버리 및 로드밸런싱을 구현하고, 단일 포트만 외부에 노출하여 보안성 강화
- **JWT 필터**를 직접 구현하여 Access Token을 HTTP-only Cookie에 저장하고, Refresh Token은 Redis에 저장하는 방식으로 인증 및 인가 처리
- **MSA** 구조로 확장성을 고려한 설계
- **Docker**와 **Docker Compose**를 사용해 Mysql, Redis, API Gateway, Eureka 등을 컨테이너화하여 일관된 개발·배포 환경 구축


<br>

## ✅ 요구사항
### 1. 사용자 관리

- 구글 SMTP 이메일 인증, 회원가입, 로그인/로그아웃
- 인증 및 인가
- 비밀번호 유효성 검사 (영문, 숫자, 특수문자 조합 8자리 이상)
- 개인정보 암호화하여 저장
- 위시리스트(찜) 추가, 수정, 삭제

### 2. 상품 관리

- 일반 상품과 한정판 상품 구분
- 한정판 상품의 경우 특정 시간에만 구매 가능
- 상품 목록 조회, 상품 상세 정보 조회 기능
- 남은 재고량 정확하게 반영하여 사용자에게 표시
- 상품의 정보가 변경될 수 있으므로 스냅샷 저장

### 3. 주문 관리

- **Redis 분산 락**을 적용하여 주문 로직에 **동시성 처리**
- **Saga 패턴**을 적용하여 주문 및 결제 트랜잭션 관리
- 결제 과정 중 고객 이탈 시나리오 반영 (결제 취소, 한도 초과 등 예외 처리)
- 배송 관리, 주문 취소, 반품 요청 등 주문 상태 관리

### 4. 대규모 트래픽 대응

- **Redis 캐싱**을 적용하여 실시간 재고 관리
- **API Gateway**를 통한 대규모 트래픽 분산
- **Spring Cloud Eureka**를 이용하여 Client-side service discovery 구현
- **SAGA 패턴**을 적용하여 분산 환경에서 트랜잭션 일관성 보장
- **Kafka** 기반 비동기 이벤트 처리

### 5. 테스트 및 운영

- Docker 기반 개발 환경 구성
- 파이썬 자동화 테스트 툴을 구축하여 대규모 트래픽 시뮬레이션 테스트

<br>

## 📋 개발 환경 및 기술 스택
### 1. 백엔드

- Java 21, Spring Boot 3.4.0, Gradle
- Spring Boot Starter Web, JPA, Security
- Spring Security, JWT
- Quartz Job Scheduler

### 2. **배포 및 운영 환경**

- Docker / Docker Compose
- Eureka, Spring Cloud Gateway
- Spring Cloud OpenFeign
- Git, Github

### 3. **데이터 처리 및 캐싱**

- MySQL 8.0, Redis, Redisson, Kafka
- MySQL Connector

### 4. **API 개발 및 테스트**

- HTTP Request / Response
- Postman
- Python
 
<br>

## 🔍 아키텍처   
- [이미지 추가]  

<br>

## 🔍 ERD
- [임시 ERD](https://www.notion.so/2-1600370a1e8b81b99f13d52498f0b0a5?pvs=4#1690370a1e8b8083ad7def38927ebede)


<br>

## 🚀 성능 개선 & 트러블 슈팅
### 1. 주문 내역 조회 속도 개선

- **복합 인덱스**(`user_id`, `created_at`)를 추가하여 특정 유저의 최신 주문 5개 조회 시 응답 속도 단축
- **개선 결과** (10000건 주문 데이터 기준)
    - 개선 전 : 평균 조회 시간 0ms
    - 개선 후 : 평균 조회 시간 0ms로 약 0% 속도 개선

### 2. 남은 수량 조회 속도 개선

- Redis 캐시 전략 중 **Look-Aside & Write Through** 방식을 사용하여 조회 성능을 향상시키고 캐시 히트율을 높임
- 캐시가 없는 경우에만 DB에서 조회 후 Redis에 저장, **TTL(Time-To-Live)** 5초 설정하여 최신 재고 수량 유지
- **개선 결과** (10000건 조회 요청 기준)
    - 개선 전 : 평균 조회 시간 0ms (DB 조회)
    - 개선 후 : 평균 조회 시간 0ms로 약 0% 속도 향상
    - DB 부하 감소율 0%, 평균 DB 요청 횟수 10000건 → 0건으로 감소

### 3. 대규모 트래픽 환경에서 동시 주문 처리 성능 개선

- 한정된 재고를 여러 사용자가 동시에 주문할 때 데이터 불일치 방지 및 성능 최적화
- **Redis 분산 락**을 적용하여 **상품별 Lock Key 설정**, **Fair Lock** 설정하여 FIFO 순으로 주문 처리
- **개선 결과** (동시 주문 10000건 처리 시)
    - 개선 전 : 평균 응답 시간 0ms, 충돌 발생률 0%
    - 개선 후 : 평균 응답 시간 0ms, 충돌 발생률 0% 감소

### 4. 트랜잭션 안정성

- **Orchestration-Based SAGA 패턴**을 적용하여 분산 환경에서도 일관된 트랜잭션 처리
- 실패 시 **보상 트랜잭션** 수행하여 데이터 일관성 유지
- **개선 결과**
    - Before : 데이터 불일치 발생률 0%
    - After : 데이터 불일치 발생률 0%로 감소

### 5. 트러블 슈팅 
- 데이터 보안을 위해 **HS512 해시 알고리즘** 사용, 개인정보 암호화하여 저장
- 로그아웃 시 Access Token을 만료 처리, Refresh Token 삭제하여 인증 정보 완전히 제거
- 재고 감소 시점을 주문 프로세스 진입 시점으로 설정하여 **재고 불일치 문제 예방**
- JWT, AES 키 길이를 32byte(256bit)로 설정하여 `WeakKeyException` 문제 해결
- `socket hang up` 오류를 Postman Proxy 비활성화 및 Request Timeout 조정하여 해결
- `@EnableEurekaServer` 인식 불가 문제를 Spring Release 저장소 추가하여 해결
- Quartz Job 관련 문제를 `SpringBeanJobFactory`, 동적 Quartz Job 생성하여 해결

<br>

## ⚖️ 기술적 의사결정
- **MSA 구조**를 사용하여 결합도가 낮고 확장성에 유리하도록 설계
    - **Eureka**로 서비스 디스커버리를 구성하고 **API Gateway**를 활용해 하나의 포트만 공개했습니다.
    - **FeignClient**를 사용하여 내부 서비스 간 통신을 구현했습니다.
- **JWT Token** 관리 시 보안성과 안정성을 고려
    - **Access Token**을 **HTTP-Only Cookie**에 저장하여 XSS 공격을 방지하였습니다.
    - **Refresh Token**을 **Redis**에 저장하여 비공개 처리하고, 세션 유지 및 빠른 검증이 가능하도록 구현했습니다.
- **ERD 설계**에서 데이터 일관성 보장
    - 변경된 상품 정보를 주문 내역에 그대로 반영하면, 과거의 주문 기록과 불일치가 발생할 수 있습니다.
    - product(상품)와 productSnapshot(상품 버전 스냅샷) 테이블을 분리하여 주문 당시의 상품 정보를 보존했습니다.
- **Docker**와 **Docker Compose**를 사용해 개발 및 배포 환경의 일관성을 유지했습니다.
    - 환경 차이로 인한 오류를 최소화하고, 일관된 환경에서 테스트 및 운영이 가능하도록 했습니다.
   
