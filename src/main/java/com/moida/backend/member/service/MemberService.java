package com.moida.backend.member.service;

import com.moida.backend.jwt.JwtTokenProvider;
import com.moida.backend.member.Member;
import com.moida.backend.member.dtos.ProfileRequest;
import com.moida.backend.member.repository.MemberRepository;
import com.moida.backend.member.dtos.LoginRequest;
import com.moida.backend.member.dtos.LoginResponse;
import com.moida.backend.member.dtos.SignupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Transactional
    public void registerProfile(String memberId, ProfileRequest profileRequest) {

        // memberRepository에서 member 객체 꺼내기
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("해당 아이디의 사용자는 존재하지 않습니다:" + memberId));

        // Entity 상태 변경
        member.setProfileInfo(profileRequest);

        // DB에 상태 저장
        memberRepository.save(member);

        log.info("사용자 [{}]의 프로필 업데이트 완료", memberId);
    }
}