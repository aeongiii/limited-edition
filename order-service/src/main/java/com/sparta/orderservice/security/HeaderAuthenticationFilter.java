package com.sparta.orderservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws java.io.IOException, jakarta.servlet.ServletException {

        // 1. X-User-Email 헤더 값 추출
        String userEmail = request.getHeader("X-User-Email");

        if (userEmail == null || userEmail.isEmpty()) {
            // 2. 헤더가 없으면 401 응답
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Missing or invalid X-User-Email header\"}");
            return;
        }

        // 3. SecurityContext에 사용자 정보 저장
        PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(userEmail, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}