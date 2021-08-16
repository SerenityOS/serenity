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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test SAM conversion of lambda expressions in combinations of different contexts,
 *           lambda body types(statement/expression), explict/implicit target type etc, to verify
 *           SAM conversion being conducted successfully as expected.
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class SamConversionComboTest {

    enum FInterface {
        A("A", "interface A { Integer m(int i); }"),
        B("B", "interface B { int m(Integer i); }"),
        C("C", "interface C { int m(Integer i) throws Exception; }");

        String interfaceType;
        String interfaceDef;

        FInterface(String interfaceType, String interfaceDef) {
            this.interfaceType = interfaceType;
            this.interfaceDef = interfaceDef;
        }

        String getParameterType() {
            switch(this) {
            case A:
                return "int";
            case B:
            case C:
                return "Integer";
            default:
                return null;
            }
        }
    }

    enum Context {
        ASSIGNMENT("#FType f = #LBody;"),
        METHOD_CALL("void method1(#FType f) { }\n" +
                    "    void method2() {\n" +
                    "        method1(#LBody);\n" +
                    "    }"),
        CONSTRUCTOR("X x = new X(#LBody);"),
        RETURN_OF_METHOD("#FType method1() {\n" +
                         "    return #LBody;\n" +
                         "}"),
        ARRAY_INITIALIZER("Object[] oarray = {\"a\", 1, (#FType)#LBody};"),
        LAMBDA_BODY("#FType f = n -> ((#FType)#LBody).m(n);"),
        CAST("void test() throws Exception { int n = ((#FType)#LBody).m(1); }"),
        CONDITIONAL_EXPRESSION("#FType f = 2 > 1 ? #LBody : null;");

        String context;

        Context(String context) {
            this.context = context;
        }

        String getContext(FInterface f, LambdaKind lk, LambdaBody lb, ReturnValue rv) {
            return context.replace("#FType", f.interfaceType).replace("#LBody", lb.getLambdaBody(f, lk, rv));
        }
    }

    enum LambdaKind {
        EXPRESSION("#VAL"),
        STATEMENT("{return #VAL;}"),
        EXCEPTION_STMT("{throw new Exception();}");

        String stmt;

        LambdaKind(String stmt) {
            this.stmt = stmt;
        }
    }

    enum ReturnValue {
        INT("i + 1"),
        INTEGER("new Integer(i+1)"),
        INT2("i.intValue() + 1"),
        STRING("i + \"\""),
        DOUBLE("i * 1.0");

        String rValue;

        ReturnValue(String rValue) {
            this.rValue = rValue;
        }
    }

    enum LambdaBody {
        IMPLICIT("i -> #RET"),//type inferred
        EXPLICIT("(#Type i) -> #RET");//explicit type

        String bodyStr;

        LambdaBody(String bodyStr) {
            this.bodyStr = bodyStr;
        }

        String getLambdaBody(FInterface fi, LambdaKind lk, ReturnValue rv) {
            return bodyStr.replace("#Type", fi.getParameterType()).replace("#RET", lk.stmt.replace("#VAL", rv.rValue));
        }
    }

    boolean checkSamConversion() {
        if(lambdaKind != LambdaKind.EXCEPTION_STMT && (returnValue == ReturnValue.DOUBLE || returnValue == ReturnValue.STRING)) //return type mismatch
            return false;
        if(context != Context.CONSTRUCTOR) {//context other than construcotr argument
            if(fInterface != FInterface.C && lambdaKind == LambdaKind.EXCEPTION_STMT)
                return false;
            if(fInterface == FInterface.A && returnValue == ReturnValue.INT2)
                return false;
        }
        else { //constructor argument context
            //match X(A a) or X(B b) or X(C c)
            if (lambdaKind == LambdaKind.EXCEPTION_STMT) {
                return false; //ambiguous target type
            }
            else if(lambdaBody == LambdaBody.IMPLICIT) {
                return false;
            }
            else { //explicit parameter type
                if(fInterface.getParameterType().equals("Integer")) //ambiguous target type
                //e.g. X x = new X((Integer i) -> i + 1);
                    return false;
                if(returnValue == ReturnValue.INT2)
                //e.g. X x = new X(int i -> i.intValue() + 1);
                    return false;
            }
        }
        return true;
    }

    SourceFile samSourceFile = new SourceFile("FInterface.java", "#C") {
        public String toString() {
            String interfaces = "";
            for(FInterface fi : FInterface.values())
                interfaces += fi.interfaceDef + "\n";
            return template.replace("#C", interfaces);
        }
    };

    String clientTemplate = "class Client {\n" +
                            "    #Context\n" +
                            "}\n\n" +

                            "class X {\n" +
                            "    int value = 0;\n\n" +

                            "    X(A a) {\n" +
                            "        value = a.m(6);\n" +
                            "    }\n\n" +

                            "    X(B b) {\n" +
                            "        value = b.m(7);\n" +
                            "    }\n\n" +

                            "    X(C c) {\n" +
                            "        try {\n" +
                            "            value = c.m(8);\n" +
                            "        } catch (Exception e){}\n" +
                            "    }\n" +
                            "}";
    SourceFile clientSourceFile = new SourceFile("Client.java", clientTemplate) {
        public String toString() {
            return template.replace("#Context", context.getContext(fInterface, lambdaKind, lambdaBody, returnValue));
        }
    };

    void test() throws Exception {
        System.out.println("\n====================================");
        System.out.println(fInterface + ", " +  context + ", " + lambdaKind + ", " + lambdaBody + ", " + returnValue);
        System.out.println(samSourceFile + "\n");
        String clientFileStr = clientSourceFile.toString();
        System.out.println(clientFileStr.substring(0, clientFileStr.indexOf("\n\n")));

        DiagnosticChecker dc = new DiagnosticChecker();
        JavacTask ct = (JavacTask)comp.getTask(null, fm, dc, null, null, Arrays.asList(samSourceFile, clientSourceFile));
        try {
            ct.analyze();
        } catch (Exception e) {
            throw new AssertionError("failing SAM source file \n" + samSourceFile + "\n\n" + "failing client source file \n"+ clientSourceFile);
        }
        if (dc.errorFound == checkSamConversion()) {
            throw new AssertionError(samSourceFile + "\n\n" + clientSourceFile);
        }
        count++;
    }

    abstract class SourceFile extends SimpleJavaFileObject {

        protected String template;

        public SourceFile(String filename, String template) {
            super(URI.create("myfo:/" + filename), JavaFileObject.Kind.SOURCE);
            this.template = template;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return toString();
        }

        public abstract String toString();
    }

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errorFound = true;
            }
        }
    }

    FInterface fInterface;
    Context context;
    LambdaBody lambdaBody;
    LambdaKind lambdaKind;
    ReturnValue returnValue;
    static int count = 0;

    static JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    static JavaFileManager fm = comp.getStandardFileManager(null, null, null);

    SamConversionComboTest(FInterface f, Context c, LambdaBody lb, LambdaKind lk, ReturnValue rv) {
        fInterface = f;
        context = c;
        lambdaKind = lk;
        lambdaBody = lb;
        returnValue = rv;
    }

    public static void main(String[] args) throws Exception {
        try {
            for(Context ct : Context.values()) {
                for (FInterface fi : FInterface.values()) {
                    for (LambdaKind lk: LambdaKind.values()) {
                        for (LambdaBody lb : LambdaBody.values()) {
                            for(ReturnValue rv : ReturnValue.values()) {
                                new SamConversionComboTest(fi, ct, lb, lk, rv).test();
                            }
                        }
                    }
                }
            }
        System.out.println("total tests: " + count);
        } finally {
            fm.close();
        }
    }
}
