package com.nikhilm.hourglass.goal.resources;

import com.nikhilm.hourglass.goal.exceptions.ApiError;
import com.nikhilm.hourglass.goal.exceptions.RequestBodyValidationException;
import com.nikhilm.hourglass.goal.exceptions.ValidationException;
import com.nikhilm.hourglass.goal.model.Goal;
import com.nikhilm.hourglass.goal.model.GoalResponse;
import com.nikhilm.hourglass.goal.model.GoalStatus;
import com.nikhilm.hourglass.goal.services.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Slf4j
@RestController
@CrossOrigin
public class GoalResource  {


    @Autowired
    GoalService goalService;

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
                                    @RequestParam("status") Optional<String> status) {

        if (isPageInputInvalid(page))  {
            throw new ValidationException("Wrong input!");
        }

        List<String> statusFilters = new ArrayList<>();

        if (status.isPresent()) {
            statusFilters = parseStatusFilter(status.get(), ",");
        }
        if (isStatusInvalid(statusFilters)) {
            throw new ValidationException(("Wrong input!!"));
        }

        return Mono.zip(goalService.fetchGoals(text, page, statusFilters),
                goalService.findTotalGoalCount(),
                ((goalResponse, aLong) -> {
                    GoalResponse response = new GoalResponse();
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
    public Mono<ResponseEntity<Goal>> addGoal(@RequestBody Goal goal)   {
        if (goal.getName() == null || goal.getName().trim().isEmpty())  {
            log.info("Bad request error");
            throw new ValidationException("Wrong input!");
        }
        return goalService.addGoal(goal)
                .map(savedGoal -> {
                    return ResponseEntity.created(URI.create("/"+savedGoal.getId()))
                        .body(savedGoal);
                }).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping(value = "/goal", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Goal>> updateGoalStatus(@RequestBody Goal goal)  {
        log.info("Goal " + goal);

        if (goal.getName() == null || goal.getName().trim().isEmpty())  {
            log.error("Bad request error");
            throw new ValidationException("Wrong input!");
        }
        return goalService.updateGoal(goal)
                .map(savedGoal -> {
                    log.info("Updated Goal for response" + savedGoal);
                    return ResponseEntity.ok()
                            .body(savedGoal);
                }).switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

}
