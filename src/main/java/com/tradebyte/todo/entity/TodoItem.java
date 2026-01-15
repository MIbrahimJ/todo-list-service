package com.tradebyte.todo.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "todo_items")
@Builder
@AllArgsConstructor
public class TodoItem {

    public enum Status {
        NOT_DONE("not done"),
        DONE("done"),
        PAST_DUE("past due");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "creation_datetime", nullable = false)
    private LocalDateTime creationDateTime;

    @Column(name = "due_datetime", nullable = false)
    private LocalDateTime dueDateTime;

    @Column(name = "done_datetime")
    private LocalDateTime doneDateTime;

    @Version
    private Long version;

    public TodoItem() {
        this.creationDateTime = LocalDateTime.now();
        this.status = Status.NOT_DONE;
    }

    public TodoItem(String description, LocalDateTime dueDateTime) {
        this();
        this.description = description;
        this.dueDateTime = dueDateTime;
    }

    public boolean isPastDue() {
        return status == Status.PAST_DUE;
    }

    public boolean isImmutable() {
        return isPastDue();
    }
}