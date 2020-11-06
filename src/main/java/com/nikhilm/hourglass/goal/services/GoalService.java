package com.nikhilm.hourglass.goal.services;

import com.nikhilm.hourglass.goal.exceptions.GoalException;
import com.nikhilm.hourglass.goal.model.Event;
import com.nikhilm.hourglass.goal.model.Goal;
import com.nikhilm.hourglass.goal.model.GoalResponse;
import com.nikhilm.hourglass.goal.model.GoalStatus;
import com.nikhilm.hourglass.goal.repositories.GoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static com.nikhilm.hourglass.goal.model.Event.Type.*;

@Service
@Slf4j
@EnableBinding(GoalService.MessageSources.class)
public class GoalService {

    @Autowired
    GoalRepository goalRepository;

    @Autowired
    private MessageSources messageSources;

    @Value("${pageSize}")
    private int pageSize;

    private boolean filterByStatus(Goal goal, List<String> inclusions)   {
        if (inclusions.isEmpty())   {
            return true;
        }
        return inclusions.contains(goal.getStatus().getValue());

    }

    public Mono<GoalResponse> fetchGoals(Optional<String> text, Optional<Integer> page, List<String> statusFilter ) {

        log.info("Filters " + statusFilter);

        GoalResponse response = new GoalResponse();


        Flux<Goal> goalFlux;

        int offset = page.isEmpty() ? 0 : (page.get() - 1) * pageSize;


        if (text.isPresent()) {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text.get());
            goalFlux = goalRepository.findAllBy(criteria).filter(goal -> statusFilter.isEmpty() ||
                    statusFilter.contains(goal.getStatus().getValue()))
                    .skip(offset).take(pageSize);

        }
        else {
            goalFlux = goalRepository.findAll().filter(goal -> statusFilter.isEmpty() ||
                    statusFilter.contains(goal.getStatus().getValue()))
            .skip(offset).take(pageSize);


        }

        return goalFlux.reduce(response, (goalResponse, goal)-> {
            goalResponse.getGoals().add(goal);
            return goalResponse;
        });
    }

    public Mono<Goal> addGoal(Goal goal) {

        return goalRepository.findByName(goal.getName())
                .flatMap(goal1 -> Mono.error(new GoalException(409, "Conflict!")))
                .switchIfEmpty(Mono.defer(()-> {
                        return goalRepository.save(goal)
                                .map(savedTask -> {
                                    messageSources.outputGoals().send((MessageBuilder.withPayload(new Event(GOAL_ADDED, savedTask.getId(), savedTask)).build()));
                                    log.info("Add goal event published!");
                                    return savedTask;
                                });
                    }))
                .cast(Goal.class);

    }

    public Mono<Long> findTotalGoalCount() {

        return goalRepository.findTotalCount();
    }

    public Mono<Goal> updateGoal(Goal goal) {
        return goalRepository.findByName(goal.getName())
                .map(currentGoal -> {
                    log.info("Current Goal " + currentGoal);
                    Goal updatedGoal = new Goal();
                    updatedGoal.setName(currentGoal.getName());
                    updatedGoal.setDescription(currentGoal.getDescription());
                    updatedGoal.setDueDate(currentGoal.getDueDate());
                    updatedGoal.setLevel(currentGoal.getLevel());
                    updatedGoal.setId(currentGoal.getId());
                    updatedGoal.setUserId(currentGoal.getUserId());
                    if (goal.getStatus() == GoalStatus.COMPLETED)   {
                        updatedGoal.setCompletedOn(LocalDate.now());
                        updatedGoal.setVotes(3);

                    }
                    updatedGoal.setStatus(goal.getStatus());
                    updatedGoal.setNotes(goal.getNotes());
                    return updatedGoal;
                })
                .flatMap(goalToSave -> {
                    return goalRepository.save(goalToSave);
                })
                .map(savedGoal -> {
                    Event.Type eventType = GOAL_COMPLETED;
                    switch (savedGoal.getStatus())  {
                        case ACTIVE:
                            eventType = GOAL_RESUMED;
                            break;
                        case DEFERRED:
                            eventType = GOAL_DEFERRED;
                            break;
                        case COMPLETED:
                            eventType = GOAL_COMPLETED;
                    }
                    messageSources.outputGoals().send((MessageBuilder.withPayload(new Event(eventType, savedGoal.getId(), savedGoal)).build()));
                    log.info("Goal status change event published! " + savedGoal.getStatus() + " " + savedGoal.getName());
                    return savedGoal;
                });

    }
    public interface MessageSources {

        String OUTPUT_GOALS = "output-goals";

        @Output(OUTPUT_GOALS)
        MessageChannel outputGoals();

    }
}
