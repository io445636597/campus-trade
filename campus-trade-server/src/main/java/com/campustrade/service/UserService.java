package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.dto.LoginResponse;
import com.campustrade.dto.RegisterRequest;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;

public interface UserService {

    /**
     * Register a new user
     */
    User register(RegisterRequest request);

    /**
     * Login with username, password and client IP
     */
    LoginResponse login(String username, String password, String ip);

    /**
     * Get current login user info
     */
    User getCurrentUser();

    /**
     * Update current user profile
     */
    User updateCurrentUser(User user);

    /**
     * Get user public info by ID
     */
    User getUserById(Long id);

    /**
     * Get products by user ID (paginated)
     */
    PageResult<Product> getUserProducts(Long userId, Long page, Long size);
}
