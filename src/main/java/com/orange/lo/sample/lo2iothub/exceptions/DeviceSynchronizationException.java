/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.exceptions;

public class DeviceSynchronizationException extends RuntimeException {

    private static final long serialVersionUID = -4903380570473678898L;

    public DeviceSynchronizationException(String message) {
        super(message);
    }
}
