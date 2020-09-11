/** 
* Copyright (c) Orange. All Rights Reserved.
* 
* This source code is licensed under the MIT license found in the 
* LICENSE file in the root directory of this source tree. 
*/

package com.orange.lo.sample.lo2iothub;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties()
public class ApplicationProperties {

    private List<TenantProperties> tenantList;

    public List<TenantProperties> getTenantList() {
        return tenantList;
    }

    public void setTenantList(List<TenantProperties> tenantList) {
        this.tenantList = tenantList;
    }
}
