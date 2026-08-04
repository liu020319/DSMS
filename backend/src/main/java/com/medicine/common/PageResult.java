package com.medicine.common;

import lombok.Data;

@Data
public class PageResult<T> {
    private java.util.List<T> records;
    private Long total;
    private Long current;
    private Long size;
    private Long pages;

    public PageResult() {}

    public PageResult(java.util.List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = (total + size - 1) / size;
    }
}
