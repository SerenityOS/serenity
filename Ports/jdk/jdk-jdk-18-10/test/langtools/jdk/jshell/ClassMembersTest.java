/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139829
 * @summary Test access to members of user defined class.
 * @build KullaTesting TestingInputStream ExpectedDiagnostic
 * @run testng/timeout=600 ClassMembersTest
 */

import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;

import jdk.jshell.SourceCodeAnalysis;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import jdk.jshell.TypeDeclSnippet;
import static jdk.jshell.Snippet.Status.OVERWRITTEN;
import static jdk.jshell.Snippet.Status.VALID;

public class ClassMembersTest extends KullaTesting {

    @Test(dataProvider = "memberTestCase")
    public void memberTest(AccessModifier accessModifier, CodeChunk codeChunk, Static isStaticMember, Static isStaticReference) {
        MemberTestCase testCase = new MemberTestCase(accessModifier, codeChunk, isStaticMember, isStaticReference);
        assertEval(testCase.generateSource());
        String expectedMessage = testCase.expectedMessage;
        if (testCase.codeChunk != CodeChunk.CONSTRUCTOR || testCase.isAccessible()) {
            assertEval("A a = new A();");
        }
        if (expectedMessage == null) {
            assertEval(testCase.useCodeChunk());
        } else {
            assertDeclareFail(testCase.useCodeChunk(), expectedMessage);
        }
    }

    private List<String> parseCode(String input) {
        List<String> list = new ArrayList<>();
        SourceCodeAnalysis codeAnalysis = getAnalysis();
        String source = input;
        while (!source.trim().isEmpty()) {
            SourceCodeAnalysis.CompletionInfo info = codeAnalysis.analyzeCompletion(source);
            list.add(info.source());
            source = info.remaining();
        }
        return list;
    }

    @Test(dataProvider = "memberTestCase")
    public void extendsMemberTest(AccessModifier accessModifier, CodeChunk codeChunk, Static isStaticMember, Static isStaticReference) {
        MemberTestCase testCase = new ExtendsMemberTestCase(accessModifier, codeChunk, isStaticMember, isStaticReference);
        String input = testCase.generateSource();
        List<String> ss = parseCode(input);
        assertEval(ss.get(0));
        if (testCase.codeChunk != CodeChunk.CONSTRUCTOR || testCase.isAccessible()) {
            assertEval(ss.get(1));
            assertEval("B b = new B();");
        }
        String expectedMessage = testCase.expectedMessage;
        if (expectedMessage == null) {
            assertEval(testCase.useCodeChunk());
        } else {
            assertDeclareFail(testCase.useCodeChunk(), expectedMessage);
        }
    }

    @Test
    public void interfaceTest() {
        String interfaceSource =
                "interface A {\n" +
                "   default int defaultMethod() { return 1; }\n" +
                "   static int staticMethod() { return 2; }\n" +
                "   int method();\n" +
                "   class Inner1 {}\n" +
                "   static class Inner2 {}\n" +
                "}";
        assertEval(interfaceSource);
        assertEval("A.staticMethod();", "2");
        String classSource =
                "class B implements A {\n" +
                "   public int method() { return 3; }\n" +
                "}";
        assertEval(classSource);
        assertEval("B b = new B();");
        assertEval("b.defaultMethod();", "1");
        assertDeclareFail("B.staticMethod();",
                new ExpectedDiagnostic("compiler.err.cant.resolve.location.args", 0, 14, 1, -1, -1, Diagnostic.Kind.ERROR));
        assertEval("b.method();", "3");
        assertEval("new A.Inner1();");
        assertEval("new A.Inner2();");
        assertEval("new B.Inner1();");
        assertEval("new B.Inner2();");
    }

