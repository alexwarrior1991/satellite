package com.alejandro.satellite.service;

import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.model.TelemetryPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for evaluating alerts based on telemetry data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    @Value("${app.telemetry.alerts.temperature.min:-50.0}")
    private double minTemperature;

    @Value("${app.telemetry.alerts.temperature.max:100.0}")
    private double maxTemperature;

    @Value("${app.telemetry.alerts.battery.critical:10.0}")
    private double criticalBatteryLevel;

    @Value("${app.telemetry.alerts.signal.min:-120.0}")
    private double minSignalStrength;

    /**
     * Evaluate alerts for a telemetry packet.
     * This method checks if the telemetry values are outside the acceptable ranges
     * and updates the device status accordingly.
     *
     * @param telemetryPacket the telemetry packet to evaluate
     * @return true if any alerts were triggered, false otherwise
     */
    public boolean evaluateAlerts(TelemetryPacket telemetryPacket) {
        boolean alertTriggered = false;

        // Check temperature
        if (telemetryPacket.getTemperature() != null) {
            if (telemetryPacket.getTemperature() < minTemperature || telemetryPacket.getTemperature() > maxTemperature) {
                log.warn("Temperature alert for device {}: {} (outside range [{}, {}])",
                        telemetryPacket.getDeviceId(), telemetryPacket.getTemperature(), minTemperature, maxTemperature);
                alertTriggered = true;
            }
        }

        // Check battery level
        if (telemetryPacket.getBatteryLevel() != null) {
            if (telemetryPacket.getBatteryLevel() < criticalBatteryLevel) {
                log.warn("Battery alert for device {}: {} (below critical level {})",
                        telemetryPacket.getDeviceId(), telemetryPacket.getBatteryLevel(), criticalBatteryLevel);
                telemetryPacket.setStatus(DeviceStatus.LOW_POWER);
                alertTriggered = true;
            }
        }

        // Check signal strength
        if (telemetryPacket.getSignalStrength() != null) {
            if (telemetryPacket.getSignalStrength() < minSignalStrength) {
                log.warn("Signal alert for device {}: {} (below minimum {})",
                        telemetryPacket.getDeviceId(), telemetryPacket.getSignalStrength(), minSignalStrength);
                alertTriggered = true;
            }
        }

        return alertTriggered;
    }
}