package com.moida.backend.post.dtos;

import com.moida.backend.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PostSummary {
    // 목록에서 보여줄 게시글 요약 정보 - 게시글 1개의 정보 (제목, 카테고리, 태그 같은 요약 정보)
    private String id;
    private String boardType;
    private String title;
    private String category;
    private List<String> tags;

    public static PostSummary from(Post post) {
        return PostSummary.builder()
                .id(post.getId())
                .boardType(post.getBoardType())
                .title(post.getTitle())
                .category(post.getCategory())
                .tags(post.getTags())
                .build();
    }
}
