package com.moida.backend.member;
/*
User 엔티티
 */

import com.moida.backend.member.dtos.ProfileRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

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

    /**
     * 프로필 정보 업데이트(회원가입시, 프로필 등록시)
     */
    public void setProfileInfo(ProfileRequest request) {
        // 문자열 필드 체크 (null도 아니고 ""도 아닐 때만 업데이트)
        if (StringUtils.hasText(request.getNickname())) this.nickname = request.getNickname();
        if (StringUtils.hasText(request.getMajor())) this.major = request.getMajor();
        if (StringUtils.hasText(request.getGoal())) this.goal = request.getGoal();
        if (StringUtils.hasText(request.getIntroduce())) this.introduce = request.getIntroduce();
        if (StringUtils.hasText(request.getLevel())) this.level = request.getLevel();
        if (StringUtils.hasText(request.getLocationType())) this.locationType = request.getLocationType();
        if (StringUtils.hasText(request.getRegion())) this.region = request.getRegion();

        // 리스트 필드 체크 (null도 아니고 항목이 최소 하나라도 있을 때만 업데이트)
        if (request.getInterestCategory() != null && !request.getInterestCategory().isEmpty()) {
            this.interestCategory = request.getInterestCategory();
        }
        if (request.getAvailableDays() != null && !request.getAvailableDays().isEmpty()) {
            this.availableDays = request.getAvailableDays();
        }
        if (request.getAvailableTime() != null && !request.getAvailableTime().isEmpty()) {
            this.availableTime = request.getAvailableTime();
        }
    }
}
