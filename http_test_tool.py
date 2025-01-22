import requests
from concurrent.futures import ThreadPoolExecutor
import time
import os

# 상수
BASE_URL = "http://host.docker.internal:8080"
NUM_USERS = 400  # 생성할 총 사용자 수
CONCURRENT_REQUESTS = 400  # 동시에 처리할 요청 개수
LOG_FILE_PATH = "/app/logs/test_log.txt"  # 컨테이너 내부 로그 경로

# 응답 시간 저장 리스트
create_order_times = []
stock_check_times = []
start_payment_times = []
end_payment_times = []
recent_order_times = []
payment_process_times = []

# 주문 ID 저장소
order_ids = {}
expected_stock = 10000  # 초기 재고 수량

# Race Condition 검증 변수
correct_stock_updates = 0
incorrect_stock_updates = 0

# 로그 파일 초기화
def init_log_file():
    try:
        with open(LOG_FILE_PATH, "w", encoding="utf-8") as log_file:
            log_file.write("[테스트 로그 시작]\n")
    except Exception as e:
        print(f"로그 파일 초기화 중 오류 발생: {e}")

# 로그 작성 함수
def log(message):
    try:
        with open(LOG_FILE_PATH, "a", encoding="utf-8") as log_file:
            log_file.write(message + "\n")
    except Exception as e:
        print(f"로그 작성 중 오류 발생: {e}")

# 사용자 데이터 생성
def generate_user_data(num_users):
    return [{"email": f"user{user_id}@test.com", "user_id": user_id} for user_id in range(1, num_users + 1)]

# HTTP 요청 전송
def send_request(api_url, method, data=None, params=None, headers=None, response_times_list=None):
    try:
        start_time = time.time()
        if method == "POST":
            response = requests.post(api_url, json=data if data else None, params=params, headers=headers)
        elif method == "PUT":
            response = requests.put(api_url, json=data if data else None, params=params, headers=headers)
        elif method == "GET":
            response = requests.get(api_url, params=params, headers=headers)
        else:
            raise ValueError("지원되지 않는 HTTP 메서드입니다.")
        end_time = time.time()

        # 응답 시간 기록
        if response_times_list is not None:
            response_times_list.append(end_time - start_time)

        if response.headers.get("Content-Type", "").startswith("application/json"):
            return response.status_code, response.json()
        return response.status_code, response.text
    except Exception as e:
        print(f"Error: {e}")
        return None, None

# 응답 시간 통계 출력
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

# 1️⃣ 주문 + 남은 수량 조회 API (Race Condition 체크 포함)
def test_create_order_and_check_stock(users):
    global expected_stock, correct_stock_updates, incorrect_stock_updates
    log("\n\n================= 주문 + 재고 수량 조회 요청 전송 중... =================")
    create_try, create_success, create_fail = 0, 0, 0
    stock_try, stock_success, stock_fail = 0, 0, 0
    product_id = 1  # 테스트할 상품 ID

    start_time = time.time()

    create_order_url = f"{BASE_URL}/order"
    get_stock_quantity_url = f"{BASE_URL}/product/1/quantity"

    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = {executor.submit(
            send_request, create_order_url, "POST",
            data=[{"productId": product_id, "quantity": 1}],
            headers={"X-User-Email": user["email"]},
            response_times_list=create_order_times
        ): user for user in users}

        for future in futures:
            create_try += 1
            status, response = future.result()
            if status == 200 and response:
                order_ids[futures[future]["email"]] = response["orderId"]
                create_success += 1
                log(f"✅ {futures[future]['email']} 주문 성공 - 주문 ID: {response['orderId']}")
            else:
                create_fail += 1
                log(f"❌ {futures[future]['email']} 주문 실패 - 응답 코드: {status}, 응답: {response}")

            stock_try += 1
            stock_status, stock_response = send_request(
                get_stock_quantity_url, "GET", response_times_list=stock_check_times
            )

            if stock_status == 200 and stock_response is not None:
                log(f"남은 수량: {stock_response}")
                # Race Condition 검증
                if stock_response == expected_stock - 1:
                    correct_stock_updates += 1
                else:
                    incorrect_stock_updates += 1
                    # log(f"[⚠️ Race Condition 발생] 예상 재고 {expected_stock - 1} vs 실제 재고 {stock_response}")
                stock_success += 1
                expected_stock -= 1
            else:
                stock_fail += 1

    print_stats(create_order_times, "1. 주문 API")
    print_stats(stock_check_times, "2. 재고 조회 API")

    # 성공 실패 횟수
    log(f"주문 요청: 시도={create_try}, 성공={create_success}, 실패={create_fail}")
    log(f"재고 수량 조회 요청: 시도={stock_try}, 성공={stock_success}, 실패={stock_fail}")

    # 최종 Race Condition 발생 여부 확인
    expected_final_stock = 10000 - create_success  # 예상 재고

    final_stock_status, final_stock_response = send_request( # 실제 재고
        get_stock_quantity_url.format(product_id=product_id),
        "GET",
        response_times_list=stock_check_times
    )

    if final_stock_status == 200 and final_stock_response is not None:
        log(f"\n[📌 최종 재고 확인] 예상 재고: {expected_final_stock}, 실제 재고: {final_stock_response}")

        if final_stock_response != expected_final_stock:
            incorrect_stock_updates += 1
            log(f"[⚠️ Race Condition 발생] 예상 재고 {expected_final_stock} vs 실제 재고 {final_stock_response}")

    # TPS
    total_time = time.time() - start_time
    tps = NUM_USERS / total_time if total_time > 0 else 0
    log(f"TPS: {tps:.2f}")

