package com.nikhilm.hourglass.goal.repositories;

import com.nikhilm.hourglass.goal.model.Goal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GoalRepository extends ReactiveMongoRepository<Goal, String> {

    // Paginate over a full-text search result
    Flux<Goal> findAllBy(TextCriteria criteria);

    @Query(value = "{}", count = true)
    public Mono<Long> findTotalCount();


    Mono<Goal> findByName(String name);
}
