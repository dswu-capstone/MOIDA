package com.moida.backend.member.Controller;

import com.moida.backend.member.dtos.LoginRequest;
import com.moida.backend.member.dtos.LoginResponse;
import com.moida.backend.member.dtos.SignupRequest;
import com.moida.backend.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}