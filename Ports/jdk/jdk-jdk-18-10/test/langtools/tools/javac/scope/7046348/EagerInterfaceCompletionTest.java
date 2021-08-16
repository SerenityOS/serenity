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
 * @bug     7046348
 * @summary Regression: javac complains of missing classfile for a seemingly unrelated interface
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class EagerInterfaceCompletionTest {

    JavaCompiler javacTool;
    File testDir;
    VersionKind versionKind;
    HierarchyKind hierarchyKind;
    TestKind testKind;
    ActionKind actionKind;

    EagerInterfaceCompletionTest(JavaCompiler javacTool, File testDir, VersionKind versionKind,
            HierarchyKind hierarchyKind, TestKind testKind, ActionKind actionKind) {
        this.javacTool = javacTool;
        this.versionKind = versionKind;
        this.hierarchyKind = hierarchyKind;
        this.testDir = testDir;
        this.testKind = testKind;
        this.actionKind = actionKind;
    }

    void test() {
        testDir.mkdirs();
        compile(null, hierarchyKind.source);
        actionKind.doAction(this);
        DiagnosticChecker dc = new DiagnosticChecker();
        compile(dc, testKind.source);
        if (testKind.completionFailure(versionKind, actionKind, hierarchyKind) != dc.errorFound) {
            if (dc.errorFound) {
                error("Unexpected completion failure" +
                      "\nhierarhcyKind " + hierarchyKind +
                      "\ntestKind " + testKind +
                      "\nactionKind " + actionKind);
            } else {
                error("Missing completion failure " +
                          "\nhierarhcyKind " + hierarchyKind +
                          "\ntestKind " + testKind +
                          "\nactionKind " + actionKind);
            }
        }
    }

    void compile(DiagnosticChecker dc, JavaSource... sources) {
        try {
            CompilationTask ct = javacTool.getTask(null, null, dc,
                    Arrays.asList("-d", testDir.getAbsolutePath(), "-cp",
                    testDir.getAbsolutePath(), versionKind.optsArr[0], versionKind.optsArr[1]),
                    null, Arrays.asList(sources));
            ct.call();
        }
        catch (Exception e) {
            e.printStackTrace();
            error("Internal compilation error");
        }
    }

    void removeClass(String classToRemoveStr) {
        File classToRemove = new File(testDir, classToRemoveStr);
        if (!classToRemove.exists()) {
            error("Expected file " + classToRemove + " does not exists in folder " + testDir);
        }
        classToRemove.delete();
    };

    void error(String msg) {
        System.err.println(msg);
        nerrors++;
    }

    class DiagnosticChecker implements DiagnosticListener<JavaFileObject> {

        boolean errorFound = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }

    //global declarations

    enum VersionKind {
        PRE_LAMBDA("-source", "7"),
        LAMBDA("-source", "8");

        String[] optsArr;

        VersionKind(String... optsArr) {
            this.optsArr = optsArr;
        }
    }

    enum HierarchyKind {
        INTERFACE("interface A { boolean f = false; void m(); }\n" +
                  "class B implements A { public void m() {} }"),
        CLASS("class A { boolean f; void m() {} }\n" +
              "class B extends A { void m() {} }"),
        ABSTRACT_CLASS("abstract class A { boolean f; abstract void m(); }\n" +
                       "class B extends A { void m() {} }");

        JavaSource source;

        private HierarchyKind(String code) {
            this.source = new JavaSource("Test1.java", code);
        }
    }

    enum ActionKind {
        REMOVE_A("A.class"),
        REMOVE_B("B.class");

        String classFile;

        private ActionKind(String classFile) {
            this.classFile = classFile;
        }

        void doAction(EagerInterfaceCompletionTest test) {
            test.removeClass(classFile);
        };
    }

    enum TestKind {
        ACCESS_ONLY("class C { B b; }"),
        SUPER("class C extends B {}"),
        METHOD("class C { void test(B b) { b.m(); } }"),
        FIELD("class C { void test(B b) { boolean b2 = b.f; } }"),
        CONSTR("class C { void test() { new B(); } }");

        JavaSource source;

        private TestKind(final String code) {
            this.source = new JavaSource("Test2.java", code);
        }

        boolean completionFailure(VersionKind vk, ActionKind ak, HierarchyKind hk) {
            switch (this) {
                case ACCESS_ONLY:
                case CONSTR: return ak == ActionKind.REMOVE_B;
                case FIELD:
                case SUPER: return true;
                case METHOD: return hk != HierarchyKind.INTERFACE || ak == ActionKind.REMOVE_B ||
                        (hk == HierarchyKind.INTERFACE && ak == ActionKind.REMOVE_A);
                default: throw new AssertionError("Unexpected test kind " + this);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String SCRATCH_DIR = System.getProperty("user.dir");
        JavaCompiler javacTool = ToolProvider.getSystemJavaCompiler();
        int n = 0;
        for (VersionKind versionKind : VersionKind.values()) {
            for (HierarchyKind hierarchyKind : HierarchyKind.values()) {
                for (TestKind testKind : TestKind.values()) {
                    for (ActionKind actionKind : ActionKind.values()) {
                        File testDir = new File(SCRATCH_DIR, "test"+n);
                        new EagerInterfaceCompletionTest(javacTool, testDir, versionKind,
                                hierarchyKind, testKind, actionKind).test();
                        n++;
                    }
                }
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
