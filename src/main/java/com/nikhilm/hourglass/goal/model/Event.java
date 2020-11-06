package com.nikhilm.hourglass.goal.model;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

public class Event<K, T> {

    public enum Type {TASK_ADDED, TASK_COMPLETED, GOAL_ADDED, GOAL_DEFERRED, GOAL_RESUMED, GOAL_COMPLETED}

    private Type eventType;
    private K key;
    private T data;
    private LocalDateTime eventCreatedAt;

    public Event() {
        this.eventType = null;
        this.key = null;
        this.data = null;
        this.eventCreatedAt = null;
    }

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        this.eventCreatedAt = now();
    }

    public Type getEventType() {
        return eventType;
    }

    public K getKey() {
        return key;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getEventCreatedAt() {
        return eventCreatedAt;
    }
}
