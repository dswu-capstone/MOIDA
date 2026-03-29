package com.moida.backend.post.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "posts")
@Getter
@NoArgsConstructor
public class Post {
    @Id
    private String id;

    private String memberId;      // 작성자 아이디
    private String boardType;     // 게시판 종류 (study, event 등)
    private String title;         // 제목
    private String body;          // 본문
    private String category;      // 카테고리 (개발/IT, 디자인 등)
    private List<String> tags;    // AI 생성 태그

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Post(String memberId, String boardType, String title,
                String body, String category, List<String> tags) {
        this.memberId = memberId;
        this.boardType = boardType;
        this.title = title;
        this.body = body;
        this.category = category;
        this.tags = tags;
    }
}
