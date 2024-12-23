package com.sparta.limited_edition.util;

import com.sparta.limited_edition.entity.OrderDetail;
import com.sparta.limited_edition.entity.Orders;
import com.sparta.limited_edition.entity.Product;
import com.sparta.limited_edition.repository.OrderDetailRepository;
import com.sparta.limited_edition.repository.OrderRepository;
import com.sparta.limited_edition.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReturnCompletionJob implements Job {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    // 반품 시 : [반품 신청] -> 1일 뒤 [반품 완료]로 자동 변경하는 Job
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // JobExecutionContext에서 orderId 파라미터 추출
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        System.out.println("Quartz Job 실행 시작 - 주문 ID: " + orderId + ", 실행 시각: " + LocalDateTime.now());
        try {
            // 주문 정보 가져오기
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));
            // "반품 완료"로 상태 변경
            order.setStatus("반품 완료");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            // 재고 복구
            List<OrderDetail> orderDetails = orderDetailRepository.findAllByOrdersId(orderId);
            for (OrderDetail detail : orderDetails) {
                Product product = detail.getProductSnapshot().getProduct();
                product.setStockQuantity(product.getStockQuantity() + detail.getQuantity());
                productRepository.save(product);
            }
            System.out.println("Quartz Job 실행 완료 - 반품 처리 완료: 주문 ID: " + orderId);
        } catch (Exception e) {
            // 오류 로그 추가
            System.err.println("Quartz Job 실행 중 오류 발생 - 주문 ID: " + orderId + ", 오류 메시지: " + e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
