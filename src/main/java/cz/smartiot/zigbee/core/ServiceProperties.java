package cz.smartiot.zigbee.core;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This configuration properties are exposed as a bean to be used in SpEL injections.
 */
@Configuration
@Data
@ConfigurationProperties("service.common")
@Validated
public class ServiceProperties {
    /**
     * Common configuration for interface
     */
    @NestedConfigurationProperty
    @Valid
    private final ApiConfig api = new ApiConfig();

    /**
     * Exception mapping to http status
     */
    @NestedConfigurationProperty
    @Valid
    private final Map<@NotBlank String, @NotNull HttpStatus> errorMapping = new HashMap<>();

    public Optional<HttpStatus> getStatus(Throwable thr) {
        return Optional.ofNullable(getErrorMapping().get(thr.getClass().getSimpleName()));
    }

    @Data
    public static class ApiConfig {
        @NotBlank
        private String basePath = "/api";
    }
}