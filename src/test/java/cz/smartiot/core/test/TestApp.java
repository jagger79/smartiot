package cz.smartiot.core.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApp.class)
@Slf4j
public class TestApp {
    private int[] buffer = new int[220];

    @Test
    void test() {
        int val = 0xFFEF;
        buffer[0] = val & 0xFF;
        buffer[1] = (val >> 8) & 0xFF;

        log.info("buffer[0]={}, buffer[1]={}", buffer[0], buffer[1]);

        var init1 = "1A C0 38BC 7E";
        var b = new byte[]{0x79};
        log.info("{},{},{},{}", 0x1A, 0x1A & 0xFF, 0xC0, 0xC0 & 0xFF);
    }
}
