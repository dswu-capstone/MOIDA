package com.moida.backend.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // 응답 타입을 JSON으로 설정하고 상태 코드를 401(Unauthorized)로 지정
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 에러 메시지 작성
        String errorMessage = "{\"status\": 401, \"message\": \"인증되지 않은 사용자입니다.\"}";

        // 응답 보내기
        response.getWriter().write(errorMessage);
    }
}