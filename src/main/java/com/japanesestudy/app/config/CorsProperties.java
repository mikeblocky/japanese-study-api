package com.japanesestudy.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "cors")
@Validated
public class CorsProperties {

    /**
     * Comma-separated origins; may include '*'.
     */
    @NotBlank(message = "cors.allowed-origins must not be blank")
    private String allowedOrigins = "http://localhost:5173";

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
