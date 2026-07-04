package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.service.BookmarkService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/api/product/{id}/bookmark")
    public Result<Map<String, Boolean>> toggleBookmark(@PathVariable Long id) {
        boolean bookmarked = bookmarkService.toggleBookmark(id);
        return Result.success(Map.of("bookmarked", bookmarked));
    }

    @GetMapping("/api/user/bookmarks")
    public Result<?> getMyBookmarks(@RequestParam(defaultValue = "1") Long page,
                                    @RequestParam(defaultValue = "12") Long size) {
        return Result.success(bookmarkService.getMyBookmarks(page, size));
    }
}
