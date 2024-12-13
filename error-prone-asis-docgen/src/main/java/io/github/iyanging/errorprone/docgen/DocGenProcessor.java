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

package io.github.iyanging.errorprone.docgen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.jspecify.annotations.Nullable;


@SuppressWarnings(
    {
        "unused",
        "initialization.field.uninitialized"
    }
)
@AutoService(Processor.class)
public class DocGenProcessor extends AbstractProcessor {
    private Messager messager;
    private Trees trees;
    private TreeMaker treeMaker;
    private Names names;

    private final Set<BugPatternChecker> handledCheckers = new HashSet<>();

    private void modifyLink(BugPatternChecker checker) {
        final var annoInstance = checker.annoInstance();

        if (annoInstance.linkType() != BugPattern.LinkType.AUTOGENERATED) {
            return;
        }

        final var annoInvocation = JavacUtil
            .getAnnotationInvocation(trees, checker.clazzSymbol(), BugPattern.class)
            .getFirst();

        JavacUtil.putAnnotationInvocationArguments(
            annoInvocation,
            treeMaker.Assign(
                treeMaker.Ident(names.fromString("linkType")),
                treeMaker.Select(
                    treeMaker.Select(
                        ((JCTree.JCIdent) annoInvocation.getAnnotationType()),
                        names.fromString("LinkType")
                    ),
                    names.fromString("CUSTOM")
                )
            ),
            treeMaker.Assign(
                treeMaker.Ident(names.fromString("link")),
                treeMaker.Literal(
                    "https://iyanging.github.io/error-prone-asis/%s/%s".formatted(
                        checker.category(),
                        checker.name()
                    )
                )
            )
        );
    }

    private void generateDocs(BugPatternChecker checker) throws IOException {
        final var docFile = processingEnv.getFiler()
            .createResource(
                StandardLocation.SOURCE_OUTPUT,
                "docs.%s".formatted(checker.category()),
                "%s.md".formatted(checker.name())
            );

        try (var out = docFile.openOutputStream()) {
            final var writer = new PrintWriter(out, true, StandardCharsets.UTF_8);
            writer.println(checker.annoInstance().explanation());
        }

    }

    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        for (final var element : roundEnv.getElementsAnnotatedWith(BugPattern.class)) {
            final var checker = inspectChecker(element);
            if (checker == null) {
                continue;
            }

            if (handledCheckers.contains(checker)) {
                continue;
            }

            modifyLink(checker);

            try {
                generateDocs(checker);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            handledCheckers.add(checker);
        }

        return false;
    }

    private @Nullable BugPatternChecker inspectChecker(Element element) {
        final var anno = element.getAnnotation(BugPattern.class);
        if (anno == null) {
            return null;
        }

        if (! (element instanceof Symbol.ClassSymbol clazz)) {
            messager.printError("@BugPattern can only be annotated to class", element);
            return null;
        }

        return new BugPatternChecker(
            element.toString(),
            element.getSimpleName().toString(),
            anno,
            clazz
        );
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        final var javacProcessingEnv = (JavacProcessingEnvironment) processingEnv;

        this.messager = javacProcessingEnv.getMessager();
        this.trees = Trees.instance(javacProcessingEnv);
        this.treeMaker = TreeMaker.instance(javacProcessingEnv.getContext());
        this.names = Names.instance(javacProcessingEnv.getContext());
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() { return Set.of(
        BugPattern.class.getName()
    ); }

    @Override
    public SourceVersion getSupportedSourceVersion() { return SourceVersion.latest(); }

    private record BugPatternChecker(
        String clazzName,
        String clazzSimpleName,
        BugPattern annoInstance,
        Symbol.ClassSymbol clazzSymbol
    ) {
        @Override
        public int hashCode() {
            return Objects.hash(clazzName);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof BugPatternChecker that
                && Objects.equals(this.clazzName, that.clazzName);
        }

        public String name() {
            return annoInstance.name().isEmpty()
                ? clazzSimpleName
                : annoInstance.name();
        }

        public String category() {
            return Objects.requireNonNull(clazzSymbol.getEnclosingElement())
                .getSimpleName()
                .toString();
        }
    }

    private static class JavacUtil {
        public static List<JCTree.JCAnnotation> getAnnotationInvocation(
            Trees trees,
            Element element,
            Class<? extends Annotation> annotationClazz
        ) {
            return ((JCTree.JCClassDecl) trees.getTree(element))
                .getModifiers()
                .getAnnotations()
                .stream()
                .filter(
                    anno -> ((JCTree.JCIdent) anno.getAnnotationType()).sym
                        .getQualifiedName()
                        .toString()
                        .equals(annotationClazz.getName())
                )
                .toList();
        }

        public static void putAnnotationInvocationArguments(
            JCTree.JCAnnotation annoInvocation,
            JCTree.JCAssign... arguments
        ) {
            final var updatedVars = Arrays.stream(arguments)
                .map(assign -> assign.getVariable().toString())
                .collect(Collectors.toSet());

            final var updatedArgs = new ArrayList<JCTree.JCExpression>(Arrays.asList(arguments));

            for (final var origArg : annoInvocation.getArguments()) {
                if (origArg instanceof JCTree.JCAssign origAssign) {
                    if (! updatedVars.contains(origAssign.getVariable().toString())) {
                        updatedArgs.add(origAssign);
                    }

                } else {
                    updatedArgs.add(origArg);
                }

            }

            annoInvocation.args = com.sun.tools.javac.util.List.from(updatedArgs);
        }
    }
}
