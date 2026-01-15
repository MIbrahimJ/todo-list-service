package com.tradebyte.todo.controller;

import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.TodoResponse;
import com.tradebyte.todo.dto.TodoSliceResponse;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/todos")
@Tag(name = "Todo Items", description = "Todo Items Management API")
public class TodoController {

    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    @Operation(summary = "Create a new todo item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Todo item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<TodoResponse> createTodoItem(@Valid @RequestBody TodoRequest request) {
        logger.info("Received request to create todo item");
        TodoResponse response = todoService.createTodoItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a todo item by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo item found"),
            @ApiResponse(responseCode = "404", description = "Todo item not found")
    })
    public ResponseEntity<TodoResponse> getTodoItem(
            @Parameter(description = "ID of the todo item to retrieve")
            @PathVariable Long id) {
        logger.debug("Received request to get todo item with id: {}", id);
        TodoResponse response = todoService.getTodoItem(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get todo items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo items retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<TodoSliceResponse<TodoResponse>> getTodoItems(
            @Parameter(description = "Include all items regardless of status")
            @RequestParam(defaultValue = "false") boolean includeAll,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max: 100)")
            @RequestParam(defaultValue = "20") int size
    ) {
        int validatedSize = Math.min(Math.max(size, 1), 100);

        Slice<TodoResponse> slice = todoService.getAllNotDoneItems(includeAll, page, validatedSize);

        TodoSliceResponse<TodoResponse> response = new TodoSliceResponse<>(
                slice.getContent(),
                page,
                validatedSize,
                slice.hasNext()
        );

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{id}/description")
    @Operation(summary = "Update a todo item's description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Description updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or item is past due"),
            @ApiResponse(responseCode = "404", description = "Todo item not found")
    })
    public ResponseEntity<TodoResponse> updateDescription(
            @Parameter(description = "ID of the todo item to update")
            @PathVariable Long id,
            @Valid @RequestBody UpdateDescriptionRequest request) {
        logger.info("Received request to update description for todo item id: {}", id);
        TodoResponse response = todoService.updateDescription(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/done")
    @Operation(summary = "Mark a todo item as done")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo item marked as done"),
            @ApiResponse(responseCode = "400", description = "Item is past due"),
            @ApiResponse(responseCode = "404", description = "Todo item not found")
    })
    public ResponseEntity<TodoResponse> markAsDone(
            @Parameter(description = "ID of the todo item to mark as done")
            @PathVariable Long id) {
        logger.info("Received request to mark todo item as done, id: {}", id);
        TodoResponse response = todoService.markAsDone(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/not-done")
    @Operation(summary = "Mark a todo item as not done")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todo item marked as not done"),
            @ApiResponse(responseCode = "400", description = "Item is past due"),
            @ApiResponse(responseCode = "404", description = "Todo item not found")
    })
    public ResponseEntity<TodoResponse> markAsNotDone(
            @Parameter(description = "ID of the todo item to mark as not done")
            @PathVariable Long id) {
        logger.info("Received request to mark todo item as not done, id: {}", id);
        TodoResponse response = todoService.markAsNotDone(id);
        return ResponseEntity.ok(response);
    }
}