package com.alejandro.satellite.service;

import com.alejandro.satellite.model.Alert;
import com.alejandro.satellite.model.Alert.AlertSeverity;
import com.alejandro.satellite.model.Alert.AlertType;
import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for evaluating and managing alerts based on telemetry data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final SensorService sensorService;

    @Value("${app.telemetry.alerts.temperature.min:-50.0}")
    private double minTemperature;

    @Value("${app.telemetry.alerts.temperature.max:100.0}")
    private double maxTemperature;

    @Value("${app.telemetry.alerts.battery.critical:10.0}")
    private double criticalBatteryLevel;

    @Value("${app.telemetry.alerts.signal.min:-120.0}")
    private double minSignalStrength;

    @Value("${app.telemetry.alerts.suppression.time-minutes:30}")
    private int alertSuppressionTimeMinutes;

    @Value("${app.telemetry.alerts.suppression.count:5}")
    private int alertSuppressionCount;

    @Value("${app.telemetry.alerts.sensor-deactivation.enabled:true}")
    private boolean sensorDeactivationEnabled;

    // Cache to track alert occurrences by sensor ID and alert type
    // Key: sensorId_alertType, Value: AlertTracker
    private final Map<String, AlertTracker> alertTrackerCache = new ConcurrentHashMap<>();

    /**
     * Evaluate alerts for a telemetry packet.
     * This method checks if the telemetry values are outside the acceptable ranges,
     * updates the device status accordingly, and creates alert records.
     * It also suppresses alerts if they occur too frequently and can deactivate sensors.
     *
     * @param telemetryPacket the telemetry packet to evaluate
     * @return true if any alerts were triggered, false otherwise
     */
    @Transactional
    public boolean evaluateAlerts(TelemetryPacket telemetryPacket) {
        boolean alertTriggered = false;
        Sensor sensor = telemetryPacket.getSensor();

        // Skip alert evaluation if the sensor is not active
        if (sensor != null && !sensor.isActive()) {
            log.debug("Skipping alert evaluation for inactive sensor: {}", sensor.getId());
            return false;
        }

        // Check temperature
        if (telemetryPacket.getTemperature() != null) {
            if (telemetryPacket.getTemperature() < minTemperature || telemetryPacket.getTemperature() > maxTemperature) {
                alertTriggered = processTemperatureAlert(telemetryPacket);
            } else {
                // Temperature is normal, resolve any existing temperature alerts
                resolveAlertsWhenValuesNormal(sensor, telemetryPacket.getDeviceId(), AlertType.TEMPERATURE);
            }
        }

        // Check battery level
        if (telemetryPacket.getBatteryLevel() != null) {
            if (telemetryPacket.getBatteryLevel() < criticalBatteryLevel) {
                alertTriggered = processBatteryAlert(telemetryPacket) || alertTriggered;
            } else {
                // Battery level is normal, resolve any existing battery alerts
                resolveAlertsWhenValuesNormal(sensor, telemetryPacket.getDeviceId(), AlertType.BATTERY);
            }
        }

        // Check signal strength
        if (telemetryPacket.getSignalStrength() != null) {
            if (telemetryPacket.getSignalStrength() < minSignalStrength) {
                alertTriggered = processSignalAlert(telemetryPacket) || alertTriggered;
            } else {
                // Signal strength is normal, resolve any existing signal alerts
                resolveAlertsWhenValuesNormal(sensor, telemetryPacket.getDeviceId(), AlertType.SIGNAL);
            }
        }

        // Check device status
        if (telemetryPacket.getStatus() == DeviceStatus.ERROR || 
            telemetryPacket.getStatus() == DeviceStatus.OFFLINE) {
            alertTriggered = processStatusAlert(telemetryPacket) || alertTriggered;
        } else if (telemetryPacket.getStatus() != null) {
            // Device status is normal, resolve any existing status alerts
            resolveAlertsWhenValuesNormal(sensor, telemetryPacket.getDeviceId(), AlertType.STATUS);
        }

        return alertTriggered;
    }

    /**
     * Process a temperature alert, with suppression logic.
     */
    private boolean processTemperatureAlert(TelemetryPacket telemetryPacket) {
        Sensor sensor = telemetryPacket.getSensor();
        if (sensor == null) {
            return createTemperatureAlert(telemetryPacket);
        }

        String cacheKey = sensor.getId() + "_TEMPERATURE";
        AlertTracker tracker = alertTrackerCache.computeIfAbsent(cacheKey, k -> new AlertTracker());

        if (shouldSuppressAlert(tracker)) {
            log.info("Suppressing temperature alert for sensor {}", sensor.getId());
            return false;
        }

        tracker.incrementCount();
        tracker.setLastAlertTime(Instant.now());

        return createTemperatureAlert(telemetryPacket);
    }

    /**
     * Create a temperature alert.
     */
    private boolean createTemperatureAlert(TelemetryPacket telemetryPacket) {
        String message = String.format("Temperature alert for device %s: %.2f (outside range [%.2f, %.2f])",
                telemetryPacket.getDeviceId(), telemetryPacket.getTemperature(), minTemperature, maxTemperature);
        log.warn(message);

        Alert alert = Alert.builder()
                .type(AlertType.TEMPERATURE)
                .message(message)
                .severity(telemetryPacket.getTemperature() > maxTemperature ? AlertSeverity.CRITICAL : AlertSeverity.WARNING)
                .resolved(false)
                .build();

        telemetryPacket.addAlert(alert);
        alertRepository.save(alert);

        return true;
    }

    /**
     * Process a battery alert, with suppression logic and sensor deactivation.
     */
    private boolean processBatteryAlert(TelemetryPacket telemetryPacket) {
        Sensor sensor = telemetryPacket.getSensor();
        if (sensor == null) {
            return createBatteryAlert(telemetryPacket);
        }

        String cacheKey = sensor.getId() + "_BATTERY";
        AlertTracker tracker = alertTrackerCache.computeIfAbsent(cacheKey, k -> new AlertTracker());

        if (shouldSuppressAlert(tracker)) {
            log.info("Suppressing battery alert for sensor {}", sensor.getId());

            // Deactivate sensor if configured and alert count is high enough
            if (sensorDeactivationEnabled && tracker.getCount() >= alertSuppressionCount * 2) {
                log.warn("Deactivating sensor {} due to persistent battery alerts", sensor.getId());
                sensorService.deactivateSensor(sensor.getId());
                // Clear the tracker after deactivation
                alertTrackerCache.remove(cacheKey);
            }

            return false;
        }

        tracker.incrementCount();
        tracker.setLastAlertTime(Instant.now());

        return createBatteryAlert(telemetryPacket);
    }

    /**
     * Create a battery alert.
     */
    private boolean createBatteryAlert(TelemetryPacket telemetryPacket) {
        String message = String.format("Battery alert for device %s: %.2f (below critical level %.2f)",
                telemetryPacket.getDeviceId(), telemetryPacket.getBatteryLevel(), criticalBatteryLevel);
        log.warn(message);

        // Update device status
        telemetryPacket.setStatus(DeviceStatus.LOW_POWER);

        Alert alert = Alert.builder()
                .type(AlertType.BATTERY)
                .message(message)
                .severity(AlertSeverity.CRITICAL)
                .resolved(false)
                .build();

        telemetryPacket.addAlert(alert);
        alertRepository.save(alert);

        return true;
    }

    /**
     * Process a signal alert, with suppression logic.
     */
    private boolean processSignalAlert(TelemetryPacket telemetryPacket) {
        Sensor sensor = telemetryPacket.getSensor();
        if (sensor == null) {
            return createSignalAlert(telemetryPacket);
        }

        String cacheKey = sensor.getId() + "_SIGNAL";
        AlertTracker tracker = alertTrackerCache.computeIfAbsent(cacheKey, k -> new AlertTracker());

        if (shouldSuppressAlert(tracker)) {
            log.info("Suppressing signal alert for sensor {}", sensor.getId());
            return false;
        }

        tracker.incrementCount();
        tracker.setLastAlertTime(Instant.now());

        return createSignalAlert(telemetryPacket);
    }

    /**
     * Create a signal alert.
     */
    private boolean createSignalAlert(TelemetryPacket telemetryPacket) {
        String message = String.format("Signal alert for device %s: %.2f (below minimum %.2f)",
                telemetryPacket.getDeviceId(), telemetryPacket.getSignalStrength(), minSignalStrength);
        log.warn(message);

        Alert alert = Alert.builder()
                .type(AlertType.SIGNAL)
                .message(message)
                .severity(AlertSeverity.WARNING)
                .resolved(false)
                .build();

        telemetryPacket.addAlert(alert);
        alertRepository.save(alert);

        return true;
    }

    /**
     * Process a status alert, with suppression logic.
     */
    private boolean processStatusAlert(TelemetryPacket telemetryPacket) {
        Sensor sensor = telemetryPacket.getSensor();
        if (sensor == null) {
            return createStatusAlert(telemetryPacket);
        }

        String cacheKey = sensor.getId() + "_STATUS";
        AlertTracker tracker = alertTrackerCache.computeIfAbsent(cacheKey, k -> new AlertTracker());

        if (shouldSuppressAlert(tracker)) {
            log.info("Suppressing status alert for sensor {}", sensor.getId());
            return false;
        }

        tracker.incrementCount();
        tracker.setLastAlertTime(Instant.now());

        return createStatusAlert(telemetryPacket);
    }

    /**
     * Create a status alert.
     */
    private boolean createStatusAlert(TelemetryPacket telemetryPacket) {
        String message = String.format("Status alert for device %s: %s",
                telemetryPacket.getDeviceId(), telemetryPacket.getStatus());
        log.warn(message);

        Alert alert = Alert.builder()
                .type(AlertType.STATUS)
                .message(message)
                .severity(AlertSeverity.WARNING)
                .resolved(false)
                .build();

        telemetryPacket.addAlert(alert);
        alertRepository.save(alert);

        return true;
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

    /**
     * Resolve alerts for a sensor when values return to normal.
     * This method uses a functional approach to find and resolve alerts.
     * It is designed to be thread-safe in a multi-threaded environment.
     *
     * @param sensor the sensor (can be null if only deviceId is available)
     * @param deviceId the device ID
     * @param alertType the type of alert to resolve
     * @return the number of alerts resolved
     */
    @Transactional
    public int resolveAlertsWhenValuesNormal(Sensor sensor, String deviceId, AlertType alertType) {
        try {
            // Find unresolved alerts using a functional approach
            List<Alert> unresolvedAlerts = sensor != null
                    ? alertRepository.findBySensorId(sensor.getId(), Pageable.unpaged())
                            .stream()
                            .filter(alert -> !alert.isResolved() && alert.getType() == alertType)
                            .toList()
                    : alertRepository.findByDeviceId(deviceId, Pageable.unpaged())
                            .stream()
                            .filter(alert -> !alert.isResolved() && alert.getType() == alertType)
                            .toList();

            if (unresolvedAlerts.isEmpty()) {
                return 0;
            }

            // Resolve all found alerts in a thread-safe manner
            Instant now = Instant.now();

            // Use a more efficient batch update approach
            List<Alert> resolvedAlerts = unresolvedAlerts.stream()
                    .map(alert -> {
                        alert.setResolved(true);
                        alert.setResolvedAt(now);
                        return alert;
                    })
                    .toList();

            // Save all alerts in a single batch operation
            alertRepository.saveAll(resolvedAlerts);

            log.info("Resolved {} alerts of type {} for {}", 
                    unresolvedAlerts.size(), 
                    alertType, 
                    sensor != null ? "sensor " + sensor.getId() : "device " + deviceId);

            return unresolvedAlerts.size();
        } catch (Exception e) {
            // Log the error but don't let it propagate and disrupt telemetry processing
            log.error("Error resolving alerts of type {} for {}: {}", 
                    alertType, 
                    sensor != null ? "sensor " + sensor.getId() : "device " + deviceId,
                    e.getMessage());
            return 0;
        }
    }

    /**
     * Determine if an alert should be suppressed based on frequency and time.
     */
    private boolean shouldSuppressAlert(AlertTracker tracker) {
        // If we haven't reached the count threshold, don't suppress
        if (tracker.getCount() < alertSuppressionCount) {
            return false;
        }

        // If the last alert was too long ago, reset the counter and don't suppress
        Instant now = Instant.now();
        if (tracker.getLastAlertTime() != null) {
            Duration timeSinceLastAlert = Duration.between(tracker.getLastAlertTime(), now);
            if (timeSinceLastAlert.toMinutes() > alertSuppressionTimeMinutes) {
                tracker.resetCount();
                return false;
            }
        }

        // If we've reached here, we should suppress the alert
        return true;
    }

    /**
     * Helper class to track alert occurrences.
     */
    private static class AlertTracker {
        private final AtomicInteger count = new AtomicInteger(0);
        private Instant lastAlertTime;

        public void incrementCount() {
            count.incrementAndGet();
        }

        public void resetCount() {
            count.set(0);
        }

        public int getCount() {
            return count.get();
        }

        public Instant getLastAlertTime() {
            return lastAlertTime;
        }

        public void setLastAlertTime(Instant lastAlertTime) {
            this.lastAlertTime = lastAlertTime;
        }
    }
}
