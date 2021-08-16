/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071851
 * @summary Test the -Xdoclint/package option
 * @modules jdk.compiler/com.sun.tools.javac.main
 */

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IncludePackagesTest {
    public static void main(String... args) throws Exception {
        new IncludePackagesTest().run();
    }

    JavaCompiler javac;
    StandardJavaFileManager fm;
    List<JavaFileObject> files;

    final String[] sources = new String[] {
        "p1/p1T.java",
        "package p1;\n" +
        "/** Syntax < error. */\n" +
        "public class p1T {\n" +
        "}\n",
        "p1/sp1/p1sp1T.java",
        "package p1.sp1;\n" +
        "/** Syntax < error. */\n" +
        "public class p1sp1T {\n" +
        "}\n",
        "p1/sp1/sp2/p1sp1sp2T.java",
        "package p1.sp1.sp2;\n" +
        "/** Syntax < error. */\n" +
        "public class p1sp1sp2T {\n" +
        "}\n",
        "p2/p2T.java",
        "package p2;\n" +
        "/** Syntax < error. */\n" +
        "public class p2T {\n" +
        "}\n",
        "Default.java",
        "/** Syntax < error. */\n" +
        "public class Default {\n" +
        "}\n",
    };

    final String rawDiags = "-XDrawDiagnostics";
    private enum Message {
        // doclint messages
        p1T(ERROR, "p1T.java:2:12: compiler.err.proc.messager: malformed HTML"),
        p1sp1T(ERROR, "p1sp1T.java:2:12: compiler.err.proc.messager: malformed HTML"),
        p1sp1sp2T(ERROR, "p1sp1sp2T.java:2:12: compiler.err.proc.messager: malformed HTML"),
        p2T(ERROR, "p2T.java:2:12: compiler.err.proc.messager: malformed HTML"),
        Default(ERROR, "Default.java:1:12: compiler.err.proc.messager: malformed HTML"),
        INVALID_PACKAGE_ERROR(ERROR, "error: invalid flag: -Xdoclint/package:wrong+package");

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
            files = new ArrayList<>();

            for (int si = 0; si < sources.length; si += 2) {
                files.add(new JFOImpl(sources[si], sources[si + 1]));
            }

            test(Arrays.asList(rawDiags, "-Xdoclint"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.p1T, Message.p1sp1T, Message.p1sp1sp2T,
                               Message.p2T, Message.Default));

            test(Arrays.asList(rawDiags, "-Xdoclint", "-Xdoclint/package:p1"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.p1T));

            test(Arrays.asList(rawDiags, "-Xdoclint", "-Xdoclint/package:p1.*"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.p1sp1T, Message.p1sp1sp2T));

            test(Arrays.asList(rawDiags, "-Xdoclint", "-Xdoclint/package:p1.*,-p1.sp1"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.p1sp1sp2T));

            test(Arrays.asList(rawDiags, "-Xdoclint", "-Xdoclint/package:-p1.sp1"),
                    Main.Result.ERROR,
                    EnumSet.of(Message.p1T, Message.p1sp1sp2T, Message.p2T, Message.Default));

            test(Arrays.asList(rawDiags, "-Xdoclint", "-Xdoclint/package:wrong+package"),
                    Main.Result.CMDERR,
                    EnumSet.of(Message.INVALID_PACKAGE_ERROR));

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

    class JFOImpl extends SimpleJavaFileObject {

        private final String code;

        public JFOImpl(String fileName, String code) {
            super(URI.create(fileName), JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncoding) {
            return code;
        }
    }
}
