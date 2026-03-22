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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String memberId = (String) authentication.getPrincipal();

        PostResponse response = postService.createPost(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
