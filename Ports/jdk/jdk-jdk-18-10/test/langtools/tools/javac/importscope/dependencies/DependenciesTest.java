/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 7101822
 * @summary Verify that the processing of classes in TypeEnter runs in the correct order.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build annotations.TriggersComplete annotations.TriggersCompleteRepeat annotations.Phase
 * @build DependenciesTest
 * @run main DependenciesTest
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import annotations.*;
import com.sun.source.tree.AnnotationTree;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.Dependencies;


public class DependenciesTest {
    public static void main(String... args) throws IOException {
        new DependenciesTest().run();
    }

    void run() throws IOException {
        Path src = Paths.get(System.getProperty("test.src"), "tests");

        try (Stream<Path> tests = Files.list(src)) {
            tests.map(p -> Files.isRegularFile(p) ? Stream.of(p) : silentWalk(p))
                 .forEach(this :: runTest);
        }
    }

    Stream<Path> silentWalk(Path src) {
        try {
            return Files.walk(src).filter(Files :: isRegularFile);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void runTest(Stream<Path> inputs) {
        JavacTool tool = JavacTool.create();
        try (JavacFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Path classes = Paths.get(System.getProperty("test.classes"));
            Iterable<? extends JavaFileObject> reconFiles =
                    fm.getJavaFileObjectsFromFiles(inputs.sorted().map(p -> p.toFile()) :: iterator);
            List<String> options = Arrays.asList("-classpath", classes.toAbsolutePath().toString());
            JavacTask reconTask = tool.getTask(null, fm, null, options, null, reconFiles);
            Iterable<? extends CompilationUnitTree> reconUnits = reconTask.parse();
            JavacTrees reconTrees = JavacTrees.instance(reconTask);
            SearchAnnotations scanner = new SearchAnnotations(reconTrees,
                                                              reconTask.getElements());
            List<JavaFileObject> validateFiles = new ArrayList<>();

            reconTask.analyze();
            scanner.scan(reconUnits, null);

            for (CompilationUnitTree cut : reconUnits) {
                validateFiles.add(ClearAnnotations.clearAnnotations(reconTrees, cut));
            }

            Context validateContext = new Context();
            TestDependencies.preRegister(validateContext);
            JavacTask validateTask =
                    tool.getTask(null, fm, null, options, null, validateFiles, validateContext);

            validateTask.analyze();

            TestDependencies deps = (TestDependencies) Dependencies.instance(validateContext);

            if (!scanner.topLevel2Expected.equals(deps.topLevel2Completing)) {
                throw new IllegalStateException(  "expected=" + scanner.topLevel2Expected +
                                                "; actual=" + deps.topLevel2Completing);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            inputs.close();
        }
    }

    static final class TestDependencies extends Dependencies {

        public static void preRegister(Context context) {
            context.put(dependenciesKey, (Factory<Dependencies>) TestDependencies :: new);
        }

        public TestDependencies(Context context) {
            super(context);
        }

        final Stack<PhaseDescription> inProcess = new Stack<>();

        String topLevelMemberEnter;
        Map<String, Set<PhaseDescription>> topLevel2Completing = new HashMap<>();

        @Override
        public void push(ClassSymbol s, CompletionCause phase) {
            String flatname = s.flatName().toString();
            for (Phase p : Phase.values()) {
                if (phase == p.cause) {
                    inProcess.push(new PhaseDescription(flatname, p));
                    return ;
                }
            }
            if (phase == CompletionCause.MEMBER_ENTER) {
                if (inProcess.isEmpty()) {
                    topLevelMemberEnter = flatname;
                } else {
                    for (PhaseDescription running : inProcess) {
                        if (running == null)
                            continue;

                        Set<PhaseDescription> completing =
                                topLevel2Completing.computeIfAbsent(running.flatname, $ -> new HashSet<>());

                        completing.add(new PhaseDescription(flatname, running.phase));
                    }
                }
            }
            inProcess.push(null);
        }

        @Override
        public void pop() {
            inProcess.pop();
        }

    }

    static final class SearchAnnotations extends TreePathScanner<Void, Void> {
        final Trees trees;
        final Elements elements;
        final TypeElement triggersCompleteAnnotation;
        final TypeElement triggersCompleteRepeatAnnotation;
        final Map<String, Set<PhaseDescription>> topLevel2Expected =
                new HashMap<>();

        public SearchAnnotations(Trees trees, Elements elements) {
            this.trees = trees;
            this.elements = elements;
            this.triggersCompleteAnnotation =
                    elements.getTypeElement(TriggersComplete.class.getName());
            this.triggersCompleteRepeatAnnotation =
                    elements.getTypeElement(TriggersCompleteRepeat.class.getName());
        }

        @Override
        public Void visitClass(ClassTree node, Void p) {
            TypeElement te = (TypeElement) trees.getElement(getCurrentPath());
            Set<PhaseDescription> expected = new HashSet<>();

            for (AnnotationMirror am : getTriggersCompleteAnnotation(te)) {
                TypeMirror of = (TypeMirror) findAttribute(am, "of").getValue();
                Name ofName = elements.getBinaryName((TypeElement) ((DeclaredType) of).asElement());
                Element at = (Element) findAttribute(am, "at").getValue();
                Phase phase = Phase.valueOf(at.getSimpleName().toString());
                expected.add(new PhaseDescription(ofName.toString(), phase));
            }

            if (!expected.isEmpty())
                topLevel2Expected.put(elements.getBinaryName(te).toString(), expected);

            return super.visitClass(node, p);
        }

        Collection<AnnotationMirror> getTriggersCompleteAnnotation(TypeElement te) {
            for (AnnotationMirror am : te.getAnnotationMirrors()) {
                if (triggersCompleteAnnotation.equals(am.getAnnotationType().asElement())) {
                    return Collections.singletonList(am);
                }
                if (triggersCompleteRepeatAnnotation.equals(am.getAnnotationType().asElement())) {
                    return (Collection<AnnotationMirror>) findAttribute(am, "value").getValue();
                }
            }
            return Collections.emptyList();
        }

        AnnotationValue findAttribute(AnnotationMirror mirror, String name) {
            for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e :
                    mirror.getElementValues().entrySet()) {
                if (e.getKey().getSimpleName().contentEquals(name)) {
                    return e.getValue();
                }
            }

            throw new IllegalStateException("Could not find " + name + " in " + mirror);
        }
    }

    static final class ClearAnnotations extends TreePathScanner<Void, Void> {
        final SourcePositions positions;
        final List<int[]> spans2Clear = new ArrayList<>();

        ClearAnnotations(Trees trees) {
            this.positions = trees.getSourcePositions();
        }

        @Override
        public Void visitAnnotation(AnnotationTree node, Void p) {
            removeCurrentNode();
            return null;
        }

        @Override
        public Void visitImport(ImportTree node, Void p) {
            if (node.getQualifiedIdentifier().toString().startsWith("annotations.")) {
                removeCurrentNode();
                return null;
            }
            return super.visitImport(node, p);
        }

        void removeCurrentNode() {
            CompilationUnitTree topLevel = getCurrentPath().getCompilationUnit();
            Tree node = getCurrentPath().getLeaf();
            spans2Clear.add(new int[] {(int) positions.getStartPosition(topLevel, node),
                                       (int) positions.getEndPosition(topLevel, node)});
        }

        static JavaFileObject clearAnnotations(Trees trees, CompilationUnitTree cut)
                throws IOException {
            ClearAnnotations a = new ClearAnnotations(trees);
            a.scan(cut, null);
            Collections.sort(a.spans2Clear, (s1, s2) -> s2[0] - s1[0]);
            StringBuilder result = new StringBuilder(cut.getSourceFile().getCharContent(true));
            for (int[] toClear : a.spans2Clear) {
                result.delete(toClear[0], toClear[1]);
            }
            return new TestJavaFileObject(cut.getSourceFile().toUri(), result.toString());
        }

    }

    static final class PhaseDescription {
        final String flatname;
        final Phase phase;

        public PhaseDescription(String flatname, Phase phase) {
            this.flatname = flatname;
            this.phase = phase;
        }

        @Override
        public String toString() {
            return "@annotations.TriggersComplete(of=" + flatname + ".class," +
                   "at=annotations.Phase." + phase + ')';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + Objects.hashCode(this.flatname);
            hash = 89 * hash + Objects.hashCode(this.phase);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PhaseDescription other = (PhaseDescription) obj;
            if (!Objects.equals(this.flatname, other.flatname)) {
                return false;
            }
            if (this.phase != other.phase) {
                return false;
            }
            return true;
        }

    }

    static final class TestJavaFileObject extends SimpleJavaFileObject {
        private final String content;

        public TestJavaFileObject(URI uri, String content) {
            super(uri, Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }

    }
}


