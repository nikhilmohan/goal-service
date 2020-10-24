package com.nikhilm.hourglass.goal.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GoalExceptionHandler  {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleBadRequestException(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError("400", "Wrong input!"));
    }
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatchException(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError("400", "Wrong input!"));
    }




}
