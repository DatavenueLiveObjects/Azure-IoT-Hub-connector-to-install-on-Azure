package com.orange.lo.sample.lo2iothub.modify.model;

public class ModifyLiveObjectsProperties {

    private String loApiKey;

    public String getLoApiKey() {
        return loApiKey;
    }

    public void setLoApiKey(String loApiKey) {
        this.loApiKey = loApiKey;
    }

    @Override
    public String toString() {
        return "LiveObjectsProperties{" + "loApiKey=***" + '}';
    }
}
