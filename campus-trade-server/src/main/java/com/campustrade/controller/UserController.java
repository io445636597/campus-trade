package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.dto.LoginRequest;
import com.campustrade.dto.LoginResponse;
import com.campustrade.dto.RegisterRequest;
import com.campustrade.entity.User;
import com.campustrade.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.getUsername(), request.getPassword());
        return Result.success(response);
    }

    @GetMapping("/me")
    public Result<User> getCurrentUser() {
        User user = userService.getCurrentUser();
        return Result.success(user);
    }

    @PutMapping("/me")
    public Result<User> updateCurrentUser(@RequestBody User user) {
        User updated = userService.updateCurrentUser(user);
        return Result.success(updated);
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @GetMapping("/{id}/products")
    public Result<?> getUserProducts(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") Long page,
                                     @RequestParam(defaultValue = "12") Long size) {
        return Result.success(userService.getUserProducts(id, page, size));
    }
}
