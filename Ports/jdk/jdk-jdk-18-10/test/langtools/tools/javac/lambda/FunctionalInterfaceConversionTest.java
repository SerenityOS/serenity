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

/**
 * @test
 * @bug 8003280 8004102 8006694 8129962
 * @summary Add lambda tests
 *  perform several automated checks in lambda conversion, esp. around accessibility
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main FunctionalInterfaceConversionTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;


public class FunctionalInterfaceConversionTest extends ComboInstance<FunctionalInterfaceConversionTest> {

    enum PackageKind implements ComboParameter {
        NO_PKG(""),
        PKG_A("a");

        String pkg;

        PackageKind(String pkg) {
            this.pkg = pkg;
        }

        @Override
        public String expand(String optParameter) {
            return this == NO_PKG ?
                "" :
                "package " + pkg + ";";
        }

        String getImportStat() {
            return this == NO_PKG ?
                "" :
                "import " + pkg + ".*;";
        }
    }

    enum SamKind implements ComboParameter {
        CLASS("public class Sam {  }"),
        ABSTACT_CLASS("public abstract class Sam {  }"),
        ANNOTATION("public @interface Sam {  }"),
        ENUM("public enum Sam { }"),
        INTERFACE("public interface Sam { \n #{METH1}; \n }");

        String sam_str;

        SamKind(String sam_str) {
            this.sam_str = sam_str;
        }

        @Override
        public String expand(String optParameter) {
            return sam_str;
        }
    }

    enum ModifierKind implements ComboParameter {
        PUBLIC("public"),
        PACKAGE("");

        String modifier_str;

        ModifierKind(String modifier_str) {
            this.modifier_str = modifier_str;
        }

        @Override
        public String expand(String optParameter) {
            return modifier_str;
        }
    }

    enum TypeKind implements ComboParameter {
        EXCEPTION("Exception"),
        PKG_CLASS("PackageClass");

        String typeStr;

        TypeKind(String typeStr) {
            this.typeStr = typeStr;
        }

        @Override
        public String expand(String optParameter) {
            return typeStr;
        }
    }

    enum ExprKind implements ComboParameter {
        LAMBDA("x -> null"),
        MREF("this::m");

        String exprStr;

        ExprKind(String exprStr) {
            this.exprStr = exprStr;
        }

        @Override
        public String expand(String optParameter) {
            return exprStr;
        }
    }

    enum MethodKind implements ComboParameter {
        NONE(""),
        NON_GENERIC("public abstract #{RET} m(#{ARG} s) throws #{THROWN};"),
        GENERIC("public abstract <X> #{RET} m(#{ARG} s) throws #{THROWN};");

        String methodTemplate;

        MethodKind(String methodTemplate) {
            this.methodTemplate = methodTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return methodTemplate;
        }
    }

    public static void main(String[] args) throws Exception {
        new ComboTestHelper<FunctionalInterfaceConversionTest>()
                .withDimension("PKG", (x, pkg) -> x.samPkg = pkg, PackageKind.values())
                .withDimension("MOD", (x, mod) -> x.modKind = mod, ModifierKind.values())
                .withDimension("CLAZZ", (x, sam) -> x.samKind = sam, SamKind.values())
                .withDimension("METH1", (x, meth) -> x.samMeth = meth, MethodKind.values())
                .withDimension("METH2", (x, meth) -> x.clientMeth = meth, MethodKind.values())
                .withDimension("RET", (x, ret) -> x.retType = ret, TypeKind.values())
                .withDimension("ARG", (x, arg) -> x.argType = arg, TypeKind.values())
                .withDimension("THROWN", (x, thrown) -> x.thrownType = thrown, TypeKind.values())
                .withDimension("EXPR", (x, expr) -> x.exprKind = expr, ExprKind.values())
                .run(FunctionalInterfaceConversionTest::new);
    }

    PackageKind samPkg;
    ModifierKind modKind;
    SamKind samKind;
    MethodKind samMeth;
    MethodKind clientMeth;
    TypeKind retType;
    TypeKind argType;
    TypeKind thrownType;
    ExprKind exprKind;

    String samSource = "#{PKG} \n #{CLAZZ}";
    String pkgClassSource = "#{PKG}\n #{MOD} class PackageClass extends Exception { }";
    String clientSource = "#{IMP}\n abstract class Client { \n" +
                           "  Sam s = #{EXPR};\n" +
                           "  #{METH2} \n }";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate("Sam", samSource)
                .withSourceFromTemplate("PackageClass", pkgClassSource)
                .withSourceFromTemplate("Client", clientSource, this::importStmt)
                .analyze(this::check);
    }

    ComboParameter importStmt(String name) {
        switch (name) {
            case "IMP": return new ComboParameter.Constant<>(samPkg.getImportStat());
            default: return null;
        }
    }

    void check(Result<?> res) {
        if (res.hasErrors() == checkSamConversion()) {
            fail("Unexpected compilation result; " + res.compilationInfo());
        }
    }

    boolean checkSamConversion() {
        if (samKind != SamKind.INTERFACE) {
            //sam type must be an interface
            return false;
        } else if (samMeth == MethodKind.NONE) {
            //interface must have at least a method
            return false;
        } else if (exprKind == ExprKind.LAMBDA &&
                samMeth != MethodKind.NON_GENERIC) {
            //target method for lambda must be non-generic
            return false;
        } else if (exprKind == ExprKind.MREF &&
                clientMeth == MethodKind.NONE) {
            return false;
        } else if (samPkg != PackageKind.NO_PKG &&
                modKind != ModifierKind.PUBLIC &&
                (retType == TypeKind.PKG_CLASS ||
                argType == TypeKind.PKG_CLASS ||
                thrownType == TypeKind.PKG_CLASS)) {
            //target must not contain inaccessible types
            return false;
        } else {
            return true;
        }
    }
}
