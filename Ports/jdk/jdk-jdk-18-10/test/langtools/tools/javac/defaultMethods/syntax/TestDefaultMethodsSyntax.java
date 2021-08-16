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
 * @bug 7192245 8005851 8005166 8071453
 * @summary Automatic test for checking set of allowed modifiers on interface methods
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;


public class TestDefaultMethodsSyntax {

    static int checkCount = 0;

    enum VersionKind {
        PRE_LAMBDA("7"),
        LAMBDA("8"),
        POST_LAMBDA("9");

        String versionString;

        VersionKind(String versionString) {
            this.versionString = versionString;
        }

        List<String> getOptions() {
            return Arrays.asList("-XDallowStaticInterfaceMethods", "-source", versionString);
        }
    }

    enum ModifierKind {
        NONE(""),
        PUBLIC("public"),
        PROTECTED("protected"),
        PRIVATE("private"),
        ABSTRACT("abstract"),
        STATIC("static"),
        NATIVE("native"),
        SYNCHRONIZED("synchronized"),
        FINAL("final"),
        STRICTFP("strictfp"),
        DEFAULT("default");

        String modStr;

        private ModifierKind(String modStr) {
            this.modStr = modStr;
        }

        static boolean intersect(ModifierKind mk, ModifierKind... mks) {
            for (ModifierKind mk2 : mks) {
                if (mk == mk2) return true;
            }
            return false;
        }

        static boolean compatible(MethodKind mk, ModifierKind mod1, ModifierKind mod2, EnclosingKind ek) {
            if (intersect(ABSTRACT, mod1, mod2) || intersect(NATIVE, mod1, mod2)) {
                return mk == MethodKind.NO_BODY;
            } else if (intersect(DEFAULT, mod1, mod2) || intersect(STATIC, mod1, mod2)
                    || intersect(PRIVATE, mod1, mod2)) {
                return mk == MethodKind.BODY;
            } else {
                return ek == EnclosingKind.INTERFACE ?
                        mk == MethodKind.NO_BODY : mk == MethodKind.BODY;
            }
        }

        boolean compatible(EnclosingKind ek) {
            switch (this) {
                case PROTECTED:
                    return ek != EnclosingKind.INTERFACE;
                default:
                    return true;
            }
        }

        static boolean compatible(ModifierKind m1, ModifierKind m2, EnclosingKind ek) {
            Result res1 = allowedModifierPairs[m1.ordinal()][m2.ordinal()];
            Result res2 = allowedModifierPairs[m2.ordinal()][m1.ordinal()];
            if (res1 != res2) {
                throw new AssertionError(String.format("Ill-formed table: [%s,%s] != [%s,%s]", m1, m2, m2, m1));
            } else {
                return res1.compatible(ek, m1, m2);
            }
        }

        interface Result {
            boolean compatible(EnclosingKind ek, ModifierKind m1, ModifierKind m2);
        }

        static final Result T = new Result() {
            @Override
            public boolean compatible(EnclosingKind ek, ModifierKind m1, ModifierKind m2) {
                return true;
            }
        };

        static final Result F = new Result() {
            @Override
            public boolean compatible(EnclosingKind ek, ModifierKind m1, ModifierKind m2) {
                return false;
            }
        };

        static final Result C = new Result() {
            @Override
            public boolean compatible(EnclosingKind ek, ModifierKind m1, ModifierKind m2) {
                return ek != EnclosingKind.INTERFACE;
            }
        };

        static final Result I = new Result() {
            @Override
            public boolean compatible(EnclosingKind ek, ModifierKind m1, ModifierKind m2) {
                return ek == EnclosingKind.INTERFACE;
            }
        };

