package com.nikhilm.hourglass.goal.resources;

import com.nikhilm.hourglass.goal.exceptions.GoalException;
import com.nikhilm.hourglass.goal.exceptions.ValidationException;
import com.nikhilm.hourglass.goal.model.Goal;
import com.nikhilm.hourglass.goal.model.GoalDTO;
import com.nikhilm.hourglass.goal.model.GoalResponse;
import com.nikhilm.hourglass.goal.model.GoalStatus;
import com.nikhilm.hourglass.goal.services.GoalMapper;
import com.nikhilm.hourglass.goal.services.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Slf4j
@RestController
public class GoalResource  {


    public static final String WRONG_INPUT = "Wrong input!";
    public static final String SERVER_ERROR = "Internal server error!";
    @Autowired
    GoalService goalService;

    @Autowired
    ReactiveCircuitBreakerFactory factory;

    @Autowired
    GoalMapper goalMapper;

    ReactiveCircuitBreaker rcb;

    public GoalResource(ReactiveCircuitBreakerFactory factory)  {
        this.factory = factory;
        rcb = factory.create("goal");
    }


    private List<String> parseStatusFilter(String statusFilter, String delim) {

        List<String> inclusions = new ArrayList<>();

        String [] statusArray = statusFilter.split(delim);
        for (String status : statusArray)   {
            inclusions.add(status.toUpperCase());
        }
        return inclusions;

    }

    @GetMapping(value = "/goals", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<GoalResponse> goals(@RequestParam("search") Optional<String> text,
                                    @RequestParam("page") Optional<Integer> page,
                                    @RequestParam("status") Optional<String> status,
                                    @RequestHeader("user") String user
                                  ) {

        if (isPageInputInvalid(page))  {
            throw new ValidationException(WRONG_INPUT);
        }
        log.info("user : " + user);
        List<String> statusFilters = new ArrayList<>();

        if (status.isPresent()) {
            statusFilters = parseStatusFilter(status.get(), ",");
        }
        if (isStatusInvalid(statusFilters)) {
            throw new ValidationException((WRONG_INPUT));
        }

        return Mono.zip(rcb.run(goalService.fetchGoals(text, page, statusFilters, user),
                    throwable -> {
                        log.error("Failed " + throwable.getMessage());
                     return Mono.error(new GoalException(500, SERVER_ERROR));}),
                rcb.run(goalService.findTotalGoalCount(user),
                        throwable -> Mono.error(new GoalException(500, SERVER_ERROR))),
                ((goalResponse, aLong) -> {
                    GoalResponse response = new GoalResponse();
                    log.info("Total count " + aLong);
                    response.setTotalgoals(aLong);
                    response.setGoals(goalResponse.getGoals());
                    return response;
                })
        );

    }



    private boolean isPageInputInvalid(Optional<Integer> page) {
        return (page.isPresent() && page.get() < 1);
    }
    private boolean isStatusInvalid(List<String> inputs) {
        return inputs.stream()
                .anyMatch(s -> {
                    for (GoalStatus gs : GoalStatus.values()) {
                        if (gs.getValue().equalsIgnoreCase(s)) {
                            return false;
                        }
                    }
                    return true;
                });
    }


    @PostMapping(value = "/goal", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Goal>> addGoal(@RequestBody GoalDTO goal, @RequestHeader("user") String user)   {

        log.info("Goal name is " + goal.getName());
        if (goal.getName().trim().isEmpty())  {
            log.info("Bad request error");
            throw new ValidationException(WRONG_INPUT);
        }
        // inject user
        goal.setUserId(user);
        return rcb.run(goalService.addGoal(goalMapper.goalDTOtoGoal(goal)), throwable-> {
            if (throwable.getMessage().contains("Conflict")) {

                return Mono.error(throwable);
            }
            return Mono.error(new GoalException(500, SERVER_ERROR));
        })
        .map(savedGoal ->
             ResponseEntity.created(URI.create("/"+savedGoal.getId()))
                .body(savedGoal)
        ).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping(value = "/goal", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Goal>> updateGoalStatus(@RequestBody GoalDTO goal, @RequestHeader("user") String user)  {
        log.info("Goal " + goal);

        if (goal.getName().trim().isEmpty())  {
            log.error("Bad request error");
            throw new ValidationException(WRONG_INPUT);
        }
        // inject user
        goal.setUserId(user);
        return rcb.run(goalService.updateGoal(goalMapper.goalDTOtoGoal(goal)), throwable ->
                Mono.error(new GoalException(500, SERVER_ERROR)))
                .map(savedGoal -> {
                    log.info("Updated Goal for response" + savedGoal);
                    return ResponseEntity.ok()
                            .body(savedGoal);
                }).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

}
