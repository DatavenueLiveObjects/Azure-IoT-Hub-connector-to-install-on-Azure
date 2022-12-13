/**
 * Copyright (c) Orange. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.modify;

import java.io.File;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ModifyConfig {

    private static final String CONFIGURATION_FILE_NAME = "application.yml";

    @Bean
    public File configurationFile() throws IOException {
        return new ClassPathResource(CONFIGURATION_FILE_NAME).getFile();
    }
}