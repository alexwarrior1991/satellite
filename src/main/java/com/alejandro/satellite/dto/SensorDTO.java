package com.alejandro.satellite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for Sensor.
 * Used for API requests and responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorDTO {
    
    private Long id;
    
    @NotBlank(message = "Sensor name is required")
    private String name;
    
    @NotBlank(message = "Sensor type is required")
    private String type;
    
    private String description;
    
    private String location;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private boolean active;
}