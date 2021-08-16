/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6945418 6993978 8006694 7196160 8129962
 * @summary Project Coin: Simplified Varargs Method Invocation
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main Warn4
 */

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class Warn4 extends ComboInstance<Warn4> {

    final static Warning[] error = null;
    final static Warning[] none = new Warning[] {};
    final static Warning[] vararg = new Warning[] { Warning.VARARGS };
    final static Warning[] unchecked = new Warning[] { Warning.UNCHECKED };
    final static Warning[] both = new Warning[] { Warning.VARARGS, Warning.UNCHECKED };

    enum Warning {
        UNCHECKED("generic.array.creation"),
        VARARGS("varargs.non.reifiable.type");

        String key;

        Warning(String key) {
            this.key = key;
        }

        boolean isSuppressed(TrustMe trustMe, SourceLevel source,
                SuppressLevel suppressLevelClient,
                SuppressLevel suppressLevelDecl,
                ModifierKind modKind) {
            switch(this) {
                case VARARGS:
                    return source.compareTo(SourceLevel.JDK_7) < 0 ||
                            suppressLevelDecl == SuppressLevel.UNCHECKED ||
                            trustMe == TrustMe.TRUST;
                case UNCHECKED:
                    return suppressLevelClient == SuppressLevel.UNCHECKED ||
                        (trustMe == TrustMe.TRUST &&
                         (((modKind == ModifierKind.FINAL || modKind == ModifierKind.STATIC) &&
                           source.compareTo( SourceLevel.JDK_7) >= 0 ) ||
                          (modKind == ModifierKind.PRIVATE && source.compareTo( SourceLevel.JDK_9) >= 0 )));
            }

            SuppressLevel supLev = this == VARARGS ?
                suppressLevelDecl :
                suppressLevelClient;
            return supLev == SuppressLevel.UNCHECKED ||
                    (trustMe == TrustMe.TRUST && modKind != ModifierKind.NONE);
        }
    }

    enum SourceLevel {
        JDK_7("7"),
        JDK_9("9");

        String sourceKey;

        SourceLevel(String sourceKey) {
            this.sourceKey = sourceKey;
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

    enum ModifierKind implements ComboParameter {
        NONE(" "),
        FINAL("final "),
        STATIC("static "),
        PRIVATE("private ");

        String mod;

        ModifierKind(String mod) {
            this.mod = mod;
        }

        @Override
        public String expand(String optParameter) {
            return mod;
        }
    }

    enum SuppressLevel implements ComboParameter {
        NONE(""),
        UNCHECKED("unchecked");

        String lint;

        SuppressLevel(String lint) {
            this.lint = lint;
        }

        @Override
        public String expand(String optParameter) {
            return "@SuppressWarnings(\"" + lint + "\")";
        }
    }

    enum Signature implements ComboParameter {
        UNBOUND("void #NAME(List<?>#ARITY arg) { #BODY }",
            new Warning[][] {none, none, none, none, error}),
        INVARIANT_TVAR("<Z> void #NAME(List<Z>#ARITY arg) { #BODY }",
            new Warning[][] {both, both, error, both, error}),
        TVAR("<Z> void #NAME(Z#ARITY arg) { #BODY }",
            new Warning[][] {both, both, both, both, vararg}),
        INVARIANT("void #NAME(List<String>#ARITY arg) { #BODY }",
            new Warning[][] {error, error, error, both, error}),
        UNPARAMETERIZED("void #NAME(String#ARITY arg) { #BODY }",
            new Warning[][] {error, error, error, error, none});

        String template;
        Warning[][] warnings;

        Signature(String template, Warning[][] warnings) {
            this.template = template;
            this.warnings = warnings;
        }

        boolean isApplicableTo(Signature other) {
            return warnings[other.ordinal()] != null;
        }

        boolean giveUnchecked(Signature other) {
            return warnings[other.ordinal()] == unchecked ||
                    warnings[other.ordinal()] == both;
        }

        boolean giveVarargs(Signature other) {
            return warnings[other.ordinal()] == vararg ||
                    warnings[other.ordinal()] == both;
        }

        @Override
        public String expand(String optParameter) {
            if (optParameter.equals("CLIENT")) {
                return template.replaceAll("#ARITY", "")
                        .replaceAll("#NAME", "test")
                        .replaceAll("#BODY", "m(arg)");
            } else {
                return template.replaceAll("#ARITY", "...")
                        .replaceAll("#NAME", "m")
                        .replaceAll("#BODY", "");
            }
        }
    }

    public static void main(String... args) {
        new ComboTestHelper<Warn4>()
                .withFilter(Warn4::badTestFilter)
                .withDimension("SOURCE", (x, level) -> x.sourceLevel = level, SourceLevel.values())
                .withDimension("TRUSTME", (x, trustme) -> x.trustMe = trustme, TrustMe.values())
                .withArrayDimension("SUPPRESS", (x, suppress, idx) -> x.suppress[idx] = suppress, 2, SuppressLevel.values())
                .withDimension("MOD", (x, mod) -> x.modKind = mod, ModifierKind.values())
                .withArrayDimension("MTH", (x, sig, idx) -> x.sigs[idx] = sig, 2, Signature.values())
                .run(Warn4::new);
    }

    SourceLevel sourceLevel;
    TrustMe trustMe;
    SuppressLevel[] suppress = new SuppressLevel[2];
    ModifierKind modKind;
    Signature[] sigs = new Signature[2];

    boolean badTestFilter() {
        return sigs[0].isApplicableTo(sigs[1]);
    }

    final String template = "import java.util.List;\n" +
                            "class Test {\n" +
                            "   #{TRUSTME} #{SUPPRESS[0]} #{MOD} #{MTH[0].VARARG}\n" +
                            "   #{SUPPRESS[1]} #{MTH[1].CLIENT}\n" +
                            "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withOption("-Xlint:unchecked")
                .withOption("-source")
                .withOption(sourceLevel.sourceKey)
                .withSourceFromTemplate(template)
                .analyze(this::check);
    }

    void check(Result<?> res) {
        boolean[] warnArr = new boolean[] {sigs[0].giveUnchecked(sigs[1]),
                               sigs[0].giveVarargs(sigs[1])};

        Set<Warning> warnings = new HashSet<>();
        for (Diagnostic<? extends JavaFileObject> d : res.diagnosticsForKind(Kind.MANDATORY_WARNING)) {
            if (d.getCode().contains(Warning.VARARGS.key)) {
                    warnings.add(Warning.VARARGS);
            } else if(d.getCode().contains(Warning.UNCHECKED.key)) {
                warnings.add(Warning.UNCHECKED);
            }
        }

        boolean badOutput = false;
        for (Warning wkind : Warning.values()) {
            boolean isSuppressed = wkind.isSuppressed(trustMe, sourceLevel,
                    suppress[1], suppress[0], modKind);
            badOutput |= (warnArr[wkind.ordinal()] && !isSuppressed) !=
                    warnings.contains(wkind);
        }
        if (badOutput) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nExpected unchecked warning: " + warnArr[0] +
                    "\nExpected unsafe vararg warning: " + warnArr[1] +
                    "\nWarnings: " + warnings +
                    "\nSource level: " + sourceLevel);
        }
    }
}
