package com.alejandro.satellite.mapper;

import com.alejandro.satellite.dto.SensorDTO;
import com.alejandro.satellite.model.Sensor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between Sensor entity and SensorDTO.
 */
@Mapper(componentModel = "spring")
public interface SensorMapper {

    /**
     * Convert entity to DTO
     * @param sensor the entity
     * @return the DTO
     */
    SensorDTO toDto(Sensor sensor);

    /**
     * Convert DTO to entity
     * @param sensorDTO the DTO
     * @return the entity
     */
    @Mapping(target = "telemetryPackets", ignore = true)
    Sensor toEntity(SensorDTO sensorDTO);

    /**
     * Update an existing entity with data from a DTO
     * @param sensorDTO the source DTO
     * @param sensor the target entity to update
     * @return the updated entity
     */
    @Mapping(target = "id", source = "sensor.id")
    @Mapping(target = "name", source = "sensorDTO.name")
    @Mapping(target = "type", source = "sensorDTO.type")
    @Mapping(target = "description", source = "sensorDTO.description")
    @Mapping(target = "location", source = "sensorDTO.location")
    @Mapping(target = "createdAt", source = "sensor.createdAt")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "active", source = "sensorDTO.active")
    @Mapping(target = "telemetryPackets", ignore = true)
    Sensor updateEntityFromDto(SensorDTO sensorDTO, Sensor sensor);
}
