package com.alejandro.satellite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing an alert generated from telemetry data.
 */
@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Alert type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;

    @NotBlank(message = "Alert message is required")
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved")
    private boolean resolved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telemetry_packet_id")
    private TelemetryPacket telemetryPacket;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Enum representing the type of alert.
     */
    public enum AlertType {
        TEMPERATURE,
        BATTERY,
        SIGNAL,
        STATUS,
        CUSTOM
    }

    /**
     * Enum representing the severity of an alert.
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
}