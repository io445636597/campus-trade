package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.document.ProductDocument;
import com.campustrade.dto.ProductQuery;
import com.campustrade.dto.ProductRequest;
import com.campustrade.entity.Product;
import com.campustrade.service.FileUploadService;
import com.campustrade.service.ProductCacheService;
import com.campustrade.service.ProductSearchService;
import com.campustrade.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final FileUploadService fileUploadService;
    private final ProductCacheService productCacheService;
    private final ProductSearchService productSearchService;

    public ProductController(ProductService productService,
                             FileUploadService fileUploadService,
                             ProductCacheService productCacheService,
                             ProductSearchService productSearchService) {
        this.productService = productService;
        this.fileUploadService = fileUploadService;
        this.productCacheService = productCacheService;
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public Result<?> listProducts(ProductQuery query) {
        return Result.success(productService.listProducts(query));
    }

    @GetMapping("/search")
    public Result<?> searchProducts(@RequestParam String keyword,
                                    @RequestParam(defaultValue = "1") Long page,
                                    @RequestParam(defaultValue = "12") Long size) {
        // Try Elasticsearch first
        Page<ProductDocument> esResult = productSearchService.search(keyword, page.intValue(), size.intValue());
        if (esResult != null) {
            List<ProductDocument> docs = esResult.getContent();
            List<Product> products = docs.stream()
                    .map(doc -> {
                        try {
                            return productService.getProductById(doc.getId());
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return Result.success(PageResult.of(products, esResult.getTotalElements(), page, size));
        }
        // Fallback to MySQL search
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

    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过5MB");
        }
        String url = fileUploadService.uploadImage(file);
        return Result.success(Map.of("url", url));
    }

    @GetMapping("/hot")
    public Result<List<Product>> getHotProducts() {
        List<String> hotIds = productCacheService.getHotProducts(20);
        List<Product> hotProducts = new ArrayList<>();
        for (String idStr : hotIds) {
            try {
                Long id = Long.parseLong(idStr);
                Product product = productService.getProductById(id);
                hotProducts.add(product);
            } catch (Exception e) {
                // Skip invalid/missing products
            }
        }
        return Result.success(hotProducts);
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
