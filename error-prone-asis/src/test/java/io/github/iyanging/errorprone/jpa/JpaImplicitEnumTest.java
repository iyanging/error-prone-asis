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
                import jakarta.persistence.*;

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
                import jakarta.persistence.*;

                enum Color {
                    RED, YELLOW, BLUE
                }

                @Entity
                class NotEntity {
                    @Id
                    String id;

                    // BUG: Diagnostic contains: should be annotated with @Enumerated
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
