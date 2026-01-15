package com.tradebyte.todo.service;

import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.TodoResponse;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.entity.TodoItem;
import com.tradebyte.todo.exception.ResourceNotFoundException;
import com.tradebyte.todo.exception.ValidationException;
import com.tradebyte.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Todo Service Tests")
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoService todoService;

    private LocalDateTime currentDateTime;
    private TodoItem sampleTodoItem;

    @BeforeEach
    void setUp() {
        currentDateTime = LocalDateTime.now();
        sampleTodoItem = TodoItem.builder()
                .id(1L)
                .description("Complete project documentation")
                .status(TodoItem.Status.NOT_DONE)
                .creationDateTime(currentDateTime.minusHours(1))
                .dueDateTime(currentDateTime.plusDays(1))
                .build();
    }

    @Nested
    @DisplayName("Create Todo Item")
    class CreateTodoItemTests {

        @Test
        @DisplayName("Given valid todo request, when creating todo item, then return todo response with generated id")
        void givenValidTodoRequest_whenCreateTodoItem_thenReturnTodoResponse() {
            // Given
            TodoRequest request = TodoRequest.builder()
                    .description("Review API design")
                    .dueDateTime(currentDateTime.plusDays(2))
                    .build();

            TodoItem savedTodoItem = TodoItem.builder()
                    .id(1L)
                    .description("Review API design")
                    .status(TodoItem.Status.NOT_DONE)
                    .creationDateTime(currentDateTime)
                    .dueDateTime(currentDateTime.plusDays(2))
                    .build();

            when(todoRepository.save(any(TodoItem.class))).thenReturn(savedTodoItem);

            // When
            TodoResponse response = todoService.createTodoItem(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.description()).isEqualTo("Review API design");
            assertThat(response.status()).isEqualTo("not done");
            assertThat(response.dueDateTime()).isEqualTo(currentDateTime.plusDays(2));

            verify(todoRepository).save(any(TodoItem.class));
        }

        @Test
        @DisplayName("Given todo request with immediate due date, when creating todo item, then set not done status")
        void givenTodoRequestWithImmediateDueDate_whenCreateTodoItem_thenSetNotDoneStatus() {
            // Given
            TodoRequest request = TodoRequest.builder()
                    .description("Urgent task")
                    .dueDateTime(currentDateTime.plusMinutes(5))
                    .build();

            TodoItem savedTodoItem = TodoItem.builder()
                    .id(1L)
                    .description("Urgent task")
                    .status(TodoItem.Status.NOT_DONE)
                    .build();

            when(todoRepository.save(any(TodoItem.class))).thenReturn(savedTodoItem);

            // When
            TodoResponse response = todoService.createTodoItem(request);

            // Then
            assertThat(response.status()).isEqualTo("not done");
        }
    }

    @Nested
    @DisplayName("Get Todo Item")
    class GetTodoItemTests {

        @Test
        @DisplayName("Given existing todo item id, when getting todo item, then return todo response")
        void givenExistingTodoItemId_whenGetTodoItem_thenReturnTodoResponse() {
            // Given
            Long todoId = 1L;
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(sampleTodoItem));

            // When
            TodoResponse response = todoService.getTodoItem(todoId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(todoId);

            verify(todoRepository).findById(todoId);
        }

        @Test
        @DisplayName("Given non-existing todo item id, when getting todo item, then throw ResourceNotFoundException")
        void givenNonExistingTodoItemId_whenGetTodoItem_thenThrowResourceNotFoundException() {
            // Given
            Long nonExistingId = 999L;
            when(todoRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> todoService.getTodoItem(nonExistingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Todo item not found with id: " + nonExistingId);

            verify(todoRepository).findById(nonExistingId);
        }
    }

    @Nested
    @DisplayName("Update Description")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("Given valid description update for not done item, when updating description, then return updated todo response")
        void givenValidDescriptionUpdateForNotDoneItem_whenUpdateDescription_thenReturnUpdatedResponse() {
            // Given
            Long todoId = 1L;
            String newDescription = "Updated: Include diagrams in documentation";
            UpdateDescriptionRequest request = UpdateDescriptionRequest.builder()
                    .description(newDescription)
                    .build();

            when(todoRepository.findById(todoId)).thenReturn(Optional.of(sampleTodoItem));
            when(todoRepository.save(any(TodoItem.class))).thenReturn(sampleTodoItem);

            // When
            TodoResponse response = todoService.updateDescription(todoId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.description()).isEqualTo(newDescription);

            verify(todoRepository).findById(todoId);
            verify(todoRepository).save(sampleTodoItem);
        }

        @Test
        @DisplayName("Given update request for past due item, when updating description, then throw ValidationException")
        void givenUpdateRequestForPastDueItem_whenUpdateDescription_thenThrowValidationException() {
            // Given
            Long todoId = 1L;
            sampleTodoItem.setStatus(TodoItem.Status.PAST_DUE);
            UpdateDescriptionRequest request = UpdateDescriptionRequest.builder()
                    .description("Attempt to update past due")
                    .build();

            when(todoRepository.findById(todoId)).thenReturn(Optional.of(sampleTodoItem));

            // When & Then
            assertThatThrownBy(() -> todoService.updateDescription(todoId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Cannot update a past due item");

            verify(todoRepository).findById(todoId);
            verify(todoRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Mark as Done")
    class MarkAsDoneTests {

        @Test
        @DisplayName("Given not done todo item, when marking as done, then update status and set done datetime")
        void givenNotDoneTodoItem_whenMarkAsDone_thenUpdateStatusAndSetDoneDateTime() {
            // Given
            Long todoId = 1L;
            when(todoRepository.findById(todoId)).thenReturn(Optional.of(sampleTodoItem));
            when(todoRepository.save(any(TodoItem.class))).thenReturn(sampleTodoItem);

            // When
            TodoResponse response = todoService.markAsDone(todoId);

            // Then
            assertThat(response.status()).isEqualTo("done");
            assertThat(sampleTodoItem.getDoneDateTime()).isNotNull();

            verify(todoRepository).findById(todoId);
            verify(todoRepository).save(sampleTodoItem);
        }

        @Test
        @DisplayName("Given already done todo item, when marking as done, then return done status without error")
        void givenAlreadyDoneTodoItem_whenMarkAsDone_thenReturnDoneStatus() {
            // Given
            Long todoId = 1L;
            sampleTodoItem.setStatus(TodoItem.Status.DONE);
            sampleTodoItem.setDoneDateTime(currentDateTime.minusHours(1));

            when(todoRepository.findById(todoId)).thenReturn(Optional.of(sampleTodoItem));

            // When
            TodoResponse response = todoService.markAsDone(todoId);

            // Then
            assertThat(response.status()).isEqualTo("done");
        }
    }

    @Nested
    @DisplayName("Get All Not Done Items")
    class GetAllNotDoneItemsTests {

        @Test
        @DisplayName("Given includeAll is false, when getting todo items, then return only not done items")
        void givenIncludeAllFalse_whenGetTodoItems_thenReturnOnlyNotDoneItems() {
            // Given
            when(todoRepository.findByStatus(eq(TodoItem.Status.NOT_DONE), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleTodoItem)));

            // When
            Slice<TodoResponse> responses = todoService.getAllNotDoneItems(false, 0, 10);

            // Then
            assertThat(responses.getContent()).hasSize(1);
            assertThat(responses.getContent().get(0).status()).isEqualTo("not done");
            assertThat(responses.getContent().get(0).description()).isEqualTo("Complete project documentation");

            verify(todoRepository).findByStatus(eq(TodoItem.Status.NOT_DONE), any(Pageable.class));
        }

        @Test
        @DisplayName("Given includeAll is true, when getting todo items, then return all items")
        void givenIncludeAllTrue_whenGetTodoItems_thenReturnAllItems() {
            // Given
            TodoItem doneItem = TodoItem.builder()
                    .id(2L)
                    .description("Completed task")
                    .status(TodoItem.Status.DONE)
                    .build();

            when(todoRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleTodoItem, doneItem)));

            // When
            Slice<TodoResponse> responses = todoService.getAllNotDoneItems(true, 0, 10);

            // Then
            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getContent()).extracting("status")
                    .containsExactlyInAnyOrder("not done", "done");

            verify(todoRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Update Past Due Items")
    class UpdatePastDueItemsTests {

        @Test
        @DisplayName("Given not done items with past due dates, when updating past due items, then update status to past due")
        void givenNotDoneItemsWithPastDueDates_whenUpdatePastDueItems_thenUpdateStatusToPastDue() {
            // Given
            when(todoRepository.markPastDueItems(any(LocalDateTime.class)))
                    .thenReturn(1);

            // When
            int updatedCount = todoService.updatePastDueItemsBulk();

            // Then
            assertThat(updatedCount).isEqualTo(1);
            verify(todoRepository).markPastDueItems(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Given no past due items, when updating past due items, then do not save anything")
        void givenNoPastDueItems_whenUpdatePastDueItems_thenDoNotSaveAnything() {
            // Given
            when(todoRepository.markPastDueItems(any(LocalDateTime.class)))
                    .thenReturn(0);

            // When
            int updatedCount = todoService.updatePastDueItemsBulk();

            // Then
            assertThat(updatedCount).isEqualTo(0);
            verify(todoRepository).markPastDueItems(any(LocalDateTime.class));
        }
    }
}