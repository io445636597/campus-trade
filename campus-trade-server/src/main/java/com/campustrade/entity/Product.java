package com.campustrade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    @TableField("`condition`")
    private String condition;

    private String category;

    private String imageUrl;

    private String status;

    private Integer viewCount;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private User author;

    @TableField(exist = false)
    private Long bookmarkCount;

    @TableField(exist = false)
    private Long messageCount;

    @TableField(exist = false)
    private Boolean isBookmarked;
}
