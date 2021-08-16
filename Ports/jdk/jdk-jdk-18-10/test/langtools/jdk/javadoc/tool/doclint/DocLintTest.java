/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004834 8007610 8129909 8182765 8247815
 * @summary Add doclint support into javadoc
 * @modules jdk.compiler/com.sun.tools.javac.main
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import static javax.tools.Diagnostic.Kind.*;

import com.sun.tools.javac.main.Main;

public class DocLintTest {
    public static void main(String... args) throws Exception {
        new DocLintTest().run();
    }

    DocumentationTool javadoc;
    StandardJavaFileManager fm;
    Iterable<? extends JavaFileObject> files;

    final String code =
        /* 01 */    "/** Class comment. */\n" +
        /* 02 */    "public class Test {\n" +
        /* 03 */    "    /** Method comment. */\n" +
        /* 04 */    "    public void method() { }\n" +
        /* 05 */    "\n" +
        /* 06 */    "    /** Syntax < error. */\n" +
        /* 07 */    """
            \s   private void syntaxError() { }
            """ +
        /* 08 */    "\n" +
        /* 09 */    "    /** @see DoesNotExist */\n" +
        /* 10 */    """
            \s   protected void referenceError() { }
            """ +
        /* 11 */    "\n" +
        /* 12 */    "    /** @return */\n" +
        /* 13 */    """
            \s   public int emptyReturn() { return 0; }
            """ +
        /* 14 */    "}\n";

    final String p1Code =
        /* 01 */    "package p1;\n" +
        /* 02 */    "public class P1Test {\n" +
        /* 03 */    "    /** Syntax < error. */\n" +
        /* 04 */    "    public void method() { }\n" +
        /* 05 */    "}\n";

    final String p2Code =
        /* 01 */    "package p2;\n" +
        /* 02 */    "public class P2Test {\n" +
        /* 03 */    "    /** Syntax < error. */\n" +
        /* 04 */    "    public void method() { }\n" +
        /* 05 */    "}\n";

    private final String rawDiags = "-XDrawDiagnostics";
    private final String htmlVersion = "-html5";

    private enum Message {
        // doclint messages
        DL_ERR6(ERROR, "Test.java:6:16: compiler.err.proc.messager: malformed HTML"),
        DL_ERR9(ERROR, "Test.java:9:14: compiler.err.proc.messager: reference not found"),
        DL_WRN12(WARNING, "Test.java:12:9: compiler.warn.proc.messager: no description for @return"),

        DL_ERR_P1TEST(ERROR, "P1Test.java:3:16: compiler.err.proc.messager: malformed HTML"),
        DL_ERR_P2TEST(ERROR, "P2Test.java:3:16: compiler.err.proc.messager: malformed HTML"),
        DL_WARN_P1TEST(WARNING, "P1Test.java:2:8: compiler.warn.proc.messager: no comment"),
        DL_WARN_P2TEST(WARNING, "P2Test.java:2:8: compiler.warn.proc.messager: no comment"),

        // doclint messages when -XDrawDiagnostics is not in effect
        DL_ERR9A(ERROR, "Test.java:9: error: reference not found"),
        DL_WRN12A(WARNING, "Test.java:12: warning: no description for @return"),

        // javadoc messages about bad content: these should only appear when doclint is disabled
        JD_WRN10(WARNING, "Test.java:10: warning: Tag @see: reference not found: DoesNotExist"),
        JD_WRN13(WARNING, "Test.java:13: warning: @return tag has no arguments."),

        // javadoc messages for bad options
        OPT_BADARG(ERROR, "error: Invalid argument for -Xdoclint option"),
        OPT_BADQUAL(ERROR, "error: Access qualifiers not permitted for -Xdoclint arguments"),
        OPT_BADPACKAGEARG(ERROR, "error: Invalid argument for -Xdoclint/package option");

        final Diagnostic.Kind kind;
        final String text;

        static Message get(String text) {
            for (Message m: values()) {
                if (m.text.equals(text))
                    return m;
            }
            return null;
        }

        Message(Diagnostic.Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }

        @Override
        public String toString() {
            return "[" + kind + ",\"" + text + "\"]";
        }
    }

