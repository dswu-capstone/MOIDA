package com.moida.backend.post.controller;

import com.moida.backend.post.dtos.PostListResponse;
import com.moida.backend.post.dtos.PostRequest;
import com.moida.backend.post.dtos.PostResponse;
import com.moida.backend.post.service.AiService;
import com.moida.backend.post.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.*;

@Slf4j // log사용 - 에러 알기 위해서
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor // 생성자 주입
public class PostController {
    // 결과를 전달
    private final PostService postService;
    private final AiService aiService;

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

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<PostListResponse> getPostList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        PostListResponse response = postService.getPostList(search, type, tags, cursor, limit);
        return ResponseEntity.ok(response);
    }


    // AI 게시글 추천
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommendPosts(@RequestBody Map<String, Object> userInput) {
        // 모든 게시글 가져오기
        List<Map<String, Object>> allPosts = postService.getAllPostsForRecommend();

        // AI 서버에 보낼 요청 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_input", userInput);
        requestBody.put("posts", allPosts);

        Map<String, Object> result = aiService.recommendPosts(requestBody);

        if (result == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "AI 추천 서비스 오류");
            error.put("totalCount", 0);
            error.put("data", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        return ResponseEntity.ok(result);
    }

    // AI 태그 생성
    @PostMapping("/tags")
    public ResponseEntity<Map<String, Object>> generateTags(@RequestBody Map<String, Object> postData) {
        Map<String, Object> result = aiService.generateTags(postData);

        if (result == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "AI 태그 생성 실패");
            error.put("tags", new ArrayList<>());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        return ResponseEntity.ok(result);
    }
}
