package com.tradebyte.todo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TodoRequest(

        @NotBlank(message = "Description is required")
        @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
        String description,

        @NotNull(message = "Due date time is required")
        @Future(message = "Due date time must be in the future")
        LocalDateTime dueDateTime
) {}
