package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FifoPublish {

    private String fifoName;

    public FifoPublish() {
    }

    public FifoPublish(String fifoName) {
        this.fifoName = fifoName;
    }

    public String getFifoName() {
        return fifoName;
    }

    public void setFifoName(String fifoName) {
        this.fifoName = fifoName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("fifoName", fifoName).toString();
    }
}