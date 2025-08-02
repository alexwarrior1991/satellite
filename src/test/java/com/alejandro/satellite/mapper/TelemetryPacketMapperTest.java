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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
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
    }

    @Test
    void shouldMapDeviceIdToSensorIdWhenMappingToDto() {
        // Given
        telemetryPacket.setSensor(null);  // No sensor associated

        // When
        TelemetryPacketDTO dto = telemetryPacketMapper.toDto(telemetryPacket);

        // Then
        assertNotNull(dto);
        assertEquals("DEVICE-1", dto.getDeviceId());
        assertNull(dto.getSensorId());  // Since there's no sensor, sensorId should be null
    }

    @Test
    void shouldMapSensorIdToDeviceIdWhenMappingToDto() {
        // Given
        telemetryPacketDTO.setDeviceId(null);  // No deviceId
        telemetryPacketDTO.setSensorId(1L);    // Only sensorId

        // Create a new entity with the sensor set
        TelemetryPacket entity = new TelemetryPacket();
        entity.setSensor(sensor);  // Set the sensor with id=1L

        // Create a new DTO with sensorId set but no deviceId
        TelemetryPacketDTO dto = new TelemetryPacketDTO();
        dto.setSensorId(1L);

        // When - manually call the afterMapping method
        telemetryPacketMapper.afterMappingToDto(entity, dto);

        // Then
        assertNotNull(dto);
        assertEquals("1", dto.getDeviceId());  // sensorId should be used as deviceId
        assertEquals(1L, dto.getSensorId());
    }

    @Test
    void shouldUseSensorIdFromDeviceIdWhenMappingToEntity() {
        // Given
        telemetryPacketDTO.setDeviceId("1");  // deviceId is a valid Long
        telemetryPacketDTO.setSensorId(null); // No sensorId
        when(sensorRepository.findById(1L)).thenReturn(Optional.of(sensor));

        // When
        TelemetryPacket entity = telemetryPacketMapper.toEntity(telemetryPacketDTO);

        // Manually call the afterMapping method since we're using a mock
        telemetryPacketMapper.afterMappingToEntity(telemetryPacketDTO, entity);

        // Then
        assertNotNull(entity);
        assertEquals("1", entity.getDeviceId());
        assertNotNull(entity.getSensor());
        assertEquals(1L, entity.getSensor().getId());
    }

    @Test
    void shouldHandleNonNumericDeviceIdWhenMappingToEntity() {
        // Given
        telemetryPacketDTO.setDeviceId("DEVICE-ABC");  // deviceId is not a valid Long
        telemetryPacketDTO.setSensorId(null);          // No sensorId

        // When
        TelemetryPacket entity = telemetryPacketMapper.toEntity(telemetryPacketDTO);

        // Then
        assertNotNull(entity);
        assertEquals("DEVICE-ABC", entity.getDeviceId());
        assertNull(entity.getSensor());  // No sensor should be associated
    }
}
