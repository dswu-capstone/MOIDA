package com.moida.backend.post.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private String boardType;     // "study", "project" 등
    private String title;         // 제목
    private String body;          // 본문
    private String category;      // "개발/IT" 등
    private List<String> tags;    // ["웹개발", "앱개발"]
    private String openChatLink; // 오픈채팅링크
}
