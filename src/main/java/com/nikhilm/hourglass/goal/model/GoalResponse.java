package com.nikhilm.hourglass.goal.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GoalResponse {
    private List<Goal> goals = new ArrayList<>();
    private Long totalgoals;
}
