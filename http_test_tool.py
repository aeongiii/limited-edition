import requests  # HTTP 요청
from concurrent.futures import ThreadPoolExecutor  # 동시 요청
import time
import os  # 파일 경로 설정

# 상수
BASE_URL = "http://host.docker.internal:8080"
NUM_USERS = 10  # 생성할 총 사용자 수
CONCURRENT_REQUESTS = 1  # 동시에 처리할 요청의 수
LOG_FILE_PATH = "/app/logs/test_log.txt"  # 컨테이너 내부 경로

# 응답시간 저장 리스트 (API별)
create_order_times = []
stock_check_times = []
start_payment_times = []
end_payment_times = []

# 정확한 재고 감소 확인 통계
correct_stock_updates = 0
incorrect_stock_updates = 0

# TPS 측정 변수
start_time1, start_time2, start_time3= None, None, None
end_time1, end_time2, end_time3 = None, None, None

# 로그 파일 초기화
def init_log_file():
    try:
        with open(LOG_FILE_PATH, "w", encoding="utf-8") as log_file:
            log_file.write("[테스트 로그 시작]\n")
        print(f"로그 파일 생성: {LOG_FILE_PATH}")
    except Exception as e:
        print(f"로그 파일 초기화 중 오류 발생: {e}")
        raise

# 로그 작성 함수
def log(message):
    try:
        with open(LOG_FILE_PATH, "a", encoding="utf-8") as log_file:
            log_file.write(message + "\n")
    except Exception as e:
        print(f"로그 작성 중 오류 발생: {e}")

# 1. 사용자 데이터 생성
def generate_user_data(num_users):
    return [{"email": f"user{user_id}@test.com", "user_id": user_id} for user_id in range(1, num_users + 1)]

# 2. HTTP 요청 보내고 응답 결과를 출력
def send_request(api_url, method, data=None, headers=None, response_times_list=None):
    try:
        start_time = time.time()
        if method == "POST":
            response = requests.post(api_url, json=data, headers=headers)
        elif method == "PUT":
            response = requests.put(api_url, json=data, headers=headers)
        elif method == "GET":
            response = requests.get(api_url, headers=headers)
        else:
            raise ValueError("지원되지 않는 HTTP 메서드입니다.")
        end_time = time.time()

        # 응답 시간 기록
        if response_times_list is not None:
            response_times_list.append(end_time - start_time)

        # 응답 처리
        if response.headers.get("Content-Type", "").startswith("application/json"):
            return response.status_code, response.json()
        return response.status_code, response.text
    except Exception as e:
        print(f"Error: {e}")
        return None, None

# 3. 평균, 최대, 최소값 출력
def print_stats(response_times_list, api_name):
    if response_times_list:
        avg_time = sum(response_times_list) / len(response_times_list)
        max_time = max(response_times_list)
        min_time = min(response_times_list)
        log(f"\n[{api_name}] 응답 시간 통계:")
        log(f"  평균 응답 시간: {avg_time:.5f} seconds")
        log(f"  최대 응답 시간: {max_time:.5f} seconds")
        log(f"  최소 응답 시간: {min_time:.5f} seconds")
    else:
        log(f"\n[{api_name}] 데이터가 없습니다")

# 4. 메인 함수
def main():
    global correct_stock_updates, incorrect_stock_updates, start_time1, end_time1, start_time2, end_time2, start_time3, end_time3

    # 로그 파일 초기화
    init_log_file()

    # 사용자 데이터
    users = generate_user_data(NUM_USERS)

    # 엔드포인트
    create_order_url = f"{BASE_URL}/order"  # 주문 생성 API
    start_payment_url = f"{BASE_URL}/payment/{{order_id}}"  # 결제 진입 API
    end_payment_url = f"{BASE_URL}/payment/{{order_id}}"  # 결제 완료 API
    get_stock_quantity_url = f"{BASE_URL}/product/{{product_id}}/quantity"  # 남은 수량 조회 API

    # 주문 ID 저장
    order_ids = {}
    expected_stock = 10000 # 기존 수량

    # 테스트 시작 시간 기록
    start_time1 = time.time()

