package com.alejandro.satellite.dto;

import com.alejandro.satellite.model.DeviceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for TelemetryPacket.
 * Used for API requests and responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryPacketDTO {

    private Long id;


    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    private Double temperature;

    private Double batteryLevel;

    private Double signalStrength;

    private Double latitude;

    private Double longitude;

    private Double altitude;

    private DeviceStatus status;

    private String message;

    private boolean processed;

    private Instant processedAt;

    // Sensor relationship
    private Long sensorId;

    private SensorDTO sensor;

    /**
     * Get the device ID, which is the same as the sensor ID.
     * This method is provided for backward compatibility.
     * @return the sensor ID as a string
     */
    public String getDeviceId() {
        return sensorId != null ? sensorId.toString() : null;
    }

    /**
     * Set the device ID, which sets the sensor ID.
     * This method is provided for backward compatibility.
     * @param deviceId the device ID as a string
     */
    public void setDeviceId(String deviceId) {
        if (deviceId != null) {
            try {
                this.sensorId = Long.parseLong(deviceId);
            } catch (NumberFormatException e) {
                // If deviceId is not a valid Long, we can't use it as sensorId
            }
        }
    }

    // Alerts
    private List<AlertDTO> alerts = new ArrayList<>();
}
