package com.backoffice.shared.domain;

import java.util.List;

public class PageResult<T> {

    private List<T> data;
    private int page;
    private int size;
    private long total;

    public PageResult(List<T> data, int page, int size, long total) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getData() { return data; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }

    public long getTotalPages() {
        return (long) Math.ceil((double) total / size);
    }

    public boolean isHasNext() {
        return page * size < total;
    }

    public boolean isHasPrevious() {
        return page > 0;
    }
}