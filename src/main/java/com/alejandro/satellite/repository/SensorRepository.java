package com.alejandro.satellite.repository;

import com.alejandro.satellite.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Sensor entities.
 */
@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /**
     * Find a sensor by its name.
     * 
     * @param name the sensor name
     * @return the sensor, if found
     */
    Optional<Sensor> findByName(String name);
    
    /**
     * Find all sensors of a specific type.
     * 
     * @param type the sensor type
     * @return a list of sensors
     */
    List<Sensor> findByType(String type);
    
    /**
     * Find all active sensors.
     * 
     * @return a list of active sensors
     */
    List<Sensor> findByActiveTrue();
    
    /**
     * Find all sensors at a specific location.
     * 
     * @param location the location
     * @return a list of sensors
     */
    List<Sensor> findByLocation(String location);
}