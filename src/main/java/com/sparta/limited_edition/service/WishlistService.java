package com.sparta.limited_edition.service;

import com.sparta.limited_edition.dto.WishlistResponse;
import com.sparta.limited_edition.entity.Product;
import com.sparta.limited_edition.entity.User;
import com.sparta.limited_edition.entity.Wishlist;
import com.sparta.limited_edition.repository.ProductRepository;
import com.sparta.limited_edition.repository.UserRepository;
import com.sparta.limited_edition.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private WishlistRepository wishlistRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;

    public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // 위시리스트에 상품 추가
    public WishlistResponse addToWishlist(String email, long productId, int quantity) {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));
        System.out.println("유저 검증 완료");
        // 상품 검증
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품정보를 찾을 수 없습니다."));
        System.out.println("상품 검증 완료");
        // 상품 노출 false
        if (!product.isVisible()) {
            throw new IllegalArgumentException("해당 상품은 숨김 처리되었습니다.");
        }
        // 품절된 경우
        if (product.getStockQuantity() == 0) {
            throw new IllegalArgumentException("해당 상품은 품절되었습니다.");
        }
        // 위시리스트에 추가 (또는 수량 업데이트)
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseGet(() -> new Wishlist(user, product, 0)); // 일단 기본수량 0으로 세팅
        wishlist.setQuantity(wishlist.getQuantity() + quantity); // 기존수량 + 새로 받은 수량 합치기
        wishlistRepository.save(wishlist);
        // 반환
        return new WishlistResponse(
                product.getId(),
                product.getName(),
                wishlist.getQuantity(), // 누적된 수량으로 입력해야한다...
                product.getPrice(),
                product.getImageUrl(),
                "http://localhost:8080/product/" + product.getId() // 상세정보 조회 url 만들어서 같이 반환
        );
    }

    // 위시리스트 목록 조회
    public List<WishlistResponse> getWishlist(String email) {
        // userId 가져오기
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."))
                .getId();

        // 목록 조회 - 비었을 경우에도 빈 리스트 반환해야 함
        List<Wishlist> wishlists = wishlistRepository.findAllByUserId(userId);
        System.out.println("위시리스트 목록 조회 완료");

        // 위시리스트를 ResponseDto로 변환
        return wishlists.stream().map(item -> {
            Product product = item.getProduct(); // 연관된 Product 객체 가져오기
            return new WishlistResponse(
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice(),
                    product.getImageUrl(),
                    "http://localhost:8080/product/" + product.getId() // 상세정보 링크
            );
        }).collect(Collectors.toList());
    }

    // 위시리스트 상품 수량 변경
    public WishlistResponse updateWishlistQuantity (String email, Long productId, int quantity) {
        // 유저 검증
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원정보를 찾을 수 없습니다."));

        // 상품 검증
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품정보를 찾을 수 없습니다."));

        // 위시리스트에서 상품 찾기
        Wishlist wishlist = wishlistRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("위시리스트에 해당 상품이 없습니다."));

        // 수량 업데이트
        wishlist.setQuantity(quantity);
        wishlistRepository.save(wishlist);
        System.out.println("위시리스트 수량 업데이트 완료");

        // DTO 변환 후 반환
        return new WishlistResponse(
                product.getId(),
                product.getName(),
                wishlist.getQuantity(),
                product.getPrice(),
                product.getImageUrl(),
                "http://localhost:8080/product/" + product.getId() // 상세정보 링크 생성
        );
    }
}
