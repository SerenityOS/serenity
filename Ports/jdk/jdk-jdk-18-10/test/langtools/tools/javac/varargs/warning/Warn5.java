/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6993978 7097436 8006694 7196160
 * @summary Project Coin: Annotation to reduce varargs warnings
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main/othervm Warn5
 */

import java.io.IOException;
import java.util.EnumSet;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;


public class Warn5 extends ComboInstance<Warn5> {

    enum XlintOption {
        NONE("none"),
        ALL("all");

        String opt;

        XlintOption(String opt) {
            this.opt = opt;
        }

        String getXlintOption() {
            return "-Xlint:" + opt;
        }
    }

    enum TrustMe implements ComboParameter {
        DONT_TRUST(""),
        TRUST("@java.lang.SafeVarargs");

        String anno;

        TrustMe(String anno) {
            this.anno = anno;
        }

        @Override
        public String expand(String optParameter) {
            return anno;
        }
    }

    enum SuppressLevel implements ComboParameter {
        NONE,
        VARARGS;

        @Override
        public String expand(String optParameter) {
            return this == VARARGS ?
                "@SuppressWarnings(\"varargs\")" :
                "";
        }
    }

    enum ModifierKind implements ComboParameter {
        NONE(""),
        FINAL("final"),
        STATIC("static"),
        PRIVATE("private");

        String mod;

        ModifierKind(String mod) {
            this.mod = mod;
        }

        @Override
        public String expand(String optParameter) {
            return mod;
        }
    }

    enum MethodKind implements ComboParameter {
        METHOD("void m"),
        CONSTRUCTOR("Test");

        String name;

        MethodKind(String name) {
            this.name = name;
        }

        @Override
        public String expand(String optParameter) {
            return name;
        }
    }

    enum SourceLevel {
        JDK_6("6"),
        JDK_7("7"),
        JDK_9("9");

        String sourceKey;

        SourceLevel(String sourceKey) {
            this.sourceKey = sourceKey;
        }
    }

    enum SignatureKind implements ComboParameter {
        VARARGS_X("<X>#{NAME}(X... x)", false, true),
        VARARGS_STRING("#{NAME}(String... x)", true, true),
        ARRAY_X("<X>#{NAME}(X[] x)", false, false),
        ARRAY_STRING("#{NAME}(String[] x)", true, false);

        String stub;
        boolean isReifiableArg;
        boolean isVarargs;

        SignatureKind(String stub, boolean isReifiableArg, boolean isVarargs) {
            this.stub = stub;
            this.isReifiableArg = isReifiableArg;
            this.isVarargs = isVarargs;
        }

        @Override
        public String expand(String optParameter) {
            return stub;
        }
    }

    enum BodyKind implements ComboParameter {
        ASSIGN("Object o = x;", true),
        CAST("Object o = (Object)x;", true),
        METH("test(x);", true),
        PRINT("System.out.println(x.toString());", false),
        ARRAY_ASSIGN("Object[] o = x;", true),
        ARRAY_CAST("Object[] o = (Object[])x;", true),
        ARRAY_METH("testArr(x);", true);

        String body;
        boolean hasAliasing;

        BodyKind(String body, boolean hasAliasing) {
            this.body = body;
            this.hasAliasing = hasAliasing;
        }

        @Override
        public String expand(String optParameter) {
            return body;
        }
    }

    enum WarningKind {
        UNSAFE_BODY("compiler.warn.varargs.unsafe.use.varargs.param"),
        UNSAFE_DECL("compiler.warn.unchecked.varargs.non.reifiable.type"),
        MALFORMED_SAFEVARARGS("compiler.err.varargs.invalid.trustme.anno"),
        REDUNDANT_SAFEVARARGS("compiler.warn.varargs.redundant.trustme.anno");

        String code;

        WarningKind(String code) {
            this.code = code;
        }
    }

    public static void main(String[] args) {
        new ComboTestHelper<Warn5>()
                .withFilter(Warn5::badTestFilter)
                .withDimension("SOURCE", (x, level) -> x.sourceLevel = level, SourceLevel.values())
                .withDimension("LINT", (x, lint) -> x.xlint = lint, XlintOption.values())
                .withDimension("TRUSTME", (x, trustme) -> x.trustMe = trustme, TrustMe.values())
                .withDimension("SUPPRESS", (x, suppress) -> x.suppressLevel = suppress, SuppressLevel.values())
                .withDimension("MOD", (x, mod) -> x.modKind = mod, ModifierKind.values())
                .withDimension("NAME", (x, name) -> x.methKind = name, MethodKind.values())
                .withDimension("SIG", (x, sig) -> x.sig = sig, SignatureKind.values())
                .withDimension("BODY", (x, body) -> x.body = body, BodyKind.values())
                .run(Warn5::new);
    }

