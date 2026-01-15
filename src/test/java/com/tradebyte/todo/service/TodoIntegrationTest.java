package com.tradebyte.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.entity.TodoItem;
import com.tradebyte.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Todo Integration Tests")
class TodoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    private LocalDateTime currentDateTime;

    @BeforeEach
    void setUp() {
        currentDateTime = LocalDateTime.now();
        todoRepository.deleteAll();
    }

    @Nested
    @DisplayName("Todo Item Lifecycle")
    class TodoItemLifecycleTests {

        @Test
        @DisplayName("Given no todo items, when creating and retrieving todo item, then return created item with correct data")
        void givenNoTodoItems_whenCreateAndRetrieveTodoItem_thenReturnCreatedItem() throws Exception {
            // Given
            String todoDescription = "Complete integration tests";
            LocalDateTime dueDateTime = currentDateTime.plusDays(3);

            TodoRequest createRequest = TodoRequest.builder()
                    .description(todoDescription)
                    .dueDateTime(dueDateTime)
                    .build();

            // When & Then
            String createResponse = mockMvc.perform(post("/v1/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.description").value(todoDescription))
                    .andExpect(jsonPath("$.status").value("not done"))
                    .andExpect(jsonPath("$.dueDateTime").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long createdId = objectMapper.readTree(createResponse).get("id").asLong();

            // When & Then
            mockMvc.perform(get("/v1/todos/{id}", createdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(createdId))
                    .andExpect(jsonPath("$.description").value(todoDescription))
                    .andExpect(jsonPath("$.status").value("not done"));
        }

        @Test
        @DisplayName("Given existing todo item, when updating description, then return updated item with new description")
        void givenExistingTodoItem_whenUpdateDescription_thenReturnUpdatedItem() throws Exception {
            // Given
            TodoItem todoItem = TodoItem.builder()
                    .description("Original description")
                    .creationDateTime(LocalDateTime.now())
                    .status(TodoItem.Status.NOT_DONE)
                    .dueDateTime(currentDateTime.plusDays(2))
                    .build();

            TodoItem savedItem = todoRepository.save(todoItem);

            UpdateDescriptionRequest updateRequest = UpdateDescriptionRequest.builder()
                    .description("Updated: Complete with diagrams")
                    .build();

            // When & Then
            mockMvc.perform(patch("/v1/todos/{id}/description", savedItem.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedItem.getId()))
                    .andExpect(jsonPath("$.description").value("Updated: Complete with diagrams"));

            TodoItem updatedItem = todoRepository.findById(savedItem.getId()).orElseThrow();
            assertThat(updatedItem.getDescription()).isEqualTo("Updated: Complete with diagrams");
        }

        @Test
        @DisplayName("Given existing not done todo item, when marking as done, then update status and set done datetime")
        void givenExistingNotDoneTodoItem_whenMarkAsDone_thenUpdateStatusAndSetDoneDateTime() throws Exception {
            // Given
            TodoItem todoItem = TodoItem.builder()
                    .description("Task to complete")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.plusDays(1))
                    .status(TodoItem.Status.NOT_DONE)
                    .build();

            TodoItem savedItem = todoRepository.save(todoItem);

            // When & Then
            mockMvc.perform(patch("/v1/todos/{id}/done", savedItem.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedItem.getId()))
                    .andExpect(jsonPath("$.status").value("done"))
                    .andExpect(jsonPath("$.doneDateTime").exists());

            TodoItem updatedItem = todoRepository.findById(savedItem.getId()).orElseThrow();
            assertThat(updatedItem.getStatus()).isEqualTo(TodoItem.Status.DONE);
            assertThat(updatedItem.getDoneDateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Todo Item Filtering")
    class TodoItemFilteringTests {

        @Test
        @DisplayName("Given mixed status todo items, when getting not done items only, then return only not done items")
        void givenMixedStatusTodoItems_whenGetNotDoneItemsOnly_thenReturnOnlyNotDoneItems() throws Exception {
            // Given
            TodoItem notDoneItem = TodoItem.builder()
                    .description("Pending task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.plusDays(1))
                    .status(TodoItem.Status.NOT_DONE)
                    .build();

            TodoItem doneItem = TodoItem.builder()
                    .description("Completed task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.plusDays(1))
                    .status(TodoItem.Status.DONE)
                    .doneDateTime(currentDateTime)
                    .build();

            todoRepository.saveAll(List.of(notDoneItem, doneItem));

            // When & Then - Get not done items only
            mockMvc.perform(get("/v1/todos")
                            .param("page", "0")
                            .param("size", "20")) // Default includeAll=false
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].description").value("Pending task"))
                    .andExpect(jsonPath("$.content[0].status").value("not done"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }

        @Test
        @DisplayName("Given mixed status todo items, when getting all items, then return all items regardless of status")
        void givenMixedStatusTodoItems_whenGetAllItems_thenReturnAllItems() throws Exception {
            // Given
            TodoItem notDoneItem = TodoItem.builder()
                    .description("Pending task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.plusDays(1))
                    .status(TodoItem.Status.NOT_DONE)
                    .build();

            TodoItem doneItem = TodoItem.builder()
                    .description("Completed task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.plusDays(1))
                    .status(TodoItem.Status.DONE)
                    .doneDateTime(currentDateTime)
                    .build();

            todoRepository.saveAll(List.of(notDoneItem, doneItem));

            // When & Then
            mockMvc.perform(get("/v1/todos")
                            .param("includeAll", "true")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[?(@.status == 'not done')]").exists())
                    .andExpect(jsonPath("$.content[?(@.status == 'done')]").exists())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("Business Rules Validation")
    class BusinessRulesValidationTests {

        @Test
        @DisplayName("Given past due todo item, when attempting to update description, then return 400 BAD_REQUEST")
        void givenPastDueTodoItem_whenAttemptToUpdateDescription_thenReturnBadRequest() throws Exception {
            // Given
            TodoItem pastDueItem = TodoItem.builder()
                    .description("Overdue task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.minusDays(1))
                    .status(TodoItem.Status.PAST_DUE)
                    .build();

            TodoItem savedItem = todoRepository.save(pastDueItem);

            UpdateDescriptionRequest updateRequest = UpdateDescriptionRequest.builder()
                    .description("Attempt to update past due")
                    .build();

            // When & Then
            mockMvc.perform(patch("/v1/todos/{id}/description", savedItem.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot update a past due item"));
        }

        @Test
        @DisplayName("Given past due todo item, when attempting to mark as done, then return 400 BAD_REQUEST")
        void givenPastDueTodoItem_whenAttemptToMarkAsDone_thenReturnBadRequest() throws Exception {
            // Given
            TodoItem pastDueItem = TodoItem.builder()
                    .description("Overdue task")
                    .creationDateTime(LocalDateTime.now())
                    .dueDateTime(currentDateTime.minusDays(1))
                    .status(TodoItem.Status.PAST_DUE)
                    .build();

            TodoItem savedItem = todoRepository.save(pastDueItem);

            // When & Then
            mockMvc.perform(patch("/v1/todos/{id}/done", savedItem.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot change status of a past due item (id: " + savedItem.getId() + ")"));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Given non-existing todo item id, when retrieving todo item, then return 404 NOT_FOUND")
        void givenNonExistingTodoItemId_whenRetrieveTodoItem_thenReturnNotFound() throws Exception {
            // Given
            Long nonExistingId = 999L;

            // When & Then
            mockMvc.perform(get("/v1/todos/{id}", nonExistingId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Todo item not found with id: 999"));
        }

        @Test
        @DisplayName("Given invalid todo request with past due date, when creating todo item, then return 400 BAD_REQUEST")
        void givenInvalidTodoRequestWithPastDueDate_whenCreateTodoItem_thenReturnBadRequest() throws Exception {
            // Given
            TodoRequest invalidRequest = TodoRequest.builder()
                    .description("Invalid task")
                    .dueDateTime(currentDateTime.minusDays(1))
                    .build();

            // When & Then
            mockMvc.perform(post("/v1/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details.dueDateTime").value("Due date time must be in the future"));
        }
    }
}