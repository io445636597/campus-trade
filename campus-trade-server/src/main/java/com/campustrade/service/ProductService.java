package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.dto.ProductQuery;
import com.campustrade.entity.Product;

public interface ProductService {

    /**
     * List products with pagination and filters
     */
    PageResult<Product> listProducts(ProductQuery query);

    /**
     * Get product detail (with author, bookmark count, message count, isBookmarked).
     * Increments view count and view rank — only for actual product detail visits.
     */
    Product getProductById(Long id);

    /**
     * Get product detail WITHOUT incrementing view count (for list/rank displays).
     */
    Product getProductByIdReadOnly(Long id);

    /**
     * Create a new product
     */
    Product createProduct(Product product);

    /**
     * Update a product (only the author)
     */
    Product updateProduct(Long id, Product product);

    /**
     * Update product status (AVAILABLE/SOLD/REMOVED) — only the author
     */
    Product updateStatus(Long id, String status);

    /**
     * Delete a product (only the author)
     */
    void deleteProduct(Long id);

    /**
     * Full-text search products
     */
    PageResult<Product> searchProducts(String keyword, Long page, Long size);
}