    void run() throws Exception {
        javadoc = ToolProvider.getSystemDocumentationTool();
        fm = javadoc.getStandardFileManager(null, null, null);
        try {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(new File(".")));
            fm.setLocation(StandardLocation.CLASS_PATH, Collections.<File>emptyList());
            files = List.of(new TestJFO("Test.java", code));

            test(List.of(htmlVersion),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR9A, Message.DL_WRN12A));

            test(List.of(htmlVersion, rawDiags),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR9, Message.DL_WRN12));

//            test(List.of("-Xdoclint:none"),
//                    Main.Result.OK,
//                    EnumSet.of(Message.JD_WRN10, Message.JD_WRN13));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR9, Message.DL_WRN12));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:all/public"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.OPT_BADQUAL));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:all", "-public"),
                    Main.Result.OK,
                    EnumSet.of(Message.DL_WRN12));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:missing"),
                    Main.Result.OK,
                    EnumSet.of(Message.DL_WRN12));

            test(List.of(htmlVersion, rawDiags, "-private"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR6, Message.DL_ERR9, Message.DL_WRN12));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:missing,syntax", "-private"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR6, Message.DL_WRN12));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:reference"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR9));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint:badarg"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.OPT_BADARG));

            files = List.of(new TestJFO("p1/P1Test.java", p1Code),
                                  new TestJFO("p2/P2Test.java", p2Code));

            test(List.of(htmlVersion, rawDiags),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR_P1TEST, Message.DL_ERR_P2TEST,
                            Message.DL_WARN_P1TEST, Message.DL_WARN_P2TEST));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint/package:p1"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR_P1TEST,
                            Message.DL_WARN_P1TEST));

            test(List.of(htmlVersion, rawDiags, "-Xdoclint/package:*p"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.OPT_BADPACKAGEARG));

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        } finally {
            fm.close();
        }
    }

    void test(List<String> opts, Main.Result expectResult, Set<Message> expectMessages) {
        System.err.println("test: " + opts);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            DocumentationTask t = javadoc.getTask(pw, fm, null, null, opts, files);
            boolean ok = t.call();
            pw.close();
            String out = sw.toString().replaceAll("[\r\n]+", "\n");
            if (!out.isEmpty())
                System.err.println(out);
            if (ok && expectResult != Main.Result.OK) {
                error("Compilation succeeded unexpectedly");
            } else if (!ok && expectResult != Main.Result.ERROR) {
                error("Compilation failed unexpectedly");
            } else
                check(out, expectMessages);
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            String expectOut = expectMessages.iterator().next().text;
            if (expectResult != Main.Result.CMDERR)
                error("unexpected exception caught");
            else if (!e.getMessage().equals(expectOut)) {
                error("unexpected exception message: "
                        + e.getMessage()
                        + " expected: " + expectOut);
            }
        }

//        if (errors > 0)
//            throw new Error("stop");
    }

    private void check(String out, Set<Message> expect) {
        Pattern ignore = Pattern.compile("^(Building|Constructing|Generating|Loading|Standard|Starting| ) .*");
        Pattern stats = Pattern.compile("^([1-9]+) (error|warning)(s?)");
        Set<Message> found = EnumSet.noneOf(Message.class);
        int e = 0, w = 0;
        for (String line: out.split("[\r\n]+")) {
            if (ignore.matcher(line).matches())
                continue;

            Matcher s = stats.matcher(line);
            if (s.matches()) {
                int i = Integer.valueOf(s.group(1));
                if (s.group(2).equals("error"))
                    e++;
                else
                    w++;
                continue;
            }

            Message m = Message.get(line);
            if (m == null)
                error("Unexpected line: " + line);
            else
                found.add(m);
        }
        for (Message m: expect) {
            if (!found.contains(m))
                error("expected message not found: " + m.text);
        }
        for (Message m: found) {
            if (!expect.contains(m))
                error("unexpected message found: " + m.text);
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    class TestJFO extends SimpleJavaFileObject {

        private final String content;

        public TestJFO(String fileName, String content) {
            super(URI.create(fileName), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncoding) {
            return content;
        }
    };
}
