package com.moida.backend.post.repository;

import com.moida.backend.post.entity.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
}

//MongoDB에 데이터를 저장하거나 조회