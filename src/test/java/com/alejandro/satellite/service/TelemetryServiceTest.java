package com.alejandro.satellite.service;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.mapper.TelemetryPacketMapper;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.SensorRepository;
import com.alejandro.satellite.repository.TelemetryPacketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelemetryServiceTest {

    @Mock
    private TelemetryPacketRepository telemetryPacketRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private TelemetryPacketMapper telemetryPacketMapper;

    @Mock
    private ExecutorService virtualThreadExecutor;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private TelemetryService telemetryService;

    @Captor
    private ArgumentCaptor<TelemetryPacket> telemetryPacketCaptor;

    private Sensor sensor;
    private TelemetryPacket telemetryPacket;
    private TelemetryPacketDTO telemetryPacketDTO;

    @BeforeEach
    void setUp() {
        // Create test data
        sensor = new Sensor();
        sensor.setId(1L);
        sensor.setName("Test Sensor");
        sensor.setType("Temperature");
        sensor.setActive(true);

        telemetryPacket = new TelemetryPacket();
        telemetryPacket.setId(1L);
        telemetryPacket.setDeviceId("DEVICE-1");
        telemetryPacket.setSensor(sensor);
        telemetryPacket.setTemperature(25.0);
        telemetryPacket.setTimestamp(Instant.now());

        telemetryPacketDTO = new TelemetryPacketDTO();
        telemetryPacketDTO.setId(1L);
        telemetryPacketDTO.setDeviceId("DEVICE-1");
        telemetryPacketDTO.setSensorId(1L);
        telemetryPacketDTO.setTemperature(25.0);
        telemetryPacketDTO.setTimestamp(Instant.now());

        // Configure mocks
        when(telemetryPacketRepository.save(any(TelemetryPacket.class))).thenReturn(telemetryPacket);
        when(telemetryPacketMapper.toEntity(any(TelemetryPacketDTO.class))).thenReturn(telemetryPacket);
        when(telemetryPacketMapper.toDto(any(TelemetryPacket.class))).thenReturn(telemetryPacketDTO);
        when(virtualThreadExecutor.submit(any(Runnable.class))).thenReturn(null);
    }

    @Test
    void shouldAssociateSensorFromDeviceIdWhenProcessingPacket() {
        // Given
        telemetryPacket.setSensor(null); // No sensor associated initially
        telemetryPacketDTO.setSensorId(null); // No sensorId in DTO
        telemetryPacketDTO.setDeviceId("1"); // deviceId is a valid Long

        when(telemetryPacketMapper.toEntity(telemetryPacketDTO)).thenReturn(telemetryPacket);
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(sensor));

        // When
        TelemetryPacketDTO result = telemetryService.processTelemetryPacket(telemetryPacketDTO);

        // Then
        verify(telemetryPacketRepository).save(telemetryPacketCaptor.capture());
        TelemetryPacket savedPacket = telemetryPacketCaptor.getValue();

        assertNotNull(savedPacket);
        assertNotNull(savedPacket.getSensor());
        assertEquals(1L, savedPacket.getSensor().getId());
    }

    @Test
    void shouldHandleNonNumericDeviceIdWhenProcessingPacket() {
        // Given
        telemetryPacket.setSensor(null); // No sensor associated initially
        telemetryPacketDTO.setSensorId(null); // No sensorId in DTO
        telemetryPacketDTO.setDeviceId("DEVICE-ABC"); // deviceId is not a valid Long

        when(telemetryPacketMapper.toEntity(telemetryPacketDTO)).thenReturn(telemetryPacket);

        // When
        TelemetryPacketDTO result = telemetryService.processTelemetryPacket(telemetryPacketDTO);

        // Then
        verify(telemetryPacketRepository).save(telemetryPacketCaptor.capture());
        TelemetryPacket savedPacket = telemetryPacketCaptor.getValue();

        assertNotNull(savedPacket);
        assertNull(savedPacket.getSensor()); // No sensor should be associated
    }

    @Test
    void shouldAssociateSensorFromDeviceIdWhenProcessingBatch() {
        // Given
        telemetryPacket.setSensor(null); // No sensor associated initially
        telemetryPacketDTO.setSensorId(null); // No sensorId in DTO
        telemetryPacketDTO.setDeviceId("1"); // deviceId is a valid Long

        when(telemetryPacketMapper.toEntity(telemetryPacketDTO)).thenReturn(telemetryPacket);
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(sensor));
        when(telemetryPacketRepository.saveAll(anyList())).thenReturn(List.of(telemetryPacket));

        // When
        List<TelemetryPacketDTO> result = telemetryService.processTelemetryPackets(List.of(telemetryPacketDTO));

        // Then
        verify(telemetryPacketRepository).saveAll(anyList());

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
