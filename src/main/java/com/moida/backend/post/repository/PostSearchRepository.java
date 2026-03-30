package com.moida.backend.post.repository;

import com.moida.backend.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostSearchRepository {
    private final MongoTemplate mongoTemplate;

    public List<Post> searchPosts(String search, String type, List<String> tags, String cursor, int limit) {
        Query query = new Query();

        // 검색어 필터 (제목 or 본문에서 검색)
        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("body").regex(search, "i")
            ));
        }

        // 게시글 타입 필터
        if (type != null && !type.isBlank() && !type.equals("total")) {
            query.addCriteria(Criteria.where("boardType").is(type));
        }

        // 태그 필터 (선택된 태그를 모두 포함하는 게시글)
//        if (tags != null && !tags.isEmpty()) {
//            query.addCriteria(Criteria.where("tags").all(tags));
//        }
        if (tags != null && !tags.isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("tags").all(tags),
                    Criteria.where("category").in(tags)
            ));
        }

        // 커서 기반 페이지네이션
        if (cursor != null && !cursor.isBlank()) {
            query.addCriteria(Criteria.where("id").lt(cursor));
        }

        // 최신순 정렬 + limit 적용
        query.with(Sort.by(Sort.Direction.DESC, "id"));
        query.limit(limit);

        return mongoTemplate.find(query, Post.class);
    }

    public long countPosts(String search, String type, List<String> tags) {
        Query query = new Query();

        if (search != null && !search.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(search, "i"),
                    Criteria.where("body").regex(search, "i")
            ));
        }

        if (type != null && !type.isBlank() && !type.equals("total")) {
            query.addCriteria(Criteria.where("boardType").is(type));
        }

//        if (tags != null && !tags.isEmpty()) {
//            query.addCriteria(Criteria.where("tags").all(tags));
//        }
        // tags 또는 category에서 검색
        if (tags != null && !tags.isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("tags").all(tags),
                    Criteria.where("category").in(tags)
            ));
        }

        return mongoTemplate.count(query, Post.class);
    }
}
