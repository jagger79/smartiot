package cz.smartiot.zigbee.core.service;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * In this class must implement the method serialEvent, through it we learn about
 * events that happened to our port. But we will not report on all events but only
 * those that we put in the mask. In this case the arrival of the data and change the
 * status lines CTS and DSR
 */
@Slf4j
@RequiredArgsConstructor
public class MyPortListener implements SerialPortEventListener {
    private final SerialPort port;

    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) { // data is available
            // read data, if 10 bytes available
            if (event.getEventValue() == 10) {
                try {
                    byte[] buffer = port.readBytes(10);
                } catch (SerialPortException ex) {
                    log.error("", ex);
                }
            }
        } else if (event.isCTS()) { // CTS line has changed state
            if (event.getEventValue() == 1) { // line is ON
                System.out.println("CTS - ON");
            } else {
                System.out.println("CTS - OFF");
            }
        } else if (event.isDSR()) { // DSR line has changed state
            if (event.getEventValue() == 1) { // line is ON
                System.out.println("DSR - ON");
            } else {
                System.out.println("DSR - OFF");
            }
        }
        log.info("listener,received,value={},type={}", String.format("%02X", event.getEventValue()), event.getEventType());
        synchronized (this) {
            notifyAll();
        }
    }
}
