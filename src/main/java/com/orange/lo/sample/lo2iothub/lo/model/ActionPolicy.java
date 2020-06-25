package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionPolicy {

    private String id;
    private String name;
    private Boolean enabled;
    private Triggers triggers;
    private Actions actions;

    public ActionPolicy() {
    }

    public ActionPolicy(String id, String name, Boolean enabled, Triggers triggers, Actions actions) {
        super();
        this.id = id;
        this.name = name;
        this.enabled = enabled;
        this.triggers = triggers;
        this.actions = actions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Triggers getTriggers() {
        return triggers;
    }

    public void setTriggers(Triggers triggers) {
        this.triggers = triggers;
    }

    public Actions getActions() {
        return actions;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("name", name).append("enabled", enabled).append("triggers", triggers).append("actions", actions).toString();
    }

}