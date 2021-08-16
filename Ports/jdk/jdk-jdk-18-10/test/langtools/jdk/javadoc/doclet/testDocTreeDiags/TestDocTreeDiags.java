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
 * @bug      8268420
 * @summary  new Reporter method to report a diagnostic within a DocTree node
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* MyTaglet
 * @run main TestDocTreeDiags
 */

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

/**
 * Tests the ability to write diagnostics related to a (start,pos,end) range in those
 * DocTrees that wrap a String value.
 *
 * Ideally, this would be tested by using a custom doclet which scans all the doc comments,
 * generating diagnostics for eligible nodes. However, one of the cases that is tested is
 * a DocTypeTree, which only occurs in the context of an HTML file in a doc-files subdirectory,
 * which is very specific to the Standard Doclet. Therefore, we use the Standard Doclet
 * in conjunction with a non-standard use of a custom taglet, which is used to access and
 * scan the doc comments that enclose the tags that trigger the taglet.
 */
public class TestDocTreeDiags extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocTreeDiags tester = new TestDocTreeDiags();
        tester.runTests(m -> new Object[] { Path.of(m.getName())} );
    }

    ToolBox tb = new ToolBox();
    Path src;
    DocumentationTool tool;

    boolean showOutput = false; // set true for to set output written by javadoc

    TestDocTreeDiags() throws IOException {
        src = Path.of("src");
        // Note: the following comments are somewhat stylized, and need to follow some
        // simple rules to avoid exceptions and false positives.
        // 1. Each fragment must be at least 7 (and preferably 9) characters long,
        //    in order to contain the range that will be generated in the diagnostic.
        // 2. There must be no non-trivial duplication in the fragments, particularly
        //    in the area where the range of characters will be generated for the
        //    diagnostic. This is because we use String.indexOf to determine the
        //    expected values of the range.
        tb.writeJavaFiles(src,
                """
                    package p;
                    /**
                     * First sentence. &quot;  Second sentence.
                     * {@link java.lang.String first phrase; &quot;  second phrase }
                     * And now ... <!-- this is a comment --> and so it was.
                     * @scanMe
                     */
                    public class C {
                        /**
                         * Sentence for method m(). More details for the method.
                         * Embedded {@link java.lang.Object} link.
                         * And another <!-- unusual comment --> strange comment.
                         * @scanMe
                         */
                         public void m() { }
                    }
                    """);
        tb.writeFile(src.resolve("p").resolve("doc-files").resolve("extra.html"),
                """
                    <!doctype doctype-description>
                    <html>
                    <head><title>Document Title</title></head>
                    <body>
                    Extra content. More content.
                    @scanMe
                    </body>
                    </html>
                    """
                );

        tool = ToolProvider.getSystemDocumentationTool();
    }

    /**
     * Tests the diagnostics generated to the output stream when there is no
     * diagnostic listener in use.
     *
     * By default, in this context, the start and end of the range of characters are not
     * presented. The caret should point at the preferred position for the diagnostic.
     */
    @Test
    public void testStdout(Path base) throws Exception {
        StringWriter outWriter = new StringWriter();
        javadoc(outWriter, null, base.resolve("api"));

        // analyze and verify the generated diagnostics
        List<String> lines = outWriter.toString().lines().toList();
        Iterator<String> iter = lines.iterator();
        while (iter.hasNext()) {
            String l = iter.next();
            if (l.startsWith("src")) {
                checkDiag(null, l, iter.next(), iter.next());
            }
        }
    }

    /**
     * Tests the diagnostics received by a DiagnosticListener.
     *
     * In this context, various detailed coordinate information is available.
     */
    @Test
    public void testDiagListener(Path base) throws Exception {
        StringWriter outWriter = new StringWriter();
        DiagnosticListener dl = diagnostic -> {
            if (diagnostic.getPosition() != -1) {
                List<String> lines = List.of(diagnostic.toString().split("\\R"));
                assert lines.size() == 3;
                String msgLine = lines.get(0);
                String srcLine = lines.get(1);
                String caretLine = lines.get(2);
                checkDiag(diagnostic, msgLine, srcLine, caretLine);
            }
        };
        javadoc(outWriter, dl, base.resolve("api"));
    }

    /**
     * Runs javadoc on package {@code p} in the {@code src} directory,
     * using the specified writer and optional diagnostic listener.
     *
     * @param writer the writer
     * @param dl     the diagnostic listener, or {@code null}
     * @param outDir the output directory
     *
     * @throws IOException if an IO error occurs
     */
    void javadoc(StringWriter writer, DiagnosticListener dl, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of(src));
            fm.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT, List.of(outDir));
            fm.setLocationFromPaths(DocumentationTool.Location.TAGLET_PATH, List.of(Path.of(System.getProperty("test.classes"))));
            Iterable<? extends JavaFileObject> files = Collections.emptyList();
            Iterable<String> options = List.of("-taglet", MyTaglet.class.getName(), "-XDaccessInternalAPI", "p");
            DocumentationTool.DocumentationTask t = tool.getTask(writer, fm, dl, null, options, files);

            checking("exit");
            boolean ok = t.call();

            if (showOutput) {
                out.println("OUT: >>>" + writer.toString().replace("\n", NL) + "<<<");
            }

            if (ok) {
                passed("javadoc exited OK, as expected");
            } else {
                failed("javadoc failed");
            }
        }
    }

    /**
     * Checks the diagnostic output against information encoded in the diagnostics.
     *
     * The message in the message line contains a string that indicates where the
     * caret should be pointing in the source line.
     *
     * @param diag          the diagnostic, or null
     * @param msgLine       file:line: message   >>>detail<<<
     * @param srcLine       the source line
     * @param caretLine     the line with the caret
     */
    void checkDiag(Diagnostic diag, String msgLine, String srcLine, String caretLine) {
        if (diag != null) {
            out.printf("DIAG:  %d:%d:%d  %d:%d vvv%n%s%n^^^%n",
                    diag.getStartPosition(), diag.getPosition(), diag.getEndPosition(),
                    diag.getLineNumber(), diag.getColumnNumber(),
                    diag.toString().replace("\\R", NL) );
        }
        out.println(msgLine);
        out.println(srcLine);
        out.println(caretLine);

        String srcFileLine = msgLine.substring(0, msgLine.indexOf(": "));
        int caretIndex = caretLine.indexOf('^');
        Pattern p = Pattern.compile(">>>([^<]*)<<<");
        Matcher m = p.matcher(msgLine);
        if (!m.find()) {
            throw new IllegalArgumentException("detail pattern not found: " + msgLine);
        }
        String rawDetail = m.group(1);
        String detail = rawDetail.replaceAll("[\\[\\]]", "");

        if (diag != null) {
            checking("coords-column: " + srcFileLine);
            int col = (int) diag.getColumnNumber();
            // line and column are 1-based, so col should be 1 more than caretIndex
            if (col - 1 == caretIndex) {
                passed("col: " + col + " caret: " + caretIndex);
            } else {
                failed("col: " + col + " caret: " + caretIndex);
            }

            checking("coords-start-end: " + srcFileLine);
            String fileStr = readFile(".", msgLine.substring(0, msgLine.indexOf(":")));
            int start = (int) diag.getStartPosition();
            int end = (int) diag.getEndPosition();
            String fileRange = fileStr.substring(start, end);
            if (fileRange.equals(detail)) {
                passed("file: >>>" + fileRange + "<<<  message: >>>" + detail + "<<<");
            } else {
                failed("file: >>>" + fileRange + "<<<  message: >>>" + detail + "<<<");
            }
        }

        checking("message-caret: " + srcFileLine);
        int srcIndex = srcLine.indexOf(detail);
        int pad = (detail.length() - 1) / 2;
        int srcIndexPad = srcIndex + pad;
        if (srcIndexPad == caretIndex) {
            passed("src: " + srcIndexPad + " caret: " + caretIndex);
        } else {
            failed("src: " + srcIndexPad + " caret: " + caretIndex);
        }
    }
}

