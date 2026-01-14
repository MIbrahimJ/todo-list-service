package com.tradebyte.todo.service;

import com.tradebyte.todo.dto.TodoRequest;
import com.tradebyte.todo.dto.TodoResponse;
import com.tradebyte.todo.dto.UpdateDescriptionRequest;
import com.tradebyte.todo.entity.TodoItem;
import com.tradebyte.todo.exception.ResourceNotFoundException;
import com.tradebyte.todo.exception.ValidationException;
import com.tradebyte.todo.repository.TodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional
    public TodoResponse createTodoItem(TodoRequest request) {
        logger.info("Creating new todo item with description: {}", request.getDescription());

        TodoItem todoItem = new TodoItem(request.getDescription(), request.getDueDateTime());
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
    public List<TodoResponse> getAllNotDoneItems(boolean includeAll) {
        logger.debug("Fetching todo items, includeAll: {}", includeAll);

        if (includeAll) {
            return todoRepository.findAll().stream()
                    .map(TodoResponse::new)
                    .collect(Collectors.toList());
        }

        return todoRepository.findByStatus(TodoItem.Status.NOT_DONE).stream()
                .map(TodoResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public TodoResponse updateDescription(Long id, UpdateDescriptionRequest request) {
        logger.info("Updating description for todo item id: {}", id);

        TodoItem todoItem = findTodoItemOrThrow(id);

        if (todoItem.isImmutable()) {
            throw new ValidationException("Cannot update a past due item");
        }

        todoItem.setDescription(request.getDescription());
        TodoItem updatedItem = todoRepository.save(todoItem);

        logger.debug("Updated description for todo item id: {}", id);
        return new TodoResponse(updatedItem);
    }

    @Transactional
    public TodoResponse markAsDone(Long id) {
        logger.info("Marking todo item as done, id: {}", id);

        TodoItem todoItem = findTodoItemOrThrow(id);

        if (todoItem.isImmutable()) {
            throw new ValidationException("Cannot mark a past due item as done");
        }

        todoItem.setStatus(TodoItem.Status.DONE);
        todoItem.setDoneDateTime(LocalDateTime.now());
        TodoItem updatedItem = todoRepository.save(todoItem);

        logger.debug("Marked todo item as done, id: {}", id);
        return new TodoResponse(updatedItem);
    }

    @Transactional
    public TodoResponse markAsNotDone(Long id) {
        logger.info("Marking todo item as not done, id: {}", id);

        TodoItem todoItem = findTodoItemOrThrow(id);

        if (todoItem.isImmutable()) {
            throw new ValidationException("Cannot mark a past due item as not done");
        }

        todoItem.setStatus(TodoItem.Status.NOT_DONE);
        todoItem.setDoneDateTime(null);
        TodoItem updatedItem = todoRepository.save(todoItem);

        logger.debug("Marked todo item as not done, id: {}", id);
        return new TodoResponse(updatedItem);
    }

    @Transactional
    public void updatePastDueItems() {
        logger.info("Updating past due items");

        LocalDateTime now = LocalDateTime.now();
        List<TodoItem> pastDueItems = todoRepository.findPastDueNotDoneItems(now);

        if (!pastDueItems.isEmpty()) {
            pastDueItems.forEach(item -> item.setStatus(TodoItem.Status.PAST_DUE));
            todoRepository.saveAll(pastDueItems);
            logger.info("Updated {} items to past due status", pastDueItems.size());
        }
    }

    private TodoItem findTodoItemOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Todo item not found with id: {}", id);
                    return new ResourceNotFoundException("Todo item not found with id: " + id);
                });
    }
}