package com.gepardec.zep.service;

public class PageContext {

    private static final ThreadLocal<Integer> PAGE_NUMBER = new ThreadLocal<>();

    public static void setPage(int page) {
        PAGE_NUMBER.set(page);
    }

    public static Integer getPage() {
        return PAGE_NUMBER.get();
    }

    public static void clear() {
        PAGE_NUMBER.remove();
    }
}
