package io.github.iyanging.errorprone.jpa;

import javax.lang.model.element.ElementKind;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;


@AutoService(BugChecker.class)
@BugPattern(
    name = "JpaImplicitEnum",
    summary = "A persistable enum field should be annotated with @Enumerated",
    severity = BugPattern.SeverityLevel.WARNING,
    linkType = BugPattern.LinkType.NONE,
    tags = { BugPattern.StandardTags.FRAGILE_CODE, BugPattern.StandardTags.LIKELY_ERROR }
)
public class JpaImplicitEnum extends BugChecker implements BugChecker.ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree classTree, VisitorState visitorState) {
        if (! ASTHelpers.hasAnnotation(classTree, "jakarta.persistence.Entity", visitorState)) {
            return Description.NO_MATCH;
        }

        for (final var fieldTree : JpaUtil.getColumns(classTree, visitorState)) {
            final var fieldType = ASTHelpers.getType(fieldTree);

            // only check for enum type
            if (fieldType == null || fieldType.asElement().getKind() != ElementKind.ENUM) {
                continue;
            }

            if (
                ! ASTHelpers
                    .hasAnnotation(fieldTree, "jakarta.persistence.Enumerated", visitorState)
            ) {
                visitorState.reportMatch(
                    buildDescription(fieldTree)
                        .setMessage("A persistable enum field should be annotated with @Enumerated")
                        .build()
                );
            }

        }

        return Description.NO_MATCH;
    }
}
