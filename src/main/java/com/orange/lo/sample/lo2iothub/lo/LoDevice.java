package com.orange.lo.sample.lo2iothub.lo;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoDevice {

    private String id;
    private String name;
    
    @JsonCreator
    public LoDevice(@JsonProperty("id")String id, @JsonProperty("name")String name) {
		this.id = id;
		this.name = name;		
	}
    
    public String getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Lo Device [id: " + id + "; name: " + name + "; ]";
    }
}