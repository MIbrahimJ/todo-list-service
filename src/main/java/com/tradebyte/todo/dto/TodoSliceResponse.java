package com.tradebyte.todo.dto;

import java.util.List;

public record TodoSliceResponse<T>(
        List<T> content,
        int page,
        int size,
        boolean hasNext
) {}
