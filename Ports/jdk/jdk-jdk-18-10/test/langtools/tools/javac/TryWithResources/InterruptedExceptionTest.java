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
 * @bug 7027157
 * @summary Project Coin: javac warnings for AutoCloseable.close throwing InterruptedException
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class InterruptedExceptionTest {

    enum XlintOption {
        NONE("none"),
        TRY("try");

        String opt;

        XlintOption(String opt) {
            this.opt = opt;
        }

        String getXlintOption() {
            return "-Xlint:" + opt;
        }
    }

    enum SuppressLevel {
        NONE,
        SUPPRESS;

        String getSuppressAnno() {
            return this == SUPPRESS ?
                "@SuppressWarnings(\"try\")" :
                "";
        }
    }

    enum ClassKind {
        ABSTRACT_CLASS("abstract class", "implements", false),
        CLASS("class", "implements", true),
        INTERFACE("interface", "extends", false);

        String kindName;
        String extendsClause;
        boolean hasBody;

        private ClassKind(String kindName, String extendsClause, boolean hasBody) {
            this.kindName = kindName;
            this.extendsClause = extendsClause;
            this.hasBody = hasBody;
        }

        String getBody() {
            return hasBody ? "{}" : ";";
        }
    }

    enum ExceptionKind {
        NONE("", false),
        EXCEPTION("Exception", true),
        INTERRUPTED_EXCEPTION("InterruptedException", true),
        ILLEGAL_ARGUMENT_EXCEPTION("IllegalArgumentException", false),
        X("X", false);

        String exName;
        boolean shouldWarn;

        private ExceptionKind(String exName, boolean shouldWarn) {
            this.exName = exName;
            this.shouldWarn = shouldWarn;
        }

        String getThrowsClause() {
            return this == NONE ? "" : "throws " + exName;
        }

        String getTypeArguments(ExceptionKind decl) {
            return (decl != X || this == NONE) ? "" : "<" + exName + ">";
        }

        String getTypeParameter() {
            return this == X ? "<X extends Exception>" : "";
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (XlintOption xlint : XlintOption.values()) {
                for (SuppressLevel suppress_decl : SuppressLevel.values()) {
                    for (SuppressLevel suppress_use : SuppressLevel.values()) {
                        for (ClassKind ck : ClassKind.values()) {
                            for (ExceptionKind ek_decl : ExceptionKind.values()) {
                                for (ExceptionKind ek_use : ExceptionKind.values()) {
                                    new InterruptedExceptionTest(xlint, suppress_decl,
                                            suppress_use, ck, ek_decl, ek_use).run(comp, fm);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    XlintOption xlint;
    SuppressLevel suppress_decl;
    SuppressLevel suppress_use;
    ClassKind ck;
    ExceptionKind ek_decl;
    ExceptionKind ek_use;
    JavaSource source;
    DiagnosticChecker diagChecker;

    InterruptedExceptionTest(XlintOption xlint, SuppressLevel suppress_decl, SuppressLevel suppress_use,
            ClassKind ck, ExceptionKind ek_decl, ExceptionKind ek_use) {
        this.xlint = xlint;
        this.suppress_decl = suppress_decl;
        this.suppress_use = suppress_use;
        this.ck = ck;
        this.ek_decl = ek_decl;
        this.ek_use = ek_use;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "#S1 #CK Resource#G #EC AutoCloseable {\n" +
                              "public void close() #TK #BK\n" +
                          "}\n" +
                          "class Test {\n" +
                              "#S2 <X> void test() {\n" +
                                 "try (Resource#PK r = null) { }\n" +
                              "}\n" +
                          "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replace("#S1", suppress_decl.getSuppressAnno())
                    .replace("#S2", suppress_use.getSuppressAnno())
                    .replace("#CK", ck.kindName)
                    .replace("#EC", ck.extendsClause)
                    .replace("#G", ek_decl.getTypeParameter())
                    .replace("#TK", ek_decl.getThrowsClause())
                    .replace("#BK", ck.getBody())
                    .replace("#PK", ek_use.getTypeArguments(ek_decl));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                Arrays.asList(xlint.getXlintOption()), null, Arrays.asList(source));
        ct.analyze();
        check();
    }

    void check() {

        boolean shouldWarnDecl = ek_decl.shouldWarn &&
                xlint == XlintOption.TRY &&
                suppress_decl != SuppressLevel.SUPPRESS;

        boolean shouldWarnUse = (ek_decl.shouldWarn ||
                ((ek_use.shouldWarn || ek_use == ExceptionKind.NONE) && ek_decl == ExceptionKind.X)) &&
                xlint == XlintOption.TRY &&
                suppress_use != SuppressLevel.SUPPRESS;

        int foundWarnings = 0;

        if (shouldWarnDecl) foundWarnings++;
        if (shouldWarnUse) foundWarnings++;

        if (foundWarnings != diagChecker.tryWarnFound) {
            throw new Error("invalid diagnostics for source:\n" +
                source.getCharContent(true) +
                "\nOptions: " + xlint.getXlintOption() +
                "\nFound warnings: " + diagChecker.tryWarnFound +
                "\nExpected decl warning: " + shouldWarnDecl +
                "\nExpected use warning: " + shouldWarnUse);
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        int tryWarnFound;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.WARNING &&
                    diagnostic.getCode().contains("try.resource.throws.interrupted.exc")) {
                tryWarnFound++;
            }
        }
    }
}
