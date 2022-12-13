package com.orange.lo.sample.lo2iothub.modify.model;

public class ModifyAzureIotHubProperties {
    private String iotConnectionString;
    private String iotHostName;
    private String loMessagesTopic;
    private String loDevicesTopic;
    private String loDevicesGroup;

    public String getIotConnectionString() {
        return iotConnectionString;
    }

    public void setIotConnectionString(String iotConnectionString) {
        this.iotConnectionString = iotConnectionString;
    }

    public String getIotHostName() {
        return iotHostName;
    }

    public void setIotHostName(String iotHostName) {
        this.iotHostName = iotHostName;
    }

    public String getLoMessagesTopic() {
        return loMessagesTopic;
    }

    public void setLoMessagesTopic(String loMessagesTopic) {
        this.loMessagesTopic = loMessagesTopic;
    }

    public String getLoDevicesTopic() {
        return loDevicesTopic;
    }

    public void setLoDevicesTopic(String loDevicesTopic) {
        this.loDevicesTopic = loDevicesTopic;
    }

    public String getLoDevicesGroup() {
        return loDevicesGroup;
    }

    public void setLoDevicesGroup(String loDevicesGroup) {
        this.loDevicesGroup = loDevicesGroup;
    }

    @Override
    public String toString() {
        return "AzureIotHubProperties{" +
                "iotConnectionString=***" +
                ", iotHostName='" + iotHostName + '\'' +
                ", loMessagesTopic='" + loMessagesTopic + '\'' +
                ", loDevicesTopic='" + loDevicesTopic + '\'' +
                ", loDevicesGroup='" + loDevicesGroup + '\'' +
                '}';
    }
}