    @Test
    public void enumTest() {
        String enumSource =
                "enum E {A(\"s\");\n" +
                "   private final String s;\n" +
                "   private E(String s) { this.s = s; }\n" +
                "   public String method() { return s; }\n" +
                "   private String privateMethod() { return s; }\n" +
                "   public static String staticMethod() { return staticPrivateMethod(); }\n" +
                "   private static String staticPrivateMethod() { return \"a\"; }\n" +
                "}";
        assertEval(enumSource);
        assertEval("E a = E.A;", "A");
        assertDeclareFail("a.s;",
                new ExpectedDiagnostic("compiler.err.report.access", 0, 3, 1, -1, -1, Diagnostic.Kind.ERROR));
        assertDeclareFail("new E(\"q\");",
                new ExpectedDiagnostic("compiler.err.enum.cant.be.instantiated", 0, 10, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertEval("a.method();", "\"s\"");
        assertDeclareFail("a.privateMethod();",
                new ExpectedDiagnostic("compiler.err.report.access", 0, 15, 1, -1, -1, Diagnostic.Kind.ERROR));
        assertEval("E.staticMethod();", "\"a\"");
        assertDeclareFail("a.staticPrivateMethod();",
                new ExpectedDiagnostic("compiler.err.report.access", 0, 21, 1, -1, -1, Diagnostic.Kind.ERROR));
        assertDeclareFail("E.method();",
                new ExpectedDiagnostic("compiler.err.non-static.cant.be.ref", 0, 8, 1, -1, -1, Diagnostic.Kind.ERROR));
    }

    @Test(dataProvider = "retentionPolicyTestCase")
    public void annotationTest(RetentionPolicy policy) {
        assertEval("import java.lang.annotation.*;");
        String annotationSource =
                "@Retention(RetentionPolicy." + policy.toString() + ")\n" +
                "@interface A {}";
        assertEval(annotationSource);
        String classSource =
                "@A class C {\n" +
                "   @A C() {}\n" +
                "   @A void f() {}\n" +
                "   @A int f;\n" +
                "   @A class Inner {}\n" +
                "}";
        assertEval(classSource);
        String isRuntimeVisible = policy == RetentionPolicy.RUNTIME ? "true" : "false";
        assertEval("C.class.getAnnotationsByType(A.class).length > 0;", isRuntimeVisible);
        assertEval("C.class.getDeclaredConstructor().getAnnotationsByType(A.class).length > 0;", isRuntimeVisible);
        assertEval("C.class.getDeclaredMethod(\"f\").getAnnotationsByType(A.class).length > 0;", isRuntimeVisible);
        assertEval("C.class.getDeclaredField(\"f\").getAnnotationsByType(A.class).length > 0;", isRuntimeVisible);
        assertEval("C.Inner.class.getAnnotationsByType(A.class).length > 0;", isRuntimeVisible);
    }

    @DataProvider(name = "retentionPolicyTestCase")
    public Object[][] retentionPolicyTestCaseGenerator() {
        List<Object[]> list = new ArrayList<>();
        for (RetentionPolicy policy : RetentionPolicy.values()) {
            list.add(new Object[]{policy});
        }
        return list.toArray(new Object[list.size()][]);
    }

    @DataProvider(name = "memberTestCase")
    public Object[][] memberTestCaseGenerator() {
        List<Object[]> list = new ArrayList<>();
        for (AccessModifier accessModifier : AccessModifier.values()) {
            for (Static isStaticMember : Static.values()) {
                for (Static isStaticReference : Static.values()) {
                    for (CodeChunk codeChunk : CodeChunk.values()) {
                        if (codeChunk == CodeChunk.CONSTRUCTOR && isStaticMember == Static.STATIC) {
                            continue;
                        }
                        list.add(new Object[]{ accessModifier, codeChunk, isStaticMember, isStaticReference });
                    }
                }
            }
        }
        return list.toArray(new Object[list.size()][]);
    }

    public static class ExtendsMemberTestCase extends MemberTestCase {

        public ExtendsMemberTestCase(AccessModifier accessModifier, CodeChunk codeChunk, Static isStaticMember, Static isStaticReference) {
            super(accessModifier, codeChunk, isStaticMember, isStaticReference);
        }

        @Override
        public String getSourceTemplate() {
            return super.getSourceTemplate() + "\n"
                    + "class B extends A {}";
        }

        @Override
        public String errorMessage() {
            if (!isAccessible()) {
                if (codeChunk == CodeChunk.METHOD) {
                    return "compiler.err.cant.resolve.location.args";
                }
                if (codeChunk == CodeChunk.CONSTRUCTOR) {
                    return "compiler.err.cant.resolve.location";
                }
            }
            return super.errorMessage();
        }

        @Override
        public String useCodeChunk() {
            return useCodeChunk("B");
        }
    }

    public static class MemberTestCase {
        public final AccessModifier accessModifier;
        public final CodeChunk codeChunk;
        public final Static isStaticMember;
        public final Static isStaticReference;
        public final String expectedMessage;

        public MemberTestCase(AccessModifier accessModifier, CodeChunk codeChunk, Static isStaticMember,
                              Static isStaticReference) {
            this.accessModifier = accessModifier;
            this.codeChunk = codeChunk;
            this.isStaticMember = isStaticMember;
            this.isStaticReference = isStaticReference;
            this.expectedMessage = errorMessage();
        }

        public String getSourceTemplate() {
            return  "class A {\n" +
                    "    #MEMBER#\n" +
                    "}";
        }

        public boolean isAccessible() {
            return accessModifier != AccessModifier.PRIVATE;
        }

        public String errorMessage() {
            if (!isAccessible()) {
                return "compiler.err.report.access";
            }
            if (codeChunk == CodeChunk.INNER_INTERFACE) {
                return "compiler.err.abstract.cant.be.instantiated";
            }
            if (isStaticMember == Static.STATIC) {
                if (isStaticReference == Static.NO && codeChunk == CodeChunk.INNER_CLASS) {
                    return "compiler.err.qualified.new.of.static.class";
                }
                return null;
            }
            if (isStaticReference == Static.STATIC) {
                if (codeChunk == CodeChunk.CONSTRUCTOR) {
                    return null;
                }
                if (codeChunk == CodeChunk.INNER_CLASS) {
                    return "compiler.err.encl.class.required";
                }
                return "compiler.err.non-static.cant.be.ref";
            }
            return null;
        }

        public String generateSource() {
            return getSourceTemplate().replace("#MEMBER#", codeChunk.generateSource(accessModifier, isStaticMember));
        }

        protected String useCodeChunk(String className) {
            String name = className.toLowerCase();
            switch (codeChunk) {
                case CONSTRUCTOR:
                    return String.format("new %s();", className);
                case METHOD:
                    if (isStaticReference == Static.STATIC) {
                        return String.format("%s.method();", className);
                    } else {
                        return String.format("%s.method();", name);
                    }
                case FIELD:
                    if (isStaticReference == Static.STATIC) {
                        return String.format("%s.field;", className);
                    } else {
                        return String.format("%s.field;", name);
                    }
                case INNER_CLASS:
                    if (isStaticReference == Static.STATIC) {
                        return String.format("new %s.Inner();", className);
                    } else {
                        return String.format("%s.new Inner();", name);
                    }
                case INNER_INTERFACE:
                    return String.format("new %s.Inner();", className);
                default:
                    throw new AssertionError("Unknown code chunk: " + this);
            }
        }

        public String useCodeChunk() {
            return useCodeChunk("A");
        }
    }

    public enum AccessModifier {
        PUBLIC("public"),
        PROTECTED("protected"),
        PACKAGE_PRIVATE(""),
        PRIVATE("private");

        private final String modifier;

        AccessModifier(String modifier) {
            this.modifier = modifier;
        }

        public String getModifier() {
            return modifier;
        }
    }

    public enum Static {
        STATIC("static"), NO("");

        private final String modifier;

        Static(String modifier) {
            this.modifier = modifier;
        }

        public String getModifier() {
            return modifier;
        }
    }

    public enum CodeChunk {
        CONSTRUCTOR("#MODIFIER# A() {}"),
        METHOD("#MODIFIER# int method() { return 10; }"),
        FIELD("#MODIFIER# int field = 10;"),
        INNER_CLASS("#MODIFIER# class Inner {}"),
        INNER_INTERFACE("#MODIFIER# interface Inner {}");

        private final String code;

        CodeChunk(String code) {
            this.code = code;
        }

        public String generateSource(AccessModifier accessModifier, Static isStatic) {
            return code.replace("#MODIFIER#", accessModifier.getModifier() + " " + isStatic.getModifier());
        }
    }
}
