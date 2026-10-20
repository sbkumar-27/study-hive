package com.studyhive.desktop.api;

import com.studyhive.desktop.model.AuthResponse;
import com.studyhive.desktop.model.LoginRequest;
import com.studyhive.desktop.model.RegisterRequest;
import com.studyhive.desktop.model.User;

public class AuthApi {

    private final ApiClient client;

    public AuthApi(ApiClient client) {
        this.client = client;
    }

    public AuthResponse register(RegisterRequest request) {
        return client.post("/auth/register", request, AuthResponse.class);
    }

    public AuthResponse login(LoginRequest request) {
        return client.post("/auth/login", request, AuthResponse.class);
    }

    public User me() {
        return client.get("/auth/me", User.class);
    }
}
