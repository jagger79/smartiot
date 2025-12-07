package cz.smartiot.zigbee.core.controller;

import cz.smartiot.zigbee.core.service.ZigbeeDongleInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ZigbeeController {
    private final ZigbeeDongleInitService service;

    @RequestMapping("serial")
    public ResponseEntity<Object> serial() throws Exception {
        return ResponseEntity.ok(service.getSerialPorts());
    }

    @RequestMapping("dongle")
    public ResponseEntity<Object> dongle(@RequestParam("port") String s) throws Exception {
        return ResponseEntity.ok(service.initializeDongle(s));
    }

    @RequestMapping("network")
    public ResponseEntity<Object> network() throws Exception {
        return ResponseEntity.ok(service.startNetworkManager());
    }
}

