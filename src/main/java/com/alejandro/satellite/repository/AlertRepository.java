package com.alejandro.satellite.repository;

import com.alejandro.satellite.model.Alert;
import com.alejandro.satellite.model.Alert.AlertSeverity;
import com.alejandro.satellite.model.Alert.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Alert entities.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Find all alerts for a specific telemetry packet.
     * 
     * @param telemetryPacketId the telemetry packet ID
     * @return a list of alerts
     */
    List<Alert> findByTelemetryPacketId(Long telemetryPacketId);
    
    /**
     * Find all alerts of a specific type.
     * 
     * @param type the alert type
     * @param pageable pagination information
     * @return a page of alerts
     */
    Page<Alert> findByType(AlertType type, Pageable pageable);
    
    /**
     * Find all alerts with a specific severity.
     * 
     * @param severity the alert severity
     * @param pageable pagination information
     * @return a page of alerts
     */
    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);
    
    /**
     * Find all unresolved alerts.
     * 
     * @return a list of unresolved alerts
     */
    List<Alert> findByResolvedFalse();
    
    /**
     * Find all alerts created within a time range.
     * 
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of alerts
     */
    Page<Alert> findByCreatedAtBetween(Instant startTime, Instant endTime, Pageable pageable);
    
    /**
     * Find all alerts for a specific device.
     * 
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @Query("SELECT a FROM Alert a JOIN a.telemetryPacket t WHERE t.deviceId = :deviceId")
    Page<Alert> findByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);
    
    /**
     * Find all alerts for a specific sensor.
     * 
     * @param sensorId the sensor ID
     * @param pageable pagination information
     * @return a page of alerts
     */
    @Query("SELECT a FROM Alert a JOIN a.telemetryPacket t WHERE t.sensor.id = :sensorId")
    Page<Alert> findBySensorId(@Param("sensorId") Long sensorId, Pageable pageable);
}