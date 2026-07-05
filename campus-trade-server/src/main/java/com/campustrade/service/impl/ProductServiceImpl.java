package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.BusinessException;
import com.campustrade.common.PageResult;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Bookmark;
import com.campustrade.entity.Message;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.mapper.BookmarkMapper;
import com.campustrade.mapper.MessageMapper;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.LoginUser;
import com.campustrade.service.ProductCacheService;
import com.campustrade.service.ProductService;
import com.campustrade.service.ViewCountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final BookmarkMapper bookmarkMapper;
    private final MessageMapper messageMapper;
    private final ProductCacheService cacheService;
    private final ViewCountService viewCountService;
    public ProductServiceImpl(ProductMapper productMapper,
                               UserMapper userMapper,
                               BookmarkMapper bookmarkMapper,
                               MessageMapper messageMapper,
                               ProductCacheService cacheService,
                               ViewCountService viewCountService) {
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.bookmarkMapper = bookmarkMapper;
        this.messageMapper = messageMapper;
        this.cacheService = cacheService;
        this.viewCountService = viewCountService;
    }

    @Override
    public PageResult<Product> listProducts(ProductQuery query) {
        Page<Product> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        // Only AVAILABLE products
        wrapper.eq(Product::getStatus, "AVAILABLE");

        // Category filter
        wrapper.eq(query.getCategory() != null && !query.getCategory().isEmpty(),
                Product::getCategory, query.getCategory());

        // Condition filter
        wrapper.eq(query.getCondition() != null && !query.getCondition().isEmpty(),
                Product::getCondition, query.getCondition());

        // Keyword search
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w
                    .like(Product::getTitle, query.getKeyword())
                    .or()
                    .like(Product::getDescription, query.getKeyword()));
        }

        // Sort
        String sort = query.getSort();
        if ("cheapest".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("expensive".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }

        IPage<Product> result = productMapper.selectPage(page, wrapper);

        // Fill author info
        List<Product> records = result.getRecords().stream().peek(p -> {
            User author = userMapper.selectById(p.getUserId());
            if (author != null) {
                p.setAuthor(author);
            }
        }).collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public Product getProductById(Long id) {
        Product product = fetchAndFillProduct(id);
        // Increment view count — only for actual detail page visits
        viewCountService.incrementView(id);
        cacheService.incrementViewRank(id);
        return product;
    }

    @Override
    @Transactional
    public Product getProductByIdReadOnly(Long id) {
        // Same as getProductById but WITHOUT view counting side effects
        return fetchAndFillProduct(id);
    }

    /**
     * Shared logic: fetch product (cache first, then DB), fill transient fields.
     */
    private Product fetchAndFillProduct(Long id) {
        // Try cache first
        Product product = cacheService.getProductDetail(id);
        boolean fromCache = (product != null);

        if (!fromCache) {
            // Cache miss, query DB
            product = productMapper.selectById(id);
            if (product == null) {
                throw new BusinessException(404, "商品不存在");
            }
            // Cache the raw product (before filling transient fields)
            cacheService.cacheProductDetail(product);
        }

        // Merge DB view_count with Redis buffer for accurate display
        int redisViews = viewCountService.getViewCount(id);
        product.setViewCount((product.getViewCount() == null ? 0 : product.getViewCount()) + redisViews);

        // Fill author
        User author = userMapper.selectById(product.getUserId());
        if (author != null) {
            product.setAuthor(author);
        }

        // Fill bookmark count
        Long bookmarkCount = bookmarkMapper.selectCount(
                new LambdaQueryWrapper<Bookmark>()
                        .eq(Bookmark::getProductId, id));
        product.setBookmarkCount(bookmarkCount);

        // Fill message count
        Long messageCount = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getProductId, id));
        product.setMessageCount(messageCount);

        // Fill isBookmarked
        Long currentUserId = LoginUser.getUserId();
        if (currentUserId != null) {
            Long count = bookmarkMapper.selectCount(
                    new LambdaQueryWrapper<Bookmark>()
                            .eq(Bookmark::getProductId, id)
                            .eq(Bookmark::getUserId, currentUserId));
            product.setIsBookmarked(count > 0);
        } else {
            product.setIsBookmarked(false);
        }

        return product;
    }

    @Override
    @Transactional
    public Product createProduct(Product product) {
        product.setUserId(LoginUser.getRequiredUserId());
        product.setStatus("AVAILABLE");
        product.setViewCount(0);
        productMapper.insert(product);
        // Cache the new product
        cacheService.cacheProductDetail(product);
        // Sync to Elasticsearch
        return product;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product productUpdate) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!existing.getUserId().equals(LoginUser.getRequiredUserId())) {
            throw new BusinessException(403, "无权修改他人商品");
        }

        if (productUpdate.getTitle() != null) {
            existing.setTitle(productUpdate.getTitle());
        }
        if (productUpdate.getDescription() != null) {
            existing.setDescription(productUpdate.getDescription());
        }
        if (productUpdate.getPrice() != null) {
            existing.setPrice(productUpdate.getPrice());
        }
        if (productUpdate.getCondition() != null) {
            existing.setCondition(productUpdate.getCondition());
        }
        if (productUpdate.getCategory() != null) {
            existing.setCategory(productUpdate.getCategory());
        }
        if (productUpdate.getImageUrl() != null) {
            existing.setImageUrl(productUpdate.getImageUrl());
        }

        productMapper.updateById(existing);
        // Evict cache after update
        cacheService.evictProduct(id);
        // Sync to Elasticsearch
                return existing;
    }

    @Override
    @Transactional
    public Product updateStatus(Long id, String status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!product.getUserId().equals(LoginUser.getRequiredUserId())) {
            throw new BusinessException(403, "无权修改他人商品");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        // Sync status change to Elasticsearch
        return product;
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        if (!product.getUserId().equals(LoginUser.getRequiredUserId())) {
            throw new BusinessException(403, "无权删除他人商品");
        }
        productMapper.deleteById(id);
        // Evict cache after delete
        cacheService.evictProduct(id);
        // Remove from Elasticsearch
    }

    @Override
    public PageResult<Product> searchProducts(String keyword, Long pageNum, Long size) {
        Page<Product> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "AVAILABLE")
                .and(w -> w
                        .like(Product::getTitle, keyword)
                        .or()
                        .like(Product::getDescription, keyword))
                .orderByDesc(Product::getCreatedAt);

        IPage<Product> result = productMapper.selectPage(page, wrapper);

        List<Product> records = result.getRecords().stream().peek(p -> {
            User author = userMapper.selectById(p.getUserId());
            if (author != null) {
                p.setAuthor(author);
            }
        }).collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
}
