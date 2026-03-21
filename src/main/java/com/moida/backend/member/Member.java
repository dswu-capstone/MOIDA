package com.moida.backend.member;
/*
User 엔티티
 */

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "members")
@Getter
@NoArgsConstructor
public class Member {
    @Id
    private String id; // 내부 식별자(ObjectId)

    // 회원가입 정보
    private String memberId; // 사용자 아이디(중복 여부 판단)
    private String pw; // 비밀번호(암호화)
    private String nickname; //닉네임
    private String major; // 전공
    private List<String> interestCategory; // 관심 카테고리
    private String goal; // 목표

    // 프로필 정보
    private List<String> availableDays; // 가능 요일
    private List<String> availableTime; // 가능 시간
    private String introduce; //자기소개
    private String level; // 선호 레벨
    private String locationType; // 온/오프라인
    private String region; // 선호 지역

    @CreatedDate
    private LocalDateTime createdAt; // 생성일

    @LastModifiedDate
    private LocalDateTime updatedAt; // 수정일

    @Builder
    public Member(String memberId, String pw, String nickname, String major,
                  List<String> interestCategory, String goal, List<String> availableDays,
                  List<String> availableTime, String introduce, String level,
                  String locationType, String region) {
        this.memberId = memberId;
        this.pw = pw;
        this.nickname = nickname;
        this.major = major;
        this.interestCategory = interestCategory;
        this.goal = goal;
        this.availableDays = availableDays;
        this.availableTime = availableTime;
        this.introduce = introduce;
        this.level = level;
        this.locationType = locationType;
        this.region = region;
    }
}
