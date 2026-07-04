package com.campustrade.service;

import com.campustrade.document.ProductDocument;
import com.campustrade.entity.Product;
import com.campustrade.repository.ProductSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations esOps;

    public ProductSearchService(ProductSearchRepository searchRepository, ElasticsearchOperations esOps) {
        this.searchRepository = searchRepository;
        this.esOps = esOps;
    }

    /**
     * Search products by keyword with ES. Returns null on failure so callers can fall back to MySQL.
     */
    public Page<ProductDocument> search(String keyword, int page, int size) {
        try {
            Criteria criteria = new Criteria("title").matches(keyword)
                    .or(new Criteria("description").matches(keyword));
            Query query = new CriteriaQuery(criteria)
                    .addCriteria(new Criteria("status").is("AVAILABLE"));
            query.setPageable(PageRequest.of(page - 1, size));
            SearchHits<ProductDocument> hits = esOps.search(query, ProductDocument.class);
            List<ProductDocument> docs = hits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
            return new PageImpl<>(docs, PageRequest.of(page - 1, size), hits.getTotalHits());
        } catch (Exception e) {
            log.warn("ES search failed, falling back to MySQL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Index a product document from a Product entity. Failures are logged but never thrown.
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument doc = toDocument(product);
            searchRepository.save(doc);
            log.debug("Indexed product {}", product.getId());
        } catch (Exception e) {
            log.warn("Failed to index product {}: {}", product.getId(), e.getMessage());
        }
    }

    /**
     * Delete a product from the ES index. Failures are logged but never thrown.
     */
    public void deleteIndex(Long productId) {
        try {
            searchRepository.deleteById(productId);
            log.debug("Deleted index for product {}", productId);
        } catch (Exception e) {
            log.warn("Failed to delete index {}: {}", productId, e.getMessage());
        }
    }

    private ProductDocument toDocument(Product p) {
        ProductDocument doc = new ProductDocument();
        doc.setId(p.getId());
        doc.setTitle(p.getTitle());
        doc.setDescription(p.getDescription());
        doc.setPrice(p.getPrice());
        doc.setCondition(p.getCondition());
        doc.setCategory(p.getCategory());
        doc.setStatus(p.getStatus());
        doc.setImageUrl(p.getImageUrl());
        doc.setUserId(p.getUserId());
        doc.setCreatedAt(p.getCreatedAt());
        return doc;
    }
}
