package com.alejandro.satellite.controller;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.service.TelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for telemetry data.
 */
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Slf4j
public class TelemetryController {

    private final TelemetryService telemetryService;

    /**
     * Submit a single telemetry packet.
     *
     * @param telemetryPacketDTO the telemetry packet to submit
     * @return the processed telemetry packet
     */
    @PostMapping
    public ResponseEntity<TelemetryPacketDTO> submitTelemetryPacket(
            @Valid @RequestBody TelemetryPacketDTO telemetryPacketDTO) {
        log.debug("Received telemetry packet: {}", telemetryPacketDTO);
        TelemetryPacketDTO processedPacket = telemetryService.processTelemetryPacket(telemetryPacketDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(processedPacket);
    }

    /**
     * Submit multiple telemetry packets in batch.
     *
     * @param telemetryPacketDTOs the list of telemetry packets to submit
     * @return the list of processed telemetry packets
     */
    @PostMapping("/batch")
    public ResponseEntity<List<TelemetryPacketDTO>> submitTelemetryPackets(
            @Valid @RequestBody List<TelemetryPacketDTO> telemetryPacketDTOs) {
        log.debug("Received {} telemetry packets", telemetryPacketDTOs.size());
        List<TelemetryPacketDTO> processedPackets = telemetryService.processTelemetryPackets(telemetryPacketDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(processedPackets);
    }

    /**
     * Get a telemetry packet by ID.
     *
     * @param id the ID of the telemetry packet
     * @return the telemetry packet, if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TelemetryPacketDTO> getTelemetryPacket(@PathVariable Long id) {
        return telemetryService.getTelemetryPacket(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get telemetry packets for a specific device.
     *
     * @param deviceId the device ID
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Page<TelemetryPacketDTO>> getTelemetryPacketsForDevice(
            @PathVariable String deviceId, Pageable pageable) {
        Page<TelemetryPacketDTO> packets = telemetryService.getTelemetryPacketsForDevice(deviceId, pageable);
        return ResponseEntity.ok(packets);
    }

    /**
     * Get telemetry packets for a specific device within a time range.
     *
     * @param deviceId the device ID
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return a page of telemetry packets
     */
    @GetMapping("/device/{deviceId}/timerange")
    public ResponseEntity<Page<TelemetryPacketDTO>> getTelemetryPacketsForDeviceInTimeRange(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            Pageable pageable) {
        Page<TelemetryPacketDTO> packets = telemetryService.getTelemetryPacketsForDeviceInTimeRange(
                deviceId, startTime, endTime, pageable);
        return ResponseEntity.ok(packets);
    }
}