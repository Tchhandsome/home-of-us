package com.homeofus.common.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分页查询结果。
 *
 * @param <T> 数据类型
 * @author tanchaohong
 */
@Getter
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;

    private long total;

    private int pageNo;

    private int pageSize;
}

