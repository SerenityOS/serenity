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

/*
 * @test
 * @bug 8050106
 * @summary JavaCompiler relies on inappropriate result from comparison
 * @modules jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.jvm
 */

import java.io.*;
import java.util.*;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.jvm.Target;

public class SourceTargetTest {
    public static void main(String... args) throws Exception {
        SourceTargetTest t = new SourceTargetTest();
        t.run();
    }

    public void run() throws Exception {
        try (FileWriter out = new FileWriter("C.java")) {
            out.write("class C { }");
        }

        for (Source s: Source.values()) {
            test(s, null, "source", getKind(s, Source.MIN));
        }

        for (Target t: Target.values()) {
            test(Source.values()[0], t, "target", getKind(t, Target.MIN));
        }

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(Source s, Target t, String select, Kind kind) {
        System.err.println("Test: source:" + s + ", target:" + t + " " + select + " " + kind);
        List<String> args = new ArrayList<>();
        args.add("-XDrawDiagnostics");
        args.add("-source");
        args.add(s.name);
        if (t != null) {
            args.add("-target");
            args.add(t.name);
        }
        args.add("C.java");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);
        pw.close();
        String out = sw.toString();
        System.err.print(out);

        switch (kind) {
            case INVALID:
                check(out, "removed." + select, true);
                check(out, "obsolete." + select, false);
                break;

            case OBSOLETE:
                check(out, "removed." + select, false);
                check(out, "obsolete." + select, true);
                break;

            case VALID:
                check(out, "removed." + select, false);
                check(out, "obsolete." + select, false);
                break;
        }

        System.err.println();
    }

    enum Kind { INVALID, OBSOLETE, VALID };

    <T extends Comparable<T>> Kind getKind(T t1, T t2) {
        if (t1.compareTo(t2) < 0)
            return Kind.INVALID;
        else if (t1 == t2)
            return Kind.OBSOLETE;
        else
            return Kind.VALID;
    }

    void check(String out, String text, boolean expect) {
        if (out.contains(text) == expect) {
            if (expect)
                System.err.println("expected string found: " + text);
            else
                System.err.println("string not found, as expected: " + text);
        } else {
            if (expect)
                error("expected string not found: " + text);
            else
                error("unexpected string found: " + text);
        }
    }

    void error(String msg) {
        System.err.println("error: " + msg);
        errors++;
    }

    int errors;
}
