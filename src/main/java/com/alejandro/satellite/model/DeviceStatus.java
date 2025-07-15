package com.alejandro.satellite.model;

/**
 * Enum representing the possible statuses of a satellite or IoT device.
 */
public enum DeviceStatus {
    ONLINE,
    OFFLINE,
    STANDBY,
    MAINTENANCE,
    ERROR,
    LOW_POWER,
    UNKNOWN
}