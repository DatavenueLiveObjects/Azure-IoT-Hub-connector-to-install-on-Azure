/**
 * Copyright (c) Orange. All Rights Reserved.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.exceptions;

public class CommandException extends RuntimeException {

    private static final long serialVersionUID = 8723908007122875989L;

    public CommandException(String message) {
        super(message);
    }

    public CommandException(Throwable t) {
        super(t);
    }

}