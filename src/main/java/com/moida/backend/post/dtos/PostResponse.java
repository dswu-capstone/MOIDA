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
    private String id;
    private String memberId;
    private String boardType;
    private String title;
    private String body;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static PostResponse from(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .memberId(post.getMemberId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .body(post.getBody())
                .category(post.getCategory())
                .tags(post.getTags())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
