package com.moida.backend.member.controller;

import com.moida.backend.member.dtos.*;
import com.moida.backend.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@RequestBody SignupRequest request) {
        // 회원가입 처리 (저장)
        memberService.signup(request);

        // 가입 완료 후 바로 로그인 로직 적용(토큰 반환)
        LoginRequest loginRequest = LoginRequest.from(request);

        return ResponseEntity.ok(memberService.login(loginRequest));
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    // 프로필 등록 및 수정 API
    @PostMapping("/profile")
    public ResponseEntity<Map<String, String>> registerProfile(@RequestBody ProfileRequest profileRequest) {
        Map<String, String> response = new HashMap<>();

        try {
            // SecurityContextHolder에서 JWT 인증 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 인증 정보에서 memberId 꺼내기
            String memberId = (String) authentication.getPrincipal();

            memberService.registerProfile(memberId, profileRequest);
            response.put("message", "프로필 등록 및 수정 성공");

            return ResponseEntity.ok(response);
        } catch(Exception e) {
            log.error("프로필 저장 중 에러 발생: {}", e.getMessage());
            response.put("message", "프로필 저장 중 오류 발생");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String memberId = (String) authentication.getPrincipal();
            ProfileResponse response = memberService.getProfile(memberId);
            return ResponseEntity.ok(response);

        } catch(Exception e) {
            log.error("프로필 조회 중 에러 발생: {}", e.getMessage());
            ProfileResponse errorResponse = ProfileResponse.builder()
                    .message("프로필 가져오기 실패")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}