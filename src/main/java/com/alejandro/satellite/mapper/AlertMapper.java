package com.alejandro.satellite.mapper;

import com.alejandro.satellite.dto.AlertDTO;
import com.alejandro.satellite.model.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for converting between Alert entity and AlertDTO.
 */
@Mapper(componentModel = "spring")
public interface AlertMapper {

    /**
     * Convert entity to DTO
     * @param alert the entity
     * @return the DTO
     */
    @Mapping(target = "telemetryPacketId", source = "telemetryPacket.id")
    @Mapping(target = "sensorId", source = "telemetryPacket.sensor.id")
    @Mapping(target = "sensorName", source = "telemetryPacket.sensor.name")
    AlertDTO toDto(Alert alert);

    /**
     * Convert DTO to entity
     * @param alertDTO the DTO
     * @return the entity
     */
    @Mapping(target = "telemetryPacket", ignore = true)
    Alert toEntity(AlertDTO alertDTO);

    /**
     * Update an existing entity with data from a DTO
     * @param alertDTO the source DTO
     * @param alert the target entity to update
     * @return the updated entity
     */
    @Mapping(target = "id", source = "alert.id")
    @Mapping(target = "type", source = "alertDTO.type")
    @Mapping(target = "message", source = "alertDTO.message")
    @Mapping(target = "severity", source = "alertDTO.severity")
    @Mapping(target = "createdAt", source = "alert.createdAt")
    @Mapping(target = "resolvedAt", source = "alertDTO.resolvedAt")
    @Mapping(target = "resolved", source = "alertDTO.resolved")
    @Mapping(target = "telemetryPacket", ignore = true)
    Alert updateEntityFromDto(AlertDTO alertDTO, Alert alert);
}
