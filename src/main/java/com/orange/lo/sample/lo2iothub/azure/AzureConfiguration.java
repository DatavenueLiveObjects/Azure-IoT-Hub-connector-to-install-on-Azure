/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub.azure;

import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.devicetwin.DeviceTwin;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfiguration {

    private AzureProperties azureProperties;

    public AzureConfiguration(AzureProperties azureProperties) {
        this.azureProperties = azureProperties;
    }

    @Bean
    public DeviceTwin deviceTwin() throws IOException {
        return DeviceTwin.createFromConnectionString(azureProperties.getIotConnectionString());
    }

    @Bean
    public RegistryManager registryManager() throws IOException {
        return RegistryManager.createFromConnectionString(azureProperties.getIotConnectionString());
    }
}