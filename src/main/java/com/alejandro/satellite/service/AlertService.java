package com.alejandro.satellite.service;

import com.alejandro.satellite.model.Alert;
import com.alejandro.satellite.model.Alert.AlertSeverity;
import com.alejandro.satellite.model.Alert.AlertType;
import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for evaluating and managing alerts based on telemetry data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

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
     * This method checks if the telemetry values are outside the acceptable ranges,
     * updates the device status accordingly, and creates alert records.
     *
     * @param telemetryPacket the telemetry packet to evaluate
     * @return true if any alerts were triggered, false otherwise
     */
    @Transactional
    public boolean evaluateAlerts(TelemetryPacket telemetryPacket) {
        boolean alertTriggered = false;

        // Check temperature
        if (telemetryPacket.getTemperature() != null) {
            if (telemetryPacket.getTemperature() < minTemperature || telemetryPacket.getTemperature() > maxTemperature) {
                String message = String.format("Temperature alert for device %s: %.2f (outside range [%.2f, %.2f])",
                        telemetryPacket.getDeviceId(), telemetryPacket.getTemperature(), minTemperature, maxTemperature);
                log.warn(message);

                // Create and save temperature alert
                Alert alert = Alert.builder()
                        .type(AlertType.TEMPERATURE)
                        .message(message)
                        .severity(telemetryPacket.getTemperature() > maxTemperature ? AlertSeverity.CRITICAL : AlertSeverity.WARNING)
                        .resolved(false)
                        .build();

                telemetryPacket.addAlert(alert);
                alertRepository.save(alert);

                alertTriggered = true;
            }
        }

        // Check battery level
        if (telemetryPacket.getBatteryLevel() != null) {
            if (telemetryPacket.getBatteryLevel() < criticalBatteryLevel) {
                String message = String.format("Battery alert for device %s: %.2f (below critical level %.2f)",
                        telemetryPacket.getDeviceId(), telemetryPacket.getBatteryLevel(), criticalBatteryLevel);
                log.warn(message);

                // Update device status
                telemetryPacket.setStatus(DeviceStatus.LOW_POWER);

                // Create and save battery alert
                Alert alert = Alert.builder()
                        .type(AlertType.BATTERY)
                        .message(message)
                        .severity(AlertSeverity.CRITICAL)
                        .resolved(false)
                        .build();

                telemetryPacket.addAlert(alert);
                alertRepository.save(alert);

                alertTriggered = true;
            }
        }

        // Check signal strength
        if (telemetryPacket.getSignalStrength() != null) {
            if (telemetryPacket.getSignalStrength() < minSignalStrength) {
                String message = String.format("Signal alert for device %s: %.2f (below minimum %.2f)",
                        telemetryPacket.getDeviceId(), telemetryPacket.getSignalStrength(), minSignalStrength);
                log.warn(message);

                // Create and save signal alert
                Alert alert = Alert.builder()
                        .type(AlertType.SIGNAL)
                        .message(message)
                        .severity(AlertSeverity.WARNING)
                        .resolved(false)
                        .build();

                telemetryPacket.addAlert(alert);
                alertRepository.save(alert);

                alertTriggered = true;
            }
        }

        // Check device status
        if (telemetryPacket.getStatus() == DeviceStatus.ERROR || 
            telemetryPacket.getStatus() == DeviceStatus.OFFLINE) {
            String message = String.format("Status alert for device %s: %s",
                    telemetryPacket.getDeviceId(), telemetryPacket.getStatus());
            log.warn(message);

            // Create and save status alert
            Alert alert = Alert.builder()
                    .type(AlertType.STATUS)
                    .message(message)
                    .severity(AlertSeverity.WARNING)
                    .resolved(false)
                    .build();

            telemetryPacket.addAlert(alert);
            alertRepository.save(alert);

            alertTriggered = true;
        }

        return alertTriggered;
    }

    /**
     * Get all alerts.
     *
     * @param pageable pagination information
     * @return a page of alerts
     */
    @Transactional(readOnly = true)
    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    /**
     * Get an alert by ID.
     *
     * @param id the alert ID
     * @return the alert, if found
     */
    @Transactional(readOnly = true)
    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * Get all alerts for a specific telemetry packet.
     *
     * @param telemetryPacketId the telemetry packet ID
     * @return a list of alerts
     */
    @Transactional(readOnly = true)
    public List<Alert> getAlertsForTelemetryPacket(Long telemetryPacketId) {
        return alertRepository.findByTelemetryPacketId(telemetryPacketId);
    }

    /**
     * Get all alerts for a specific device.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @Transactional(readOnly = true)
    public Page<Alert> getAlertsForDevice(String deviceId, Pageable pageable) {
        return alertRepository.findByDeviceId(deviceId, pageable);
    }

    /**
     * Get all alerts for a specific sensor.
     *
     * @param sensorId the sensor ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @Transactional(readOnly = true)
    public Page<Alert> getAlertsForSensor(Long sensorId, Pageable pageable) {
        return alertRepository.findBySensorId(sensorId, pageable);
    }

    /**
     * Get all unresolved alerts.
     *
     * @return a list of unresolved alerts
     */
    @Transactional(readOnly = true)
    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByResolvedFalse();
    }

    /**
     * Resolve an alert.
     *
     * @param alertId the alert ID
     * @return the resolved alert, if found
     */
    @Transactional
    public Optional<Alert> resolveAlert(Long alertId) {
        return alertRepository.findById(alertId)
                .map(alert -> {
                    alert.setResolved(true);
                    alert.setResolvedAt(Instant.now());
                    return alertRepository.save(alert);
                });
    }
}
