/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(
        packages = "com.orange.lo.sample.lo2iothub",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
class NamingConventionTest {

    @ArchTest
    void configurationAnnotatedClassesShouldEndWithConfig(JavaClasses classes) {
        classes().that().areAnnotatedWith(Configuration.class)
                .should().haveSimpleNameEndingWith("Config")
                .check(classes);
    }

    @ArchTest
    void springBootApplicationAnnotatedClassesEndWithApplication(JavaClasses classes) {
        classes().that().areAnnotatedWith(SpringBootApplication.class)
                .should().haveSimpleNameEndingWith("Application")
                .check(classes);
    }

    @ArchTest
    void springBootApplicationAnnotatedClassesEndWithApplication2(JavaClasses classes) {
        classes().that().areAssignableTo(Exception.class)
                .should().haveSimpleNameEndingWith("Exception")
                .check(classes);
    }
}