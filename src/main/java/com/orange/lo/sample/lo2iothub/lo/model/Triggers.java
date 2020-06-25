package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Triggers {

    private DeviceCreated deviceCreated;
    private DeviceDeleted deviceDeleted;
    private DataMessage dataMessage;

    public Triggers() {
    }

    public Triggers(DataMessage dataMessage) {
        this.dataMessage = dataMessage;
    }

    public Triggers(DeviceCreated deviceCreated, DeviceDeleted deviceDeleted) {
        this.deviceCreated = deviceCreated;
        this.deviceDeleted = deviceDeleted;
    }

    public DeviceCreated getDeviceCreated() {
        return deviceCreated;
    }

    public DeviceDeleted getDeviceDeleted() {
        return deviceDeleted;
    }

    public DataMessage getDataMessage() {
        return dataMessage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("deviceCreated", deviceCreated).append("deviceDeleted", deviceDeleted).toString();
    }

    public void setDeviceCreated(DeviceCreated deviceCreated) {
        this.deviceCreated = deviceCreated;
    }

    public void setDeviceDeleted(DeviceDeleted deviceDeleted) {
        this.deviceDeleted = deviceDeleted;
    }

    public void setDataMessage(DataMessage dataMessage) {
        this.dataMessage = dataMessage;
    }
}