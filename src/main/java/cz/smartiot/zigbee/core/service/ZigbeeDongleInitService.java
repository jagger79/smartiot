package cz.smartiot.zigbee.core.service;

import com.zsmartsystems.zigbee.*;
import com.zsmartsystems.zigbee.app.basic.ZigBeeBasicServerExtension;
import com.zsmartsystems.zigbee.app.discovery.ZigBeeDiscoveryExtension;
import com.zsmartsystems.zigbee.app.iasclient.ZigBeeIasCieExtension;
import com.zsmartsystems.zigbee.app.otaserver.ZigBeeOtaUpgradeExtension;
import com.zsmartsystems.zigbee.console.main.EmberNcpHardwareReset;
import com.zsmartsystems.zigbee.console.main.ZigBeeDataStore;
import com.zsmartsystems.zigbee.dongle.ember.EmberSerialProtocol;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EzspConfigId;
import com.zsmartsystems.zigbee.security.ZigBeeKey;
import com.zsmartsystems.zigbee.serial.ZigBeeSerialPort;
import com.zsmartsystems.zigbee.serialization.DefaultDeserializer;
import com.zsmartsystems.zigbee.serialization.DefaultSerializer;
import com.zsmartsystems.zigbee.transport.*;
import jssc.SerialPort;
import jssc.SerialPortList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static jssc.SerialPort.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZigbeeDongleInitService {
    private final GenericWebApplicationContext context;
    private final ObjectProvider<ZigBeeDongleEzsp> dongleProvider;

    public HashSet<String> getSerialPorts() {
        var ports = new HashSet<String>();
        //-d EMBER -p /dev/ttyUSB0 -reset true -f hardware -channel 10 -baud 128000 -pan 6734 -epan 23232323

        for (String portName : SerialPortList.getPortNames()) {
            var port = new SerialPort(portName);
            try {
                port.openPort();
                port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE);
                log.info("opened,port={}", portName);
                MyPortListener listener = new MyPortListener(port);
                port.addEventListener(listener);
                var ret = port.writeBytes(new byte[]{1, 10, 12, 0, 3, 8, 11, 12, 7, 14});
                log.info("written,port={},succeeded={}", portName, ret);

                try {
                    synchronized (listener) {
                        listener.wait(10 * 1000);
                    }
                } catch (Exception e) {
                    log.error("", e);
                }

                try {
                    port.closePort();
                } catch (Exception ex) {
                    log.error("", ex);
                }
                ports.add(portName);
            } catch (Exception e) {
                try {
                    port.closePort();
                } catch (Exception ex) {
                    log.error("", ex);
                }
            }
        }
        return ports;
    }

    public Map<String, String> initializeDongle(String portName) {
        var serialPort = new ZigBeeSerialPort(portName, SerialPort.BAUDRATE_128000, ZigBeePort.FlowControl.FLOWCONTROL_OUT_XONOFF);

        final ZigBeeDongleEzsp dongleEzsp = ezspDongleTransport(serialPort);
        log.info("networkKey={}", dongleEzsp.setZigBeeNetworkKey(ZigBeeKey.createRandom()));
        log.info("channel={}", dongleEzsp.setZigBeeChannel(ZigBeeChannel.create(23)));

        log.info("registering bean,port={}", portName);
        try {
            context.registerBean(ZigBeeDongleEzsp.class, () -> dongleEzsp, bd -> {
                bd.setDestroyMethodName("shutdown");
            });
        } catch (Exception e) {
            return Map.of("dongle", "initialized",
                    "registration", "failed: " + e.getMessage());
        }
        return Map.of("dongle", "initialized");
    }

    private ZigBeeDongleEzsp ezspDongleTransport(ZigBeeSerialPort serialPort) {
        var dongle = new ZigBeeDongleEzsp(serialPort, EmberSerialProtocol.ASH2);
        dongle.updateDefaultConfiguration(EzspConfigId.EZSP_CONFIG_ADDRESS_TABLE_SIZE, 16);
        dongle.updateDefaultConfiguration(EzspConfigId.EZSP_CONFIG_SOURCE_ROUTE_TABLE_SIZE, 100);
        dongle.updateDefaultConfiguration(EzspConfigId.EZSP_CONFIG_APS_UNICAST_MESSAGE_COUNT, 16);
        dongle.updateDefaultConfiguration(EzspConfigId.EZSP_CONFIG_NEIGHBOR_TABLE_SIZE, 24);
        dongle.setEmberNcpResetProvider(new EmberNcpHardwareReset());
        return dongle;
    }

    public Object startNetworkManager() {
        String networkId = "ostravar";
        ZigBeeDongleEzsp dongle;
        try {
            dongle = dongleProvider.getObject();
        } catch (Exception ex) {
            return Map.of("dongle", "not-registered");
        }

        var dataStore = new ZigBeeDataStore(networkId);
        var networkManager = new ZigBeeNetworkManager(dongle);
        networkManager.setNetworkDataStore(dataStore);
        networkManager.setSerializer(DefaultSerializer.class, DefaultDeserializer.class);

        ZigBeeStatus initResponse = networkManager.initialize();
        if (initResponse != ZigBeeStatus.SUCCESS) {
            throw new RuntimeException("Error initializing ZigBeeNetworkManager");
        }

        log.info("recreating current...,networkKey={},PAN ID={},Extended PAN ID={},Channel={},ezsp.firmware={},{},{}",
                networkManager.getZigBeeNetworkKey(), networkManager.getZigBeePanId(),
                networkManager.getZigBeeExtendedPanId(), networkManager.getZigBeeChannel(),
                dongle.getFirmwareVersion(), dongle.getIeeeAddress(), dongle.getZigBeeChannel());

        networkManager.setZigBeeChannel(ZigBeeChannel.create(23));
        networkManager.setZigBeePanId(ThreadLocalRandom.current().nextInt(1, 0x10000));
        networkManager.setZigBeeExtendedPanId(ExtendedPanId.createRandom());
        networkManager.setZigBeeNetworkKey(ZigBeeKey.createRandom());
        networkManager.setZigBeeLinkKey(new ZigBeeKey(new int[]{0x5A, 0x69, 0x67, 0x42, 0x65, 0x65, 0x41, 0x6C, 0x6C, 0x69, 0x61,
                0x6E, 0x63, 0x65, 0x30, 0x39}));
        networkManager.setDefaultProfileId(ZigBeeProfileType.ZIGBEE_HOME_AUTOMATION.getKey());

        // Configure the concentrator
        // Max Hops defaults to system max
        var concentratorConfig = new ConcentratorConfig();
        concentratorConfig.setType(ConcentratorType.LOW_RAM);
        concentratorConfig.setMaxFailures(8);
        concentratorConfig.setMaxHops(0);
        concentratorConfig.setRefreshMinimum(60);
        concentratorConfig.setRefreshMaximum(3600);

        final var transportCfg = new TransportConfig();
        transportCfg.addOption(TransportConfigOption.RADIO_TX_POWER, 4);
        transportCfg.addOption(TransportConfigOption.CONCENTRATOR_CONFIG, concentratorConfig);

        log.info("recreated,PAN ID={},Extended PAN ID={},Channel={}", networkManager.getZigBeePanId(),
                networkManager.getZigBeeExtendedPanId(), networkManager.getZigBeeChannel());

        transportCfg.addOption(TransportConfigOption.TRUST_CENTRE_JOIN_MODE, TrustCentreJoinMode.TC_JOIN_SECURE);

        // Add the default ZigBeeAlliance09 HA link key

        transportCfg.addOption(TransportConfigOption.TRUST_CENTRE_LINK_KEY, new ZigBeeKey(new int[]{0x5A, 0x69,
                0x67, 0x42, 0x65, 0x65, 0x41, 0x6C, 0x6C, 0x69, 0x61, 0x6E, 0x63, 0x65, 0x30, 0x39}));
        // transportCfg.addOption(TransportConfigOption.TRUST_CENTRE_LINK_KEY, new ZigBeeKey(new int[] { 0x41, 0x61,
        // 0x8F, 0xC0, 0xC8, 0x3B, 0x0E, 0x14, 0xA5, 0x89, 0x95, 0x4B, 0x16, 0xE3, 0x14, 0x66 }));

        //ezspDongleTransport.updateTransportConfig(transportCfg);

        // Add the extensions to the network
        networkManager.addExtension(new ZigBeeIasCieExtension());
        networkManager.addExtension(new ZigBeeOtaUpgradeExtension());
        networkManager.addExtension(new ZigBeeBasicServerExtension());

        var discoveryExtension = new ZigBeeDiscoveryExtension();
        discoveryExtension.setUpdateMeshPeriod(0);
        discoveryExtension.setUpdateOnChange(false);
        //networkManager.addExtension(discoveryExtension);

        var reinitialize = true;
        ZigBeeStatus status = networkManager.startup(reinitialize);
        log.info("ZigBee network,status={}", status);

        log.info("initialized,linkKey={},networkKey={},PAN ID={},Extended PAN ID={},Channel={},ezsp.firmware={},ieee={}",
                networkManager.getZigBeeLinkKey(), networkManager.getZigBeeNetworkKey(), networkManager.getZigBeePanId(),
                networkManager.getZigBeeExtendedPanId(), networkManager.getZigBeeChannel(),
                dongle.getFirmwareVersion(), dongle.getIeeeAddress());
        var attrs = new HashMap<>();
        attrs.put("linkKey", networkManager.getZigBeeLinkKey());
        attrs.put("networkKey", networkManager.getZigBeeNetworkKey());
        attrs.put("zigbee.networkKey", dongle.getZigBeeNetworkKey());
        attrs.put("PAN_ID", networkManager.getZigBeePanId());
        attrs.put("Extended_PAN_ID", networkManager.getZigBeeExtendedPanId());
        attrs.put("Channel", networkManager.getZigBeeChannel());
            attrs.put("dongle.version", dongle.getVersionString());
        attrs.put("IeeeAddress", dongle.getIeeeAddress());
        return Map.of("network", "initialized",
                "status", status,
                "attrs", attrs);
    }
}
