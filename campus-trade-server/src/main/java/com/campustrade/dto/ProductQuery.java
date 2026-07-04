package com.campustrade.dto;

import lombok.Data;

@Data
public class ProductQuery {

    private Long page = 1L;
    private Long size = 12L;
    private String category;
    private String condition;
    private String sort;
    private String keyword;
}
