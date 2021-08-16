/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8002099 8010822
 * @summary Add support for intersection types in cast expression
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.util.ListBuffer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class IntersectionTargetTypeTest {

    static int checkCount = 0;

    enum BoundKind {
        INTF,
        CLASS;
    }

    enum MethodKind {
        NONE(false),
        ABSTRACT_M(true),
        DEFAULT_M(false),
        ABSTRACT_G(true),
        DEFAULT_G(false);

        boolean isAbstract;

        MethodKind(boolean isAbstract) {
            this.isAbstract = isAbstract;
        }
    }

    enum TypeKind {
        A("interface A { }\n", "A", BoundKind.INTF, MethodKind.NONE),
        B("interface B { default void m() { } }\n", "B", BoundKind.INTF, MethodKind.DEFAULT_M),
        C("interface C { void m(); }\n", "C", BoundKind.INTF, MethodKind.ABSTRACT_M),
        D("interface D extends B { }\n", "D", BoundKind.INTF, MethodKind.DEFAULT_M),
        E("interface E extends C { }\n", "E", BoundKind.INTF, MethodKind.ABSTRACT_M),
        F("interface F extends C { void g(); }\n", "F", BoundKind.INTF, MethodKind.ABSTRACT_G, MethodKind.ABSTRACT_M),
        G("interface G extends B { void g(); }\n", "G", BoundKind.INTF, MethodKind.ABSTRACT_G, MethodKind.DEFAULT_M),
        H("interface H extends A { void g(); }\n", "H", BoundKind.INTF, MethodKind.ABSTRACT_G),
        OBJECT("", "Object", BoundKind.CLASS),
        STRING("", "String", BoundKind.CLASS);

        String declStr;
        String typeStr;
        BoundKind boundKind;
        MethodKind[] methodKinds;

        private TypeKind(String declStr, String typeStr, BoundKind boundKind, MethodKind... methodKinds) {
            this.declStr = declStr;
            this.typeStr = typeStr;
            this.boundKind = boundKind;
            this.methodKinds = methodKinds;
        }

        boolean compatibleSupertype(TypeKind tk) {
            if (tk == this) return true;
            switch (tk) {
                case B:
                    return this != C && this != E && this != F;
                case C:
                    return this != B && this != C && this != D && this != G;
                case D: return compatibleSupertype(B);
                case E:
                case F: return compatibleSupertype(C);
                case G: return compatibleSupertype(B);
                case H: return compatibleSupertype(A);
                default:
                    return true;
            }
        }
    }

    enum CastKind {
        ONE_ARY("(#B0)", 1),
        TWO_ARY("(#B0 & #B1)", 2),
        THREE_ARY("(#B0 & #B1 & #B2)", 3);

        String castTemplate;
        int nbounds;

        CastKind(String castTemplate, int nbounds) {
            this.castTemplate = castTemplate;
            this.nbounds = nbounds;
        }
    }

    enum ExpressionKind {
        LAMBDA("()->{}", true),
        MREF("this::m", true),
        //COND_LAMBDA("(true ? ()->{} : ()->{})", true), re-enable if spec allows this
        //COND_MREF("(true ? this::m : this::m)", true),
        STANDALONE("null", false);

        String exprString;
        boolean isFunctional;

        private ExpressionKind(String exprString, boolean isFunctional) {
            this.exprString = exprString;
            this.isFunctional = isFunctional;
        }
    }

    static class CastInfo {
        CastKind kind;
        TypeKind[] types;

        CastInfo(CastKind kind, TypeKind... types) {
            this.kind = kind;
            this.types = types;
        }

        String getCast() {
            String temp = kind.castTemplate;
            for (int i = 0; i < kind.nbounds ; i++) {
                temp = temp.replace(String.format("#B%d", i), types[i].typeStr);
            }
            return temp;
        }

        boolean wellFormed() {
            //check for duplicate types
            for (int i = 0 ; i < types.length ; i++) {
                for (int j = 0 ; j < types.length ; j++) {
                    if (i != j && types[i] == types[j]) {
                        return false;
                    }
                }
            }
            //check that classes only appear as first bound
            boolean classOk = true;
            for (int i = 0 ; i < types.length ; i++) {
                if (types[i].boundKind == BoundKind.CLASS &&
                        !classOk) {
                    return false;
                }
                classOk = false;
            }
            //check that supertypes are mutually compatible
            for (int i = 0 ; i < types.length ; i++) {
                for (int j = 0 ; j < types.length ; j++) {
                    if (!types[i].compatibleSupertype(types[j]) && i != j) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public static void main(String... args) throws Exception {
        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (CastInfo cInfo : allCastInfo()) {
                for (ExpressionKind ek : ExpressionKind.values()) {
                    new IntersectionTargetTypeTest(cInfo, ek).run(comp, fm);
                }
            }
            System.out.println("Total check executed: " + checkCount);
        }
    }

    static List<CastInfo> allCastInfo() {
        ListBuffer<CastInfo> buf = new ListBuffer<>();
        for (CastKind kind : CastKind.values()) {
            for (TypeKind b1 : TypeKind.values()) {
                if (kind.nbounds == 1) {
                    buf.append(new CastInfo(kind, b1));
                    continue;
                } else {
                    for (TypeKind b2 : TypeKind.values()) {
                        if (kind.nbounds == 2) {
                            buf.append(new CastInfo(kind, b1, b2));
                            continue;
                        } else {
                            for (TypeKind b3 : TypeKind.values()) {
                                buf.append(new CastInfo(kind, b1, b2, b3));
                            }
                        }
                    }
                }
            }
        }
        return buf.toList();
    }

    CastInfo cInfo;
    ExpressionKind ek;
    JavaSource source;
    DiagnosticChecker diagChecker;

    IntersectionTargetTypeTest(CastInfo cInfo, ExpressionKind ek) {
        this.cInfo = cInfo;
        this.ek = ek;
        this.source = new JavaSource();
        this.diagChecker = new DiagnosticChecker();
    }

    class JavaSource extends SimpleJavaFileObject {

        String bodyTemplate = "class Test {\n" +
                              "   void m() { }\n" +
                              "   void test() {\n" +
                              "      Object o = #C#E;\n" +
                              "   } }";

        String source = "";

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            for (TypeKind tk : TypeKind.values()) {
                source += tk.declStr;
            }
            source += bodyTemplate.replaceAll("#C", cInfo.getCast()).replaceAll("#E", ek.exprString);
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
            throw new AssertionError("Error thrown when compiling the following code:\n" + source.getCharContent(true));
        }
        check();
    }

    void check() {
        checkCount++;

        boolean errorExpected = !cInfo.wellFormed();

        if (ek.isFunctional) {
            List<MethodKind> mks = new ArrayList<>();
            for (TypeKind tk : cInfo.types) {
                if (tk.boundKind == BoundKind.CLASS && !tk.typeStr.equals("Object")) {
                    errorExpected = true;
                    break;
                } else {
                    mks = mergeMethods(mks, Arrays.asList(tk.methodKinds));
                }
            }
            int abstractCount = 0;
            for (MethodKind mk : mks) {
                if (mk.isAbstract) {
                    abstractCount++;
                }
            }
            errorExpected |= abstractCount != 1;
        }

        if (errorExpected != diagChecker.errorFound) {
            throw new Error("invalid diagnostics for source:\n" +
                source.getCharContent(true) +
                "\nFound error: " + diagChecker.errorFound +
                "\nExpected error: " + errorExpected);
        }
    }

    List<MethodKind> mergeMethods(List<MethodKind> l1, List<MethodKind> l2) {
        List<MethodKind> mergedMethods = new ArrayList<>(l1);
        for (MethodKind mk2 : l2) {
            boolean add = !mergedMethods.contains(mk2);
            switch (mk2) {
                case ABSTRACT_G:
                    add = add && !mergedMethods.contains(MethodKind.DEFAULT_G);
                    break;
                case ABSTRACT_M:
                    add = add && !mergedMethods.contains(MethodKind.DEFAULT_M);
                    break;
                case DEFAULT_G:
                    mergedMethods.remove(MethodKind.ABSTRACT_G);
                case DEFAULT_M:
                    mergedMethods.remove(MethodKind.ABSTRACT_M);
                case NONE:
                    add = false;
                    break;
            }
            if (add) {
                mergedMethods.add(mk2);
            }
        }
        return mergedMethods;
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
