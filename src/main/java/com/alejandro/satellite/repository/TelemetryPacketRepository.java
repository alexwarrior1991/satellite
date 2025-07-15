package com.alejandro.satellite.repository;

import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.model.TelemetryPacket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for TelemetryPacket entities.
 */
@Repository
public interface TelemetryPacketRepository extends JpaRepository<TelemetryPacket, Long> {

    /**
     * Find all telemetry packets for a specific device.
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    Page<TelemetryPacket> findByDeviceId(String deviceId, Pageable pageable);

    /**
     * Find all telemetry packets for a specific device within a time range.
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    Page<TelemetryPacket> findByDeviceIdAndTimestampBetween(
            String deviceId, Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find all telemetry packets with a specific status.
     * @param status the device status
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    Page<TelemetryPacket> findByStatus(DeviceStatus status, Pageable pageable);

    /**
     * Find all unprocessed telemetry packets.
     * @return a list of unprocessed telemetry packets
     */
    List<TelemetryPacket> findByProcessedFalse();

    /**
     * Find all telemetry packets with temperature outside the specified range.
     * @param minTemp the minimum temperature
     * @param maxTemp the maximum temperature
     * @return a list of telemetry packets with temperature alerts
     */
    @Query("SELECT t FROM TelemetryPacket t WHERE t.temperature < :minTemp OR t.temperature > :maxTemp")
    List<TelemetryPacket> findTemperatureAlerts(@Param("minTemp") Double minTemp, @Param("maxTemp") Double maxTemp);

    /**
     * Find all telemetry packets with battery level below the critical threshold.
     * @param criticalLevel the critical battery level
     * @return a list of telemetry packets with battery alerts
     */
    @Query("SELECT t FROM TelemetryPacket t WHERE t.batteryLevel < :criticalLevel")
    List<TelemetryPacket> findBatteryAlerts(@Param("criticalLevel") Double criticalLevel);

    /**
     * Find all telemetry packets with signal strength below the minimum threshold.
     * @param minSignal the minimum signal strength
     * @return a list of telemetry packets with signal alerts
     */
    @Query("SELECT t FROM TelemetryPacket t WHERE t.signalStrength < :minSignal")
    List<TelemetryPacket> findSignalAlerts(@Param("minSignal") Double minSignal);
}