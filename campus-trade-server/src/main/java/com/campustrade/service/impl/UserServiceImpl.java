package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campustrade.common.BusinessException;
import com.campustrade.common.PageResult;
import com.campustrade.dto.LoginResponse;
import com.campustrade.dto.RegisterRequest;
import com.campustrade.entity.Product;
import com.campustrade.entity.User;
import com.campustrade.mapper.ProductMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.JwtUtils;
import com.campustrade.security.LoginUser;
import com.campustrade.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserServiceImpl(UserMapper userMapper,
                           ProductMapper productMapper,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        // Check duplicate username
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        // Check duplicate email
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) > 0) {
            throw new BusinessException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone("");
        user.setAvatarUrl("");
        user.setSchool("");
        user.setBio("");

        userMapper.insert(user);
        return user;
    }

    @Override
    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId());
        return new LoginResponse(token, user);
    }

    @Override
    public User getCurrentUser() {
        Long userId = LoginUser.getRequiredUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public User updateCurrentUser(User userUpdate) {
        Long userId = LoginUser.getRequiredUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // Only update non-null fields
        if (userUpdate.getPhone() != null) {
            user.setPhone(userUpdate.getPhone());
        }
        if (userUpdate.getAvatarUrl() != null) {
            user.setAvatarUrl(userUpdate.getAvatarUrl());
        }
        if (userUpdate.getSchool() != null) {
            user.setSchool(userUpdate.getSchool());
        }
        if (userUpdate.getBio() != null) {
            user.setBio(userUpdate.getBio());
        }
        if (userUpdate.getEmail() != null) {
            // Check email uniqueness if changing
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, userUpdate.getEmail())
                            .ne(User::getId, userId));
            if (existing != null) {
                throw new BusinessException("邮箱已被使用");
            }
            user.setEmail(userUpdate.getEmail());
        }

        userMapper.updateById(user);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public PageResult<Product> getUserProducts(Long userId, Long pageNum, Long size) {
        // Verify user exists
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(404, "用户不存在");
        }

        Page<Product> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .eq(Product::getStatus, "AVAILABLE")
                .orderByDesc(Product::getCreatedAt);

        IPage<Product> result = productMapper.selectPage(page, wrapper);

        // Populate author info for each product
        List<Product> records = result.getRecords().stream().peek(p -> {
            User author = userMapper.selectById(p.getUserId());
            if (author != null) {
                p.setAuthor(author);
            }
        }).collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
}
