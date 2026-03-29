package com.moida.backend.member.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupRequest {
    private String memberId;               // 사용자 아이디 (중복 체크 기준)
    private String pw;                     // 비밀번호
    private String nickname;               // 닉네임
    private String major;                  // 학과
    private List<String> interestCategory; // 관심 카테고리 ["IT/개발", "디자인"]
    private String goal;                   // 목표
}