# 4-1. 주문 API + 남은 수량 조회 API 동시 실행
    log("\n\n\n================= 주문 및 재고 수량 조회 요청 전송 중... =================")
    create_try, create_success, create_fail = 0, 0, 0
    stock_try, stock_success, stock_fail = 0, 0, 0
    product_id = 1  # 테스트할 상품 ID

    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = []
        for user_index, user in enumerate(users, start=1):
            future = executor.submit(
                send_request,
                create_order_url,
                "POST",
                data=[{"productId": product_id, "quantity": 1}],
                headers={"X-User-Email": user["email"]},
                response_times_list=create_order_times
            )
            futures.append((user_index, user, future))

        for user_index, user, future in futures:
            create_try += 1
            status, response = future.result()

            # 숨김 상태 체크 및 로그 기록
            if status == 400 and "숨김 처리되어 주문할 수 없습니다" in str(response):
                log(f"{user_index}번째 유저의 주문 상품은 숨김 처리된 상태입니다.")
                continue

            if status == 200 and response:
                order_ids[user["email"]] = response["orderId"]
                create_success += 1
                log(f"{user_index}번째 유저가 주문했습니다.")
            else:
                create_fail += 1

            stock_try += 1
            stock_status, stock_response = send_request(
                get_stock_quantity_url.format(product_id=product_id),
                "GET",
                response_times_list=stock_check_times
            )
            if stock_status == 200 and stock_response is not None:
                log(f"남은 수량: {stock_response}")

                # 정확한 재고 감소 확인
                if stock_response == expected_stock - 1:
                    correct_stock_updates += 1
                else:
                    incorrect_stock_updates += 1

                expected_stock -= 1
                stock_success += 1
            else:
                stock_fail += 1

    # 테스트 종료 시간 기록
    end_time1 = time.time()

    # 통계 출력
    print_stats(create_order_times, " 1. 주문하기 API")
    print_stats(stock_check_times, "2. 수량 조회 API")
    log(f"주문 요청: 시도={create_try}, 성공={create_success}, 실패={create_fail}")
    log(f"재고 수량 조회 요청: 시도={stock_try}, 성공={stock_success}, 실패={stock_fail}")
    log(f"\n정확한 재고 감소 확인: 성공={correct_stock_updates}, 실패={incorrect_stock_updates}")

    # TPS 계산 및 출력
    total_time = end_time1 - start_time1
    tps = NUM_USERS / total_time if total_time > 0 else 0
    log(f"\n총 요청 수: {NUM_USERS}")
    log(f"테스트 소요 시간: {total_time:.2f} 초")
    log(f"TPS (Transactions Per Second): {tps:.2f}")

    # 테스트 시작 시간 기록
    start_time2 = time.time()

    # 4-2. 결제 진입 API
    log("\n\n\n================= 결제 진입 요청 전송 중... =================")
    start_try, start_success, start_leave, start_fail = 0, 0, 0, 0
    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = []
        for user in users:
            if user["email"] in order_ids:
                future = executor.submit(
                    send_request,
                    start_payment_url.format(order_id=order_ids[user["email"]]),
                    "POST",
                    headers={"X-User-Email": user["email"]},
                    response_times_list=start_payment_times
                )
                futures.append(future)

        for future in futures:
            start_try += 1
            status, response = future.result()
            if status == 200:
                start_success += 1
            elif status == 400 and "사용자가 결제를 취소했습니다." in str(response):
                start_leave += 1  # 이탈로 분류
            else:
                start_fail += 1

    print_stats(start_payment_times, "3. 결제 진입 API")
    log(f"결제 진입 요청: 시도={start_try}, 성공={start_success}, 이탈={start_leave}, 실패={start_fail}")

    # 테스트 종료 시간 기록
    end_time2 = time.time()

    # TPS 계산 및 출력
    total_time = end_time2 - start_time2
    tps = NUM_USERS / total_time if total_time > 0 else 0
    log(f"\n총 요청 수: {NUM_USERS}")
    log(f"테스트 소요 시간: {total_time:.2f} 초")
    log(f"TPS (Transactions Per Second): {tps:.2f}")

    # 테스트 시작 시간 기록
    start_time3 = time.time()

    # 4-3. 결제 완료 API
    log("\n\n\n================= 결제 완료 요청 전송 중... =================")
    end_try, end_success, end_leave, end_fail = 0, 0, 0, 0
    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = []
        for user in users:
            if user["email"] in order_ids:
                future = executor.submit(
                    send_request,
                    end_payment_url.format(order_id=order_ids[user["email"]]),
                    "PUT",
                    headers={"X-User-Email": user["email"]},
                    response_times_list=end_payment_times
                )
                futures.append(future)

        for future in futures:
            end_try += 1
            status, response = future.result()
            if status == 200:
                end_success += 1
            elif status == 400 and "결제에 실패했습니다." in str(response):
                end_leave += 1  # 이탈로 분류
            else:
                end_fail += 1

    print_stats(end_payment_times, "4. 결제 완료 API")
    log(f"결제 완료 요청: 시도={end_try}, 성공={end_success}, 이탈={end_leave}, 실패={end_fail}")

    # 테스트 종료 시간 기록
    end_time3 = time.time()

    # TPS 계산 및 출력
    total_time = end_time3 - start_time3
    tps = NUM_USERS / total_time if total_time > 0 else 0
    log(f"\n총 요청 수: {NUM_USERS}")
    log(f"테스트 소요 시간: {total_time:.2f} 초")
    log(f"TPS (Transactions Per Second): {tps:.2f}")

    print("각 API 테스트 완료")

# 메인 함수 실행
if __name__ == "__main__":
    main()






# # ======================== 10000명 회원가입 하기 ============================
#
# # 1. 사용자 데이터 생성
# def generate_user_data(num_users):
#     return [
#         {
#             "email": f"user{user_id}@test.com",
#             "password": "password123!",
#             "name": "name",
#             "address": "address",
#             "authNumber": "asdf"  # 임시 인증번호
#         }
#         for user_id in range(1, num_users + 1)
#     ]
#
# # 2. HTTP 요청 보내고 응답 결과를 출력
# def send_request(api_url, data):
#     try:
#         response = requests.post(api_url, json=data, headers={"Content-Type": "application/json"})
#         if response.status_code == 200:
#             print(f"회원가입 성공: {data['email']}")
#             return True
#         else:
#             print(f"회원가입 실패: {data['email']} - {response.status_code} {response.text}")
#             return False
#     except Exception as e:
#         print(f"회원가입 요청 중 에러 발생: {e}")
#         return False
#
# # 3. 메인 함수에서 회원가입 요청 관리
# def main():
#     # 사용자 데이터를 준비
#     users = generate_user_data(NUM_USERS)
#     signup_url = f"{BASE_URL}/users/signup"  # 회원가입 API URL
#
#     # 회원가입 요청
#     print("회원가입 요청 전송 중...")
#     success_count = 0
#     fail_count = 0
#
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = [executor.submit(send_request, signup_url, user) for user in users]
#         for future in futures:
#             if future.result():
#                 success_count += 1
#             else:
#                 fail_count += 1
#
#     print(f"회원가입 성공: {success_count}, 실패: {fail_count}")
#
# # 메인 함수 실행
# if __name__ == "__main__":
#     main()
