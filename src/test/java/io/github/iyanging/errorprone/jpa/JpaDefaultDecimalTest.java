package io.github.iyanging.errorprone.jpa;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;


public class JpaDefaultDecimalTest {
    private final CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(JpaDefaultDecimal.class, getClass());

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
    public void entityWithoutBigDecimal() {
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
    public void bigDecimalWithoutColumn() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    // BUG: Diagnostic contains: should specify scale
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigDecimalWithColumnNoArgs() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column
                    // BUG: Diagnostic contains: should specify scale
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigDecimalWithColumnIrrelevantArgs() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(name = "a")
                    // BUG: Diagnostic contains: should specify scale
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigDecimalWithColumnOnlyPrecision() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(precision = 16)
                    // BUG: Diagnostic contains: should specify scale
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigDecimalWithColumnOnlyScale() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(scale = 4)
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigDecimalWithColumnPrecisionAndScale() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigDecimal;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(precision = 16, scale = 4)
                    BigDecimal number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigIntegerWithColumnNoArgs() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigInteger;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column
                    // BUG: Diagnostic contains: should specify precision
                    BigInteger number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigIntegerWithColumnOnlyPrecision() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigInteger;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(precision = 16)
                    BigInteger number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigIntegerWithColumnOnlyScale() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigInteger;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(scale = 2)
                    // BUG: Diagnostic contains: cannot specify scale
                    BigInteger number;
                }
                """
        ).doTest();
    }

    @Test
    public void bigIntegerWithColumnPrecisionAndScale() {
        compilationHelper.addSourceLines(
            "A.java",
            """
                import jakarta.persistence.*;
                import java.math.BigInteger;

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    @Column(precision = 16, scale = 4)
                    // BUG: Diagnostic contains: cannot specify scale
                    BigInteger number;
                }
                """
        ).doTest();
    }
}
