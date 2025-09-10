package com.alejandro.satellite.mapper;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryPacketMapperTest {

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private SensorMapper sensorMapper;

    @Mock
    private AlertMapper alertMapper;

    @Spy
    @InjectMocks
    private TelemetryPacketMapperImpl telemetryPacketMapper;

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
        telemetryPacket.setSensor(sensor);
        telemetryPacket.setTemperature(25.0);
        telemetryPacket.setTimestamp(Instant.now());

        telemetryPacketDTO = new TelemetryPacketDTO();
        telemetryPacketDTO.setId(1L);
        telemetryPacketDTO.setSensorId(1L);
        telemetryPacketDTO.setTemperature(25.0);
        telemetryPacketDTO.setTimestamp(Instant.now());
    }

    @Test
    void shouldMapSensorIdToEntitySensor() {
        // Given
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(sensor));

        // When
        TelemetryPacket entity = telemetryPacketMapper.toEntity(telemetryPacketDTO);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getSensor());
        assertEquals(1L, entity.getSensor().getId());
    }

    @Test
    void shouldMapEntitySensorToDtoSensorId() {
        // When
        TelemetryPacketDTO dto = telemetryPacketMapper.toDto(telemetryPacket);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getSensorId());
    }
}
