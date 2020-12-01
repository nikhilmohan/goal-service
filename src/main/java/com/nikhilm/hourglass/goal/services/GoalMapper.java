package com.nikhilm.hourglass.goal.services;


import com.nikhilm.hourglass.goal.model.Goal;
import com.nikhilm.hourglass.goal.model.GoalDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalMapper {

    Goal goalDTOtoGoal(GoalDTO goal);
}
