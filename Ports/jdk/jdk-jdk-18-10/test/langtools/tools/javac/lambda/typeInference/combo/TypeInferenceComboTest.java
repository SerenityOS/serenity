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
 * @bug 8003280 8006694 8129962
 * @summary Add lambda tests
 *  perform automated checks in type inference in lambda expressions
 *  in different contexts
 *  temporarily workaround combo tests are causing time out in several platforms
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @compile  TypeInferenceComboTest.java
 * @run main TypeInferenceComboTest
 */

import java.io.IOException;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class TypeInferenceComboTest extends ComboInstance<TypeInferenceComboTest> {
    enum Context {
        ASSIGNMENT("SAM#Type s = #LBody;"),
        METHOD_CALL("#GenericDeclKind void method1(SAM#Type s) { }\n" +
                    "void method2() {\n" +
                    "    method1(#LBody);\n" +
                    "}"),
        RETURN_OF_METHOD("SAM#Type method1() {\n" +
                "    return #LBody;\n" +
                "}"),
        LAMBDA_RETURN_EXPRESSION("SAM2 s2 = () -> {return (SAM#Type)#LBody;};\n"),
        ARRAY_INITIALIZER("Object[] oarray = {\"a\", 1, (SAM#Type)#LBody};");

        String context;

        Context(String context) {
            this.context = context;
        }

        String getContext(SamKind sk, TypeKind samTargetT, Keyword kw,
                TypeKind parameterT, TypeKind returnT, LambdaKind lk,
                ParameterKind pk, GenericDeclKind gdk, LambdaBody lb) {
            String result = context;
            if (sk == SamKind.GENERIC) {
                if(this == Context.METHOD_CALL) {
                    result = result.replaceAll("#GenericDeclKind",
                            gdk.getGenericDeclKind(samTargetT));
                    if(gdk == GenericDeclKind.NON_GENERIC)
                        result = result.replaceAll("#Type", "<" +
                                samTargetT.typeStr + ">");
                    else //#GenericDeclKind is <T> or <T extends xxx>
                        result = result.replaceAll("#Type", "<T>");
                }
                else {
                    if(kw == Keyword.VOID)
                        result = result.replaceAll("#Type", "<" +
                                samTargetT.typeStr + ">");
                    else
                        result = result.replaceAll("#Type", "<? " + kw.keyStr +
                                " " + samTargetT.typeStr + ">");
                }
            }
            else
                result = result.replaceAll("#Type", "").
                        replaceAll("#GenericDeclKind", "");

            return result.replaceAll("#LBody",
                    lb.getLambdaBody(samTargetT, parameterT, returnT, lk, pk));
        }
    }

    enum SamKind {
        GENERIC("interface SAM<T> { #R m(#ARG); }"),
        NON_GENERIC("interface SAM { #R m(#ARG); }");

        String sam_str;

        SamKind(String sam_str) {
            this.sam_str = sam_str;
        }

        String getSam(TypeKind parameterT, TypeKind returnT) {
            return sam_str.replaceAll("#ARG",
                    parameterT == TypeKind.VOID ?
                        "" : parameterT.typeStr + " arg")
                    .replaceAll("#R", returnT.typeStr);
        }
    }

    enum TypeKind {
        VOID("void", ""),
        STRING("String", "\"hello\""),
        INTEGER("Integer", "1"),
        INT("int", "0"),
        COMPARATOR("java.util.Comparator<String>",
                "(java.util.Comparator<String>)(a, b) -> a.length()-b.length()"),
        SAM("SAM2", "null"),
        GENERIC("T", null);

        String typeStr;
        String valStr;

        TypeKind(String typeStr, String valStr) {
            this.typeStr = typeStr;
            this.valStr = valStr;
        }
    }

    enum LambdaKind {
        EXPRESSION("#VAL"),
        STATEMENT("{return #VAL;}");

        String stmt;

        LambdaKind(String stmt) {
            this.stmt = stmt;
        }
    }

    enum ParameterKind {
        EXPLICIT("#TYPE"),
        IMPLICIT("");

        String paramTemplate;

        ParameterKind(String paramTemplate) {
             this.paramTemplate = paramTemplate;
        }
    }

    enum Keyword {
        SUPER("super"),
        EXTENDS("extends"),
        VOID("");

        String keyStr;

        Keyword(String keyStr) {
            this.keyStr = keyStr;
        }
    }

    enum LambdaBody {
        //no parameters, return type is one of the TypeKind
        RETURN_VOID("() -> #RET"),
        //has parameters, return type is one of the TypeKind
        RETURN_ARG("(#PK arg) -> #RET");

        String bodyStr;

        LambdaBody(String bodyStr) {
            this.bodyStr = bodyStr;
        }

        String getLambdaBody(TypeKind samTargetT, TypeKind parameterT,
                TypeKind returnT, LambdaKind lk, ParameterKind pk) {
            String result = bodyStr.replaceAll("#PK", pk.paramTemplate);

            if(result.contains("#TYPE")) {
                if (parameterT == TypeKind.GENERIC && this != RETURN_VOID)
                    result = result.replaceAll("#TYPE",
                            samTargetT == null? "": samTargetT.typeStr);
                else
                    result = result.replaceAll("#TYPE", parameterT.typeStr);
            }
            if (this == RETURN_ARG && parameterT == returnT)
                return result.replaceAll("#RET", lk.stmt.replaceAll("#VAL", "arg"));
            else {
                if(returnT != TypeKind.GENERIC)
                    return result.replaceAll("#RET", lk.stmt.replaceAll("#VAL",
                            (returnT==TypeKind.VOID &&
                            lk==LambdaKind.EXPRESSION) ? "{}" : returnT.valStr));
                else
                    return result.replaceAll("#RET",
                            lk.stmt.replaceAll("#VAL", samTargetT.valStr));
            }
        }
    }

    enum GenericDeclKind {
        NON_GENERIC(""),
        GENERIC_NOBOUND("<T>"),
        GENERIC_BOUND("<T extends #ExtendedType>");
        String typeStr;

        GenericDeclKind(String typeStr) {
            this.typeStr = typeStr;
        }

        String getGenericDeclKind(TypeKind et) {
            return typeStr.replaceAll("#ExtendedType", et==null? "":et.typeStr);
        }
    }

    public static void main(String[] args) {
        new ComboTestHelper<TypeInferenceComboTest>()
                .withFilter(TypeInferenceComboTest::badTestFilter)
                .withFilter(TypeInferenceComboTest::redundantTestFilter)
                .withDimension("SAM", (x, sam) -> x.samKind = sam, SamKind.values())
                .withDimension("SAMTARGET", (x, target) -> x.samTargetType = target, TypeKind.values())
                .withDimension("PARAMTYPE", (x, param) -> x.parameterType = param, TypeKind.values())
                .withDimension("RETTYPE", (x, ret) -> x.returnType = ret, TypeKind.values())
                .withDimension("CTX", (x, ctx) -> x.context = ctx, Context.values())
                .withDimension("LAMBDABODY", (x, body) -> x.lambdaBodyType = body, LambdaBody.values())
                .withDimension("LAMBDAKIND", (x, lambda) -> x.lambdaKind = lambda, LambdaKind.values())
                .withDimension("PARAMKIND", (x, param) -> x.parameterKind = param, ParameterKind.values())
                .withDimension("KEYWORD", (x, kw) -> x.keyword = kw, Keyword.values())
                .withDimension("GENDECL", (x, gk) -> x.genericDeclKind = gk, GenericDeclKind.values())
                .run(TypeInferenceComboTest::new);
    }

    SamKind samKind;
    TypeKind samTargetType;
    TypeKind parameterType;
    TypeKind returnType;
    Context context;
    LambdaBody lambdaBodyType;
    LambdaKind lambdaKind;
    ParameterKind parameterKind;
    Keyword keyword;
    GenericDeclKind genericDeclKind;

    boolean badTestFilter() {
        if (samKind == SamKind.NON_GENERIC) {
            return (parameterType != TypeKind.GENERIC && returnType != TypeKind.GENERIC);
        } else {
            return (samTargetType != TypeKind.VOID &&
                   samTargetType != TypeKind.INT &&
                   samTargetType != TypeKind.GENERIC &&
                   (parameterType == TypeKind.GENERIC ||
                   returnType == TypeKind.GENERIC));
        }
    }

    boolean redundantTestFilter() {
        if (samKind == SamKind.NON_GENERIC) {
            return keyword.ordinal() == 0 && samTargetType.ordinal() == 0 && genericDeclKind.ordinal() == 0;
        } else {
            return context == Context.METHOD_CALL || genericDeclKind.ordinal() == 0;
        }
    }

    String sam_template = "#{SAM}\n" +
                         "interface SAM2 {\n" +
                         "    SAM m();\n" +
                         "}\n";


    String client_template = "class Client { \n" +
                             "    #{CONTEXT}\n" +
                             "}";

    @Override
    public void doWork() throws IOException {
        newCompilationTask()
                .withSourceFromTemplate("Sam", sam_template, this::samClass)
                .withSourceFromTemplate("Client", client_template, this::clientContext)
                .analyze(res -> {
            if (res.hasErrors() == checkTypeInference()) {
                fail("Unexpected compilation output when compiling instance: " + res.compilationInfo());
            }
        });
    }

    ComboParameter samClass(String parameterName) {
        switch (parameterName) {
            case "SAM":
                return new ComboParameter.Constant<>(samKind.getSam(parameterType, returnType));
            default:
                return null;
        }
    }

    ComboParameter clientContext(String parameterName) {
        switch (parameterName) {
            case "CONTEXT":
                return new ComboParameter.Constant<>(context.getContext(samKind, samTargetType,
                        keyword, parameterType, returnType, lambdaKind, parameterKind, genericDeclKind, lambdaBodyType));
            default:
                return null;
        }
    }

    boolean checkTypeInference() {
        if (parameterType == TypeKind.VOID) {
            if (lambdaBodyType != LambdaBody.RETURN_VOID)
                return false;
        }
        else if (lambdaBodyType != LambdaBody.RETURN_ARG)
            return false;

        return true;
    }
}
