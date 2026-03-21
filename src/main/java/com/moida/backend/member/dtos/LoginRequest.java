package com.moida.backend.member.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String memberId;
    private String pw;

    public static LoginRequest from(SignupRequest signupRequest) {
        return new LoginRequest(signupRequest.getMemberId(), signupRequest.getPw());
    }
}