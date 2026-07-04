package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.BusinessException;
import com.campustrade.common.PageResult;
import com.campustrade.config.RabbitMQConfig;
import com.campustrade.dto.NotificationEvent;
import com.campustrade.entity.Bookmark;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.mapper.BookmarkMapper;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.LoginUser;
import com.campustrade.service.BookmarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkMapper bookmarkMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final RabbitTemplate rabbitTemplate;

    public BookmarkServiceImpl(BookmarkMapper bookmarkMapper,
                                ProductMapper productMapper,
                                UserMapper userMapper,
                                RabbitTemplate rabbitTemplate) {
        this.bookmarkMapper = bookmarkMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public boolean toggleBookmark(Long productId) {
        Long userId = LoginUser.getRequiredUserId();

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        Bookmark existing = bookmarkMapper.selectOne(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getUserId, userId)
                        .eq(Bookmark::getProductId, productId));

        if (existing != null) {
            bookmarkMapper.deleteById(existing.getId());
            return false;
        } else {
            Bookmark bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setProductId(productId);
            bookmarkMapper.insert(bookmark);

            try {
                NotificationEvent event = new NotificationEvent();
                event.setType("BOOKMARK");
                event.setFromUserId(userId);
                event.setToUserId(product.getUserId());
                event.setProductId(productId);
                event.setTimestamp(LocalDateTime.now());
                rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE, RabbitMQConfig.BOOKMARK_KEY, event);
            } catch (Exception e) {
                log.warn("Failed to send bookmark notification: {}", e.getMessage());
            }

            return true;
        }
    }

    @Override
    public PageResult<Product> getMyBookmarks(Long pageNum, Long size) {
        Long userId = LoginUser.getRequiredUserId();

        Page<Bookmark> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<Bookmark> wrapper = new LambdaQueryWrapper<Bookmark>()
                .eq(Bookmark::getUserId, userId)
                .orderByDesc(Bookmark::getCreatedAt);

        IPage<Bookmark> bookmarkPage = bookmarkMapper.selectPage(page, wrapper);

        List<Product> products = bookmarkPage.getRecords().stream().map(b -> {
            Product product = productMapper.selectById(b.getProductId());
            if (product != null) {
                User author = userMapper.selectById(product.getUserId());
                if (author != null) {
                    product.setAuthor(author);
                }
                product.setBookmarkCount(null);
            }
            return product;
        }).filter(p -> p != null).collect(Collectors.toList());

        return PageResult.of(products, bookmarkPage.getTotal(),
                bookmarkPage.getCurrent(), bookmarkPage.getSize());
    }
}
