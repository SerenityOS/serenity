/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8041648
 * @summary Verify that end positions are sane if semicolons are missing.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main MissingSemicolonTest MissingSemicolonTest.java
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;

import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.*;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;

public class MissingSemicolonTest {
    public static void main(String... args) throws IOException {
        String testSrc = System.getProperty("test.src");
        File baseDir = new File(testSrc);
        boolean ok = new MissingSemicolonTest().run(baseDir, args);
        if (!ok) {
            throw new Error("failed");
        }
    }

    boolean run(File baseDir, String... args) throws IOException {
        try {
            if (args.length == 0) {
                throw new IllegalStateException("Needs input files.");
            }

            for (String arg : args) {
                File file = new File(baseDir, arg);
                if (file.exists())
                    test(file);
                else
                    error("File not found: " + file);
            }

            System.err.println(fileCount + " files read");
            if (errors > 0)
                System.err.println(errors + " errors");

            return errors == 0;
        } finally {
            fm.close();
        }
    }

    void test(File file) {
        if (file.isFile() && file.getName().endsWith(".java")) {
            try {
                fileCount++;
                String content = new String(Files.readAllBytes(file.toPath()));
                List<int[]> spans = gatherTreeSpans(file, content);
                int nextSemicolon = -1;

                //remove semicolons, one at a time, and verify the positions are still meaningful:
                while ((nextSemicolon = content.indexOf(';', nextSemicolon + 1)) != (-1)) {
                    String updatedContent =
                            content.substring(0, nextSemicolon) +
                                                 " " +
                                                 content.substring(nextSemicolon + 1);
                    verifyTreeSpans(file, spans, updatedContent, nextSemicolon);
                }
            } catch (IOException e) {
                error("Error reading " + file + ": " + e);
            }
        }
    }

    public List<int[]> gatherTreeSpans(File file, String content) throws IOException {
        JCCompilationUnit unit = read(file.toURI(), content);
        List<int[]> spans = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree != null) {
                    int start = ((JCTree) tree).getStartPosition();
                    int end = ((JCTree) tree).getEndPosition(unit.endPositions);

                    spans.add(new int[] {start, end});
                }
                return super.scan(tree, p);
            }
        }.scan(unit, null);
        return spans;
    }

    public void verifyTreeSpans(File file, List<int[]> spans,
                                String updatedContent, int semicolon) throws IOException {
        JCCompilationUnit updated = read(file.toURI(), updatedContent);
        Iterator<int[]> nextSpan = spans.iterator();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree != null) {
                    int start = ((JCTree) tree).getStartPosition();
                    int end = ((JCTree) tree).getEndPosition(updated.endPositions);

                    if (tree.getKind() != Kind.ERRONEOUS) {
                        int[] expected = nextSpan.next();
                        int expectedEnd = expected[1];

                        if (expectedEnd == semicolon + 1) {
                            Scanner scanner = scannerFactory.newScanner(updatedContent, true);
                            scanner.nextToken();
                            while (scanner.token().pos < expectedEnd)
                                scanner.nextToken();
                            expectedEnd = scanner.token().pos;
                        }

                        if (expected[0] != start || expectedEnd != end) {
                            error(updatedContent + "; semicolon: " + semicolon + "; expected: " +
                                  expected[0] + "-" + expectedEnd + "; found=" + start + "-" + end +
                                  ";" + tree);
                        }
                    }
                }
                return super.scan(tree, p);
            }
        }.scan(updated, null);
    }

    DiagnosticListener<JavaFileObject> devNull = (d) -> {};
    JavacTool tool = JavacTool.create();
    StandardJavaFileManager fm = tool.getStandardFileManager(devNull, null, null);
    ScannerFactory scannerFactory = ScannerFactory.instance(new Context());

    /**
     * Read a file.
     * @param file the file to be read
     * @return the tree for the content of the file
     * @throws IOException if any IO errors occur
     * @throws MissingSemicolonTest.ParseException if any errors occur while parsing the file
     */
    JCCompilationUnit read(URI uri, String content) throws IOException {
        JavacTool tool = JavacTool.create();
        JavacTask task = tool.getTask(null, fm, devNull, Collections.<String>emptyList(), null,
                Arrays.<JavaFileObject>asList(new JavaSource(uri, content)));
        Iterable<? extends CompilationUnitTree> trees = task.parse();
        Iterator<? extends CompilationUnitTree> iter = trees.iterator();
        if (!iter.hasNext())
            throw new Error("no trees found");
        JCCompilationUnit t = (JCCompilationUnit) iter.next();
        if (iter.hasNext())
            throw new Error("too many trees found");
        return t;
    }

    class JavaSource extends SimpleJavaFileObject {

        private final String content;
        public JavaSource(URI uri, String content) {
            super(uri, JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    /**
     * Report an error. When the program is complete, the program will either
     * exit or throw an Error if any errors have been reported.
     * @param msg the error message
     */
    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    /** Number of files that have been analyzed. */
    int fileCount;
    /** Number of errors reported. */
    int errors;

}

class TestCase {
    String str1;
    String str2;
    public TestCase() {
        super();
        super.hashCode();
    }
    public TestCase(String str1, String str2) {
        super();
        this.str1 = str1;
        this.str2 = str2;
        assert true;
    }

    void newClass() {
        new String();
        new String();
    }

    void localVars() {
        String str1 = "";
        String str2;
        String str3;
        final String str4;
    }

    void throwsException() {
        throw new IllegalStateException();
    }

    int returnWithExpression() {
        return 1;
    }

    void returnWithoutExpression() {
        return ;
    }

    void doWhileBreakContinue() {
        do {
            if (true)
                break;
            if (false)
                continue;
        } while(true);
    }

    void labelled() {
        LABEL: doWhileBreakContinue();
    }

}
