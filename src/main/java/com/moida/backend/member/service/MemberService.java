package com.moida.backend.member.service;

import com.moida.backend.jwt.JwtTokenProvider;
import com.moida.backend.member.Member;
import com.moida.backend.member.repository.MemberRepository;
import com.moida.backend.member.dtos.LoginRequest;
import com.moida.backend.member.dtos.LoginResponse;
import com.moida.backend.member.dtos.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입: 저장만 담당
    public void signup(SignupRequest request) {
        if (memberRepository.existsByMemberId(request.getMemberId())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        Member member = Member.builder()
                .memberId(request.getMemberId())
                .pw(passwordEncoder.encode(request.getPw()))
                .nickname(request.getNickname())
                .major(request.getMajor())
                .interestCategory(request.getInterestCategory())
                .goal(request.getGoal())
                .build();

        memberRepository.save(member);
    }

    // 로그인: 검증 및 토큰 생성 담당
    public LoginResponse login(LoginRequest request) {
        // 아이디 확인
        Member member = memberRepository.findByMemberId(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 틀렸습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPw(), member.getPw())) {
            throw new RuntimeException("아이디 또는 비밀번호가 틀렸습니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

        return LoginResponse.of("로그인 완료", accessToken, refreshToken);
    }
}