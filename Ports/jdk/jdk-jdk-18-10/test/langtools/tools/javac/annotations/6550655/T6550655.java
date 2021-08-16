/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6550655
 * @summary javac crashes when compiling against an annotated class
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.net.URI;
import java.util.Arrays;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T6550655 {

    JavaCompiler javacTool;
    File testDir;
    TestKind testKind;
    EnumActionKind actionKind;

    String testSource = "enum E { NORTH, SOUTH, WEST, EAST; }\n" +
                        "@I(val = E.NORTH)class A {}\n" +
                        "@interface I { E val(); }";

    T6550655(JavaCompiler javacTool, File testDir, TestKind testKind, EnumActionKind actionKind) {
        this.javacTool = javacTool;
        this.testDir = testDir;
        this.testKind = testKind;
        this.actionKind = actionKind;
    }

    void test() {
        testDir.mkdirs();
        compile(null, new JavaSource("Test.java", testSource));
        actionKind.doAction(this);
        compile(new DiagnosticChecker(), testKind.source);
    }

    void compile(DiagnosticChecker dc, JavaSource... sources) {
        try {
            CompilationTask ct = javacTool.getTask(null, null, dc,
                    Arrays.asList("-d", testDir.getAbsolutePath(), "-cp", testDir.getAbsolutePath()),
                    null, Arrays.asList(sources));
            ct.call();
        }
        catch (Exception e) {
            error("Internal compilation error");
        }
    }

    void replaceEnum(String newSource) {
        compile(null, new JavaSource("Replace.java", newSource));
    };

    void removeEnum() {
        File enumClass = new File(testDir, "E.class");
        if (!enumClass.exists()) {
            error("Expected file E.class does not exists in folder " + testDir);
        }
        enumClass.delete();
    };

    void error(String msg) {
        System.err.println(msg);
        nerrors++;
    }

    class DiagnosticChecker implements DiagnosticListener<JavaFileObject> {

        String[][] expectedKeys = new String[][] {
         //             DIRECT,                                         INDIRECT
        {/*REPLACE1*/   "compiler.err.cant.resolve.location"     ,      "compiler.warn.unknown.enum.constant" },
        {/*REPLACE2*/   "compiler.err.cant.resolve.location.args",      "compiler.warn.annotation.method.not.found" },
        {/*REMOVE*/     "compiler.err.cant.resolve"              ,      "compiler.warn.unknown.enum.constant.reason" } };

        String keyToIgnore = "compiler.err.attribute.value.must.be.constant";

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            String expectedCode = expectedKeys[actionKind.ordinal()][testKind.ordinal()];
            if (!diagnostic.getCode().equals(keyToIgnore) &&
                    !diagnostic.getCode().equals(expectedCode)) {
                error("Unexpected diagnostic" +
                      "\nfound " + diagnostic.getCode() +
                      "\nexpected " + expectedCode +
                      "\ntestKind " + testKind +
                      "\nactionKind " + actionKind);
            }
        }
    }

    //global declarations

    enum EnumActionKind {
        REPLACE1("enum E { SOUTH, WEST, EAST; }") {
            @Override
            void doAction(T6550655 test) {
                test.replaceEnum(optionalSource);
            }
        },
        REPLACE2("@interface I { E valNew() default E.EAST; }") {
            @Override
            void doAction(T6550655 test) {
                test.replaceEnum(optionalSource);
            }
        },
        REMOVE(null) {
            @Override
            void doAction(T6550655 test) { test.removeEnum(); }
        };

        String optionalSource;

        private EnumActionKind(String optionalSource) {
            this.optionalSource = optionalSource;
        }

        abstract void doAction(T6550655 test);
    }

    enum TestKind {
        DIRECT("@I(val = E.NORTH)class C1 {}"),
        INDIRECT("class C2 { A a; }");

        JavaSource source;

        private TestKind(final String code) {
            this.source = new JavaSource("Test.java", code);
        }
    }

    public static void main(String[] args) throws Exception {
        String SCRATCH_DIR = System.getProperty("user.dir");
        JavaCompiler javacTool = ToolProvider.getSystemJavaCompiler();
        int n = 0;
        for (TestKind testKind : TestKind.values()) {
            for (EnumActionKind actionKind : EnumActionKind.values()) {
                File testDir = new File(SCRATCH_DIR, "test"+n);
                new T6550655(javacTool, testDir, testKind, actionKind).test();
                n++;
            }
        }
        if (nerrors > 0) {
            throw new AssertionError("Some errors have been detected");
        }
    }

    static class JavaSource extends SimpleJavaFileObject {

        String source;

        public JavaSource(String filename, String source) {
            super(URI.create("myfo:/" + filename), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    static int nerrors = 0;
}
