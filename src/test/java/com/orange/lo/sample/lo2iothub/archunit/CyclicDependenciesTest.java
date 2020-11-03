package com.orange.lo.sample.lo2iothub.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "com.orange.lo.sample.lo2iothub",
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class
        }
)
class CyclicDependenciesTest {

    @ArchTest
    void configurationAnnotatedClassesShouldEndWithConfig(JavaClasses classes) {
        slices().matching("com.orange.lo.sample.lo2iothub.(**)")
                .should().beFreeOfCycles()
                .check(classes);
    }

}