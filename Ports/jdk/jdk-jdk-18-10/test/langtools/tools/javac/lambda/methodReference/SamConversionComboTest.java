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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test SAM conversion of method references in combinations of different contexts,
 *           lambda body types(statement/expression), boxing/unboxing etc, to verify
 *           SAM conversion being conducted successfully as expected.
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;

public class SamConversionComboTest {

    enum FInterface {
        A("A", "interface A { Integer m(int i); }"),
        B("B", "interface B { int m(Integer i); }"),
        C("C", "interface C { int m(int i) throws Exception; }");

        String interfaceType;
        String interfaceDef;

        FInterface(String interfaceType, String interfaceDef) {
            this.interfaceType = interfaceType;
            this.interfaceDef = interfaceDef;
        }
    }

    enum Context {
        ASSIGNMENT("#FType f = #MR;"),
        METHOD_CALL("void method1(#FType f) { }\n" +
                    "void method2() {\n" +
                    "    method1(#MR);\n" +
                    "}"),
        CONSTRUCTOR("X x = new X(#MR);"),
        RETURN_OF_METHOD("#FType method1() {\n" +
                         "    return #MR;\n" +
                         "}"),
        ARRAY_INITIALIZER("#FType[] oarray = {#MR};"),
        LAMBDA_BODY("#FType f = n -> ((#FType)#MR).m(n);"),
        CAST("void test() throws Exception { int n = ((#FType)#MR).m(1); }"),
        CONDITIONAL_EXPRESSION("#FType f = 2 > 1 ? #MR : null;");

        String context;

        Context(String context) {
            this.context = context;
        }

        String getContext(FInterface f, MethodReference mr) {
            return context.replace("#FType", f.interfaceType).replace("#MR", mr.mrValue);
        }
    }

    enum MethodReference {
        METHOD1("X::method1"),
        METHOD2("new X()::method2"),
        METHOD3("X::method3"),
        METHOD4("new X()::method4"),
        METHOD5("new X()::method5"),
        METHOD6("X::method6"),
        METHOD7("X::method7"),
        METHOD8("X::method8");

        String mrValue;

        MethodReference(String mr) {
            mrValue = mr;
        }
    }

    enum MethodDef {
        METHOD1("    static Integer method1(int n) {\n" +
                "        return n + 1;\n" +
                "    }\n", 0),
        METHOD2("    int method2(Integer n) {\n" +
                "        return value == 0 ? n + 2 : n + value;\n" +
                "    }\n", 1),
        METHOD3("    static int method3(int n) {\n" +
                "        return n + 3;\n" +
                "    }\n", 2),
        METHOD4("    Integer method4(Integer n) {\n" +
                "        return value == 0 ? n + 4 : n + value;\n" +
                "    }\n", 3),
        METHOD5("    Integer method5(Integer n) {\n" +
                "        return value == 0 ? new Integer(n + 5) : new Integer(n + value);\n" +
                "    }\n", 4),
        METHOD6("    static int method6(Integer n) throws Exception{\n" +
                "        throw new Exception();\n" +
                "    }\n", 5),
        METHOD7("    static int method7(String s){\n" +
                "        return s.length();\n" +
                "    }\n", 6),
        METHOD8("    static String method8(Integer n){\n" +
                "        return n + \"\";\n" +
                "    }\n", 7);

        String methodStr;
        int index;

        MethodDef(String ms, int i) {
            methodStr = ms;
            index = i;
        }

        MethodReference getMethodReference() {
            return MethodReference.values()[index];
        }
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

                            "    X() {\n" +
                            "    }\n\n" +

                            "    X(A a) {\n" +
                            "        value = a.m(9);\n" +
                            "    }\n\n" +

                            "    X(B b) {\n" +
                            "        value = b.m(9);\n" +
                            "    }\n\n" +

                            "    X(C c) {\n" +
                            "        try {\n" +
                            "            value = c.m(9);\n" +
                            "        } catch (Exception e){}\n" +
                            "    }\n\n" +

                            "#MethodDef" +
                            "}";

    SourceFile clientSourceFile = new SourceFile("Client.java", clientTemplate) {
        public String toString() {
            return template.replace("#Context", context.getContext(fInterface, methodReference)).replace("#MethodDef", methodDef.methodStr);
        }
    };

    boolean checkSamConversion() {
        if(methodDef == MethodDef.METHOD7 || methodDef == MethodDef.METHOD8)//method signature mismatch
            return false;
        if(context != Context.CONSTRUCTOR && fInterface != FInterface.C && methodDef == MethodDef.METHOD6)
        //method that throws exceptions not thrown by the interface method is a mismatch
            return false;
        if(context == Context.CONSTRUCTOR)
               return false;
        return true;
    }

    void test() throws Exception {
        System.out.println("\n====================================");
        System.out.println(fInterface + ", " +  context + ", " + methodReference);
        System.out.println(samSourceFile + "\n" + clientSourceFile);

        DiagnosticChecker dc = new DiagnosticChecker();
        JavacTask ct = (JavacTask)comp.getTask(null, fm, dc, null, null, Arrays.asList(samSourceFile, clientSourceFile));
        ct.analyze();
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
    MethodDef methodDef;
    MethodReference methodReference;
    static int count = 0;

    static JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    static StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    SamConversionComboTest(FInterface f, Context c, MethodDef md) {
        fInterface = f;
        context = c;
        methodDef = md;
        methodReference = md.getMethodReference();
    }

    public static void main(String[] args) throws Exception {
        try {
            for(Context ct : Context.values()) {
                for (FInterface fi : FInterface.values()) {
                    for (MethodDef md: MethodDef.values()) {
                        new SamConversionComboTest(fi, ct, md).test();
                    }
                }
            }
            System.out.println("total tests: " + count);
        } finally {
            fm.close();
        }
    }
}
