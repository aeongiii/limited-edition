package com.sparta.wishlistservice.service;

import com.sparta.common.dto.ProductResponse;
import com.sparta.common.dto.UserResponse;
import com.sparta.wishlistservice.client.ProductServiceClient;
import com.sparta.wishlistservice.client.UserServiceClient;
import com.sparta.wishlistservice.dto.WishlistResponse;
import com.sparta.wishlistservice.entity.Wishlist;
import com.sparta.wishlistservice.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    public WishlistService(WishlistRepository wishlistRepository, UserServiceClient userServiceClient, ProductServiceClient productServiceClient) {
        this.wishlistRepository = wishlistRepository;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
    }

    // 1. 위시리스트에 상품 추가
    public WishlistResponse addToWishlist(String email, long productId, int quantity) {
        UserResponse user = validateUser(email); // 사용자 검증
        ProductResponse product = validateProduct(productId); // 상품 검증
        System.out.println("===========================================");
        System.out.println("상품의 노출 여부 값 : " + product.isVisible());
        // 상품 노출 false
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
        // 품절된 경우
        if (product.getStockQuantity() == 0) {
            throw new IllegalArgumentException("해당 상품은 품절되었습니다.");
        }
        // 위시리스트에 추가 (또는 수량 업데이트)
        Wishlist wishlist = createOrUpdateWishlist(user, product, quantity);
        return createWishlistResponse(product, quantity);


    }

    // 2. 위시리스트 목록 조회
    public List<WishlistResponse> getWishlist(String email) {
        Long userId = validateUser(email).getId(); // userId 가져오기
        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId); // 목록 조회

        // 위시리스트를 ResponseDto로 변환
        return wishlists.stream()
                .map(item ->  {
                    ProductResponse product = productServiceClient.getProductById(item.getProductId());
                    return createWishlistResponse(product, item.getQuantity());
                })
                .collect(Collectors.toList());
    }



    // 3. 위시리스트 상품 수량 변경
    public WishlistResponse updateWishlistQuantity (String email, Long productId, int quantity) {
        UserResponse user = validateUser(email); // 사용자 검증
        ProductResponse product = validateProduct(productId); // 상품 검증
        Wishlist wishlist = validateWishlist(user, product); // 위시리스트에서 상품 찾기

        // 수량 업데이트
        wishlist.setQuantity(quantity);
        wishlistRepository.save(wishlist);

        // 반환할 wishlistResponse 만들기
        return createWishlistResponse(product, quantity);
    }

    // 4. 위시리스트에서 상품 삭제
    public void removeWishlistItem(String email, Long productId) {
        UserResponse user = validateUser(email); // 사용자 검증
        ProductResponse product = validateProduct(productId); // 상품 검증
        Wishlist wishlist = validateWishlist(user, product); // 위시리스트에서 상품 찾기
        // 삭제
        wishlistRepository.delete(wishlist);
    }


    // =========================

    // 사용자 검증
    private UserResponse validateUser(String email) {
        UserResponse user = userServiceClient.getUserByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }
        return user;
    }

    // id로 상품 검증
    private ProductResponse validateProduct(Long productId) {
        ProductResponse product = productServiceClient.getProductById(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다.");
        }
        System.out.println("validateProduct 응답 확인: " + product);
        return product;
    }

    // 위시리스트에 추가, 또는 수량 업데이트
    private Wishlist createOrUpdateWishlist(UserResponse user, ProductResponse product, int quantity) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(() -> new Wishlist(user.getId(), product.getId(), 0)); // 일단 기본수량 0으로 세팅
        wishlist.setQuantity(wishlist.getQuantity() + quantity); // 기존수량 + 새로 받은 수량 합치기
        wishlistRepository.save(wishlist);
        return wishlist;
    }

    // user, product로 위시리스트 찾기
    private Wishlist validateWishlist(UserResponse user, ProductResponse product) {
        return wishlistRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("위시리스트에 해당 상품이 없습니다."));
    }

    // 반환할 wishlistResponse 만들기
    private WishlistResponse createWishlistResponse(ProductResponse product, int quantity) {
        return new WishlistResponse(
                product.getId(),
                product.getName(),
                quantity,
                product.getPrice(),
                product.getImageUrl(),
                "http://localhost:8080/product/" + product.getId() // 상세정보 조회 url 만들어서 같이 반환
        );
    }
}
