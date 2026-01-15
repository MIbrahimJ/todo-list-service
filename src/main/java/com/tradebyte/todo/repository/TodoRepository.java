package com.tradebyte.todo.repository;

import com.tradebyte.todo.entity.TodoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    Page<TodoItem> findByStatus(TodoItem.Status status, Pageable pageable);

    @Query("SELECT t FROM TodoItem t WHERE t.status = 'NOT_DONE' AND t.dueDateTime < :currentDateTime")
    List<TodoItem> findPastDueNotDoneItems(@Param("currentDateTime") LocalDateTime currentDateTime);

}
