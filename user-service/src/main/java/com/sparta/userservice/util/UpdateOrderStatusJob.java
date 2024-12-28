package com.sparta.userservice.util;

import com.sparta.userservice.entity.Orders;
import com.sparta.userservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
// 주문 시 : [주문 완료] -> 1일 뒤 [배송중] -> 2일 뒤 [배송 완료]로 주문상태 자동 변경하는 Job
public class UpdateOrderStatusJob implements Job {
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        String newStatus = context.getJobDetail().getJobDataMap().getString("newStatus");
        System.out.println("Quartz Job 실행 시작 - 주문 ID: " + orderId + ", 실행 시각: " + LocalDateTime.now());
        try {
            // 주문정보 가져오기
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다"));
            // 주문상태 업데이트 + 수정날짜 업데이트 해서 다시 저장
            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            System.out.println("Quartz Job 실행 완료 - 주문 ID: " + orderId + ", 실행 시각: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Quartz Job 실행 중 오류 발생 - 주문 ID: " + orderId + ", 오류 메시지: " + e.getMessage());
            throw new JobExecutionException(e);
        }
    }
}
