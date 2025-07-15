package com.alejandro.satellite.controller;

import com.alejandro.satellite.dto.AlertDTO;
import com.alejandro.satellite.mapper.AlertMapper;
import com.alejandro.satellite.model.Alert;
import com.alejandro.satellite.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for alert management.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final AlertMapper alertMapper;

    /**
     * Get all alerts.
     *
     * @param pageable pagination information
     * @return a page of alerts
     */
    @GetMapping
    public ResponseEntity<Page<AlertDTO>> getAllAlerts(Pageable pageable) {
        log.debug("REST request to get all alerts");
        Page<AlertDTO> alerts = alertService.getAllAlerts(pageable)
                .map(alertMapper::toDto);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get an alert by ID.
     *
     * @param id the alert ID
     * @return the alert, if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertDTO> getAlert(@PathVariable Long id) {
        log.debug("REST request to get alert with ID: {}", id);
        return alertService.getAlertById(id)
                .map(alertMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all alerts for a specific telemetry packet.
     *
     * @param telemetryPacketId the telemetry packet ID
     * @return a list of alerts
     */
    @GetMapping("/telemetry/{telemetryPacketId}")
    public ResponseEntity<List<AlertDTO>> getAlertsForTelemetryPacket(@PathVariable Long telemetryPacketId) {
        log.debug("REST request to get alerts for telemetry packet with ID: {}", telemetryPacketId);
        List<AlertDTO> alerts = alertService.getAlertsForTelemetryPacket(telemetryPacketId).stream()
                .map(alertMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get all alerts for a specific device.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Page<AlertDTO>> getAlertsForDevice(
            @PathVariable String deviceId, Pageable pageable) {
        log.debug("REST request to get alerts for device with ID: {}", deviceId);
        Page<AlertDTO> alerts = alertService.getAlertsForDevice(deviceId, pageable)
                .map(alertMapper::toDto);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get all alerts for a specific sensor.
     *
     * @param sensorId the sensor ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @GetMapping("/sensor/{sensorId}")
    public ResponseEntity<Page<AlertDTO>> getAlertsForSensor(
            @PathVariable Long sensorId, Pageable pageable) {
        log.debug("REST request to get alerts for sensor with ID: {}", sensorId);
        Page<AlertDTO> alerts = alertService.getAlertsForSensor(sensorId, pageable)
                .map(alertMapper::toDto);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get all unresolved alerts.
     *
     * @return a list of unresolved alerts
     */
    @GetMapping("/unresolved")
    public ResponseEntity<List<AlertDTO>> getUnresolvedAlerts() {
        log.debug("REST request to get unresolved alerts");
        List<AlertDTO> alerts = alertService.getUnresolvedAlerts().stream()
                .map(alertMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    /**
     * Resolve an alert.
     *
     * @param id the alert ID
     * @return the resolved alert, if found
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable Long id) {
        log.debug("REST request to resolve alert with ID: {}", id);
        return alertService.resolveAlert(id)
                .map(alertMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
