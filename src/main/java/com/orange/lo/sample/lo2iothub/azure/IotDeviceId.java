/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.azure;

public class IotDeviceId {

    private final String id;

    public IotDeviceId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}