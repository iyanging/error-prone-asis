package io.github.iyanging.errorprone.jpa;

import java.util.List;

import javax.lang.model.element.Modifier;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;


public class JpaUtil {
    private static final Matcher<VariableTree> IS_FINAL_OR_STATIC = Matchers.anyOf(
        Matchers.hasModifier(Modifier.FINAL),
        Matchers.hasModifier(Modifier.STATIC)
    );

    private JpaUtil() {
    }

    @SuppressWarnings("RedundantIfStatement")
    public static List<VariableTree> getColumns(ClassTree classTree, VisitorState visitorState) {
        if (
            ! ASTHelpers.hasAnnotation(
                classTree,
                "jakarta.persistence.Entity",
                visitorState
            )
        ) {
            return List.of();
        }

        return classTree.getMembers()
            .stream()
            .filter(
                memberTree -> {
                    if (memberTree instanceof VariableTree fieldTree) {
                        // https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a19

                        // persistable field cannot be final or static
                        if (IS_FINAL_OR_STATIC.matches(fieldTree, visitorState)) {
                            return false;
                        }

                        // persistable field cannot be annotated with @Transient
                        if (
                            ASTHelpers.hasAnnotation(
                                fieldTree,
                                "jakarta.persistence.Transient",
                                visitorState
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
}