        static Result[][] allowedModifierPairs = {
            /*                     NONE  PUBLIC  PROTECTED  PRIVATE  ABSTRACT  STATIC  NATIVE  SYNCHRONIZED  FINAL  STRICTFP  DEFAULT */
            /* NONE */           { T   , T    , C        , T       , T       , T     , C     , C           , C    , C       , I   },
            /* PUBLIC */         { T   , F    , F        , F       , T       , T     , C     , C           , C    , C       , I   },
            /* PROTECTED */      { C   , F    , F        , F       , C       , C     , C     , C           , C    , C       , F   },
            /* PRIVATE */        { T   , F    , F        , F       , F       , T     , C     , C           , C    , T       , F   },
            /* ABSTRACT */       { T   , T    , C        , F       , F       , F     , F     , F           , F    , F       , F   },
            /* STATIC */         { T   , T    , C        , T       , F       , F     , C     , C           , C    , T       , F   },
            /* NATIVE */         { C   , C    , C        , C       , F       , C     , F     , C           , C    , F       , F   },
            /* SYNCHRONIZED */   { C   , C    , C        , C       , F       , C     , C     , F           , C    , C       , F   },
            /* FINAL */          { C   , C    , C        , C       , F       , C     , C     , C           , F    , C       , F   },
            /* STRICTFP */       { C   , C    , C        , T       , F       , T     , F     , C           , C    , F       , I   },
            /* DEFAULT */        { I   , I    , F        , F       , F       , F     , F     , F           , F    , I       , F   }};
    }

    enum MethodKind {
        NO_BODY("void m();"),
        BODY("void m() { }");

        String methStr;

        private MethodKind(String methStr) {
            this.methStr = methStr;
        }
    }

    enum EnclosingKind {
        ABSTRACT_CLASS("abstract class Test "),
        INTERFACE("interface Test ");

        String enclStr;

        EnclosingKind(String enclStr) {
            this.enclStr = enclStr;
        }
    }

    public static void main(String... args) throws Exception {

        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (VersionKind vk : VersionKind.values()) {
                for (EnclosingKind ek : EnclosingKind.values()) {
                    for (MethodKind mk : MethodKind.values()) {
                        for (ModifierKind modk1 : ModifierKind.values()) {
                            for (ModifierKind modk2 : ModifierKind.values()) {
                                new TestDefaultMethodsSyntax(vk, ek, mk, modk1, modk2).run(comp, fm);
                            }
                        }
                    }
                }
            }
            System.out.println("Total check executed: " + checkCount);
        }
    }

    VersionKind vk;
    EnclosingKind ek;
    MethodKind mk;
    ModifierKind modk1, modk2;
    JavaSource source;
    DiagnosticChecker diagChecker;

    TestDefaultMethodsSyntax(VersionKind vk, EnclosingKind ek, MethodKind mk, ModifierKind modk1, ModifierKind modk2) {
        this.vk = vk;
        this.ek = ek;
        this.mk = mk;
        this.modk1 = modk1;
        this.modk2 = modk2;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "#EK {\n" +
                          "   #MOD1 #MOD2 #METH\n" +
                          "}\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replaceAll("#EK", ek.enclStr)
                    .replaceAll("#MOD1", modk1.modStr)
                    .replaceAll("#MOD2", modk2.modStr)
                    .replaceAll("#METH", mk.methStr);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    void run(JavaCompiler tool, StandardJavaFileManager fm) throws Exception {
        JavacTask ct = (JavacTask)tool.getTask(null, fm, diagChecker,
                vk.getOptions(), null, Arrays.asList(source));
        try {
            ct.analyze();
        } catch (Throwable ex) {
            throw new AssertionError("Error thrown when analyzing the following source:\n" + source.getCharContent(true));
        }
        check();
    }

    void check() {
        boolean errorExpected = !ModifierKind.compatible(modk1, modk2, ek);

        errorExpected |= !ModifierKind.compatible(mk, modk1, modk2, ek);

        errorExpected |= !modk1.compatible(ek) || !modk2.compatible(ek);

        errorExpected |= ModifierKind.intersect(ModifierKind.DEFAULT, modk1, modk2) &&
                vk == VersionKind.PRE_LAMBDA;

        errorExpected |= ModifierKind.intersect(ModifierKind.STATIC, modk1, modk2) &&
                ek == EnclosingKind.INTERFACE && vk == VersionKind.PRE_LAMBDA;

        errorExpected |= ModifierKind.intersect(ModifierKind.PRIVATE, modk1, modk2) &&
                ek == EnclosingKind.INTERFACE && (vk == VersionKind.LAMBDA || vk == VersionKind.PRE_LAMBDA);

        checkCount++;
        if (diagChecker.errorFound != errorExpected) {
            throw new AssertionError("Problem when compiling source:\n" + source.getCharContent(true) +
                    "\nfound error: " + diagChecker.errorFound);
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
