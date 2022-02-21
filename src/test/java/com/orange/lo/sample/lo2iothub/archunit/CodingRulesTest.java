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
import com.tngtech.archunit.library.GeneralCodingRules;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static com.orange.lo.sample.lo2iothub.archunit.rules.CodingRules.avoidDirectlyInstantiatingManagedBeans;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

@AnalyzeClasses(
        packages = "com.orange.lo.sample.lo2iothub",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
public class CodingRulesTest {

    @ArchTest
    void shouldLoggerBePrivateStaticFinalAndComplyWithNamingConvention(JavaClasses classes) {
        fields().that().haveRawType(Logger.class)
                .should().bePrivate()
                .andShould().beStatic()
                .andShould().beFinal()
                .andShould().haveNameMatching("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$")
                .check(classes);
    }

    @ArchTest
    void constructorsShouldNotBeMarkedAsAutowired(JavaClasses classes) {
        constructors().should().notBeAnnotatedWith(Autowired.class).check(classes);
    }

    @ArchTest
    void classesUsedAsManagedBeanShouldNotBeDirectlyInstanced(JavaClasses classes) {
        classes().should(avoidDirectlyInstantiatingManagedBeans()).check(classes);
    }

    @ArchTest
    void shouldFollowGeneralCodingRules(JavaClasses classes) {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes);
        GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes);
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(classes);
    }
}
