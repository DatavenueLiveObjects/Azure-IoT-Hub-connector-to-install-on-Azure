package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Actions {

    private List<FifoPublish> fifoPublish = new ArrayList<FifoPublish>();

    public Actions() {
    }

    public List<FifoPublish> getFifoPublish() {
        return fifoPublish;
    }

    public void setFifoPublish(List<FifoPublish> fifoPublish) {
        this.fifoPublish = fifoPublish;
    }

    public void addFifoPublish(FifoPublish fifoPublish) {
        this.fifoPublish.add(fifoPublish);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("fifoPublish", fifoPublish).toString();
    }
}