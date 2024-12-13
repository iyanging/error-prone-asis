package io.github.iyanging.errorprone.jpa;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;

import io.github.iyanging.errorprone.util.JpaUtil;


@AutoService(BugChecker.class)
@BugPattern(
    summary = "A decimal column should specify precision and scale",
    severity = BugPattern.SeverityLevel.WARNING,
    tags = { BugPattern.StandardTags.FRAGILE_CODE, BugPattern.StandardTags.LIKELY_ERROR }
)
public class JpaDefaultDecimal extends BugChecker implements BugChecker.ClassTreeMatcher {
    private static final Set<String> DECIMAL_TYPES_MUST_PRECISION_CANNOT_SCALE = Set.of(
        BigInteger.class.getName()
    );
    private static final Set<String> DECIMAL_TYPES_MUST_SCALE = Set.of(
        BigDecimal.class.getName()
    );
    private static final Set<String> DECIMAL_TYPES = Stream.concat(
        DECIMAL_TYPES_MUST_PRECISION_CANNOT_SCALE.stream(),
        DECIMAL_TYPES_MUST_SCALE.stream()
    ).collect(Collectors.toSet());

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (! JpaUtil.isEntity(tree, state)) {
            return Description.NO_MATCH;
        }

        for (final var column : JpaUtil.getColumns(tree, state)) {
            final var columnType = ASTHelpers.getType(column);
            if (columnType == null) {
                continue;
            }

            final var columnTypeName = columnType.asElement().getQualifiedName().toString();

            if (! DECIMAL_TYPES.contains(columnTypeName)) {
                continue;
            }

            final var annoColumn = JpaUtil.getAnnotationColumn(column, state);

            if (annoColumn == null) {
                if (DECIMAL_TYPES_MUST_PRECISION_CANNOT_SCALE.contains(columnTypeName)) {
                    state.reportMatch(
                        buildDescription(column)
                            .setMessage(
                                "%s column should specify precision but not scale"
                                    .formatted(columnTypeName)
                            )
                            .build()
                    );
                } else if (DECIMAL_TYPES_MUST_SCALE.contains(columnTypeName)) {
                    state.reportMatch(
                        buildDescription(column)
                            .setMessage(
                                "%s column should specify scale"
                                    .formatted(columnTypeName)
                            )
                            .build()
                    );
                }

            } else if (DECIMAL_TYPES_MUST_PRECISION_CANNOT_SCALE.contains(columnTypeName)) {
                if (annoColumn.precision() == null) {
                    state.reportMatch(
                        buildDescription(column)
                            .setMessage(
                                "%s column should specify precision"
                                    .formatted(columnTypeName)
                            )
                            .build()
                    );
                }

                final var scale = annoColumn.scale();
                if (scale != null && scale != 0) {
                    state.reportMatch(
                        buildDescription(column)
                            .setMessage(
                                "%s column cannot specify scale".formatted(columnTypeName)
                            )
                            .build()
                    );
                }

            } else if (DECIMAL_TYPES_MUST_SCALE.contains(columnTypeName)) {
                if (annoColumn.scale() == null) {
                    state.reportMatch(
                        buildDescription(column)
                            .setMessage(
                                "%s column should specify scale".formatted(columnTypeName)
                            )
                            .build()
                    );
                }

            }

        }

        return Description.NO_MATCH;
    }
}
