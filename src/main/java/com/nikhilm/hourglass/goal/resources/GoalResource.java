package com.nikhilm.hourglass.goal.resources;

import ch.qos.logback.classic.spi.IThrowableProxy;
import com.nikhilm.hourglass.goal.exceptions.ApiError;
import com.nikhilm.hourglass.goal.exceptions.GoalException;
import com.nikhilm.hourglass.goal.exceptions.ValidationException;
import com.nikhilm.hourglass.goal.model.Goal;
import com.nikhilm.hourglass.goal.model.GoalResponse;
import com.nikhilm.hourglass.goal.model.GoalStatus;
import com.nikhilm.hourglass.goal.services.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Slf4j
@RestController
public class GoalResource  {


    @Autowired
    GoalService goalService;

    @Autowired
    ReactiveCircuitBreakerFactory factory;

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
            throw new ValidationException("Wrong input!");
        }
        log.info("user : " + user);
        List<String> statusFilters = new ArrayList<>();

        if (status.isPresent()) {
            statusFilters = parseStatusFilter(status.get(), ",");
        }
        if (isStatusInvalid(statusFilters)) {
            throw new ValidationException(("Wrong input!!"));
        }

        return Mono.zip(rcb.run(goalService.fetchGoals(text, page, statusFilters, user),
                    throwable -> Mono.error(new GoalException(500, "Internal server error!"))),
                rcb.run(goalService.findTotalGoalCount(user),
                        throwable -> Mono.error(new GoalException(500, "Internal server error!"))),
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
        if (page.isPresent() && page.get() < 1) {
            return true;
        }

        return false;

    }
    private boolean isStatusInvalid(List<String> inputs) {


        Arrays.asList(GoalStatus.values().toString().toUpperCase()).forEach(System.out::println);
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
    public Mono<ResponseEntity<Goal>> addGoal(@RequestBody Goal goal, @RequestHeader("user") String user)   {

        log.info("Goal name is " + goal.getName());
        if (goal.getName() == null || goal.getName().trim().isEmpty())  {
            log.info("Bad request error");
            throw new ValidationException("Wrong input!");
        }
        // inject user
        goal.setUserId(user);
        return rcb.run(goalService.addGoal(goal), throwable-> {
            if (throwable.getMessage().contains("Conflict")) {

                return Mono.error(throwable);
            }
            return Mono.error(new GoalException(500, "Internal server error!"));
        })
        .map(savedGoal -> {
            return ResponseEntity.created(URI.create("/"+savedGoal.getId()))
                .body(savedGoal);
        }).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping(value = "/goal", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Goal>> updateGoalStatus(@RequestBody Goal goal, @RequestHeader("user") String user)  {
        log.info("Goal " + goal);

        if (goal.getName() == null || goal.getName().trim().isEmpty())  {
            log.error("Bad request error");
            throw new ValidationException("Wrong input!");
        }
        // inject user
        goal.setUserId(user);
        return rcb.run(goalService.updateGoal(goal), throwable -> Mono.error(new GoalException(500, "Internal server error!")))
                .map(savedGoal -> {
                    log.info("Updated Goal for response" + savedGoal);
                    return ResponseEntity.ok()
                            .body(savedGoal);
                }).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

}
