package cz.smartiot.zigbee.core;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties("service.smartiot")
@Validated
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class SmartIoTProperties implements InitializingBean {
    /**
     * Publisher configuration
     */
    @NestedConfigurationProperty
    @Valid
    final PublisherConfig publisher = new PublisherConfig();

    @PostConstruct
    public void initialize() {
        log.info("Initializing SmartIoT properties");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initializing SmartIoT properties,{}", this);
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PublisherConfig {
        @NotNull
        Boolean enabled = true;
    }
}
