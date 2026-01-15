package com.tradebyte.todo.scheduler;

import com.tradebyte.todo.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PastDueScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PastDueScheduler.class);

    private final TodoService todoService;

    @Value("${todo.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    public PastDueScheduler(TodoService todoService) {
        this.todoService = todoService;
        logger.info("PastDueScheduler bean created");
    }

    @Scheduled(fixedRateString = "${todo.scheduler.fixed-rate:60000}")
    public void updatePastDueItems() {

        if (!schedulerEnabled) {
            logger.debug("PastDueScheduler is disabled. Skipping execution.");
            return;
        }

        try {
            logger.debug("Executing PastDueScheduler bulk update");

            int updatedCount = todoService.updatePastDueItemsBulk();

            if (updatedCount > 0) {
                logger.info("Scheduler updated {} past due todo items", updatedCount);
            } else {
                logger.debug("Scheduler run completed with no updates");
            }

        } catch (Exception ex) {
            logger.error("Error during PastDueScheduler execution", ex);
        }
    }
}
