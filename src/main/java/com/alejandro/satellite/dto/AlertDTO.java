package com.alejandro.satellite.dto;

import com.alejandro.satellite.model.Alert.AlertSeverity;
import com.alejandro.satellite.model.Alert.AlertType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for Alert.
 * Used for API requests and responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {
    
    private Long id;
    
    @NotNull(message = "Alert type is required")
    private AlertType type;
    
    @NotBlank(message = "Alert message is required")
    private String message;
    
    private AlertSeverity severity;
    
    private Instant createdAt;
    
    private Instant resolvedAt;
    
    private boolean resolved;
    
    private Long telemetryPacketId;
    
    // Include basic information about the related telemetry packet
    private Long sensorId;
    
    private String sensorName;
}