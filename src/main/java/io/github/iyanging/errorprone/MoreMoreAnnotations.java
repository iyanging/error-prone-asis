package io.github.iyanging.errorprone;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

import org.jspecify.annotations.Nullable;


public class MoreMoreAnnotations {
    private static class AnnotationValueBooleanVisitor
        extends
            SimpleAnnotationValueVisitor8<Boolean, Void> {
        @Override
        public @Nullable Boolean visitBoolean(boolean b, Void unused) {
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
