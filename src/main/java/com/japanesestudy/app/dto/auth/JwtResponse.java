package com.japanesestudy.app.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * JWT authentication response returned after successful login.
 */
@Getter
@Setter
@NoArgsConstructor
public class JwtResponse {

    private String accessToken;
    private String type = "Bearer";
    private long id;
    private String username;
    private List<String> roles;

    public JwtResponse(String accessToken, long id, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.roles = roles;
    }

    public String getTokenType() {
        return type;
    }

    public void setTokenType(String tokenType) {
        this.type = tokenType;
    }
}
