package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.entity.Product;

public interface BookmarkService {

    /**
     * Toggle bookmark: bookmark if not bookmarked, un-bookmark if already bookmarked.
     * Returns true if now bookmarked, false if now un-bookmarked.
     */
    boolean toggleBookmark(Long productId);

    /**
     * Get current user's bookmarked products (paginated)
     */
    PageResult<Product> getMyBookmarks(Long page, Long size);
}
