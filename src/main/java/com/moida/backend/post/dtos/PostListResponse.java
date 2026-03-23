package com.moida.backend.post.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PostListResponse {
    // 목록 조회 전체 응답을 감싸는 DTO
    // PostSummary 여러 개 + message + totalCount를 한번에 묶어서 보냄
    private String message;
    private long totalCount;
    private List<PostSummary> posts;

    public static PostListResponse of(long totalCount, List<PostSummary> posts) {
        return PostListResponse.builder()
                .message("게시글 목록 조회 성공")
                .totalCount(totalCount)
                .posts(posts)
                .build();
    }
}
