package com.orange.lo.sample.lo2iothub.modify.model;

import com.orange.lo.sample.lo2iothub.modify.model.ModifyAzureIotHubProperties;
import com.orange.lo.sample.lo2iothub.modify.model.ModifyLiveObjectsProperties;

import java.util.List;

public class ModifyTenantProperties {

    ModifyLiveObjectsProperties liveObjectsProperties;

    private List<ModifyAzureIotHubProperties> azureIotHubList;

    public ModifyLiveObjectsProperties getLiveObjectsProperties() {
        return liveObjectsProperties;
    }

    public void setLiveObjectsProperties(ModifyLiveObjectsProperties liveObjectsProperties) {
        this.liveObjectsProperties = liveObjectsProperties;
    }

    public List<ModifyAzureIotHubProperties> getAzureIotHubList() {
        return azureIotHubList;
    }

    public void setAzureIotHubList(List<ModifyAzureIotHubProperties> azureIotHubList) {
        this.azureIotHubList = azureIotHubList;
    }

    @Override
    public String toString() {
        return "ModifyTenantProperties{" +
                "liveObjectsProperties=" + liveObjectsProperties +
                ", azureIotHubList=" + azureIotHubList +
                '}';
    }
}