    SourceLevel sourceLevel;
    XlintOption xlint;
    TrustMe trustMe;
    SuppressLevel suppressLevel;
    ModifierKind modKind;
    MethodKind methKind;
    SignatureKind sig;
    BodyKind body;

    boolean badTestFilter() {
        return (methKind != MethodKind.CONSTRUCTOR || modKind == ModifierKind.NONE);
    }

    String template = "import com.sun.tools.javac.api.*;\n" +
                      "import java.util.List;\n" +
                      "class Test {\n" +
                      "   static void test(Object o) {}\n" +
                      "   static void testArr(Object[] o) {}\n" +
                      "   #{TRUSTME} #{SUPPRESS} #{MOD} #{SIG} { #{BODY} }\n" +
                      "}\n";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption(xlint.getXlintOption())
                .withOption("-source")
                .withOption(sourceLevel.sourceKey)
                .withSourceFromTemplate(template)
                .analyze(this::check);
    }

    void check(Result<?> res) {

        EnumSet<WarningKind> foundWarnings = EnumSet.noneOf(WarningKind.class);
        for (Diagnostic.Kind kind : new Kind[] { Kind.ERROR, Kind.MANDATORY_WARNING, Kind.WARNING}) {
            for (Diagnostic<? extends JavaFileObject> diag : res.diagnosticsForKind(kind)) {
                for (WarningKind wk : WarningKind.values()) {
                    if (wk.code.equals(diag.getCode())) {
                        foundWarnings.add(wk);
                    }
                }
            }
        }

        EnumSet<WarningKind> expectedWarnings =
                EnumSet.noneOf(WarningKind.class);

        if (sourceLevel.compareTo(SourceLevel.JDK_7) >= 0 &&
                trustMe == TrustMe.TRUST &&
                suppressLevel != SuppressLevel.VARARGS &&
                xlint != XlintOption.NONE &&
                sig.isVarargs &&
                !sig.isReifiableArg &&
                body.hasAliasing &&
                (methKind == MethodKind.CONSTRUCTOR ||
                (methKind == MethodKind.METHOD &&
                 modKind == ModifierKind.FINAL || modKind == ModifierKind.STATIC ||
                 (modKind == ModifierKind.PRIVATE && sourceLevel.compareTo(SourceLevel.JDK_9) >= 0)))) {
            expectedWarnings.add(WarningKind.UNSAFE_BODY);
        }

        if (sourceLevel.compareTo(SourceLevel.JDK_7) >= 0 &&
                trustMe == TrustMe.DONT_TRUST &&
                sig.isVarargs &&
                !sig.isReifiableArg &&
                xlint == XlintOption.ALL) {
            expectedWarnings.add(WarningKind.UNSAFE_DECL);
        }

        if (sourceLevel.compareTo(SourceLevel.JDK_7) >= 0 &&
                trustMe == TrustMe.TRUST &&
                (!sig.isVarargs ||
                 ((modKind == ModifierKind.NONE ||
                 modKind == ModifierKind.PRIVATE && sourceLevel.compareTo(SourceLevel.JDK_9) < 0 ) &&
                 methKind == MethodKind.METHOD))) {
            expectedWarnings.add(WarningKind.MALFORMED_SAFEVARARGS);
        }

        if (sourceLevel.compareTo(SourceLevel.JDK_7) >= 0 &&
                trustMe == TrustMe.TRUST &&
                xlint != XlintOption.NONE &&
                suppressLevel != SuppressLevel.VARARGS &&
                (modKind == ModifierKind.FINAL || modKind == ModifierKind.STATIC ||
                 (modKind == ModifierKind.PRIVATE && sourceLevel.compareTo(SourceLevel.JDK_9) >= 0) ||
                 methKind == MethodKind.CONSTRUCTOR) &&
                sig.isVarargs &&
                sig.isReifiableArg) {
            expectedWarnings.add(WarningKind.REDUNDANT_SAFEVARARGS);
        }

        if (!expectedWarnings.containsAll(foundWarnings) ||
                !foundWarnings.containsAll(expectedWarnings)) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nOptions: " + xlint.getXlintOption() +
                    "\nSource Level: " + sourceLevel +
                    "\nExpected warnings: " + expectedWarnings +
                    "\nFound warnings: " + foundWarnings);
        }
    }
}
