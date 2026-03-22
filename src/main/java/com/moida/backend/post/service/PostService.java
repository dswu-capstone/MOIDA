package com.moida.backend.post.service;


import com.moida.backend.member.repository.MemberRepository;
import com.moida.backend.post.dtos.PostRequest;
import com.moida.backend.post.dtos.PostResponse;
import com.moida.backend.post.entity.Post;
import com.moida.backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public PostResponse createPost(String memberId, PostRequest request) {
        if (!memberRepository.existsByMemberId(memberId)) {
            throw new RuntimeException("존재하지 않는 사용자입니다: " + memberId);
        }

        Post post = Post.builder()
                .memberId(memberId)
                .boardType(request.getBoardType())
                .title(request.getTitle())
                .body(request.getBody())
                .category(request.getCategory())
                .tags(request.getTags())
                .build();

        Post savedPost = postRepository.save(post);
        log.info("게시글 등록 완료 - 작성자: {}, 제목: {}", memberId, savedPost.getTitle());

        return PostResponse.from(savedPost);
    }
}
