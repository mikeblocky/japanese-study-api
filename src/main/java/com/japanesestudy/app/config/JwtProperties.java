package com.japanesestudy.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtProperties {

    @NotBlank(message = "jwt.secret must not be blank")
    private String secret;

    @Min(value = 1000, message = "jwt.expiration must be >= 1000ms")
    private int expiration;
}
