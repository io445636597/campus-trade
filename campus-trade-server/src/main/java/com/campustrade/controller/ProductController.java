package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.ProductQuery;
import com.campustrade.dto.ProductRequest;
import com.campustrade.entity.Product;
import com.campustrade.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<?> listProducts(ProductQuery query) {
        return Result.success(productService.listProducts(query));
    }

    @GetMapping("/search")
    public Result<?> searchProducts(@RequestParam String keyword,
                                    @RequestParam(defaultValue = "1") Long page,
                                    @RequestParam(defaultValue = "12") Long size) {
        return Result.success(productService.searchProducts(keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @PostMapping
    public Result<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = toProduct(request);
        return Result.success(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    public Result<Product> updateProduct(@PathVariable Long id,
                                         @Valid @RequestBody ProductRequest request) {
        Product product = toProduct(request);
        return Result.success(productService.updateProduct(id, product));
    }

    @PatchMapping("/{id}/status")
    public Result<Product> updateStatus(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return Result.success(productService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    private Product toProduct(ProductRequest request) {
        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCondition(request.getCondition());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        return product;
    }
}
