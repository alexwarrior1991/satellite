package com.alejandro.satellite.simulator;

import com.alejandro.satellite.dto.SensorDTO;
import com.alejandro.satellite.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Initializes a set of predefined sensors when the application starts.
 * This component is only active when the "simulator" profile is active.
 * It runs before the TelemetrySimulator to ensure sensors are created.
 */
@Component
@Profile("simulator")
@Order(0) // Run before TelemetrySimulator
@RequiredArgsConstructor
@Slf4j
public class SensorInitializer implements CommandLineRunner {

    private final SensorService sensorService;


    @Override
    public void run(String... args) throws Exception {

        log.info("Initializing predefined sensors for simulation");

        // Define sensor types
        List<String> sensorTypes = Arrays.asList(
                "Temperature",
                "Pressure",
                "Humidity",
                "Radiation",
                "GPS",
                "Camera",
                "Spectrometer",
                "Magnetometer",
                "Solar Panel",
                "Battery"
        );

        // Define sensor locations
        List<String> locations = Arrays.asList(
                "Earth Orbit",
                "Moon",
                "Mars",
                "Deep Space",
                "ISS"
        );

        // Create sensors with different types and locations
        for (int i = 0; i < 10; i++) {
            String type = sensorTypes.get(i % sensorTypes.size());
            String location = locations.get(i % locations.size());

            SensorDTO sensorDTO = SensorDTO.builder()
                    .name("Sensor-" + i)
                    .type(type)
                    .description("Predefined sensor for simulation")
                    .location(location)
                    .active(true)
                    .build();

            try {
                SensorDTO createdSensor = sensorService.createSensor(sensorDTO);
                log.info("Created sensor: {} (ID: {})", createdSensor.getName(), createdSensor.getId());
            } catch (Exception e) {
                log.error("Error creating sensor {}: {}", sensorDTO.getName(), e.getMessage());
            }
        }

        log.info("Sensor initialization completed");
    }
}
