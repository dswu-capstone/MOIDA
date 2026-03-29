package com.moida.backend.member.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private String message;
    // 가입 시 입력했지만 수정 가능한 필드들
    private String nickname;               // 닉네임
    private String major;                  // 학과
    private List<String> interestCategory; // 관심 카테고리 ["IT/개발", "디자인"]
    private String goal;                   // 목표

    // 새로 추가되는 프로필 상세 정보들
    private List<String> availableDays; // 가능 요일
    private List<String> availableTime; // 가능 시간
    private String introduce; //자기소개
    private String level; // 선호 레벨
    private String locationType; // 온/오프라인
    private String region; // 선호 지역
}
