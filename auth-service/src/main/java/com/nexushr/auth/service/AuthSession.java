package com.nexushr.auth.service;

import com.nexushr.auth.entity.User;

public record AuthSession(
    User user,
    String accessToken,
    String refreshToken,
    long accessTokenExpiresInSeconds,
    long refreshTokenExpiresInSeconds
) {}
