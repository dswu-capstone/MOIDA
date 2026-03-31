package com.moida.backend.post.dtos;

import com.moida.backend.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PostResponse {
    private String message;
    private String id; // 게시글 구분 id. MongoDB가 자동으로 생성
    private String memberId;
    private String boardType;
    private String title;
    private String body;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private String writer;        // 작성자 닉네임
    private String openChatLink;
    
    public static PostResponse from(Post post, String nickname) {
        return PostResponse.builder()
                .message("게시글 등록 성공")
                .id(post.getId())
                .writer(nickname)
                .memberId(post.getMemberId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .body(post.getBody())
                .category(post.getCategory())
                .tags(post.getTags())
                .openChatLink(post.getOpenChatLink())
                .createdAt(post.getCreatedAt())
                .build();
    }

    // 게시글 상세 조회 응답용
    public static PostResponse detailOf(Post post, String nickname) {
        return PostResponse.builder()
                .message("게시글 상세 조회 성공")
                .id(post.getId())
                .writer(nickname)
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .body(post.getBody())
                .category(post.getCategory())
                .tags(post.getTags())
                .openChatLink(post.getOpenChatLink())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
