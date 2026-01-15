package com.tradebyte.todo.repository;

import com.tradebyte.todo.entity.TodoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    Page<TodoItem> findByStatus(TodoItem.Status status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE TodoItem t
        SET t.status = 'PAST_DUE'
        WHERE t.status = 'NOT_DONE'
          AND t.dueDateTime < :now
    """)
    int markPastDueItems(@Param("now") LocalDateTime now);

}
