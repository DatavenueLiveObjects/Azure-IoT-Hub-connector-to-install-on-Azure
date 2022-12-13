/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

public class ErrorDto {

    private String message;
    private int httpCode;

    public ErrorDto(String message, int httpCode) {
        this.message = message;
        this.httpCode = httpCode;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpCode() {
        return httpCode;
    }
}