package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDeleted {

    private Integer version;

    public DeviceDeleted() {
    }

    public DeviceDeleted(Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("version", version).toString();
    }
}