package com.moida.backend.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
/*
1. 헤더에서 JWT 토큰 추출
2. jwtTokenProvider로 토큰 검증, 식별자 추출,
3. memberId를 SecurityContext에 저장
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Request Header에서 JWT 토큰 추출
        String token = resolveToken(request);

        // 토큰 유효성 검사
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

            // 토큰이 유효하면 유저 식별자(memberId) 추출
            String memberId = jwtTokenProvider.getMemberIdFromToken(token);

            // 인증 객체 생성
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());

            // SecurityContext에 인증 정보 저장 (이후 컨트롤러에서 유저 정보를 알 수 있게 됨)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Authenticated user: {}, request URI: {}", memberId, request.getRequestURI());
        }

        // 다음 필터로 이동
        filterChain.doFilter(request, response);
    }

    // Request Header에서 JWT 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}