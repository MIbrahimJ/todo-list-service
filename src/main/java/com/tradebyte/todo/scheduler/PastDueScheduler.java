package com.tradebyte.todo.scheduler;

import com.tradebyte.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PastDueScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PastDueScheduler.class);

    private final TodoService todoService;

    public PastDueScheduler(TodoService todoService) {
        this.todoService = todoService;
    }

    @Scheduled(fixedRate = 60000)
    public void updatePastDueItems() {
        logger.debug("Running scheduled past due items update");
        try {
            todoService.updatePastDueItems();
        } catch (Exception e) {
            logger.error("Error updating past due items: {}", e.getMessage(), e);
        }
    }
}
