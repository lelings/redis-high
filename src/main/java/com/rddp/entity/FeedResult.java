package com.rddp.entity;

import lombok.Data;

import java.util.List;

@Data
public class FeedResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
