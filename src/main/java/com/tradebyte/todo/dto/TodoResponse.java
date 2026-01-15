package com.tradebyte.todo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradebyte.todo.entity.TodoItem;
import lombok.Builder;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record TodoResponse(
        Long id,
        String description,
        String status,
        LocalDateTime creationDateTime,
        LocalDateTime dueDateTime,
        LocalDateTime doneDateTime
) {

    public TodoResponse(TodoItem todoItem) {
        this(
                todoItem.getId(),
                todoItem.getDescription(),
                todoItem.getStatus().getValue(),
                todoItem.getCreationDateTime(),
                todoItem.getDueDateTime(),
                todoItem.getDoneDateTime()
        );
    }
}
