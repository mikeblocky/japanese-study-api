package com.japanesestudy.app.dto.auth;

import lombok.Getter;

import java.util.List;

/**
 * JWT authentication response returned after successful login.
 */
@Getter
public class JwtResponse {
    private final String accessToken;
    private final String type = "Bearer";
    private final long id;
    private final String username;
    private final List<String> roles;

    public JwtResponse(String accessToken, long id, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }
}
