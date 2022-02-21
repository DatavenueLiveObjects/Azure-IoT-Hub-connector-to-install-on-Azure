/**
 * Copyright (c) Orange, Inc. and its affiliates. All Rights Reserved.
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.orange.lo.sample.lo2iothub.archunit.rules;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public class CodingRules {

    @PublicAPI(usage = ACCESS)
    public static ArchCondition<JavaClass> avoidDirectlyInstantiatingManagedBeans() {
        return new DirectlyInstantiatingClassUsedAsManagedBeanCondition();
    }

    private static class DirectlyInstantiatingClassUsedAsManagedBeanCondition extends ArchCondition<JavaClass> {

        public static final List<Class<? extends Annotation>> MANAGED_BEANS_ANNOTATIONS = Arrays.asList(
                Component.class,
                Service.class,
                Repository.class,
                Controller.class,
                RestController.class
        );

        DirectlyInstantiatingClassUsedAsManagedBeanCondition() {
            super("Avoid directly instantiating class used as managed bean");
        }

        @Override
        public void check(final JavaClass clazz, final ConditionEvents events) {
            if (isAnnotatedAsBean(clazz.getAnnotations())) {
                for (JavaAccess<?> access : clazz.getConstructorCallsToSelf()) {
                    events.add(new SimpleConditionEvent(access, false, access.getDescription()));
                }
            }
        }

        private boolean isAnnotatedAsBean(Set<JavaAnnotation<JavaClass>> annotations) {
            Optional<JavaAnnotation<JavaClass>> first = annotations.stream()
                    .filter(DirectlyInstantiatingClassUsedAsManagedBeanCondition::isAssignableTo)
                    .findFirst();
            return first.isPresent();
        }

        private static boolean isAssignableTo(JavaAnnotation<JavaClass> javaClassJavaAnnotation) {
            JavaClass rawType = javaClassJavaAnnotation.getRawType();
            Optional<Class<? extends Annotation>> any = MANAGED_BEANS_ANNOTATIONS.stream()
                    .filter(rawType::isAssignableTo)
                    .findAny();
            return any.isPresent();
        }
    }
}
