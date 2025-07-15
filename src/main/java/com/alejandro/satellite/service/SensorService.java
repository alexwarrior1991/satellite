package com.alejandro.satellite.service;

import com.alejandro.satellite.dto.SensorDTO;
import com.alejandro.satellite.mapper.SensorMapper;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.repository.SensorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing sensors.
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class SensorService {

    private final SensorRepository sensorRepository;
    private final SensorMapper sensorMapper;

    /**
     * Create a new sensor.
     *
     * @param sensorDTO the sensor data
     * @return the created sensor
     */
    @Transactional
    public SensorDTO createSensor(@Valid SensorDTO sensorDTO) {
        log.debug("Creating sensor: {}", sensorDTO);
        
        Sensor sensor = sensorMapper.toEntity(sensorDTO);
        sensor.setCreatedAt(Instant.now());
        sensor.setActive(true);
        
        sensor = sensorRepository.save(sensor);
        
        return sensorMapper.toDto(sensor);
    }

    /**
     * Update an existing sensor.
     *
     * @param id the sensor ID
     * @param sensorDTO the updated sensor data
     * @return the updated sensor, if found
     */
    @Transactional
    public Optional<SensorDTO> updateSensor(Long id, @Valid SensorDTO sensorDTO) {
        log.debug("Updating sensor with ID {}: {}", id, sensorDTO);
        
        return sensorRepository.findById(id)
                .map(existingSensor -> {
                    Sensor updatedSensor = sensorMapper.updateEntityFromDto(sensorDTO, existingSensor);
                    updatedSensor.setUpdatedAt(Instant.now());
                    return sensorRepository.save(updatedSensor);
                })
                .map(sensorMapper::toDto);
    }

    /**
     * Get a sensor by ID.
     *
     * @param id the sensor ID
     * @return the sensor, if found
     */
    @Transactional(readOnly = true)
    public Optional<SensorDTO> getSensor(Long id) {
        log.debug("Getting sensor with ID: {}", id);
        
        return sensorRepository.findById(id)
                .map(sensorMapper::toDto);
    }

    /**
     * Get a sensor by name.
     *
     * @param name the sensor name
     * @return the sensor, if found
     */
    @Transactional(readOnly = true)
    public Optional<SensorDTO> getSensorByName(String name) {
        log.debug("Getting sensor with name: {}", name);
        
        return sensorRepository.findByName(name)
                .map(sensorMapper::toDto);
    }

    /**
     * Get all sensors.
     *
     * @param pageable pagination information
     * @return a page of sensors
     */
    @Transactional(readOnly = true)
    public Page<SensorDTO> getAllSensors(Pageable pageable) {
        log.debug("Getting all sensors");
        
        return sensorRepository.findAll(pageable)
                .map(sensorMapper::toDto);
    }

    /**
     * Get all sensors of a specific type.
     *
     * @param type the sensor type
     * @return a list of sensors
     */
    @Transactional(readOnly = true)
    public List<SensorDTO> getSensorsByType(String type) {
        log.debug("Getting sensors of type: {}", type);
        
        return sensorRepository.findByType(type).stream()
                .map(sensorMapper::toDto)
                .toList();
    }

    /**
     * Get all active sensors.
     *
     * @return a list of active sensors
     */
    @Transactional(readOnly = true)
    public List<SensorDTO> getActiveSensors() {
        log.debug("Getting active sensors");
        
        return sensorRepository.findByActiveTrue().stream()
                .map(sensorMapper::toDto)
                .toList();
    }

    /**
     * Deactivate a sensor.
     *
     * @param id the sensor ID
     * @return the deactivated sensor, if found
     */
    @Transactional
    public Optional<SensorDTO> deactivateSensor(Long id) {
        log.debug("Deactivating sensor with ID: {}", id);
        
        return sensorRepository.findById(id)
                .map(sensor -> {
                    sensor.setActive(false);
                    sensor.setUpdatedAt(Instant.now());
                    return sensorRepository.save(sensor);
                })
                .map(sensorMapper::toDto);
    }

    /**
     * Activate a sensor.
     *
     * @param id the sensor ID
     * @return the activated sensor, if found
     */
    @Transactional
    public Optional<SensorDTO> activateSensor(Long id) {
        log.debug("Activating sensor with ID: {}", id);
        
        return sensorRepository.findById(id)
                .map(sensor -> {
                    sensor.setActive(true);
                    sensor.setUpdatedAt(Instant.now());
                    return sensorRepository.save(sensor);
                })
                .map(sensorMapper::toDto);
    }

    /**
     * Delete a sensor.
     *
     * @param id the sensor ID
     */
    @Transactional
    public void deleteSensor(Long id) {
        log.debug("Deleting sensor with ID: {}", id);
        
        sensorRepository.deleteById(id);
    }
}