package com.sparta.limited_edition.util;

import com.sparta.limited_edition.entity.OrderDetail;
import com.sparta.limited_edition.entity.Orders;
import com.sparta.limited_edition.repository.OrderDetailRepository;
import com.sparta.limited_edition.repository.OrderRepository;
import com.sparta.limited_edition.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ReturnCompletionJob implements Job {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional // 데이터 변경 작업이니까 얘도 트랜잭션 필요
    public void execute (JobExecutionContext context) throws JobExecutionException {
        performReturnCompletion();
    }
    private void performReturnCompletion() {
        // 1. "반품 신청" 상태인 모든 주문 가져오기
        List<Orders> returnPendingOrders = orderRepository.findAllByStatus("반품 신청");
        // 2. 주문 하나씩 확인하고 24시간 지났으면 "반품 완료"로 변경
        for (Orders order : returnPendingOrders) {
            // 3. 반품 신청한지 24시간 지났는지 확인
            if (order.getUpdatedAt().plusDays(1).isBefore(LocalDateTime.now())) {
                // 4. 상태 변경
                order.setStatus("반품 완료");
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                // 5. 재고 복구
                List<OrderDetail> orderDetailList = orderDetailRepository.findAllByOrdersId(order.getId());
                for (OrderDetail orderDetail : orderDetailList) {
                    orderDetail.getProductSnapshot().getProduct().setStockQuantity(
                            orderDetail.getProductSnapshot().getProduct().getStockQuantity() + orderDetail.getQuantity()
                    );
                    productRepository.save(orderDetail.getProductSnapshot().getProduct());
                    System.out.println("반품이 완료되었습니다. orderId : " + order.getId());
                }

            }
        }
    }
}