# 2️⃣ 결제 진입 API
def test_start_payment(users):
    log("\n\n================= 결제 진입 요청 전송 중... =================")
    start_payment_url = f"{BASE_URL}/payment/{{order_id}}"

    start_time = time.time()
    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = {executor.submit(
            send_request, start_payment_url.format(order_id=order_ids[user["email"]]), "POST",
            headers={"X-User-Email": user["email"]},
            response_times_list=start_payment_times
        ): user for user in users if user["email"] in order_ids}

        for future in futures:
            status, response = future.result()
            log(f"결제 진입 응답: {status}, {response}")

    print_stats(start_payment_times, "3. 결제 진입 API")

# 3️⃣ 결제 완료 API
def test_end_payment(users):
    log("\n\n================= 결제 완료 요청 전송 중... =================")
    end_payment_url = f"{BASE_URL}/payment/{{order_id}}"

    start_time = time.time()
    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = {executor.submit(
            send_request, end_payment_url.format(order_id=order_ids[user["email"]]), "PUT",
            headers={"X-User-Email": user["email"]},
            response_times_list=end_payment_times
        ): user for user in users if user["email"] in order_ids}

        for future in futures:
            status, response = future.result()
            log(f"결제 완료 응답: {status}, {response}")

    print_stats(end_payment_times, "4. 결제 완료 API")

# 4️⃣ 전체 결제 프로세스 API (Orchestrator)
def test_payment_process(users):
    log("\n\n================= 결제 프로세스 요청 전송 중... =================")
    process_payment_url = f"{BASE_URL}/payment/process"

    process_try, process_success, process_leave, process_fail = 0, 0, 0, 0
    start_time = time.time()

    with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = {executor.submit(
            send_request, process_payment_url, "POST",
            headers={"X-User-Email": user["email"]},
            params={"productId": 1, "quantity": 1},
            response_times_list=payment_process_times
        ): user for user in users}

        for future in futures:
            process_try += 1
            status, response = future.result()

            if status == 200:
                process_success += 1
            elif status == 400 and "결제 실패" in str(response):
                process_leave += 1  # 이탈로 분류
            else:
                process_fail += 1

            log(f"✅ 결제 프로세스 응답: {status}, {response}")
    end_time = time.time()

    print_stats(payment_process_times, "5. 결제 프로세스 API")

    # 요청 횟수 통계
    log(f"결제 프로세스 요청: 시도={process_try}, 성공={process_success}, 이탈={process_leave}, 실패={process_fail}")

    # TPS 계산
    total_time = end_time - start_time
    tps = NUM_USERS / total_time if total_time > 0 else 0
    log(f"TPS (Transactions Per Second): {tps:.2f}")

