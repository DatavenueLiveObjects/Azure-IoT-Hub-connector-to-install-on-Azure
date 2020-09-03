/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

public class AzureIotHubProperties {

    private String iotConnectionString;
    private String iotHostName;
    private int synchronizationThreadPoolSize;
    private int messagingThreadPoolSize;
    private int deviceClientConnectionTimeout;
    private String tagPlatformKey;
    private String tagPlatformValue;

    private String loMessagesTopic;
    private String loDevicesTopic;
    private String loDevicesGroup;

    public String getIotHostName() {
        return iotHostName;
    }

    public void setIotHostName(String iotHostName) {
        this.iotHostName = iotHostName;
    }

    public String getIotConnectionString() {
        return iotConnectionString;
    }

    public void setIotConnectionString(String iotConnectionString) {
        this.iotConnectionString = iotConnectionString;
    }

    public int getSynchronizationThreadPoolSize() {
        return synchronizationThreadPoolSize;
    }

    public void setSynchronizationThreadPoolSize(int synchronizationThreadPoolSize) {
        this.synchronizationThreadPoolSize = synchronizationThreadPoolSize;
    }

    public int getMessagingThreadPoolSize() {
        return messagingThreadPoolSize;
    }

    public void setMessagingThreadPoolSize(int messagingThreadPoolSize) {
        this.messagingThreadPoolSize = messagingThreadPoolSize;
    }

    public int getDeviceClientConnectionTimeout() {
        return deviceClientConnectionTimeout;
    }

    public void setDeviceClientConnectionTimeout(int deviceClientConnectionTimeout) {
        this.deviceClientConnectionTimeout = deviceClientConnectionTimeout;
    }

    public String getTagPlatformKey() {
        return tagPlatformKey;
    }

    public void setTagPlatformKey(String tagPlatformKey) {
        this.tagPlatformKey = tagPlatformKey;
    }

    public String getTagPlatformValue() {
        return tagPlatformValue;
    }

    public void setTagPlatformValue(String tagPlatformValue) {
        this.tagPlatformValue = tagPlatformValue;
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
}