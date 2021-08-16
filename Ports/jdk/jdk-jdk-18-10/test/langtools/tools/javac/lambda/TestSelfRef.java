/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  Check that self/forward references from lambda expressions behave
 *          consistently w.r.t. local inner classes
 * @modules jdk.compiler
 */

import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;

public class TestSelfRef {

    static int checkCount = 0;

    enum RefKind {
        SELF_LAMBDA("SAM s = x->{ System.out.println(s); };", true, false),
        FORWARD_LAMBDA("SAM s = x->{ System.out.println(f); };\nObject f = null;", false, true),
        SELF_ANON("Object s = new Object() { void test() { System.out.println(s); } };", true, false),
        FORWARD_ANON("Object s = new Object() { void test() { System.out.println(f); } }; Object f = null;", false, true);

        String refStr;
        boolean selfRef;
        boolean forwardRef;

        private RefKind(String refStr, boolean selfRef, boolean forwardRef) {
            this.refStr = refStr;
            this.selfRef = selfRef;
            this.forwardRef = forwardRef;
        }
    }

    enum EnclosingKind {
        TOPLEVEL("class C { #S }"),
        MEMBER_INNER("class Outer { class C { #S } }"),
        NESTED_INNER("class Outer { static class C { #S } }");

        String enclStr;

        private EnclosingKind(String enclStr) {
            this.enclStr = enclStr;
        }
    }

    enum InnerKind {
        NONE("#R"),
        LOCAL_NONE("class Local { #R }"),
        LOCAL_MTH("class Local { void test() { #R } }"),
        ANON_NONE("new Object() { #R };"),
        ANON_MTH("new Object() { void test() { #R } };");

        String innerStr;

        private InnerKind(String innerStr) {
            this.innerStr = innerStr;
        }

        boolean inMethodContext(SiteKind sk) {
            switch (this) {
                case LOCAL_MTH:
                case ANON_MTH: return true;
                case NONE: return sk != SiteKind.NONE;
                default:
                    return false;
            }
        }
    }

    enum SiteKind {
        NONE("#I"),
        STATIC_INIT("static { #I }"),
        INSTANCE_INIT("{ #I }"),
        CONSTRUCTOR("C() { #I }"),
        METHOD("void test() { #I }");

        String siteStr;

        private SiteKind(String siteStr) {
            this.siteStr = siteStr;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (EnclosingKind ek : EnclosingKind.values()) {
                for (SiteKind sk : SiteKind.values()) {
                    if (sk == SiteKind.STATIC_INIT && ek == EnclosingKind.MEMBER_INNER)
                        continue;
                    for (InnerKind ik : InnerKind.values()) {
                        if (ik != InnerKind.NONE && sk == SiteKind.NONE)
                            break;
                        for (RefKind rk : RefKind.values()) {
                            new TestSelfRef(ek, sk, ik, rk).run(comp, fm);
                        }
                    }
                }
            }
            System.out.println("Total check executed: " + checkCount);
        }
    }

    EnclosingKind ek;
    SiteKind sk;
    InnerKind ik;
    RefKind rk;
    JavaSource source;
    DiagnosticChecker diagChecker;

    TestSelfRef(EnclosingKind ek, SiteKind sk, InnerKind ik, RefKind rk) {
        this.ek = ek;
        this.sk = sk;
        this.ik = ik;
        this.rk = rk;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String bodyTemplate = "interface SAM { void test(Object o); }\n#B";
        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = bodyTemplate.replace("#B",
                    ek.enclStr.replace("#S", sk.siteStr.replace("#I", ik.innerStr.replace("#R", rk.refStr))));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                null, null, Arrays.asList(source));
        try {
            ct.analyze();
        } catch (Throwable ex) {
            throw new AssertionError("Error thron when compiling the following code:\n" + source.getCharContent(true));
        }
        check();
    }

    boolean isErrorExpected() {
        //illegal forward ref
        boolean result = ik.inMethodContext(sk) && (rk.selfRef || rk.forwardRef);
        result |= (rk == RefKind.SELF_LAMBDA || rk == RefKind.FORWARD_LAMBDA);
        return result;
    }

    void check() {
        checkCount++;
        boolean errorExpected = isErrorExpected();
        if (diagChecker.errorFound != errorExpected) {
            throw new Error("invalid diagnostics for source:\n" +
                source.getCharContent(true) +
                "\nFound error: " + diagChecker.errorFound +
                "\nExpected error: " + errorExpected);
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }
}
