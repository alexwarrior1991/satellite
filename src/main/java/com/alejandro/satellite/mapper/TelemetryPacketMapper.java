package com.alejandro.satellite.mapper;

import com.alejandro.satellite.dto.TelemetryPacketDTO;
import com.alejandro.satellite.model.TelemetryPacket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between TelemetryPacket entity and TelemetryPacketDTO.
 */
@Mapper(componentModel = "spring")
public interface TelemetryPacketMapper {

    /**
     * Convert entity to DTO
     * @param telemetryPacket the entity
     * @return the DTO
     */
    TelemetryPacketDTO toDto(TelemetryPacket telemetryPacket);

    /**
     * Convert DTO to entity
     * @param telemetryPacketDTO the DTO
     * @return the entity
     */
    TelemetryPacket toEntity(TelemetryPacketDTO telemetryPacketDTO);

    /**
     * Update an existing entity with data from a DTO
     * @param telemetryPacketDTO the source DTO
     * @param telemetryPacket the target entity to update
     * @return the updated entity
     */
    TelemetryPacket updateEntityFromDto(TelemetryPacketDTO telemetryPacketDTO, TelemetryPacket telemetryPacket);
}