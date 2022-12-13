/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

public class ModifyException extends RuntimeException {

    private static final long serialVersionUID = -5253401365297108832L;

    public ModifyException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}