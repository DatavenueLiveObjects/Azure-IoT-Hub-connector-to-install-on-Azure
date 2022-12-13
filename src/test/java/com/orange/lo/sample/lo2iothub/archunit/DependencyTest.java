/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.archunit;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.orange.lo.sample.lo2iothub.modify.ModifyConfigurationService;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.DependencyRules;

@AnalyzeClasses(packages = "com.orange.lo.sample.lo2iothub", importOptions = { ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class })
class DependencyTest {

    @ArchTest
    void shouldNotHaveCyclicalDependenciesBetweenClasses(JavaClasses classes) {
        slices().matching("com.orange.lo.sample.lo2iothub.(**)").should().beFreeOfCycles().check(classes);
    }

    @ArchTest
    void noClassesShouldDependsOnUpperPackages(JavaClasses classes) {
        JavaClasses allExceptModifyConfigurationService = classes.that(are(not(equivalentTo(ModifyConfigurationService.class))));
        DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES.check(allExceptModifyConfigurationService);
    }

}