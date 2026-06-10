package com.dy.liveauction.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页返回体
 */
@Data
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}
