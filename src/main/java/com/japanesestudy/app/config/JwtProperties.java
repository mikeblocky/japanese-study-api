package com.japanesestudy.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "jwt")
@Validated
public class JwtProperties {

    /**
     * Base64-encoded secret for HS256 signing.
     */
    @NotBlank(message = "jwt.secret must not be blank")
    private String secret;

    /**
     * Expiration in milliseconds.
     */
    @Min(value = 1000, message = "jwt.expiration must be >= 1000ms")
    private int expiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpiration() {
        return expiration;
    }

    public void setExpiration(int expiration) {
        this.expiration = expiration;
    }
}
