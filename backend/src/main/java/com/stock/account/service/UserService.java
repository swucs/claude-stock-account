package com.stock.account.service;

import com.stock.account.dto.*;

public interface UserService {

    UserResponse signup(SignupRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    UserResponse getUser(Long userId);

    UserResponse updateUser(Long userId, UserUpdateRequest request);
}
