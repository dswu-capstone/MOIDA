package com.moida.backend.post.service;


import com.moida.backend.member.Member;
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
//    회원이 존재하는지 확인하고, 게시글을 만들어서 저장
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // 게시글 등록
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
}
