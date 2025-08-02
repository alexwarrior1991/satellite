package com.alejandro.satellite.service;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.mapper.TelemetryPacketMapper;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.SensorRepository;
import com.alejandro.satellite.repository.TelemetryPacketRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Service for handling telemetry packets.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final TelemetryPacketRepository telemetryPacketRepository;
    private final SensorRepository sensorRepository;
    private final TelemetryPacketMapper telemetryPacketMapper;
    private final ExecutorService virtualThreadExecutor;
    private final AlertService alertService;

    @Value("${app.telemetry.processing.batch-size:100}")
    private int batchSize;

    /**
     * Process a new telemetry packet.
     * This method validates the packet, saves it to the database,
     * and submits it for asynchronous processing.
     *
     * @param telemetryPacketDTO the telemetry packet to process
     * @return the processed telemetry packet
     */
    @Transactional
    @CacheEvict(value = "telemetryPackets", key = "#result.id", condition = "#result != null")
    public TelemetryPacketDTO processTelemetryPacket(@Valid TelemetryPacketDTO telemetryPacketDTO) {
        log.debug("Processing telemetry packet: {}", telemetryPacketDTO);

        // Set timestamp if not provided
        if (telemetryPacketDTO.getTimestamp() == null) {
            telemetryPacketDTO.setTimestamp(Instant.now());
        }

        // Convert DTO to entity
        TelemetryPacket telemetryPacket = telemetryPacketMapper.toEntity(telemetryPacketDTO);

        // If no sensor is associated but a sensorId is provided, try to find the sensor
        if (telemetryPacket.getSensor() == null && telemetryPacketDTO.getSensorId() != null) {
            log.debug("Looking up sensor with ID: {}", telemetryPacketDTO.getSensorId());
            // The mapper should handle this, but just in case
            sensorRepository.findById(telemetryPacketDTO.getSensorId()).ifPresent(telemetryPacket::setSensor);
        }
        // If no sensor is associated but a deviceId is provided, try to find the sensor by deviceId
        else if (telemetryPacket.getSensor() == null && telemetryPacketDTO.getDeviceId() != null) {
            try {
                Long sensorId = Long.parseLong(telemetryPacketDTO.getDeviceId());
                log.debug("Looking up sensor with ID (from deviceId): {}", sensorId);
                sensorRepository.findById(sensorId).ifPresent(telemetryPacket::setSensor);
            } catch (NumberFormatException e) {
                log.debug("DeviceId {} is not a valid sensor ID", telemetryPacketDTO.getDeviceId());
            }
        }

        // Save to database
        telemetryPacket = telemetryPacketRepository.save(telemetryPacket);

        // Submit for asynchronous processing
        final TelemetryPacket finalPacket = telemetryPacket;
        virtualThreadExecutor.submit(() -> processPacketAsync(finalPacket));

        // Return the DTO
        return telemetryPacketMapper.toDto(telemetryPacket);
    }

    /**
     * Process multiple telemetry packets in batch.
     *
     * @param telemetryPacketDTOs the list of telemetry packets to process
     * @return the list of processed telemetry packets
     */
    @Transactional
    public List<TelemetryPacketDTO> processTelemetryPackets(List<@Valid TelemetryPacketDTO> telemetryPacketDTOs) {
        log.debug("Processing {} telemetry packets", telemetryPacketDTOs.size());

        // Convert DTOs to entities
        List<TelemetryPacket> telemetryPackets = telemetryPacketDTOs.stream()
                .map(dto -> {
                    // Set timestamp if not provided
                    if (dto.getTimestamp() == null) {
                        dto.setTimestamp(Instant.now());
                    }

                    // Convert DTO to entity
                    TelemetryPacket packet = telemetryPacketMapper.toEntity(dto);

                    // If no sensor is associated but a sensorId is provided, try to find the sensor
                    if (packet.getSensor() == null && dto.getSensorId() != null) {
                        log.debug("Looking up sensor with ID: {}", dto.getSensorId());
                        // The mapper should handle this, but just in case
                        sensorRepository.findById(dto.getSensorId()).ifPresent(packet::setSensor);
                    }
                    // If no sensor is associated but a deviceId is provided, try to find the sensor by deviceId
                    else if (packet.getSensor() == null && dto.getDeviceId() != null) {
                        try {
                            Long sensorId = Long.parseLong(dto.getDeviceId());
                            log.debug("Looking up sensor with ID (from deviceId): {}", sensorId);
                            sensorRepository.findById(sensorId).ifPresent(packet::setSensor);
                        } catch (NumberFormatException e) {
                            log.debug("DeviceId {} is not a valid sensor ID", dto.getDeviceId());
                        }
                    }

                    return packet;
                })
                .toList();

        // Save to database
        telemetryPackets = telemetryPacketRepository.saveAll(telemetryPackets);

        // Submit for asynchronous processing
        telemetryPackets.forEach(packet -> 
                virtualThreadExecutor.submit(() -> processPacketAsync(packet)));

        // Return the DTOs
        return telemetryPackets.stream()
                .map(telemetryPacketMapper::toDto)
                .toList();
    }

    /**
     * Process unprocessed telemetry packets.
     * This method is intended to be called by a scheduler to process any packets
     * that were not processed during the initial submission.
     */
    @Transactional
    public void processUnprocessedPackets() {
        log.debug("Processing unprocessed telemetry packets");

        List<TelemetryPacket> unprocessedPackets = telemetryPacketRepository.findByProcessedFalse();

        log.debug("Found {} unprocessed packets", unprocessedPackets.size());

        unprocessedPackets.forEach(packet -> 
                virtualThreadExecutor.submit(() -> processPacketAsync(packet)));
    }

    /**
     * Get a telemetry packet by ID.
     *
     * @param id the ID of the telemetry packet
     * @return the telemetry packet, if found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "telemetryPackets", key = "#id")
    public Optional<TelemetryPacketDTO> getTelemetryPacket(Long id) {
        return telemetryPacketRepository.findById(id)
                .map(telemetryPacketMapper::toDto);
    }

    /**
     * Get telemetry packets for a specific sensor.
     *
     * @param sensorId the sensor ID
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @Transactional(readOnly = true)
    public Page<TelemetryPacketDTO> getTelemetryPacketsForSensor(Long sensorId, Pageable pageable) {
        return telemetryPacketRepository.findBySensor_Id(sensorId, pageable)
                .map(telemetryPacketMapper::toDto);
    }

    /**
     * Get telemetry packets for a specific device.
     * This method is provided for backward compatibility.
     *
     * @param deviceId the device ID (which is the sensor ID as a string)
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @Transactional(readOnly = true)
    public Page<TelemetryPacketDTO> getTelemetryPacketsForDevice(String deviceId, Pageable pageable) {
        try {
            Long sensorId = Long.parseLong(deviceId);
            return getTelemetryPacketsForSensor(sensorId, pageable);
        } catch (NumberFormatException e) {
            log.debug("DeviceId {} is not a valid sensor ID", deviceId);
            return Page.empty(pageable);
        }
    }

    /**
     * Get telemetry packets for a specific sensor within a time range.
     *
     * @param sensorId the sensor ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @Transactional(readOnly = true)
    public Page<TelemetryPacketDTO> getTelemetryPacketsForSensorInTimeRange(
            Long sensorId, Instant startTime, Instant endTime, Pageable pageable) {
        return telemetryPacketRepository.findBySensor_IdAndTimestampBetween(sensorId, startTime, endTime, pageable)
                .map(telemetryPacketMapper::toDto);
    }

    /**
     * Get telemetry packets for a specific device within a time range.
     * This method is provided for backward compatibility.
     *
     * @param deviceId the device ID (which is the sensor ID as a string)
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @Transactional(readOnly = true)
    public Page<TelemetryPacketDTO> getTelemetryPacketsForDeviceInTimeRange(
            String deviceId, Instant startTime, Instant endTime, Pageable pageable) {
        try {
            Long sensorId = Long.parseLong(deviceId);
            return getTelemetryPacketsForSensorInTimeRange(sensorId, startTime, endTime, pageable);
        } catch (NumberFormatException e) {
            log.debug("DeviceId {} is not a valid sensor ID", deviceId);
            return Page.empty(pageable);
        }
    }

    /**
     * Process a telemetry packet asynchronously.
     * This method is called by the virtual thread executor.
     *
     * @param telemetryPacket the telemetry packet to process
     */
    private void processPacketAsync(TelemetryPacket telemetryPacket) {
        try {
            log.debug("Asynchronously processing telemetry packet: {}", telemetryPacket);

            // Evaluate alerts
            alertService.evaluateAlerts(telemetryPacket);

            // Mark as processed
            telemetryPacket.setProcessed(true);
            telemetryPacket.setProcessedAt(Instant.now());

            // Save the updated packet
            telemetryPacketRepository.save(telemetryPacket);

            log.debug("Telemetry packet processed successfully: {}", telemetryPacket);
        } catch (Exception e) {
            log.error("Error processing telemetry packet: {}", telemetryPacket, e);
        }
    }
}
