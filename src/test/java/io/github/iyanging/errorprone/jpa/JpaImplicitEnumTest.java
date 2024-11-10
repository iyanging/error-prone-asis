package io.github.iyanging.errorprone.jpa;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;


public class JpaImplicitEnumTest {
    private final CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(JpaImplicitEnum.class, getClass());

    @Test
    public void notEntityClass() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                class NotEntity {}
                """
        ).doTest();
    }

    @Test
    public void entityWithoutEnum() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                @Entity
                class NotEntity {
                    @Id
                    String id;
                }
                """
        ).doTest();
    }

    @Test
    public void enumFieldWithoutEnumerated() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;

                enum Color {
                    RED, YELLOW, BLUE
                }

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    // BUG: Diagnostic contains: JpaImplicitEnum
                    Color color;
                }
                """
        ).doTest();
    }

    @Test
    public void enumFieldWithEnumeratedNoArgs() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Enumerated;

                enum Color {
                    RED, YELLOW, BLUE
                }

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Enumerated
                    Color color;
                }
                """
        ).doTest();
    }

    @Test
    public void enumFieldWithEnumeratedString() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.Entity;
                import jakarta.persistence.Id;
                import jakarta.persistence.Enumerated;
                import jakarta.persistence.EnumType;

                enum Color {
                    RED, YELLOW, BLUE
                }

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Enumerated(EnumType.STRING)
                    Color color;
                }
                """
        ).doTest();
    }
}
