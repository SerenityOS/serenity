/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7031005
 * @summary javap prints "extends java.lang.Object"
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class TestSuperclass {
    enum ClassKind {
        CLASS("class"),
        INTERFACE("interface");
        ClassKind(String keyword) {
            this.keyword = keyword;
        }
        final String keyword;
    }

    enum GenericKind {
        NO(""),
        YES("<T>");
        GenericKind(String typarams) {
            this.typarams = typarams;
        }
        final String typarams;
    }

    enum SuperKind {
        NONE(null),
        SUPER("Super");
        SuperKind(String name) {
            this.name = name;
        }
        String extend() {
            return (name == null) ? "" : "extends " + name;
        }
        String decl(ClassKind ck) {
            return (name == null) ? "" : ck.keyword + " " + name + " { }";
        }
        final String name;
    }

    public static void main(String... args) throws Exception {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            int errors = 0;

            for (ClassKind ck: ClassKind.values()) {
                for (GenericKind gk: GenericKind.values()) {
                    for (SuperKind sk: SuperKind.values()) {
                        errors += new TestSuperclass(ck, gk, sk).run(comp, fm);
                    }
                }
            }

            if (errors > 0)
                throw new Exception(errors + " errors found");
        }
    }

    final ClassKind ck;
    final GenericKind gk;
    final SuperKind sk;

    TestSuperclass(ClassKind ck, GenericKind gk, SuperKind sk) {
        this.ck = ck;
        this.gk = gk;
        this.sk = sk;
    }

    int run(JavaCompiler comp, StandardJavaFileManager fm) throws IOException {
        System.err.println("test: ck:" + ck + " gk:" + gk + " sk:" + sk);
        File testDir = new File(ck + "-" + gk + "-" + sk);
        testDir.mkdirs();
        fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(testDir));

        JavaSource js = new JavaSource();
        System.err.println(js.getCharContent(false));
        CompilationTask t = comp.getTask(null, fm, null, null, null, Arrays.asList(js));
        if (!t.call())
            throw new Error("compilation failed");

        File testClass = new File(testDir, "Test.class");
        String out = javap(testClass);

        // Extract class sig from first line of Java source
        String expect = js.source.replaceAll("(?s)^(.* Test[^{]+?) *\\{.*", "$1");

        // Extract class sig from line from javap output
        String found = out.replaceAll("(?s).*\n(.* Test[^{]+?) *\\{.*", "$1");

        checkEqual("class signature", expect, found);

        return errors;
    }

    String javap(File file) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { file.getPath() };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Error("javap failed: rc=" + rc);
        return out;
    }

    void checkEqual(String label, String expect, String found) {
        if (!expect.equals(found))
            error("Unexpected " + label + " found: '" + found + "', expected: '" + expect + "'");
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    class JavaSource extends SimpleJavaFileObject {
        static final String template =
                  "#CK Test#GK #EK { }\n"
                + "#SK\n";
        final String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template
                    .replace("#CK", ck.keyword)
                    .replace("#GK", gk.typarams)
                    .replace("#EK", sk.extend())
                    .replace("#SK", sk.decl(ck));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

}
