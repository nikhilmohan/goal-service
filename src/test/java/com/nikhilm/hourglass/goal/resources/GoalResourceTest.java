package com.nikhilm.hourglass.goal.resources;

import com.nikhilm.hourglass.goal.exceptions.ApiError;
import com.nikhilm.hourglass.goal.exceptions.GoalException;
import com.nikhilm.hourglass.goal.model.*;
import com.nikhilm.hourglass.goal.services.GoalMapper;
import com.nikhilm.hourglass.goal.services.GoalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest
@Slf4j
class GoalResourceTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    GoalService goalService;

    @MockBean
    GoalMapper goalMapper;

    @Test
    public void testGetGoals()  {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);

        GoalResponse goalResponse = new GoalResponse();
        goalResponse.setTotalgoals(1L);
        goalResponse.getGoals().add(goal);

        Mockito.when(goalService.fetchGoals(any(Optional.class), any(Optional.class),
            any(List.class), eq("abc"))).thenReturn(Mono.just(goalResponse));

        Mockito.when(goalService.findTotalGoalCount("abc")).thenReturn(Mono.just(1L));
        GoalResponse response = webTestClient.get().uri("http://localhost:9000/goals")
                .header("user", "abc")
                .exchange()
                .expectBody(GoalResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(1L, response.getTotalgoals());
        assertTrue(response.getGoals().stream()
                .anyMatch(goal1 -> goal1.getName().equalsIgnoreCase("first goal")));
    }
    @Test
    public void testGetGoalsError()  {

        Mockito.when(goalService.fetchGoals(any(Optional.class), any(Optional.class),
                any(List.class), eq("abc"))).thenReturn(Mono.error(new RuntimeException()));

        Mockito.when(goalService.findTotalGoalCount("abc")).thenReturn(Mono.just(1L));
        webTestClient.get().uri("http://localhost:9000/goals")
                .header("user", "abc")
                .exchange()
                .expectStatus()
                .is5xxServerError();


    }
    @Test
    public void testSearchGoals()  {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);

        GoalResponse goalResponse = new GoalResponse();
        goalResponse.setTotalgoals(1L);
        goalResponse.getGoals().add(goal);

        Mockito.when(goalService.fetchGoals(eq(Optional.of("first")), any(Optional.class),
                any(List.class), eq("abc"))).thenReturn(Mono.just(goalResponse));

        Mockito.when(goalService.findTotalGoalCount("abc")).thenReturn(Mono.just(1L));
        GoalResponse response = webTestClient.get().uri("http://localhost:9000/goals?search=first")
                .header("user", "abc")
                .exchange()
                .expectBody(GoalResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(1L, response.getTotalgoals());
        assertTrue(response.getGoals().stream()
                .anyMatch(goal1 -> goal1.getName().equalsIgnoreCase("first goal")));
    }

    @Test
    public void testGetGoalsByPage()  {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);
        goal.setLevel(GoalLevel.EASY);

        Goal goal1 = new Goal();
        goal1.setUserId("abc");
        goal1.setDescription(" a new goal");
        goal1.setName("second goal");
        goal1.setStatus(GoalStatus.ACTIVE);
        goal1.setDueDate(localDate);
        goal1.setLevel(GoalLevel.EXTREME);

        Goal goal2 = new Goal();
        goal2.setUserId("abc");
        goal2.setDescription(" a new goal");
        goal2.setName("third goal");
        goal2.setStatus(GoalStatus.ACTIVE);
        goal2.setDueDate(localDate);
        goal2.setLevel(GoalLevel.EXTREME);

        GoalResponse goalResponse = new GoalResponse();
        goalResponse.setTotalgoals(1L);
        goalResponse.getGoals().addAll(Arrays.asList(goal, goal1));

        Mockito.when(goalService.fetchGoals(any(Optional.class), any(Optional.class),
                any(List.class), eq("abc"))).thenReturn(Mono.just(goalResponse));

        Mockito.when(goalService.findTotalGoalCount("abc")).thenReturn(Mono.just(3L));
        GoalResponse response = webTestClient.get().uri("http://localhost:9000/goals?page=1")
                .header("user", "abc")
                .exchange()
                .expectBody(GoalResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(3L, response.getTotalgoals());
        assertEquals(2L, response.getGoals().size());

    }

    @Test
    public void testSearchGoalsByStatus()  {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);

        GoalResponse goalResponse = new GoalResponse();
        goalResponse.setTotalgoals(1L);
        goalResponse.getGoals().add(goal);

        Mockito.when(goalService.fetchGoals(Optional.empty(), Optional.empty(),
                List.of("A", "D"), "abc")).thenReturn(Mono.just(goalResponse));

        Mockito.when(goalService.findTotalGoalCount("abc")).thenReturn(Mono.just(1L));
        GoalResponse response = webTestClient.get().uri("http://localhost:9000/goals?status=A,D")
                .header("user", "abc")
                .exchange()
                .expectBody(GoalResponse.class)
                .returnResult()
                .getResponseBody();

        assertEquals(1L, response.getTotalgoals());
        assertTrue(response.getGoals().stream()
                .anyMatch(goal1 -> goal1.getName().equalsIgnoreCase("first goal")));
    }

    @Test
    public void testInvalidPageInput()  {
        ApiError apiError = webTestClient.get().uri("http://localhost:9000/goals?page=-1")
                .header("user", "abc")
                .exchange()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertEquals("Wrong input!", apiError.getMessage());

    }

    @Test
    public void testInvalidStatusInput()  {
        webTestClient.get().uri("http://localhost:9000/goals?status=INVALID")
                .header("user", "abc")
                .exchange()
                .expectStatus()
                .is4xxClientError();

    }

    @Test
    public void testAddGoal()   {
        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);

        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);
        Mockito.when(goalService.addGoal(any(Goal.class))).thenReturn(Mono.just(goal));

        Goal response =  webTestClient.post().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectBody(Goal.class)
                .returnResult()
                .getResponseBody();

        assertTrue(response.getName().equalsIgnoreCase("first goal"));

    }

    @Test
    public void testAddGoalInvalid()    {

        Goal goal = new Goal();
        goal.setName("");

        log.info("name" + goal.getName());

       webTestClient.post().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
               .is4xxClientError();


    }

    @Test
    public void testAddGoalExists()    {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);
        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);

        Mockito.when(goalService.addGoal(any(Goal.class)))
                .thenReturn(Mono.error(new GoalException(409, "Conflict!")));


        webTestClient.post().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
                .is4xxClientError();


    }
    @Test
    public void testAddGoalFailure()    {

        LocalDate localDate = LocalDate.of(2020, 12, 23);

        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(localDate);
        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);

        Mockito.when(goalService.addGoal(any(Goal.class)))
                .thenReturn(Mono.error(new GoalException(500, "Internal server error!")));


        webTestClient.post().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
                .is5xxServerError();
    }

    @Test
    public void updateGoalInvalid() {
        Goal goal = new Goal();
        goal.setName("");

        webTestClient.put().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
    @Test
    public void updateGoalNotExists() {
        Goal goal = new Goal();
        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);

        Mockito.when(goalService.updateGoal(any(Goal.class)))
                .thenReturn(Mono.empty());

        webTestClient.put().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    public void updateGoal() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);

        Mockito.when(goalMapper.goalDTOtoGoal(any(GoalDTO.class))).thenReturn(goal);

        Mockito.when(goalService.updateGoal(any(Goal.class))).thenReturn(Mono.just(goal));

        webTestClient.put().uri("http://localhost:9000/goal")
                .header("user", "abc")
                .body(Mono.just(goal), Goal.class)
                .exchange()
                .expectStatus()
                .is2xxSuccessful();
    }






}