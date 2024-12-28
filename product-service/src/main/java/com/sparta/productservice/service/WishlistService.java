package com.sparta.productservice.service;

import com.sparta.productservice.dto.WishlistResponse;
import com.sparta.productservice.entity.Product;
import com.sparta.productservice.entity.User;
import com.sparta.productservice.entity.Wishlist;
import com.sparta.productservice.repository.ProductRepository;
import com.sparta.productservice.repository.UserRepository;
import com.sparta.productservice.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // 1. 위시리스트에 상품 추가
    public WishlistResponse addToWishlist(String email, long productId, int quantity) {
        User user = validateUser(email); // 사용자 검증
        Product product = validateProduct(productId); // 상품 검증
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
                .map(item ->  createWishlistResponse(item.getProduct(), item.getQuantity()))
                .collect(Collectors.toList());
    }



    // 3. 위시리스트 상품 수량 변경
    public WishlistResponse updateWishlistQuantity (String email, Long productId, int quantity) {
        User user = validateUser(email); // 사용자 검증
        Product product = validateProduct(productId); // 상품 검증
        Wishlist wishlist = validateWishlist(user, product); // 위시리스트에서 상품 찾기

        // 수량 업데이트
        wishlist.setQuantity(quantity);
        wishlistRepository.save(wishlist);

        // 반환할 wishlistResponse 만들기
        return createWishlistResponse(product, quantity);
    }

    // 4. 위시리스트에서 상품 삭제
    public void removeWishlistItem(String email, Long productId) {
        User user = validateUser(email); // 사용자 검증
        Product product = validateProduct(productId); // 상품 검증
        Wishlist wishlist = validateWishlist(user, product); // 위시리스트에서 상품 찾기
        // 삭제
        wishlistRepository.delete(wishlist);
    }


    // =========================

    // 사용자 검증
    private User validateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));
    }

    // id로 상품 검증
    private Product validateProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품정보를 찾을 수 없습니다."));
    }

    // 위시리스트에 추가, 또는 수량 업데이트
    private Wishlist createOrUpdateWishlist(User user, Product product, int quantity) {
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> new Wishlist(user, product, 0)); // 일단 기본수량 0으로 세팅
        wishlist.setQuantity(wishlist.getQuantity() + quantity); // 기존수량 + 새로 받은 수량 합치기
        wishlistRepository.save(wishlist);
        return wishlist;
    }

    // user, product로 위시리스트 찾기
    private Wishlist validateWishlist(User user, Product product) {
        return wishlistRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("위시리스트에 해당 상품이 없습니다."));
    }

    // 반환할 wishlistResponse 만들기
    private WishlistResponse createWishlistResponse(Product product, int quantity) {
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
