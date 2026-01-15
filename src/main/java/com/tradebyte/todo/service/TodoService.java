package com.tradebyte.todo.service;

import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.TodoResponse;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.entity.TodoItem;
import com.tradebyte.todo.exception.ResourceNotFoundException;
import com.tradebyte.todo.exception.ValidationException;
import com.tradebyte.todo.repository.TodoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class TodoService {

    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

    private final TodoRepository todoRepository;

    @Transactional
    public TodoResponse createTodoItem(TodoRequest request) {
        logger.info("Creating new todo item with description: {}", request.description());

        TodoItem todoItem = new TodoItem(
                request.description(),
                request.dueDateTime()
        );

        TodoItem savedItem = todoRepository.save(todoItem);

        logger.debug("Created todo item with id: {}", savedItem.getId());
        return new TodoResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public TodoResponse getTodoItem(Long id) {
        logger.debug("Fetching todo item with id: {}", id);

        TodoItem todoItem = findTodoItemOrThrow(id);
        return new TodoResponse(todoItem);
    }


    @Transactional(readOnly = true)
    public Slice<TodoResponse> getAllNotDoneItems(
            boolean includeAll,
            int page,
            int size
    ) {
        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageNumber = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        Slice<TodoItem> slice;

        if (includeAll) {
            slice = todoRepository.findAll(pageable);
        } else {
            slice = todoRepository.findByStatus(
                    TodoItem.Status.NOT_DONE,
                    pageable
            );
        }

        if (!includeAll && slice.hasNext()) {
            logger.warn("Potentially large result set detected for NOT_DONE items");
        }

        return slice.map(TodoResponse::new);
    }

    @Transactional
    public TodoResponse updateDescription(Long id, UpdateDescriptionRequest request) {
        logger.info("Updating description for todo item id: {}", id);

        TodoItem todoItem = findTodoItemOrThrow(id);

        if (todoItem.isImmutable()) {
            throw new ValidationException("Cannot update a past due item");
        }

        todoItem.setDescription(request.description());
        TodoItem updatedItem = todoRepository.save(todoItem);

        logger.debug("Updated description for todo item id: {}", id);
        return new TodoResponse(updatedItem);
    }

    @Transactional
    public TodoResponse markAsDone(Long id) {
        return updateStatus(id, TodoItem.Status.DONE);
    }

    @Transactional
    public TodoResponse markAsNotDone(Long id) {
        return updateStatus(id, TodoItem.Status.NOT_DONE);
    }

    @Transactional
    public int updatePastDueItemsBulk() {

        LocalDateTime now = LocalDateTime.now();

        logger.debug("Starting bulk update for past due items at {}", now);

        int updatedCount = todoRepository.markPastDueItems(now);

        if (updatedCount > 0) {
            logger.info("Bulk update completed: {} todo items marked as PAST_DUE", updatedCount);
        } else {
            logger.debug("No past due todo items found to update");
        }

        return updatedCount;
    }

    private TodoResponse updateStatus(Long id, TodoItem.Status newStatus) {
        logger.info("Updating todo item status, id: {}, newStatus: {}", id, newStatus);

        TodoItem todoItem = findTodoItemOrThrow(id);

        if (todoItem.isImmutable()) {
            throw new ValidationException(
                    "Cannot change status of a past due item (id: " + id + ")"
            );
        }

        if (todoItem.getStatus() == newStatus) {
            logger.info("Item {} is already marked as {}", id, newStatus);
            return new TodoResponse(todoItem);
        }

        todoItem.setStatus(newStatus);
        todoItem.setDoneDateTime(newStatus == TodoItem.Status.DONE ? LocalDateTime.now() : null);

        TodoItem updatedItem = todoRepository.save(todoItem);

        logger.debug("Updated todo item id: {} to status {}", id, newStatus);
        return new TodoResponse(updatedItem);
    }

    private TodoItem findTodoItemOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Todo item not found with id: {}", id);
                    return new ResourceNotFoundException("Todo item not found with id: " + id);
                });
    }
}
