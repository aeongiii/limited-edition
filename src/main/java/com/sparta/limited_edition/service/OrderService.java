package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.OrderItemResponse;
import com.sparta.limited_edition.dto.OrderRequest;
import com.sparta.limited_edition.dto.OrderResponse;
import com.sparta.limited_edition.entity.*;
import com.sparta.limited_edition.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductSnapshotRepository productSnapshotRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;

    public OrderService(ProductRepository productRepository, ProductSnapshotRepository productSnapshotRepository, OrderRepository orderRepository, OrderDetailRepository orderDetailRepository, UserRepository userRepository, WishlistRepository wishlistRepository) {
        this.productRepository = productRepository;
        this.productSnapshotRepository = productSnapshotRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
    }

    // 주문하기
    @Transactional
    public OrderResponse createOrder(String email, List<OrderRequest> orderItems) {
        // 사용자 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));

        // order 객체 생성
        Orders order = new Orders(user, "주문 완료", 0); // 총 금액 초기화
        orderRepository.save(order);

        // OrderDetail 객체 생성
        List<OrderDetail> orderDetails = new ArrayList<>();
        int totalAmount = 0;

        // 상품 하나하나 처리
        for (OrderRequest item : orderItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            // 4. 상품 재고 확인
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("재고가 부족합니다. (남은 재고: " + product.getStockQuantity() + ")");
            }

            // 5. 스냅샷 생성/재사용
            ProductSnapshot snapshot = productSnapshotRepository.findByProduct_Id(product.getId())
                    .orElseGet(() -> productSnapshotRepository.save(new ProductSnapshot(product)));

            // 6. 상품 재고 감소
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            // 7. 상품별 주문 금액 계산 및 저장
            int subtotalAmount = item.getQuantity() * product.getPrice();
            OrderDetail orderDetail = new OrderDetail(order, snapshot, item.getQuantity(), subtotalAmount);
            orderDetails.add(orderDetail);
            totalAmount += subtotalAmount; // 전체 금액 누적
        }

        // 8. 주문 정보 업데이트
        order.setTotalAmount(totalAmount);
        orderDetailRepository.saveAll(orderDetails);

        // 9. 위시리스트에서 삭제
        wishlistRepository.deleteAllByUserId(user.getId());

        // 10. 주문 상세 응답 생성
        List<OrderItemResponse> orderItemResponses = orderDetails.stream()
                .map(detail -> new OrderItemResponse(
                        detail.getProductSnapshot().getProductId(),
                        detail.getProductSnapshot().getName(),
                        detail.getQuantity(),
                        detail.getProductSnapshot().getPrice(),
                        detail.getSubtotalAmount()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(order.getId(), totalAmount, orderItemResponses);
    }
}
