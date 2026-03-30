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
import java.util.Map;

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

    // AI
    private List<String> keywords;        // AI 추출 키워드
    private String semanticText;          // 임베딩용 텍스트
    private List<Double> embedding;       // 임베딩 벡터
    private Map<String, Object> profile;  // AI 분석 프로필

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

//    @Builder
//    public Post(String memberId, String boardType, String title,
//                String body, String category, List<String> tags) {
//        this.memberId = memberId;
//        this.boardType = boardType;
//        this.title = title;
//        this.body = body;
//        this.category = category;
//        this.tags = tags;
//    }
    @Builder
    public Post(String memberId, String boardType, String title,
                String body, String category, List<String> tags,
                List<String> keywords, String semanticText,
                List<Double> embedding, Map<String, Object> profile) {
        this.memberId = memberId;
        this.boardType = boardType;
        this.title = title;
        this.body = body;
        this.category = category;
        this.tags = tags;
        this.keywords = keywords;
        this.semanticText = semanticText;
        this.embedding = embedding;
        this.profile = profile;
    }
}
