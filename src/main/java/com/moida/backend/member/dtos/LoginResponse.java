package com.moida.backend.member.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String message;
    private String accessToken;
    private String refreshToken;

    public static LoginResponse of(String message, String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}