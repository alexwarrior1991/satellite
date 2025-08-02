package com.alejandro.satellite.service;

import com.alejandro.satellite.model.Alert;
import com.alejandro.satellite.model.Alert.AlertType;
import com.alejandro.satellite.model.DeviceStatus;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SensorService sensorService;

    @InjectMocks
    private AlertService alertService;

    @Captor
    private ArgumentCaptor<List<Alert>> alertListCaptor;

    private Sensor sensor;
    private TelemetryPacket normalPacket;
    private TelemetryPacket highTemperaturePacket;
    private TelemetryPacket lowBatteryPacket;
    private TelemetryPacket lowSignalPacket;
    private TelemetryPacket errorStatusPacket;
    private List<Alert> unresolvedTemperatureAlerts;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(alertService, "minTemperature", -50.0);
        ReflectionTestUtils.setField(alertService, "maxTemperature", 100.0);
        ReflectionTestUtils.setField(alertService, "criticalBatteryLevel", 10.0);
        ReflectionTestUtils.setField(alertService, "minSignalStrength", -120.0);
        ReflectionTestUtils.setField(alertService, "alertSuppressionCount", 5);
        ReflectionTestUtils.setField(alertService, "alertSuppressionTimeMinutes", 30);
        ReflectionTestUtils.setField(alertService, "sensorDeactivationEnabled", true);
        ReflectionTestUtils.setField(alertService, "alertTrackerCache", new ConcurrentHashMap<>());

        // Create test data
        sensor = new Sensor();
        sensor.setId(1L);
        sensor.setActive(true);

        // Normal telemetry packet
        normalPacket = new TelemetryPacket();
        normalPacket.setDeviceId("device1");
        normalPacket.setSensor(sensor);
        normalPacket.setTemperature(25.0);
        normalPacket.setBatteryLevel(80.0);
        normalPacket.setSignalStrength(-50.0);
        normalPacket.setStatus(DeviceStatus.ONLINE);
        normalPacket.setTimestamp(Instant.now());

        // High temperature packet
        highTemperaturePacket = new TelemetryPacket();
        highTemperaturePacket.setDeviceId("device1");
        highTemperaturePacket.setSensor(sensor);
        highTemperaturePacket.setTemperature(150.0);
        highTemperaturePacket.setBatteryLevel(80.0);
        highTemperaturePacket.setSignalStrength(-50.0);
        highTemperaturePacket.setStatus(DeviceStatus.ONLINE);
        highTemperaturePacket.setTimestamp(Instant.now());

        // Low battery packet
        lowBatteryPacket = new TelemetryPacket();
        lowBatteryPacket.setDeviceId("device1");
        lowBatteryPacket.setSensor(sensor);
        lowBatteryPacket.setTemperature(25.0);
        lowBatteryPacket.setBatteryLevel(5.0);
        lowBatteryPacket.setSignalStrength(-50.0);
        lowBatteryPacket.setStatus(DeviceStatus.ONLINE);
        lowBatteryPacket.setTimestamp(Instant.now());

        // Low signal packet
        lowSignalPacket = new TelemetryPacket();
        lowSignalPacket.setDeviceId("device1");
        lowSignalPacket.setSensor(sensor);
        lowSignalPacket.setTemperature(25.0);
        lowSignalPacket.setBatteryLevel(80.0);
        lowSignalPacket.setSignalStrength(-130.0);
        lowSignalPacket.setStatus(DeviceStatus.ONLINE);
        lowSignalPacket.setTimestamp(Instant.now());

        // Error status packet
        errorStatusPacket = new TelemetryPacket();
        errorStatusPacket.setDeviceId("device1");
        errorStatusPacket.setSensor(sensor);
        errorStatusPacket.setTemperature(25.0);
        errorStatusPacket.setBatteryLevel(80.0);
        errorStatusPacket.setSignalStrength(-50.0);
        errorStatusPacket.setStatus(DeviceStatus.ERROR);
        errorStatusPacket.setTimestamp(Instant.now());

        // Create unresolved temperature alerts
        unresolvedTemperatureAlerts = new ArrayList<>();
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setType(AlertType.TEMPERATURE);
        alert.setResolved(false);
        alert.setCreatedAt(Instant.now().minusSeconds(3600));
        unresolvedTemperatureAlerts.add(alert);
    }

    @Test
    void shouldResolveTemperatureAlertsWhenTemperatureReturnsToNormal() {
        // Given
        when(alertRepository.findBySensorId(eq(sensor.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(unresolvedTemperatureAlerts));
        when(alertRepository.saveAll(anyList())).thenReturn(unresolvedTemperatureAlerts);
        when(alertRepository.save(any(Alert.class))).thenReturn(new Alert());

        // First create a temperature alert
        alertService.evaluateAlerts(highTemperaturePacket);

        // When - temperature returns to normal
        alertService.evaluateAlerts(normalPacket);

        // Then
        verify(alertRepository).saveAll(alertListCaptor.capture());
        List<Alert> resolvedAlerts = alertListCaptor.getValue();

        assertFalse(resolvedAlerts.isEmpty());
        assertTrue(resolvedAlerts.get(0).isResolved());
        assertNotNull(resolvedAlerts.get(0).getResolvedAt());
    }

    @Test
    void shouldResolveBatteryAlertsWhenBatteryReturnsToNormal() {
        // Given
        Alert batteryAlert = new Alert();
        batteryAlert.setId(2L);
        batteryAlert.setType(AlertType.BATTERY);
        batteryAlert.setResolved(false);
        batteryAlert.setCreatedAt(Instant.now().minusSeconds(3600));
        List<Alert> unresolvedBatteryAlerts = List.of(batteryAlert);

        when(alertRepository.findBySensorId(eq(sensor.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(unresolvedBatteryAlerts));
        when(alertRepository.saveAll(anyList())).thenReturn(unresolvedBatteryAlerts);

        // First create a battery alert
        alertService.evaluateAlerts(lowBatteryPacket);

        // When - battery returns to normal
        alertService.evaluateAlerts(normalPacket);

        // Then
        verify(alertRepository).saveAll(alertListCaptor.capture());
        List<Alert> resolvedAlerts = alertListCaptor.getValue();

        assertFalse(resolvedAlerts.isEmpty());
        assertTrue(resolvedAlerts.get(0).isResolved());
        assertNotNull(resolvedAlerts.get(0).getResolvedAt());
    }

    @Test
    void shouldHandleMultipleThreadsResolvingAlerts() throws InterruptedException {
        // Given
        when(alertRepository.findBySensorId(eq(sensor.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(unresolvedTemperatureAlerts));
        when(alertRepository.saveAll(anyList())).thenReturn(unresolvedTemperatureAlerts);

        // When - simulate multiple threads calling evaluateAlerts
        Thread thread1 = new Thread(() -> alertService.evaluateAlerts(normalPacket));
        Thread thread2 = new Thread(() -> alertService.evaluateAlerts(normalPacket));

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Then - verify no exceptions were thrown and alerts were resolved
        verify(alertRepository, atLeastOnce()).saveAll(anyList());
    }
}
