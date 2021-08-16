/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004833
 * @summary Integrate doclint support into javac
 * @modules jdk.compiler/com.sun.tools.javac.main
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import static javax.tools.Diagnostic.Kind.*;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.main.Main;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocLintTest {
    public static void main(String... args) throws Exception {
        new DocLintTest().run();
    }

    JavaCompiler javac;
    StandardJavaFileManager fm;
    JavaFileObject file;

    final String code =
        /* 01 */    "/** Class comment. */\n" +
        /* 02 */    "public class Test {\n" +
        /* 03 */    "    /** Method comment. */\n" +
        /* 04 */    "    public void method() { }\n" +
        /* 05 */    "\n" +
        /* 06 */    "    /** Syntax < error. */\n" +
        /* 07 */    "    private void syntaxError() { }\n" +
        /* 08 */    "\n" +
        /* 09 */    "    /** @see DoesNotExist */\n" +
        /* 10 */    "    protected void referenceError() { }\n" +
        /* 08 */    "\n" +
        /* 09 */    "    /** @return */\n" +
        /* 10 */    "    public int emptyReturn() { return 0; }\n" +
        /* 11 */    "}\n";

    final String rawDiags = "-XDrawDiagnostics";
    private enum Message {
        // doclint messages
        DL_ERR6(ERROR, "Test.java:6:16: compiler.err.proc.messager: malformed HTML"),
        DL_ERR9(ERROR, "Test.java:9:14: compiler.err.proc.messager: reference not found"),
        DL_WRN12(WARNING, "Test.java:12:9: compiler.warn.proc.messager: no description for @return"),

        OPT_BADARG(ERROR, "error: invalid flag: -Xdoclint:badarg");

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
        javac = ToolProvider.getSystemJavaCompiler();
        fm = javac.getStandardFileManager(null, null, null);
        try {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(".")));
            file = new SimpleJavaFileObject(URI.create("Test.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncoding) {
                    return code;
                }
            };

            test(Collections.<String>emptyList(),
                    Main.Result.OK,
                    EnumSet.noneOf(Message.class));

            test(Arrays.asList("-Xdoclint:none"),
                    Main.Result.OK,
                    EnumSet.noneOf(Message.class));

            test(Arrays.asList(rawDiags, "-Xdoclint"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR6, Message.DL_ERR9, Message.DL_WRN12));

            test(Arrays.asList(rawDiags, "-Xdoclint:all/public"),
                    Main.Result.OK,
                    EnumSet.of(Message.DL_WRN12));

            test(Arrays.asList(rawDiags, "-Xdoclint:syntax,missing"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR6, Message.DL_WRN12));

            test(Arrays.asList(rawDiags, "-Xdoclint:reference"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.DL_ERR9));

            test(Arrays.asList(rawDiags, "-Xdoclint:badarg"),
                    Main.Result.CMDERR,
                    EnumSet.of(Message.OPT_BADARG));

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
        List<JavaFileObject> files = Arrays.asList(file);
        try {
            JavacTask t = (JavacTask) javac.getTask(pw, fm, null, opts, null, files);
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
        Pattern stats = Pattern.compile("^([1-9]+) (error|warning)(s?)");
        Set<Message> found = EnumSet.noneOf(Message.class);
        int e = 0, w = 0;
        if (!out.isEmpty()) {
            for (String line: out.split("[\r\n]+")) {
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
}
