/*
  * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

 /*
  * @test
  * @bug      8267204
  * @summary  Expose access to underlying streams in Reporter
  * @library  /tools/lib ../../lib
  * @modules  jdk.javadoc/jdk.javadoc.internal.tool
  * @build    toolbox.ToolBox javadoc.tester.*
  * @run main TestReporterStreams
  */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import javadoc.tester.JavadocTester;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import toolbox.ToolBox;

public class TestReporterStreams extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestReporterStreams tester = new TestReporterStreams();
        tester.runTests(m -> new Object[]{Path.of(m.getName())});
    }

    ToolBox tb = new ToolBox();

    TestReporterStreams() throws IOException {
        tb.writeJavaFiles(Path.of("."), """
                    /**
                     * Comment.
                     * @since 0
                     */
                    public class C { }""");
    }

    /**
     * Tests the entry point used by the DocumentationTool API and JavadocTester, in which
     * all output is written to a single specified writer.
     */
    @Test
    public void testSingleStream(Path base) throws IOException {
        test(base, false, Output.OUT, Output.OUT);
    }

    /**
     * Tests the entry point used by the launcher, in which output is written to
     * writers that wrap {@code System.out} and {@code System.err}.
     */
    @Test
    public void testStandardStreams(Path base) throws IOException {
        test(base, true, Output.STDOUT, Output.STDERR);
    }

    void test(Path base, boolean useStdStreams, Output stdOut, Output stdErr) throws IOException {
        String testClasses = System.getProperty("test.classes");

        setOutputDirectoryCheck(DirectoryCheck.NONE);
        setUseStandardStreams(useStdStreams);
        javadoc("-docletpath", testClasses,
                "-doclet", MyDoclet.class.getName(),
                "C.java" // avoid using a directory, to avoid path separator issues in expected output
        );
        checkExit(Exit.ERROR);
        checkOutput(stdOut, true,
                "Writing to the standard writer");
        checkOutput(stdErr, true,
                "Writing to the diagnostic writer");
        checkOutput(stdErr, true,
                """
                    error: This is a ERROR with no position
                    C.java:5: error: This is a ERROR for an element
                    public class C { }
                           ^
                    C.java:2: error: This is a ERROR for a doc tree path
                     * Comment.
                       ^
                    C.java:3: error: This is a ERROR for a file position
                     * @since 0
                              ^
                    warning: This is a WARNING with no position
                    C.java:5: warning: This is a WARNING for an element
                    public class C { }
                           ^
                    C.java:2: warning: This is a WARNING for a doc tree path
                     * Comment.
                       ^
                    C.java:3: warning: This is a WARNING for a file position
                     * @since 0
                              ^
                    warning: This is a MANDATORY_WARNING with no position
                    C.java:5: warning: This is a MANDATORY_WARNING for an element
                    public class C { }
                           ^
                    C.java:2: warning: This is a MANDATORY_WARNING for a doc tree path
                     * Comment.
                       ^
                    C.java:3: warning: This is a MANDATORY_WARNING for a file position
                     * @since 0
                              ^
                    Note: This is a NOTE with no position
                    C.java:5: Note: This is a NOTE for an element
                    public class C { }
                           ^
                    C.java:2: Note: This is a NOTE for a doc tree path
                     * Comment.
                       ^
                    C.java:3: Note: This is a NOTE for a file position
                     * @since 0
                              ^
                    """);
    }

    public static class MyDoclet implements Doclet {
        private Locale locale;
        private Reporter reporter;

        @Override
        public void init(Locale locale, Reporter reporter) {
            this.locale = locale;
            this.reporter = reporter;
        }

        @Override
        public String getName() {
            return "MyDoclet";
        }

        @Override
        public Set<? extends Option> getSupportedOptions() {
            return Collections.emptySet();
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean run(DocletEnvironment environment) {
            // Write directly to the given streams
            reporter.getStandardWriter().println("Writing to the standard writer");
            reporter.getDiagnosticWriter().println("Writing to the diagnostic writer");

            // the following is little more than a null check for the locale
            reporter.print(Diagnostic.Kind.NOTE, "The locale is " + locale.getDisplayName());

            // Write different kinds of diagnostics using the different overloads
            // for printing diagnostics
            Set<? extends Element> specElems = environment.getSpecifiedElements();
            Element e = specElems.iterator().next();

            DocTrees trees = environment.getDocTrees();
            TreePath tp = trees.getPath(e);
            DocCommentTree dct = trees.getDocCommentTree(e);
            DocTreePath dtp = new DocTreePath(tp, dct);

            CompilationUnitTree cut = tp.getCompilationUnit();
            JavaFileObject fo = cut.getSourceFile();
            SinceTree st = (SinceTree) dct.getBlockTags().get(0);
            DocSourcePositions sp = trees.getSourcePositions();
            int start = (int) sp.getStartPosition(cut, dct, st);
            int pos = (int) sp.getStartPosition(cut, dct, st.getBody().get(0));
            int end = (int) sp.getEndPosition(cut, dct, st);

            for (Diagnostic.Kind k : Diagnostic.Kind.values()) {
                if (k == Diagnostic.Kind.OTHER) {
                    continue;
                }

                reporter.print(k, "This is a " + k + " with no position");
                reporter.print(k, e, "This is a " + k + " for an element");
                reporter.print(k, dtp, "This is a " + k + " for a doc tree path");
                reporter.print(k, fo, start, pos, end, "This is a " + k + " for a file position");
            }

            return true;
        }
    }
}
