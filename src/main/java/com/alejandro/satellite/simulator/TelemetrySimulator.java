package com.alejandro.satellite.simulator;

import com.alejandro.satellite.dto.SensorDTO;
import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.service.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simulator for generating telemetry data.
 * This component is only active when the "simulator" profile is active.
 */
@Component
@Profile("simulator")
@RequiredArgsConstructor
@Slf4j
public class TelemetrySimulator implements CommandLineRunner {

    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    private final SensorService sensorService;

    // List to store active sensors
    private List<SensorDTO> activeSensors;

    @Value("${app.simulator.device-count:10}")
    private int deviceCount;

    @Value("${app.simulator.packet-count:1000}")
    private int packetCount;

    @Value("${app.simulator.batch-size:100}")
    private int batchSize;

    @Value("${app.simulator.delay-ms:100}")
    private int delayMs;

    @Value("${app.simulator.api-url:http://localhost:8080/api/telemetry}")
    private String apiUrl;

    @Override
    public void run(String... args) {
        log.info("Starting telemetry simulator with {} devices, {} packets per device", deviceCount, packetCount);

        // Load active sensors
        activeSensors = sensorService.getActiveSensors();
        if (activeSensors.isEmpty()) {
            log.error("No active sensors found. Telemetry simulation cannot proceed.");
            return;
        }
        log.info("Loaded {} active sensors for simulation", activeSensors.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Create a task for each device
            for (int deviceIndex = 0; deviceIndex < deviceCount; deviceIndex++) {
                final String deviceId = "DEVICE-" + deviceIndex;
                executor.submit(() -> simulateDevice(deviceId));
            }

            // Wait for all tasks to complete
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Simulator interrupted", e);
        }

        log.info("Telemetry simulator completed");
    }

    private void simulateDevice(String deviceId) {
        log.info("Simulating device: {}", deviceId);

        try {
            List<TelemetryPacketDTO> batch = new ArrayList<>(batchSize);

            for (int i = 0; i < packetCount; i++) {
                TelemetryPacketDTO packet = generateTelemetryPacket(deviceId);
                batch.add(packet);

                // Send batch when it reaches the batch size
                if (batch.size() >= batchSize) {
                    sendBatch(batch);
                    batch = new ArrayList<>(batchSize);
                }

                // Simulate delay between packets
                if (delayMs > 0) {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                }
            }

            // Send any remaining packets
            if (!batch.isEmpty()) {
                sendBatch(batch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Device simulation interrupted: {}", deviceId, e);
        } catch (Exception e) {
            log.error("Error simulating device: {}", deviceId, e);
        }
    }

    private TelemetryPacketDTO generateTelemetryPacket(String deviceId) {
        // Generate random telemetry data
        double temperature = -30.0 + random.nextDouble() * 130.0; // -30 to 100
        double batteryLevel = random.nextDouble() * 100.0; // 0 to 100
        double signalStrength = -130.0 + random.nextDouble() * 50.0; // -130 to -80
        double latitude = -90.0 + random.nextDouble() * 180.0; // -90 to 90
        double longitude = -180.0 + random.nextDouble() * 360.0; // -180 to 180
        double altitude = random.nextDouble() * 10000.0; // 0 to 10000

        // Determine status based on battery level
        DeviceStatus status = DeviceStatus.ONLINE;
        if (batteryLevel < 10.0) {
            status = DeviceStatus.LOW_POWER;
        } else if (random.nextDouble() < 0.05) { // 5% chance of other statuses
            DeviceStatus[] statuses = DeviceStatus.values();
            status = statuses[random.nextInt(statuses.length)];
        }

        // Randomly select a sensor from the active sensors list
        SensorDTO selectedSensor = activeSensors.get(random.nextInt(activeSensors.size()));
        Long sensorId = selectedSensor.getId();

        log.debug("Selected sensor {} (ID: {}) for device {}", 
                selectedSensor.getName(), sensorId, deviceId);

        // Create the telemetry packet
        return TelemetryPacketDTO.builder()
                .sensorId(sensorId)  // Set sensorId from the randomly selected sensor
                .timestamp(Instant.now())
                .temperature(temperature)
                .batteryLevel(batteryLevel)
                .signalStrength(signalStrength)
                .latitude(latitude)
                .longitude(longitude)
                .altitude(altitude)
                .status(status)
                .message("Simulated telemetry packet from " + selectedSensor.getName())
                .build();
    }

    private void sendBatch(List<TelemetryPacketDTO> batch) {
        try {
            log.debug("Sending batch of {} packets", batch.size());
            restTemplate.postForObject(apiUrl + "/batch", batch, List.class);
            log.debug("Batch sent successfully");
        } catch (Exception e) {
            log.error("Error sending batch", e);
        }
    }
}
