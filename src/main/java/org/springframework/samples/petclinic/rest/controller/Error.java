package org.springframework.samples.petclinic.rest.controller;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Error {

    private String message;

    public Error(String message) {
        this.message = message;
    }

    public Error() {
    }

    public String toJson() {
        try {
            List<Error> list = new ArrayList<>();
            list.add(0, this);
            return new ObjectMapper().writeValueAsString(list);
        } catch(JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
