package com.sparta.orderservice;

import com.sparta.common.dto.OrderRequest;
import com.sparta.common.dto.OrderResponse;
import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.UserResponse;
import com.sparta.orderservice.client.ProductServiceClient;
import com.sparta.orderservice.client.UserServiceClient;
import com.sparta.orderservice.client.WishlistServiceClient;
import com.sparta.orderservice.repository.OrderRepository;
import com.sparta.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@ActiveProfiles("h2")
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @MockitoBean
    private WishlistServiceClient wishlistServiceClient;

    @BeforeEach
    void setup() {
        when(userServiceClient.getUserEmail("ljhkys6874@naver.com"))
                .thenReturn(new UserResponse(1L, "ljhkys6874@naver.com", "password123!", "name", "address", LocalDateTime.now()));
        when(productServiceClient.getProductById(1L))
                .thenReturn(new ProductResponse(1L, "상품 A", "설명", 10000, 10000, true, "url", "limited", LocalDateTime.now(), LocalDateTime.now()));

        when(productServiceClient.getProductById(2L))
                .thenReturn(new ProductResponse(2L, "상품 B", "설명", 10000, 10000, true, "url", "limited", LocalDateTime.now(), LocalDateTime.now()));
    }

    @Test
    void createOrder_Success() {
        // Given
        String email = "ljhkys6874@naver.com";
        List<OrderRequest> orderRequests = List.of(
                new OrderRequest(1L, 2),
                new OrderRequest(2L, 1)
        );

        // When
        OrderResponse orderResponse = orderService.createOrder(email, orderRequests);

        // Then
        assertNotNull(orderResponse);
        assertEquals("주문완료", orderResponse.getStatus());
        assertEquals(2, orderResponse.getOrderItems().size());

        verify(userServiceClient).getUserEmail(email);
        verify(productServiceClient).getProductById(1L);
        verify(productServiceClient).getProductById(2L);
        verify(productServiceClient).updateProductStock(eq(1L), eq(98));
        verify(productServiceClient).updateProductStock(eq(2L), eq(49));

    }


}
