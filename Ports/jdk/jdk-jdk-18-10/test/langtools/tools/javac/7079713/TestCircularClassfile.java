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

/*
 * @test
 * @bug 7079713
 * @summary javac hangs when compiling a class that references a cyclically inherited class
 * @modules jdk.compiler
 * @run main TestCircularClassfile
 */

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

public class TestCircularClassfile {

    enum SourceKind {
        A_EXTENDS_B("class B {} class A extends B { void m() {} }"),
        B_EXTENDS_A("class A { void m() {} } class B extends A {}");

        String sourceStr;

        private SourceKind(String sourceStr) {
            this.sourceStr = sourceStr;
        }

        SimpleJavaFileObject getSource() {
            return new SimpleJavaFileObject(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return sourceStr;
                }
            };
        }
    }

    enum TestKind {
        REPLACE_A("A.class"),
        REPLACE_B("B.class");

        String targetClass;

        private TestKind(String targetClass) {
            this.targetClass = targetClass;
        }
    }

    enum ClientKind {
        METHOD_CALL1("A a = null; a.m();"),
        METHOD_CALL2("B b = null; b.m();"),
        CONSTR_CALL1("new A();"),
        CONSTR_CALL2("new B();"),
        ASSIGN1("A a = null; B b = a;"),
        ASSIGN2("B b = null; A a = b;");

        String mainMethod;

        private ClientKind(String mainMethod) {
            this.mainMethod = mainMethod;
        }

        SimpleJavaFileObject getSource() {
            return new SimpleJavaFileObject(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return "class Test { public static void main(String[] args) { #M } }"
                            .replace("#M", mainMethod);
                }
            };
        }
    }

    public static void main(String... args) throws Exception {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            int count = 0;
            for (SourceKind sk1 : SourceKind.values()) {
                for (SourceKind sk2 : SourceKind.values()) {
                    for (TestKind tk : TestKind.values()) {
                        for (ClientKind ck : ClientKind.values()) {
                            new TestCircularClassfile("sub_"+count++, sk1, sk2, tk, ck).check(comp, fm);
                        }
                    }
                }
            }
        }
    }

    static String workDir = System.getProperty("user.dir");

    String destPath;
    SourceKind sk1;
    SourceKind sk2;
    TestKind tk;
    ClientKind ck;

    TestCircularClassfile(String destPath, SourceKind sk1, SourceKind sk2, TestKind tk, ClientKind ck) {
        this.destPath = destPath;
        this.sk1 = sk1;
        this.sk2 = sk2;
        this.tk = tk;
        this.ck = ck;
    }

    void check(JavaCompiler comp, StandardJavaFileManager fm) throws Exception {
        //step 1: compile first source code in the test subfolder
        File destDir = new File(workDir, destPath); destDir.mkdir();
        //output dir must be set explicitly as we are sharing the fm (see bug 7026941)
        fm.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, Arrays.asList(destDir));
        JavacTask ct = (JavacTask)comp.getTask(null, fm, null,
                null, null, Arrays.asList(sk1.getSource()));
        ct.generate();

        //step 2: compile second source code in a temp folder
        File tmpDir = new File(destDir, "tmp"); tmpDir.mkdir();
        //output dir must be set explicitly as we are sharing the fm (see bug 7026941)
        fm.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, Arrays.asList(tmpDir));
        ct = (JavacTask)comp.getTask(null, fm, null,
                null, null, Arrays.asList(sk2.getSource()));
        ct.generate();

        //step 3: move a classfile from the temp folder to the test subfolder
        File fileToMove = new File(tmpDir, tk.targetClass);
        File target = new File(destDir, tk.targetClass);
        target.delete();
        boolean success = fileToMove.renameTo(target);

        if (!success) {
            throw new AssertionError("error when moving file " + tk.targetClass);
        }

        //step 4: compile the client class against the classes in the test subfolder
        //input/output dir must be set explicitly as we are sharing the fm (see bug 7026941)
        fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(destDir));
        fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(destDir));
        ct = (JavacTask)comp.getTask(null, fm, null,
                null, null, Arrays.asList(ck.getSource()));

        ct.generate();
    }
}
