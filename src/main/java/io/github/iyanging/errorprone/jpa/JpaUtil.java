package io.github.iyanging.errorprone.jpa;

import java.util.List;
import java.util.Optional;

import javax.lang.model.element.Modifier;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import org.jspecify.annotations.Nullable;

import io.github.iyanging.errorprone.MoreMoreAnnotations;


public final class JpaUtil {
    private static final Matcher<VariableTree> FIELD_IS_FINAL_OR_STATIC = Matchers.anyOf(
        Matchers.hasModifier(Modifier.FINAL),
        Matchers.hasModifier(Modifier.STATIC)
    );

    private static final Matcher<AnnotationTree> ANNO_IS_COLUMN =
        Matchers.isType("jakarta.persistence.Column");

    private JpaUtil() {
    }

    public static boolean isEntity(ClassTree tree, VisitorState state) {
        return ASTHelpers.hasAnnotation(
            tree,
            "jakarta.persistence.Entity",
            state
        );
    }

    @SuppressWarnings("RedundantIfStatement")
    public static List<VariableTree> getColumns(ClassTree tree, VisitorState state) {
        if (
            ! ASTHelpers.hasAnnotation(
                tree,
                "jakarta.persistence.Entity",
                state
            )
        ) {
            return List.of();
        }

        return tree.getMembers()
            .stream()
            .filter(
                memberTree -> {
                    if (memberTree instanceof VariableTree fieldTree) {
                        // https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a19

                        // persistable field cannot be final or static
                        if (FIELD_IS_FINAL_OR_STATIC.matches(fieldTree, state)) {
                            return false;
                        }

                        // persistable field cannot be annotated with @Transient
                        if (
                            ASTHelpers.hasAnnotation(
                                fieldTree,
                                "jakarta.persistence.Transient",
                                state
                            )
                        ) {
                            return false;
                        }

                        return true;

                    } else {
                        return false;
                    }

                }
            )
            .map(memberTree -> (VariableTree) memberTree)
            .toList();
    }

    public static @Nullable AnnotationColumn getAnnotationColumn(
        VariableTree field,
        VisitorState state
    ) {
        @SuppressWarnings(
            "unchecked"
        ) final var annoColumn = ((List<AnnotationTree>) ASTHelpers.getAnnotations(field))
            .stream()
            .filter(anno -> ANNO_IS_COLUMN.matches(anno, state))
            .findFirst()
            .orElse(null);

        if (annoColumn == null) {
            return null;

        } else {
            final var annoColumnMirror = ASTHelpers.getAnnotationMirror(annoColumn);
            final var declaredValues = MoreMoreAnnotations.getElementValues(annoColumnMirror);

            return new AnnotationColumn(
                Optional.ofNullable(declaredValues.get("name"))
                    .flatMap(MoreAnnotations::asStringValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("unique"))
                    .flatMap(MoreMoreAnnotations::asBooleanValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("nullable"))
                    .flatMap(MoreMoreAnnotations::asBooleanValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("insertable"))
                    .flatMap(MoreMoreAnnotations::asBooleanValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("updatable"))
                    .flatMap(MoreMoreAnnotations::asBooleanValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("columnDefinition"))
                    .flatMap(MoreAnnotations::asStringValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("table"))
                    .flatMap(MoreAnnotations::asStringValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("length"))
                    .flatMap(MoreAnnotations::asIntegerValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("precision"))
                    .flatMap(MoreAnnotations::asIntegerValue)
                    .orElse(null),
                Optional.ofNullable(declaredValues.get("scale"))
                    .flatMap(MoreAnnotations::asIntegerValue)
                    .orElse(null)
            );
        }

    }

    public record AnnotationColumn(
        @Nullable String name,
        @Nullable Boolean unique,
        @Nullable Boolean nullable,
        @Nullable Boolean insertable,
        @Nullable Boolean updatable,
        @Nullable String columnDefinition,
        @Nullable String table,
        @Nullable Integer length,
        @Nullable Integer precision,
        @Nullable Integer scale
    ) {}
}
