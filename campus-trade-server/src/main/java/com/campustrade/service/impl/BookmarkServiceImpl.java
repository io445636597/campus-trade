package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.BusinessException;
import com.campustrade.common.PageResult;
import com.campustrade.entity.Bookmark;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.mapper.BookmarkMapper;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.LoginUser;
import com.campustrade.service.BookmarkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkMapper bookmarkMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public BookmarkServiceImpl(BookmarkMapper bookmarkMapper,
                                ProductMapper productMapper,
                                UserMapper userMapper) {
        this.bookmarkMapper = bookmarkMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
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
