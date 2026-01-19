package com.japanesestudy.app.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

public class AuthDtos {
    
    @Getter
    public static class JwtResponse {
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

    @Getter
    @Setter
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter
    @Setter
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        private String username;
        
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        private String password;
    }
}
