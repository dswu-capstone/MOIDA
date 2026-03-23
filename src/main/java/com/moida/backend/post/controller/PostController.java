package com.moida.backend.post.controller;

import com.moida.backend.post.dtos.PostRequest;
import com.moida.backend.post.dtos.PostResponse;
import com.moida.backend.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j // log사용 - 에러 알기 위해서
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor // 생성자 주입
public class PostController {
    // 결과를 전달
    private final PostService postService;

    // 게시글 등록
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberId = (String) authentication.getPrincipal();

        PostResponse response = postService.createPost(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable String id) {
        PostResponse response = postService.getPost(id);
        return ResponseEntity.ok(response);
    }
}
