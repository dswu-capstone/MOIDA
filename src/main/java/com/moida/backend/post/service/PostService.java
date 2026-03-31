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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final AiService aiService;

    // AI연동
    // 게시글 등록 (AI 전처리 포함)
    public PostResponse createPost(String memberId, PostRequest request) {
        String nickname = "anonymous";

        if (!"anonymous".equals(memberId)) {
            Member member = memberRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자입니다: " + memberId));
            nickname = member.getNickname();
        }

        // AI 전처리 호출
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", request.getTitle());
        postData.put("body", request.getBody());
        postData.put("boardType", request.getBoardType());
        postData.put("category", request.getCategory());
        postData.put("tags", request.getTags());

        List<String> keywords = new ArrayList<>();
        String semanticText = "";
        List<Double> embedding = new ArrayList<>();
        Map<String, Object> profile = new HashMap<>();

        Map<String, Object> aiResult = aiService.preprocessPost(postData);
        if (aiResult != null && aiResult.get("data") != null) {
            Map<String, Object> data = (Map<String, Object>) aiResult.get("data");
            keywords = (List<String>) data.getOrDefault("keywords", new ArrayList<>());
            semanticText = (String) data.getOrDefault("semantic_text", "");
            embedding = (List<Double>) data.getOrDefault("embedding", new ArrayList<>());
            profile = (Map<String, Object>) data.getOrDefault("profile", new HashMap<>());

            // AI가 생성한 키워드를 tags에도 반영
            if (request.getTags() == null || request.getTags().isEmpty()) {
                List<String> aiKeywords = (List<String>) data.getOrDefault("keywords", new ArrayList<>());
                if (!aiKeywords.isEmpty()) {
                    request = new PostRequest(
                            request.getBoardType(), request.getTitle(),
                            request.getBody(), request.getCategory(), aiKeywords,
                            request.getOpenChatLink()
                    );
                }
            }
        }

        Post post = Post.builder()
                .memberId(memberId)
                .boardType(request.getBoardType())
                .title(request.getTitle())
                .body(request.getBody())
                .category(request.getCategory())
                .tags(request.getTags())
                .openChatLink(request.getOpenChatLink())
                .keywords(keywords)
                .semanticText(semanticText)
                .embedding(embedding)
                .profile(profile)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("게시글 등록 완료 - 작성자: {}, 제목: {}", memberId, savedPost.getTitle());

        return PostResponse.from(savedPost, nickname);
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


    // AI 추천용 - 모든 게시글 데이터 반환
    public List<Map<String, Object>> getAllPostsForRecommend() {
        List<Post> posts = postRepository.findAll();

        return posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("boardType", post.getBoardType());
            map.put("title", post.getTitle());
            map.put("body", post.getBody());
            map.put("category", post.getCategory());
            map.put("tags", post.getTags() != null ? post.getTags() : new ArrayList<>());
            map.put("keywords", post.getKeywords() != null ? post.getKeywords() : new ArrayList<>());
            map.put("semantic_text", post.getSemanticText() != null ? post.getSemanticText() : "");
            map.put("embedding", post.getEmbedding() != null ? post.getEmbedding() : new ArrayList<>());
            map.put("profile", post.getProfile() != null ? post.getProfile() : new HashMap<>());
            return map;
        }).collect(Collectors.toList());
    }
}
