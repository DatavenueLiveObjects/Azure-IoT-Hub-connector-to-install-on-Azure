/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import java.util.List;

public class TenantProperties {

    private LiveObjectsProperties liveObjectsProperties;
    private List<AzureIotHubProperties> azureIotHubList;

    public List<AzureIotHubProperties> getAzureIotHubList() {
        return azureIotHubList;
    }

    public void setAzureIotHubList(List<AzureIotHubProperties> azureIotHubList) {
        this.azureIotHubList = azureIotHubList;
    }

    public LiveObjectsProperties getLiveObjectsProperties() {
        return liveObjectsProperties;
    }

    public void setLiveObjectsProperties(LiveObjectsProperties liveObjectsProperties) {
        this.liveObjectsProperties = liveObjectsProperties;
    }
}