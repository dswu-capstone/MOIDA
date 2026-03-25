package com.moida.backend.post.service;


import com.moida.backend.member.Member;
import com.moida.backend.member.repository.MemberRepository;
import com.moida.backend.post.dtos.PostListResponse;
import com.moida.backend.post.dtos.PostRequest;
import com.moida.backend.post.dtos.PostResponse;
import com.moida.backend.post.dtos.PostSummary;
import com.moida.backend.post.entity.Post;
import com.moida.backend.post.repository.PostRepository;
import com.moida.backend.post.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
//    회원이 존재하는지 확인하고, 게시글을 만들어서 저장
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostSearchRepository postSearchRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.server.base-url:http://127.0.0.1:8002}")
    private String aiServerBaseUrl;

    // 게시글 등록
//    public PostResponse createPost(String memberId, PostRequest request) {
//        if (!memberRepository.existsByMemberId(memberId)) {
//            throw new RuntimeException("존재하지 않는 사용자입니다: " + memberId);
//        }
//
//        Post post = Post.builder()
//                .memberId(memberId)
//                .boardType(request.getBoardType())
//                .title(request.getTitle())
//                .body(request.getBody())
//                .category(request.getCategory())
//                .tags(request.getTags())
//                .build();
//
//        Post savedPost = postRepository.save(post);
//        log.info("게시글 등록 완료 - 작성자: {}, 제목: {}", memberId, savedPost.getTitle());
//
//        return PostResponse.from(savedPost);
//    }
    public PostResponse createPost(String memberId, PostRequest request) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다: " + memberId));

        Post post = Post.builder()
                .memberId(memberId)
                .boardType(request.getBoardType())
                .title(request.getTitle())
                .body(request.getBody())
                .category(request.getCategory())
                .tags(request.getTags())
                .build();

        Post savedPost = postRepository.save(post);
        triggerSavePostEmbedding(savedPost);
        log.info("게시글 등록 완료 - 작성자: {}, 제목: {}", memberId, savedPost.getTitle());

        return PostResponse.from(savedPost, member.getNickname());
    }

    private void triggerSavePostEmbedding(Post post) {
        try {
            String url = aiServerBaseUrl + "/ai/recommend/embedding";

            Map<String, Object> postDoc = new HashMap<>();
            postDoc.put("title", post.getTitle());
            postDoc.put("body", post.getBody());
            postDoc.put("category", post.getCategory());
            postDoc.put("tags", post.getTags());

            Map<String, Object> payload = new HashMap<>();
            payload.put("post_id", post.getId());
            payload.put("post_doc", postDoc);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("AI 임베딩 저장 호출 완료 - postId: {}, status: {}", post.getId(), response.getStatusCode());
        } catch (Exception e) {
            // 게시글 저장 자체는 성공 처리하고, AI 연동 실패는 로그만 남김
            log.warn("AI 임베딩 저장 호출 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
        }
    }
    // 게시글 상세 조회
    public PostResponse getPost(String postId) {
        // 게시글 찾기
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 게시글입니다: " + postId));

        // 작성자 닉네임 가져오기
        Member member = memberRepository.findByMemberId(post.getMemberId())
                .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없습니다."));

        return PostResponse.detailOf(post, member.getNickname());
    }


    // 게시글 목록 조회
    public PostListResponse getPostList(String search, String type, List<String> tags, String cursor, int limit) {
        List<Post> posts = postSearchRepository.searchPosts(search, type, tags, cursor, limit);
        long totalCount = postSearchRepository.countPosts(search, type, tags);

        List<PostSummary> postSummaries = posts.stream()
                .map(PostSummary::from)
                .collect(Collectors.toList());

        return PostListResponse.of(totalCount, postSummaries);
    }
}
