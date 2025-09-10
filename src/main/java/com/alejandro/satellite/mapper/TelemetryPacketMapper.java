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

}
