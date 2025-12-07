package cz.smartiot.zigbee.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class SmartIoTApplication {
    static void main(String[] args) {
        new SpringApplicationBuilder(SmartIoTApplication.class)
                .run(args);
    }
}
