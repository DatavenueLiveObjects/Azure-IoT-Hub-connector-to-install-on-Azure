/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

import com.orange.lo.sample.lo2iothub.modify.model.ModifyConfigurationProperties;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ModifyConfigurationController {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ModifyConfigurationService modifyConfigurationService;

    public ModifyConfigurationController(ModifyConfigurationService modifyConfigurationService) {
        this.modifyConfigurationService = modifyConfigurationService;
    }

    @GetMapping()
    public ResponseEntity<ModifyConfigurationProperties> get() {
        return ResponseEntity.ok(modifyConfigurationService.getProperties());
    }

    @PatchMapping
    public ResponseEntity<Void> modify(@RequestBody ModifyConfigurationProperties modifyConfigurationProperties) {
        modifyConfigurationService.modify(modifyConfigurationProperties);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({ ModifyException.class, IOException.class })
    public ResponseEntity<ErrorDto> handleException(ModifyException ex) {
        LOG.error(ex.getMessage(), ex);
        ErrorDto errorDto = new ErrorDto(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.badRequest().body(errorDto);
    }
}