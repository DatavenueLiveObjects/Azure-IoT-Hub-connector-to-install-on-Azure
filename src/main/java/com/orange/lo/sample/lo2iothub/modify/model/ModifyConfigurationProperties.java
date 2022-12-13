package com.orange.lo.sample.lo2iothub.modify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModifyConfigurationProperties {

    private String applicationInsights;

    private List<ModifyTenantProperties> tenantList;

    public String getApplicationInsights() {
        return applicationInsights;
    }

    public void setApplicationInsights(String applicationInsights) {
        this.applicationInsights = applicationInsights;
    }

    public List<ModifyTenantProperties> getTenantList() {
        return tenantList;
    }

    public void setTenantList(List<ModifyTenantProperties> tenantList) {
        this.tenantList = tenantList;
    }

    @Override
    public String toString() {
        return "ModifyConfigurationProperties{" +
                "applicationInsights='" + applicationInsights + '\'' +
                ", tenantList=" + tenantList +
                '}';
    }

}