package com.nikhilm.hourglass.goal.exceptions;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.List;

public class RequestBodyValidationException extends RuntimeException{

    private final String messages;

    public RequestBodyValidationException(List<ObjectError> listErrors) {
        super();
        StringBuilder sb = new StringBuilder();
        for (ObjectError error : listErrors) {
            if (error instanceof FieldError) {
                sb.append(((FieldError) error).getField()).append(" ").append(((FieldError) error).getDefaultMessage())
                        .append(". ");
            } else {
                sb.append(" ").append(error.getDefaultMessage()).append(". ");
            }
        }
        messages = sb.toString();

    }

    public String getMessages() {
        return messages;
    }
}

