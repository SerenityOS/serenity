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
 * @bug 7023233
 * @summary False positive for -Xlint:try with nested try with resources blocks
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.JCDiagnostic;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class UnusedResourcesTest {

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

    enum TwrStmt {
        TWR1("res1"),
        TWR2("res2"),
        TWR3("res3");

        final String resourceName;

        private TwrStmt(String resourceName) {
            this.resourceName = resourceName;
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

    enum ResourceUsage {
        NONE(null),
        USE_R1(TwrStmt.TWR1),
        USE_R2(TwrStmt.TWR2),
        USE_R3(TwrStmt.TWR3);

        TwrStmt stmt;

        private ResourceUsage(TwrStmt stmt) {
            this.stmt = stmt;
        }

        String usedResourceName() {
            return stmt != null ? stmt.resourceName : null;
        }

        boolean isUsedIn(TwrStmt res, TwrStmt stmt) {
            return this.stmt == res &&
                    stmt.ordinal() >= this.stmt.ordinal();
        }

        String getUsage(TwrStmt stmt) {
            return this != NONE && stmt.ordinal() >= this.stmt.ordinal() ?
                "use(" + usedResourceName() + ");" :
                "";
        }
    }

    static class JavaSource extends SimpleJavaFileObject {

        String template = "class Resource implements AutoCloseable {\n" +
                              "public void close() {}\n" +
                          "}\n" +
                          "class Test {\n" +
                              "void use(Resource r) {}\n" +
                              "#S void test() {\n" +
                                 "try (Resource #R1 = new Resource()) {\n" +
                                    "#U1_R1\n" +
                                    "#U1_R2\n" +
                                    "#U1_R3\n" +
                                    "try (Resource #R2 = new Resource()) {\n" +
                                       "#U2_R1\n" +
                                       "#U2_R2\n" +
                                       "#U2_R3\n" +
                                       "try (Resource #R3 = new Resource()) {\n" +
                                           "#U3_R1\n" +
                                           "#U3_R2\n" +
                                           "#U3_R3\n" +
                                       "}\n" +
                                    "}\n" +
                                 "}\n" +
                              "}\n" +
                          "}\n";

        String source;

        public JavaSource(SuppressLevel suppressLevel, ResourceUsage usage1,
                ResourceUsage usage2, ResourceUsage usage3) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replace("#S", suppressLevel.getSuppressAnno()).
                    replace("#R1", TwrStmt.TWR1.resourceName).
                    replace("#R2", TwrStmt.TWR2.resourceName).
                    replace("#R3", TwrStmt.TWR3.resourceName).
                    replace("#U1_R1", usage1.getUsage(TwrStmt.TWR1)).
                    replace("#U1_R2", usage2.getUsage(TwrStmt.TWR1)).
                    replace("#U1_R3", usage3.getUsage(TwrStmt.TWR1)).
                    replace("#U2_R1", usage1.getUsage(TwrStmt.TWR2)).
                    replace("#U2_R2", usage2.getUsage(TwrStmt.TWR2)).
                    replace("#U2_R3", usage3.getUsage(TwrStmt.TWR2)).
                    replace("#U3_R1", usage1.getUsage(TwrStmt.TWR3)).
                    replace("#U3_R2", usage2.getUsage(TwrStmt.TWR3)).
                    replace("#U3_R3", usage3.getUsage(TwrStmt.TWR3));
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws Exception {
        try {
            for (XlintOption xlint : XlintOption.values()) {
                for (SuppressLevel suppressLevel : SuppressLevel.values()) {
                    for (ResourceUsage usage1 : ResourceUsage.values()) {
                        for (ResourceUsage usage2 : ResourceUsage.values()) {
                            for (ResourceUsage usage3 : ResourceUsage.values()) {
                                    test(xlint,
                                            suppressLevel,
                                            usage1,
                                            usage2,
                                            usage3);
                            }
                        }
                    }
                }
            }
        } finally {
            fm.close();
        }
    }

    // Create a single file manager and reuse it for each compile to save time.
    static StandardJavaFileManager fm = JavacTool.create().getStandardFileManager(null, null, null);

    static void test(XlintOption xlint, SuppressLevel suppressLevel, ResourceUsage usage1,
                ResourceUsage usage2, ResourceUsage usage3) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavaSource source = new JavaSource(suppressLevel, usage1, usage2, usage3);
        DiagnosticChecker dc = new DiagnosticChecker();
        JavacTask ct = (JavacTask)tool.getTask(null, fm, dc,
                Arrays.asList(xlint.getXlintOption()), null, Arrays.asList(source));
        ct.analyze();
        check(source, xlint, suppressLevel, usage1, usage2, usage3, dc);
    }

    static void check(JavaSource source, XlintOption xlint, SuppressLevel suppressLevel,
                ResourceUsage usage1, ResourceUsage usage2, ResourceUsage usage3, DiagnosticChecker dc) {

        ResourceUsage[] usages = { usage1, usage2, usage3 };
        boolean[] unusedFound = { dc.unused_r1, dc.unused_r2, dc.unused_r3 };
        boolean[] usedResources = { false, false, false };

        for (TwrStmt res : TwrStmt.values()) {
            outer: for (TwrStmt stmt : TwrStmt.values()) {
                for (ResourceUsage usage : usages) {
                    if (usage.isUsedIn(res, stmt)) {
                        usedResources[res.ordinal()] = true;
                        break outer;
                    }
                }
            }
        }

        for (TwrStmt stmt : TwrStmt.values()) {
            boolean unused = !usedResources[stmt.ordinal()] &&
                    xlint == XlintOption.TRY &&
                    suppressLevel != SuppressLevel.SUPPRESS;
            if (unused != unusedFound[stmt.ordinal()]) {
                throw new Error("invalid diagnostics for source:\n" +
                    source.getCharContent(true) +
                    "\nOptions: " + xlint.getXlintOption() +
                    "\nFound unused res1: " + unusedFound[0] +
                    "\nFound unused res2: " + unusedFound[1] +
                    "\nFound unused res3: " + unusedFound[2] +
                    "\nExpected unused res1: " + !usedResources[0] +
                    "\nExpected unused res2: " + !usedResources[1] +
                    "\nExpected unused res3: " + !usedResources[2]);
            }
        }
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean unused_r1 = false;
        boolean unused_r2 = false;
        boolean unused_r3 = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.WARNING &&
                    diagnostic.getCode().contains("try.resource.not.referenced")) {
                String varName = unwrap(diagnostic).getArgs()[0].toString();
                if (varName.equals(TwrStmt.TWR1.resourceName)) {
                    unused_r1 = true;
                } else if (varName.equals(TwrStmt.TWR2.resourceName)) {
                    unused_r2 = true;
                } else if (varName.equals(TwrStmt.TWR3.resourceName)) {
                    unused_r3 = true;
                }
            }
        }

        private JCDiagnostic unwrap(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic instanceof JCDiagnostic)
                return (JCDiagnostic) diagnostic;
            if (diagnostic instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper)
                return ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diagnostic).d;
            throw new IllegalArgumentException();
        }
    }
}
