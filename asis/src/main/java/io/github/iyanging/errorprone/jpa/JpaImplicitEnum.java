package io.github.iyanging.errorprone.jpa;

import javax.lang.model.element.ElementKind;

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
    summary = "A persistable enum field should be annotated with @Enumerated",
    severity = BugPattern.SeverityLevel.WARNING,
    linkType = BugPattern.LinkType.NONE,
    tags = { BugPattern.StandardTags.FRAGILE_CODE, BugPattern.StandardTags.LIKELY_ERROR }
)
public class JpaImplicitEnum extends BugChecker implements BugChecker.ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (! JpaUtil.isEntity(tree, state)) {
            return Description.NO_MATCH;
        }

        for (final var column : JpaUtil.getColumns(tree, state)) {
            final var columnType = ASTHelpers.getType(column);

            // only check for enum type
            if (columnType == null || columnType.asElement().getKind() != ElementKind.ENUM) {
                continue;
            }

            if (
                ! ASTHelpers.hasAnnotation(
                    column,
                    "jakarta.persistence.Enumerated",
                    state
                )
            ) {
                state.reportMatch(
                    buildDescription(column)
                        .setMessage("A persistable enum field should be annotated with @Enumerated")
                        .build()
                );
            }

        }

        return Description.NO_MATCH;
    }
}
