package com.moida.backend.post.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class AiService {
    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // 게시글 전처리 (태그 생성 + 임베딩)
    public Map<String, Object> preprocessPost(Map<String, Object> postData) {
        String url = aiServerUrl + "/ai/recommend/preprocess";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of("post", postData);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("AI 전처리 성공");
            return response.getBody();
        } catch (Exception e) {
            log.error("AI 전처리 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    // 게시글 추천
    public Map<String, Object> recommendPosts(Map<String, Object> requestBody) {
        String url = aiServerUrl + "/ai/recommend/";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            log.info("AI 추천 성공");
            return response.getBody();
        } catch (Exception e) {
            log.error("AI 추천 호출 실패: {}", e.getMessage());
            return null;
        }
    }
}