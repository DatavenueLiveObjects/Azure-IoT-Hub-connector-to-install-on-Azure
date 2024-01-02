/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

public class AzureIotHubProperties {

    private String iotConnectionString;
    private String iotHostName;
    private int deviceRegistrationThreadPoolSize;
    private int deviceRegistrationPeriod;
    private int deviceReestablishSessionDelay;
    private int deviceClientConnectionTimeout;
    private int messageExpiryTime;
    private int messageSendMaxAttempts;
    private int messageResendDelay;
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

    public int getDeviceRegistrationThreadPoolSize() {
        return deviceRegistrationThreadPoolSize;
    }

    public void setDeviceRegistrationThreadPoolSize(int deviceRegistrationThreadPoolSize) {
        this.deviceRegistrationThreadPoolSize = deviceRegistrationThreadPoolSize;
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

    public int getDeviceRegistrationPeriod() {
        return deviceRegistrationPeriod;
    }

    public void setDeviceRegistrationPeriod(int deviceRegistrationPeriod) {
        this.deviceRegistrationPeriod = deviceRegistrationPeriod;
    }

    public int getDeviceReestablishSessionDelay() {
        return deviceReestablishSessionDelay;
    }

    public void setDeviceReestablishSessionDelay(int deviceReestablishSessionDelay) {
        this.deviceReestablishSessionDelay = deviceReestablishSessionDelay;
    }

    public int getMessageExpiryTime() {
        return messageExpiryTime;
    }

    public void setMessageExpiryTime(int messageExpiryTime) {
        this.messageExpiryTime = messageExpiryTime;
    }

    public int getMessageSendMaxAttempts() {
        return messageSendMaxAttempts;
    }

    public void setMessageSendMaxAttempts(int messageSendMaxAttempts) {
        this.messageSendMaxAttempts = messageSendMaxAttempts;
    }

    public int getMessageResendDelay() {
        return messageResendDelay;
    }

    public void setMessageResendDelay(int messageResendDelay) {
        this.messageResendDelay = messageResendDelay;
    }
}