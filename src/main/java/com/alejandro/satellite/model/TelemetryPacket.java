package com.alejandro.satellite.model;

import jakarta.persistence.*;
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
 * Entity representing a telemetry packet received from a satellite or IoT device.
 */
@Entity
@Table(name = "telemetry_packets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryPacket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Device ID is required")
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @NotNull(message = "Timestamp is required")
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "battery_level")
    private Double batteryLevel;

    @Column(name = "signal_strength")
    private Double signalStrength;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatus status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "processed")
    private boolean processed;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @OneToMany(mappedBy = "telemetryPacket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alert> alerts = new ArrayList<>();

    /**
     * Add an alert to this telemetry packet.
     * 
     * @param alert the alert to add
     * @return the added alert
     */
    public Alert addAlert(Alert alert) {
        alerts.add(alert);
        alert.setTelemetryPacket(this);
        return alert;
    }

    /**
     * Remove an alert from this telemetry packet.
     * 
     * @param alert the alert to remove
     */
    public void removeAlert(Alert alert) {
        alerts.remove(alert);
        alert.setTelemetryPacket(null);
    }
}
