package com.tradebyte.todo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradebyte.todo.entity.TodoItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TodoResponse {
    private Long id;
    private String description;
    private String status;
    private LocalDateTime creationDateTime;
    private LocalDateTime dueDateTime;
    private LocalDateTime doneDateTime;

    public TodoResponse() {}

    public TodoResponse(TodoItem todoItem) {
        this.id = todoItem.getId();
        this.description = todoItem.getDescription();
        this.status = todoItem.getStatus().getValue();
        this.creationDateTime = todoItem.getCreationDateTime();
        this.dueDateTime = todoItem.getDueDateTime();
        this.doneDateTime = todoItem.getDoneDateTime();
    }

}
