/*
 * Copyright (c) 2024 iyanging
 *
 * error-prone-asis is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 */

package io.github.iyanging.errorprone.util;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;


public class MoreMoreAnnotations {
    private static class AnnotationValueBooleanVisitor
        extends
            SimpleAnnotationValueVisitor8<Boolean, Void> {
        @Override
        public Boolean visitBoolean(boolean b, Void unused) {
            return b;
        }
    }

    public static Optional<Boolean> asBooleanValue(AnnotationValue a) {
        return Optional.ofNullable(a.accept(new AnnotationValueBooleanVisitor(), null));
    }

    public static Map<String, AnnotationValue> getElementValues(AnnotationMirror mirror) {
        return mirror.getElementValues()
            .entrySet()
            .stream()
            .map(
                entry -> Map.entry(
                    entry.getKey().getSimpleName().toString(),
                    entry.getValue()
                )
            )
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
