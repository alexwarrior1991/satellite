package com.alejandro.satellite.mapper;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.model.Sensor;
import com.alejandro.satellite.model.TelemetryPacket;
import com.alejandro.satellite.repository.SensorRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

/**
 * MapStruct mapper for converting between TelemetryPacket entity and TelemetryPacketDTO.
 */
@Mapper(componentModel = "spring", uses = {SensorMapper.class, AlertMapper.class})
public abstract class TelemetryPacketMapper {

    @Autowired
    private SensorRepository sensorRepository;

    /**
     * Convert entity to DTO
     * @param telemetryPacket the entity
     * @return the DTO
     */
    @Mapping(target = "sensorId", source = "sensor.id")
    public abstract TelemetryPacketDTO toDto(TelemetryPacket telemetryPacket);

    /**
     * Convert DTO to entity
     * @param telemetryPacketDTO the DTO
     * @return the entity
     */
    @Mapping(target = "sensor", source = "sensorId", qualifiedByName = "sensorFromId")
    @Mapping(target = "alerts", ignore = true)
    public abstract TelemetryPacket toEntity(TelemetryPacketDTO telemetryPacketDTO);

    /**
     * Update an existing entity with data from a DTO
     * @param telemetryPacketDTO the source DTO
     * @param telemetryPacket the target entity to update
     * @return the updated entity
     */
    @Mapping(target = "sensor", source = "sensorId", qualifiedByName = "sensorFromId")
    @Mapping(target = "alerts", ignore = true)
    public abstract TelemetryPacket updateEntityFromDto(TelemetryPacketDTO telemetryPacketDTO, @MappingTarget TelemetryPacket telemetryPacket);


    /**
     * Convert a sensor ID to a Sensor entity
     * @param id the sensor ID
     * @return the Sensor entity, or null if not found
     */
    @Named("sensorFromId")
    protected Sensor sensorFromId(Long id) {
        if (id == null) {
            return null;
        }
        return sensorRepository.findById(id).orElse(null);
    }

    /**
     * After mapping from entity to DTO, ensure deviceId is set based on sensor ID if needed
     * @param source the source entity
     * @param target the target DTO
     */
    public void afterMappingToDto(TelemetryPacket source, TelemetryPacketDTO target) {
        if (target.getDeviceId() == null && source.getSensor() != null) {
            target.setDeviceId(source.getSensor().getId().toString());
        }
    }

    /**
     * After mapping from DTO to entity, ensure sensor is set based on deviceId if needed
     * @param source the source DTO
     * @param target the target entity
     */
    public void afterMappingToEntity(TelemetryPacketDTO source, TelemetryPacket target) {
        if (target.getSensor() == null && source.getDeviceId() != null) {
            try {
                Long sensorId = Long.parseLong(source.getDeviceId());
                Sensor sensor = sensorRepository.findById(sensorId).orElse(null);
                target.setSensor(sensor);
            } catch (NumberFormatException e) {
                // If deviceId is not a valid Long, we can't use it as a sensorId
            }
        }
    }
}