# 메인 실행
def main():
    init_log_file()
    users = generate_user_data(NUM_USERS)

    # test_create_order_and_check_stock(users) # 1️⃣ 주문 + 남은 수량 조회 API (Race Condition 체크 포함)
    # test_start_payment(users) # 2️⃣ 결제 진입 API
    # test_end_payment(users) # 3️⃣ 결제 완료 API
    test_payment_process(users) # 4️⃣ 결제 프로세스 API (Orchestrator)

    print("각 API 테스트 완료")


if __name__ == "__main__":
    main()
























# import requests  # HTTP 요청
# from concurrent.futures import ThreadPoolExecutor  # 동시 요청
# import time
# import os  # 파일 경로 설정
#
# # 상수
# BASE_URL = "http://host.docker.internal:8080"
# NUM_USERS = 1  # 생성할 총 사용자 수
# CONCURRENT_REQUESTS = 1  # 동시에 처리할 요청의 수
# LOG_FILE_PATH = "/app/logs/test_log.txt"  # 컨테이너 내부 경로
#
# # 응답시간 저장 리스트 (API별)
# create_order_times = []
# stock_check_times = []
# start_payment_times = []
# end_payment_times = []
# recent_order_times = []  # 최신 주문내역 5개 조회
# payment_process_times = []
#
# # 정확한 재고 감소 확인 통계
# correct_stock_updates = 0
# incorrect_stock_updates = 0
#
# # TPS 측정 변수
# start_time1, start_time2, start_time3, start_time4 = None, None, None, None
# end_time1, end_time2, end_time3, end_time4 = None, None, None, None
# recent_try, recent_success, recent_fail = 0, 0, 0
#
#
# # 로그 파일 초기화
# def init_log_file():
#     try:
#         with open(LOG_FILE_PATH, "w", encoding="utf-8") as log_file:
#             log_file.write("[테스트 로그 시작]\n")
#         print(f"로그 파일 생성: {LOG_FILE_PATH}")
#     except Exception as e:
#         print(f"로그 파일 초기화 중 오류 발생: {e}")
#         raise
#
# # 로그 작성 함수
# def log(message):
#     try:
#         with open(LOG_FILE_PATH, "a", encoding="utf-8") as log_file:
#             log_file.write(message + "\n")
#     except Exception as e:
#         print(f"로그 작성 중 오류 발생: {e}")
#
# # 1. 사용자 데이터 생성
# def generate_user_data(num_users):
#     return [{"email": f"user{user_id}@test.com", "user_id": user_id} for user_id in range(1, num_users + 1)]
#
# # 2. HTTP 요청 보내고 응답 결과를 출력
# def send_request(api_url, method, data=None, headers=None, response_times_list=None):
#     try:
#         start_time = time.time()
#         if method == "POST":
#             response = requests.post(api_url, json=data, headers=headers)
#         elif method == "PUT":
#             response = requests.put(api_url, json=data, headers=headers)
#         elif method == "GET":
#             response = requests.get(api_url, headers=headers)
#         else:
#             raise ValueError("지원되지 않는 HTTP 메서드입니다.")
#         end_time = time.time()
#
#         # 응답 시간 기록
#         if response_times_list is not None:
#             response_times_list.append(end_time - start_time)
#
#         # 응답 처리
#         if response.headers.get("Content-Type", "").startswith("application/json"):
#             return response.status_code, response.json()
#         return response.status_code, response.text
#     except Exception as e:
#         print(f"Error: {e}")
#         return None, None
#
# # 3. 평균, 최대, 최소값 출력
# def print_stats(response_times_list, api_name):
#     if response_times_list:
#         avg_time = sum(response_times_list) / len(response_times_list)
#         max_time = max(response_times_list)
#         min_time = min(response_times_list)
#         log(f"\n[{api_name}] 응답 시간 통계:")
#         log(f"  평균 응답 시간: {avg_time:.5f} seconds")
#         log(f"  최대 응답 시간: {max_time:.5f} seconds")
#         log(f"  최소 응답 시간: {min_time:.5f} seconds")
#     else:
#         log(f"\n[{api_name}] 데이터가 없습니다")
#
# # 4. 최신 주문내역 5개 조회 함수 추가
# def get_recent_orders(user_id):
#     global recent_try, recent_success, recent_fail
#
#     recent_try += 1
#     recent_orders_url = f"{BASE_URL}/order/top5"
#     headers = {"X-User-Email": f"user{user_id}@test.com"}
#
#     status, response = send_request(
#         recent_orders_url, "GET", headers=headers, response_times_list=recent_order_times
#     )
#
#     if status is None:
#         recent_fail += 1
#         log(f"[ERROR] 최신 주문내역 조회 실패 - 사용자 {user_id}, 응답 코드: {status}, 요청 URL: {recent_orders_url}")
#     elif status == 404:
#         recent_fail += 1
#         log(f"[ERROR] 최신 주문내역 조회 실패 - 사용자 {user_id}, 응답 코드: 404, 요청 URL: {recent_orders_url}")
#         log(f"  응답 내용: {response}")
#     elif status == 200:
#         recent_success += 1
#     else:
#         recent_fail += 1
#         log(f"[ERROR] 최신 주문내역 조회 실패 - 사용자 {user_id}, 응답 코드: {status}, 요청 URL: {recent_orders_url}")
#         log(f"  응답 내용: {response}")
#
#     return status, response
#
# # 5. 메인 함수
# def main():
#     global correct_stock_updates, incorrect_stock_updates
#     global start_time1, end_time1, start_time2, end_time2, start_time3, end_time3, start_time4, end_time4
#
#     # 로그 파일 초기화
#     init_log_file()
#
#     # 사용자 데이터
#     users = generate_user_data(NUM_USERS)
#
#     # 엔드포인트
#     create_order_url = f"{BASE_URL}/order"  # 주문 생성 API
#     start_payment_url = f"{BASE_URL}/payment/{{order_id}}"  # 결제 진입 API
#     end_payment_url = f"{BASE_URL}/payment/{{order_id}}"  # 결제 완료 API
#     get_stock_quantity_url = f"{BASE_URL}/product/{{product_id}}/quantity"  # 남은 수량 조회 API
#     payment_process_url = f"{BASE_URL}/payment/process" # 주문(결제) 프로세스 전체 API
#
#     # 주문 ID 저장
#     order_ids = {}
#     expected_stock = 10000 # 기존 수량
#
#
#
# # 4-1. 주문 API + 남은 수량 조회 API 동시 실행
#     log("\n\n\n================= 주문 및 재고 수량 조회 요청 전송 중... =================")
#
#     # 테스트 시작 시간 기록
#     start_time1 = time.time()
#
#     create_try, create_success, create_fail = 0, 0, 0
#     stock_try, stock_success, stock_fail = 0, 0, 0
#     product_id = 1  # 테스트할 상품 ID
#
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = []
#         for user_index, user in enumerate(users, start=1):
#             future = executor.submit(
#                 send_request,
#                 create_order_url,
#                 "POST",
#                 data=[{"productId": product_id, "quantity": 1}],
#                 headers={"X-User-Email": user["email"]},
#                 response_times_list=create_order_times
#             )
#             futures.append((user_index, user, future))
#
#         for user_index, user, future in futures:
#             create_try += 1
#             status, response = future.result()
#
#             # 숨김 상태 체크 및 로그 기록
#             if status == 400 and "숨김 처리되어 주문할 수 없습니다" in str(response):
#                 log(f"{user_index}번째 유저의 주문 상품은 숨김 처리된 상태입니다.")
#                 continue
#
#             if status == 200 and response:
#                 order_ids[user["email"]] = response["orderId"]
#                 create_success += 1
#                 log(f"{user_index}번째 유저가 주문했습니다.")
#             else:
#                 create_fail += 1
#
#             stock_try += 1
#             stock_status, stock_response = send_request(
#                 get_stock_quantity_url.format(product_id=product_id),
#                 "GET",
#                 response_times_list=stock_check_times
#             )
#             if stock_status == 200 and stock_response is not None:
#                 log(f"남은 수량: {stock_response}")
#
#                 # 정확한 재고 감소 확인
#                 if stock_response == expected_stock - 1:
#                     correct_stock_updates += 1
#                 else:
#                     incorrect_stock_updates += 1
#                     log(f"[Race Condition 발생] 예상 재고: {expected_stock - 1}, 실제 재고: {stock_response}")
#
#                 expected_stock -= 1
#                 stock_success += 1
#             else:
#                 stock_fail += 1
#
#     # 2️⃣ Race Condition 발생 여부 확인 (초기 재고 - 주문 요청 수 vs. 최종 재고)
#     expected_final_stock = 10000 - create_success  # 예상 재고
#
#     # 모든 주문이 끝난 후, 최종적으로 남은 재고 조회
#     final_stock_status, final_stock_response = send_request(
#         get_stock_quantity_url.format(product_id=product_id),
#         "GET",
#         response_times_list=stock_check_times  # 기존 리스트 활용
#     )
#
#     if final_stock_status == 200 and final_stock_response is not None:
#         log(f"\n[최종 재고 확인] 예상 재고: {expected_final_stock}, 실제 재고: {final_stock_response}")
#
#         if final_stock_response != expected_final_stock:
#             incorrect_stock_updates += 1
#             log(f"[Race Condition 발생] 예상 재고 {expected_final_stock} vs 실제 재고 {final_stock_response}")
#
#     # 테스트 종료 시간 기록
#     end_time1 = time.time()
#
#     # 통계 출력
#     print_stats(create_order_times, " 1. 주문하기 API")
#     print_stats(stock_check_times, "2. 수량 조회 API")
#     log(f"주문 요청: 시도={create_try}, 성공={create_success}, 실패={create_fail}")
#     log(f"재고 수량 조회 요청: 시도={stock_try}, 성공={stock_success}, 실패={stock_fail}")
#     log(f"\n정확한 재고 감소 확인: 성공={correct_stock_updates}, 실패={incorrect_stock_updates}")
#
#     # TPS 계산 및 출력
#     total_time = end_time1 - start_time1
#     tps = NUM_USERS / total_time if total_time > 0 else 0
#     log(f"\n총 요청 수: {NUM_USERS}")
#     log(f"테스트 소요 시간: {total_time:.2f} 초")
#     log(f"TPS (Transactions Per Second): {tps:.2f}")
#
#
#
#     # 4-2. 결제 진입 API
#     log("\n\n\n================= 결제 진입 요청 전송 중... =================")
#
#     # 테스트 시작 시간 기록
#     start_time2 = time.time()
#
#     start_try, start_success, start_leave, start_fail = 0, 0, 0, 0
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = []
#         for user in users:
#             if user["email"] in order_ids:
#                 future = executor.submit(
#                     send_request,
#                     start_payment_url.format(order_id=order_ids[user["email"]]),
#                     "POST",
#                     headers={"X-User-Email": user["email"]},
#                     response_times_list=start_payment_times
#                 )
#                 futures.append(future)
#
#         for future in futures:
#             start_try += 1
#             status, response = future.result()
#             if status == 200:
#                 start_success += 1
#             elif status == 400 and "사용자가 결제를 취소했습니다." in str(response):
#                 start_leave += 1  # 이탈로 분류
#             else:
#                 start_fail += 1
#
#     print_stats(start_payment_times, "3. 결제 진입 API")
#     log(f"결제 진입 요청: 시도={start_try}, 성공={start_success}, 이탈={start_leave}, 실패={start_fail}")
#
#     # 테스트 종료 시간 기록
#     end_time2 = time.time()
#
#     # TPS 계산 및 출력
#     total_time = end_time2 - start_time2
#     tps = NUM_USERS / total_time if total_time > 0 else 0
#     log(f"\n총 요청 수: {NUM_USERS}")
#     log(f"테스트 소요 시간: {total_time:.2f} 초")
#     log(f"TPS (Transactions Per Second): {tps:.2f}")
#
#
#
#     # 4-3. 결제 완료 API
#     log("\n\n\n================= 결제 완료 요청 전송 중... =================")
#
#     # 테스트 시작 시간 기록
#     start_time3 = time.time()
#
#     end_try, end_success, end_leave, end_fail = 0, 0, 0, 0
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = []
#         for user in users:
#             if user["email"] in order_ids:
#                 future = executor.submit(
#                     send_request,
#                     end_payment_url.format(order_id=order_ids[user["email"]]),
#                     "PUT",
#                     headers={"X-User-Email": user["email"]},
#                     response_times_list=end_payment_times
#                 )
#                 futures.append(future)
#
#         for future in futures:
#             end_try += 1
#             status, response = future.result()
#             if status == 200:
#                 end_success += 1
#             elif status == 400 and "결제에 실패했습니다." in str(response):
#                 end_leave += 1  # 이탈로 분류
#             else:
#                 end_fail += 1
#
#     print_stats(end_payment_times, "4. 결제 완료 API")
#     log(f"결제 완료 요청: 시도={end_try}, 성공={end_success}, 이탈={end_leave}, 실패={end_fail}")
#
#     # 테스트 종료 시간 기록
#     end_time3 = time.time()
#
#     # TPS 계산 및 출력
#     total_time = end_time3 - start_time3
#     tps = NUM_USERS / total_time if total_time > 0 else 0
#     log(f"\n총 요청 수: {NUM_USERS}")
#     log(f"테스트 소요 시간: {total_time:.2f} 초")
#     log(f"TPS (Transactions Per Second): {tps:.2f}")
#
#
#     # 4-4. 최신 주문내역 5개 조회 API 테스트
#     log("\n\n\n================= 최신 주문내역 5개 조회 요청 전송 중... =================")
#     start_time4 = time.time()
#
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = []
#         for user in users:
#             future = executor.submit(get_recent_orders, user["user_id"])
#             futures.append(future)
#
#         for future in futures:
#             future.result()  # 요청 실행
#
#     end_time4 = time.time()
#
#     # 응답 시간 통계 출력
#     print_stats(recent_order_times, "5. 최신 주문내역 조회 API")
#     log(f"최신 주문내역 조회 요청: 시도={recent_try}, 성공={recent_success}, 실패={recent_fail}")
#
#     # TPS 계산 및 출력
#     total_time4 = end_time4 - start_time4
#     tps4 = NUM_USERS / total_time4 if total_time4 > 0 else 0
#     log(f"\n총 요청 수 (최신 주문내역 조회): {NUM_USERS}")
#     log(f"테스트 소요 시간 (최신 주문내역 조회): {total_time4:.2f} 초")
#     log(f"TPS (Transactions Per Second, 최신 주문내역 조회): {tps4:.2f}")
#
#
#     # 4-5. 주문(결제) 프로세스 전체 API 테스트 (OrchestratorService)
#     log("\n\n\n================= 주문 프로세스 요청 전송 중... =================")
#
#     start_time5 = time.time()
#
#     process_try, process_success, process_leave, process_fail = 0, 0, 0, 0
#     with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
#         futures = []
#         for user in users:
#             if user["email"] in order_ids:
#                 future = executor.submit(
#                     send_request,
#                     payment_process_url.format(order_id=order_ids[user["email"]]),
#                     "POST",
#                     headers={"X-User-Email": user["email"]},
#                     response_times_list=payment_process_times
#                 )
#                 futures.append(future)
#
#         for future in futures:
#             process_try += 1
#             status, response = future.result()
#             if status == 200:
#                 process_success += 1
#             elif status == 400 and "결제 프로세스 중 문제가 발생했습니다." in str(response):
#                 process_leave += 1  # 이탈로 분류
#             else:
#                 process_fail += 1
#
#     print_stats(payment_process_times, "5. 주문 프로세스 API")
#     log(f"주문 프로세스 요청: 시도={process_try}, 성공={process_success}, 이탈={process_leave}, 실패={process_fail}")
#
#     # 테스트 종료 시간 기록
#     end_time5 = time.time()
#
#     # TPS 계산 및 출력
#     total_time = end_time5 - start_time5
#     tps = NUM_USERS / total_time if total_time > 0 else 0
#     log(f"\n총 요청 수: {NUM_USERS}")
#     log(f"테스트 소요 시간: {total_time:.2f} 초")
#     log(f"TPS (Transactions Per Second): {tps:.2f}")
#
# print("각 API 테스트 완료")
#
# # 메인 함수 실행
# if __name__ == "__main__":
#     main()






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
