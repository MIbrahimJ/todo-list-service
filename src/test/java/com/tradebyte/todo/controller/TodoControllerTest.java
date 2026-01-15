package com.tradebyte.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.TodoResponse;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.exception.ResourceNotFoundException;
import com.tradebyte.todo.exception.ValidationException;
import com.tradebyte.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
@DisplayName("Todo Controller Tests")
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    private LocalDateTime futureDateTime;
    private LocalDateTime pastDateTime;

    @BeforeEach
    void setUp() {
        futureDateTime = LocalDateTime.now().plusDays(1);
        pastDateTime = LocalDateTime.now().minusDays(1);
    }

    @Nested
    @DisplayName("POST /api/v1/todos - Create Todo Item")
    class CreateTodoItemTests {

        @Test
        @DisplayName("Given valid todo request, when creating todo item, then return 201 CREATED with response")
        void givenValidTodoRequest_whenCreateTodoItem_thenReturnCreated() throws Exception {
            // Given
            TodoRequest request = TodoRequest.builder()
                    .description("Complete project documentation")
                    .dueDateTime(futureDateTime)
                    .build();

            TodoResponse response = TodoResponse.builder()
                    .id(1L)
                    .description("Complete project documentation")
                    .status("not done")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(futureDateTime)
                    .build();

            when(todoService.createTodoItem(any(TodoRequest.class))).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.description").value("Complete project documentation"))
                    .andExpect(jsonPath("$.status").value("not done"))
                    .andExpect(jsonPath("$.dueDateTime").exists());
        }

        @Test
        @DisplayName("Given todo request with empty description, when creating todo item, then return 400 BAD_REQUEST")
        void givenTodoRequestWithEmptyDescription_whenCreateTodoItem_thenReturnBadRequest() throws Exception {
            // Given
            TodoRequest request = TodoRequest.builder()
                    .description("")
                    .dueDateTime(futureDateTime)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details.description").exists());        }

        @Test
        @DisplayName("Given todo request with past due date, when creating todo item, then return 400 BAD_REQUEST")
        void givenTodoRequestWithPastDueDate_whenCreateTodoItem_thenReturnBadRequest() throws Exception {
            // Given
            TodoRequest request = TodoRequest.builder()
                    .description("Complete project documentation")
                    .dueDateTime(pastDateTime)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details.dueDateTime").value("Due date time must be in the future"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/todos/{id} - Get Todo Item")
    class GetTodoItemTests {

        @Test
        @DisplayName("Given existing todo item id, when getting todo item, then return 200 OK with item")
        void givenExistingTodoItemId_whenGetTodoItem_thenReturnOk() throws Exception {
            // Given
            Long todoId = 1L;
            TodoResponse response = TodoResponse.builder()
                    .id(todoId)
                    .description("Review pull requests")
                    .status("not done")
                    .creationDateTime(LocalDateTime.now().minusHours(2))
                    .dueDateTime(futureDateTime)
                    .build();

            when(todoService.getTodoItem(todoId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/v1/todos/{id}", todoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(todoId))
                    .andExpect(jsonPath("$.description").value("Review pull requests"))
                    .andExpect(jsonPath("$.status").value("not done"))
                    .andExpect(jsonPath("$.creationDateTime").exists())
                    .andExpect(jsonPath("$.dueDateTime").exists());
        }

        @Test
        @DisplayName("Given non-existing todo item id, when getting todo item, then return 404 NOT_FOUND")
        void givenNonExistingTodoItemId_whenGetTodoItem_thenReturnNotFound() throws Exception {
            // Given
            Long nonExistingId = 999L;
            when(todoService.getTodoItem(nonExistingId))
                    .thenThrow(new ResourceNotFoundException("Todo item not found with id: " + nonExistingId));

            // When & Then
            mockMvc.perform(get("/api/v1/todos/{id}", nonExistingId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Todo item not found with id: 999"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/todos/{id}/description - Update Todo Description")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("Given valid update description request, when updating todo description, then return 200 OK")
        void givenValidUpdateRequest_whenUpdateDescription_thenReturnOk() throws Exception {
            // Given
            Long todoId = 1L;
            String newDescription = "Updated: Complete project documentation with diagrams";

            UpdateDescriptionRequest request = UpdateDescriptionRequest.builder()
                    .description(newDescription)
                    .build();

            TodoResponse response = TodoResponse.builder()
                    .id(todoId)
                    .description(newDescription)
                    .status("not done")
                    .build();

            when(todoService.updateDescription(eq(todoId), any(UpdateDescriptionRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(patch("/api/v1/todos/{id}/description", todoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(todoId))
                    .andExpect(jsonPath("$.description").value(newDescription));
        }

        @Test
        @DisplayName("Given empty description in update request, when updating todo description, then return 400 BAD_REQUEST")
        void givenEmptyDescription_whenUpdateDescription_thenReturnBadRequest() throws Exception {
            // Given
            Long todoId = 1L;
            UpdateDescriptionRequest request = UpdateDescriptionRequest.builder()
                    .description("")
                    .build();

            // When & Then
            mockMvc.perform(patch("/api/v1/todos/{id}/description", todoId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/todos/{id}/done - Mark Todo as Done")
    class MarkAsDoneTests {

        @Test
        @DisplayName("Given existing todo item, when marking as done, then return 200 OK with done status")
        void givenExistingTodoItem_whenMarkAsDone_thenReturnOk() throws Exception {
            // Given
            Long todoId = 1L;
            TodoResponse response = TodoResponse.builder()
                    .id(todoId)
                    .description("Complete project documentation")
                    .status("done")
                    .doneDateTime(LocalDateTime.now())
                    .build();

            when(todoService.markAsDone(todoId)).thenReturn(response);

            // When & Then
            mockMvc.perform(patch("/api/v1/todos/{id}/done", todoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(todoId))
                    .andExpect(jsonPath("$.status").value("done"))
                    .andExpect(jsonPath("$.doneDateTime").exists());
        }

        @Test
        @DisplayName("Given past due todo item, when marking as done, then return 400 BAD_REQUEST")
        void givenPastDueTodoItem_whenMarkAsDone_thenReturnBadRequest() throws Exception {
            // Given
            Long todoId = 1L;
            when(todoService.markAsDone(todoId))
                    .thenThrow(new ValidationException("Cannot mark a past due item as done"));

            // When & Then
            mockMvc.perform(patch("/api/v1/todos/{id}/done", todoId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot mark a past due item as done"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/todos/{id}/not-done - Mark Todo as Not Done")
    class MarkAsNotDoneTests {

        @Test
        @DisplayName("Given existing done todo item, when marking as not done, then return 200 OK with not done status")
        void givenExistingDoneTodoItem_whenMarkAsNotDone_thenReturnOk() throws Exception {
            // Given
            Long todoId = 1L;
            TodoResponse response = TodoResponse.builder()
                    .id(todoId)
                    .description("Complete project documentation")
                    .status("not done")
                    .doneDateTime(null)
                    .build();

            when(todoService.markAsNotDone(todoId)).thenReturn(response);

            // When & Then
            mockMvc.perform(patch("/api/v1/todos/{id}/not-done", todoId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(todoId))
                    .andExpect(jsonPath("$.status").value("not done"));
        }
    }
}