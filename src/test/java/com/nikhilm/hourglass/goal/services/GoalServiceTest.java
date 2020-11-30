package com.nikhilm.hourglass.goal.services;

import com.nikhilm.hourglass.goal.model.*;
import com.nikhilm.hourglass.goal.repositories.GoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@Slf4j
class GoalServiceTest {

    @Mock
    GoalRepository goalRepository;

    @Mock
    GoalService.MessageSources messageSources;


    @InjectMocks
    GoalService goalService;

    @BeforeEach
    public void setup() {
        goalService.setPageSize(5);
    }

    @Test
    public void testFetchGoals() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));

        Mockito.when(goalRepository.findAllBy(any(TextCriteria.class))).thenReturn(Flux.just(goal));
        Mockito.when(goalRepository.findAllByUserId(anyString())).thenReturn(Flux.just(goal));
        StepVerifier.create(goalService.fetchGoals(Optional.empty(), Optional.empty(), List.of(), "abc"))
                .expectSubscription()
                .expectNextCount(1L)
                .verifyComplete();
    }

    @Test
    public void testFetchGoalsBySearch() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        Mockito.when(goalRepository.findAllBy(any(TextCriteria.class))).thenReturn(Flux.just(goal));
        Mockito.when(goalRepository.findAllByUserId(anyString())).thenReturn(Flux.just(goal));
        StepVerifier.create(goalService.fetchGoals(Optional.of("first"), Optional.empty(), List.of(), "abc"))
                .expectSubscription()
                .expectNextCount(1L)
                .verifyComplete();
    }

    @Test
    public void testFetchGoalsByStatus() {


        Mockito.when(goalRepository.findAllByUserId(anyString())).thenReturn(Flux.empty());
        StepVerifier.create(goalService.fetchGoals(Optional.empty(), Optional.empty(), List.of("C", "D"), "abc"))
                .expectSubscription()
                .expectNextMatches(goalResponse -> goalResponse.getGoals().isEmpty())
                .verifyComplete();
    }

    @Test
    public void testFetchMultipleGoals() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);
        Goal goal1 = new Goal();
        goal1.setUserId("abc");
        goal1.setDescription(" another goal");
        goal1.setName("second  goal");
        goal1.setStatus(GoalStatus.ACTIVE);
        goal1.setDueDate(LocalDate.now().plusDays(5L));
        goal1.setLevel(GoalLevel.EASY);

        Mockito.when(goalRepository.findAllByUserId(anyString())).thenReturn(Flux.fromIterable(List.of(goal, goal1)));
        StepVerifier.create(goalService.fetchGoals(Optional.empty(), Optional.empty(), List.of(), "abc"))
                .expectSubscription()
                .expectNextMatches(goalResponse -> goalResponse.getGoals().size() == 2)
                .verifyComplete();
    }

    @Test
    public void testAddGoal() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);

        MessageChannel channel = mock(MessageChannel.class);

        Mockito.when(goalRepository.findByNameAndUserId(anyString(), anyString())).thenReturn(Mono.empty());
        Mockito.when(goalRepository.save(any(Goal.class))).thenReturn(Mono.just(goal));
        Mockito.when(messageSources.outputGoals()).thenReturn(channel);
        Mockito.when(channel.send(any(Message.class))).thenReturn(true);

        StepVerifier.create(goalService.addGoal(goal))
                .expectSubscription()
                .expectNextMatches(goal1 -> goal1.getName().equalsIgnoreCase("first goal"))
                .verifyComplete();

    }

    @Test
    public void testAddGoalExists() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);

        MessageChannel channel = mock(MessageChannel.class);

        Mockito.when(goalRepository.findByNameAndUserId(anyString(), anyString())).thenReturn(Mono.just(goal));
        Mockito.when(goalRepository.save(any(Goal.class))).thenReturn(Mono.just(goal));
        Mockito.when(messageSources.outputGoals()).thenReturn(channel);
        Mockito.when(channel.send(any(Message.class))).thenReturn(true);

        StepVerifier.create(goalService.addGoal(goal))
                .expectSubscription()
                .expectErrorMessage("Conflict!")
                .verify();

    }

    @Test
    public void testTotalGoalCount() {
        Mockito.when(goalRepository.findTotalCount(anyString())).thenReturn(Mono.just(10L));
        StepVerifier.create(goalService.findTotalGoalCount("abc"))
                .expectSubscription()
                .expectNextMatches(aLong -> aLong.equals(10L))
                .verifyComplete();
    }

    @Test
    public void testUpdateGoal() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);
        MessageChannel channel = mock(MessageChannel.class);

        ArgumentCaptor<Goal> argumentCaptor = ArgumentCaptor.forClass(Goal.class);

        Mockito.when(goalRepository.save(argumentCaptor.capture())).thenReturn(Mono.just(goal));
        Mockito.when(messageSources.outputGoals()).thenReturn(channel);
        Mockito.when(channel.send(any(Message.class))).thenReturn(true);
        Mockito.when(goalRepository.findByNameAndUserId(anyString(), anyString())).thenReturn(Mono.just(goal));

        StepVerifier.create(goalService.updateGoal(goal))
                .expectSubscription()
                .expectNextMatches(goal1 -> goal1.getStatus().equals(GoalStatus.COMPLETED))
                .verifyComplete();

        assertTrue(LocalDate.now().equals(argumentCaptor.getValue().getCompletedOn()));

    }

    @Test
    public void testUpdateGoalDeferred() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.DEFERRED);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);
        MessageChannel channel = mock(MessageChannel.class);

        ArgumentCaptor<Goal> argumentCaptor = ArgumentCaptor.forClass(Goal.class);

        Mockito.when(goalRepository.save(argumentCaptor.capture())).thenReturn(Mono.just(goal));
        Mockito.when(messageSources.outputGoals()).thenReturn(channel);
        Mockito.when(channel.send(any(Message.class))).thenReturn(true);
        Mockito.when(goalRepository.findByNameAndUserId(anyString(), anyString())).thenReturn(Mono.just(goal));

        StepVerifier.create(goalService.updateGoal(goal))
                .expectSubscription()
                .expectNextMatches(goal1 -> goal1.getStatus().equals(GoalStatus.DEFERRED))
                .verifyComplete();


    }
    @Test
    public void testUpdateGoalResumed() {
        Goal goal = new Goal();
        goal.setUserId("abc");
        goal.setDescription(" a new goal");
        goal.setName("first goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setDueDate(LocalDate.now().plusDays(10L));
        goal.setLevel(GoalLevel.EXTREME);
        goal.setId("abcdef");
        MessageChannel channel = mock(MessageChannel.class);

        ArgumentCaptor<Goal> argumentCaptor = ArgumentCaptor.forClass(Goal.class);

        ArgumentCaptor<Message> eventArgumentCaptor = ArgumentCaptor.forClass(Message.class);

        Mockito.when(goalRepository.save(argumentCaptor.capture())).thenReturn(Mono.just(goal));
        Mockito.when(messageSources.outputGoals()).thenReturn(channel);
        Mockito.when(channel.send(eventArgumentCaptor.capture())).thenReturn(true);
        Mockito.when(goalRepository.findByNameAndUserId(anyString(), anyString())).thenReturn(Mono.just(goal));

        StepVerifier.create(goalService.updateGoal(goal))
                .expectSubscription()
                .expectNextMatches(goal1 -> goal1.getStatus().equals(GoalStatus.ACTIVE))
                .verifyComplete();

        Event event = (Event)eventArgumentCaptor.getValue().getPayload();

        assertEquals("abcdef", event.getKey());
        assertEquals(Event.Type.GOAL_RESUMED, event.getEventType());


    }
}
