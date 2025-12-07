import jssc.*;
import lombok.extern.slf4j.Slf4j;

import static jssc.SerialPort.*;

@Slf4j
public class Test {
    static void main() throws Exception {

        for (String port : SerialPortList.getPortNames()) {
            log.info(port);
        }

        String portName = "/dev/ttyUSB0";
        var port = new SerialPort(portName);
        port.openPort();
        //port.setParams(BAUDRATE_9600, DATABITS_8, STOPBITS_1, PARITY_NONE);
        // port.setParams(9600, 8, 1, 0); // alternate technique
        port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE);
        port.addEventListener(new MyPortListener(port) /* defined below */);

        var init1 = "1A C0 38BC 7E";
        byte[] init = {0x1A, (byte) 0xC0, (byte) 0x38BC, 0x7E};
        port.writeBytes(init);
        log.info("{},opened={}", portName, port.isOpened());
        log.info("{},opened={}", portName, port.isOpened());

        byte[] buffer = port.readBytes(10);
        log.info("read={}", new String(buffer));

        int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;
        port.setEventsMask(mask);

        port.closePort();
        log.info("opened={}", port.isOpened());
    }
}

/**
 * In this class must implement the method serialEvent, through it we learn about
 * events that happened to our port. But we will not report on all events but only
 * those that we put in the mask. In this case the arrival of the data and change the
 * status lines CTS and DSR
 */
class MyPortListener implements SerialPortEventListener {
    SerialPort port;

    public MyPortListener(SerialPort port) {
        this.port = port;
    }

    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) { // data is available
            // read data, if 10 bytes available
            if (event.getEventValue() == 10) {
                try {
                    byte[] buffer = port.readBytes(10);
                } catch (SerialPortException ex) {
                    System.out.println(ex);
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
    }
}