/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.lo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoGroup {

    private final String id;
    private String pathNode;

    @JsonCreator
    public LoGroup(@JsonProperty("id") String id, @JsonProperty("pathNode") String pathNode) {
        this.id = id;
        this.pathNode = pathNode;
    }

    public LoGroup(@JsonProperty("id") String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getPathNode() {
        return pathNode;
    }
}
