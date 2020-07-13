/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties()
public class AzureProperties {

    private List<AzureIotHubProperties> azureIotHubList;

    public List<AzureIotHubProperties> getAzureIotHubList() {
        return azureIotHubList;
    }

    public void setAzureIotHubList(List<AzureIotHubProperties> azureIotHubList) {
        this.azureIotHubList = azureIotHubList;
    }
}