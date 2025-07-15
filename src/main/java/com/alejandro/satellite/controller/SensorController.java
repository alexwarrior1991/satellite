package com.alejandro.satellite.controller;

import com.alejandro.satellite.dto.SensorDTO;
import com.alejandro.satellite.service.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for sensor management.
 */
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Slf4j
public class SensorController {

    private final SensorService sensorService;

    /**
     * Create a new sensor.
     *
     * @param sensorDTO the sensor data
     * @return the created sensor
     */
    @PostMapping
    public ResponseEntity<SensorDTO> createSensor(@Valid @RequestBody SensorDTO sensorDTO) {
        log.debug("REST request to create sensor: {}", sensorDTO);
        SensorDTO createdSensor = sensorService.createSensor(sensorDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSensor);
    }

    /**
     * Update an existing sensor.
     *
     * @param id the sensor ID
     * @param sensorDTO the updated sensor data
     * @return the updated sensor, if found
     */
    @PutMapping("/{id}")
    public ResponseEntity<SensorDTO> updateSensor(
            @PathVariable Long id, @Valid @RequestBody SensorDTO sensorDTO) {
        log.debug("REST request to update sensor with ID {}: {}", id, sensorDTO);
        return sensorService.updateSensor(id, sensorDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a sensor by ID.
     *
     * @param id the sensor ID
     * @return the sensor, if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<SensorDTO> getSensor(@PathVariable Long id) {
        log.debug("REST request to get sensor with ID: {}", id);
        return sensorService.getSensor(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a sensor by name.
     *
     * @param name the sensor name
     * @return the sensor, if found
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<SensorDTO> getSensorByName(@PathVariable String name) {
        log.debug("REST request to get sensor with name: {}", name);
        return sensorService.getSensorByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all sensors.
     *
     * @param pageable pagination information
     * @return a page of sensors
     */
    @GetMapping
    public ResponseEntity<Page<SensorDTO>> getAllSensors(Pageable pageable) {
        log.debug("REST request to get all sensors");
        Page<SensorDTO> sensors = sensorService.getAllSensors(pageable);
        return ResponseEntity.ok(sensors);
    }

    /**
     * Get all sensors of a specific type.
     *
     * @param type the sensor type
     * @return a list of sensors
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<SensorDTO>> getSensorsByType(@PathVariable String type) {
        log.debug("REST request to get sensors of type: {}", type);
        List<SensorDTO> sensors = sensorService.getSensorsByType(type);
        return ResponseEntity.ok(sensors);
    }

    /**
     * Get all active sensors.
     *
     * @return a list of active sensors
     */
    @GetMapping("/active")
    public ResponseEntity<List<SensorDTO>> getActiveSensors() {
        log.debug("REST request to get active sensors");
        List<SensorDTO> sensors = sensorService.getActiveSensors();
        return ResponseEntity.ok(sensors);
    }

    /**
     * Deactivate a sensor.
     *
     * @param id the sensor ID
     * @return the deactivated sensor, if found
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<SensorDTO> deactivateSensor(@PathVariable Long id) {
        log.debug("REST request to deactivate sensor with ID: {}", id);
        return sensorService.deactivateSensor(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Activate a sensor.
     *
     * @param id the sensor ID
     * @return the activated sensor, if found
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<SensorDTO> activateSensor(@PathVariable Long id) {
        log.debug("REST request to activate sensor with ID: {}", id);
        return sensorService.activateSensor(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a sensor.
     *
     * @param id the sensor ID
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long id) {
        log.debug("REST request to delete sensor with ID: {}", id);
        sensorService.deleteSensor(id);
        return ResponseEntity.noContent().build();
    }
}