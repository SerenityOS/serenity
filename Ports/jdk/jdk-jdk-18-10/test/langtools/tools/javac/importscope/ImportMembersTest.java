/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8065360
 * @summary The test checks possibility of class members to be imported.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox ImportMembersTest
 * @run main ImportMembersTest
 */

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import toolbox.ToolBox;

/**
 * The test checks that members of a class, an enum, an interface or annotation
 * can be imported with help of a static import or an import statement.
 * The tests generates a code, compiles it and checks whether it can be compiled
 * successfully or fails with a proper message.
 * The following is the example of a test case:
 * package pkg;
 * class ChildA extends A {}
 *
 * package pkg;
 * class A {
 *     static class Inner {}
 *     static Object field;
 *     static void method() {}
 * }
 *
 * package pkg;
 * import static pkg.ChildA.method;
 * public class Test {{
 *     method();
 * }}
 *
 */
public class ImportMembersTest {

    private static final String[] expectedErrorMessages = {
            "Test.java:\\d+:\\d+: compiler.err.cant.resolve.location: .*\n1 error\n",
            "Test.java:\\d+:\\d+: compiler.err.import.requires.canonical: .*\n1 error\n"
    };

    private static final String sourceTemplate =
            "package pkg;\n" +
            "#IMPORT\n" +
            "public class Test {{\n" +
            "    #STATEMENT\n" +
            "}}\n";

    public static void main(String[] args) {
        new ImportMembersTest().test();
    }

    public void test() {
        int passed = 0;
        int total = 0;
        for (ClassType classType : ClassType.values()) {
            for (ImportType importType : ImportType.values()) {
                for (MemberType memberType : MemberType.values()) {
                    ++total;
                    List<ToolBox.JavaSource> sources = classType.getSources();
                    sources.add(new ToolBox.JavaSource("Test.java",
                            generateSource(classType, memberType, importType)));

                    CompilationResult compilationResult = compile(sources);
                    boolean isErrorExpected = importType.hasError(classType, memberType);
                    if (!compilationResult.isSuccessful) {
                        if (isErrorExpected) {
                            String expectedErrorMessage =
                                    getExpectedErrorMessage(classType, importType, memberType);
                            if (compilationResult.message.matches(expectedErrorMessage)) {
                                ++passed;
                            } else {
                                reportFailure(sources, String.format("Expected compilation failure message:\n" +
                                                "%s\ngot message:\n%s",
                                        expectedErrorMessage, compilationResult.message));
                            }
                        } else {
                            reportFailure(sources, String.format("Unexpected compilation failure:\n%s",
                                    compilationResult.message));
                        }
                    } else {
                        if (isErrorExpected) {
                            reportFailure(sources, "Expected compilation failure.");
                        } else {
                            ++passed;
                        }
                    }
                }
            }
        }
        String message = String.format(
                "Total test cases run: %d, passed: %d, failed: %d.",
                total, passed, total - passed);
        if (passed != total) {
            throw new RuntimeException(message);
        }
        echo(message);
    }

    private String getExpectedErrorMessage(ClassType classType, ImportType importType, MemberType memberType) {
        String expectedErrorMessage;
        if (importType == ImportType.IMPORT && classType == ClassType.CHILD_A &&
                memberType == MemberType.CLASS) {
            expectedErrorMessage = expectedErrorMessages[1];
        } else {
            expectedErrorMessage = expectedErrorMessages[0];
        }
        return expectedErrorMessage;
    }

    private void reportFailure(List<ToolBox.JavaSource> sources, String message) {
        echo("Test case failed!");
        printSources(sources);
        echo(message);
        echo();
    }

    private String generateSource(ClassType classType, MemberType memberType, ImportType importType) {
        String importString = importType.generateImport(classType.getClassName(), memberType.getMemberType());
        String statement;
        if (importType.hasError(classType, memberType)) {
            // if the source code has a compilation error, nothing is added.
            // just to prevent the compiler from appending additional
            // compilation errors to output
            statement = "";
        } else if (memberType == MemberType.STAR) {
            // in case of import-on-demand, every class member is used
            if (importType == ImportType.STATIC_IMPORT) {
                statement = MemberType.CLASS.getStatement() + "\n    "
                        + MemberType.FIELD.getStatement();
                // an annotation does not have a static method.
                if (classType != ClassType.D) {
                    statement += "\n    " + MemberType.METHOD.getStatement() + "\n";
                }
            } else {
                statement = classType != ClassType.CHILD_A
                        ? MemberType.CLASS.getStatement() : "";
            }
        } else {
            statement = memberType.getStatement();
        }
        return sourceTemplate
                .replace("#IMPORT", importString)
                .replace("#STATEMENT", statement);
    }

    private static class CompilationResult {
        public final boolean isSuccessful;
        public final String message;

        public CompilationResult(boolean isSuccessful, String message) {
            this.isSuccessful = isSuccessful;
            this.message = message;
        }
    }

    private CompilationResult compile(List<ToolBox.JavaSource> sources) {
        StringWriter writer = new StringWriter();
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        Boolean call = jc.getTask(writer, null, null, Arrays.asList("-XDrawDiagnostics"), null, sources).call();
        return new CompilationResult(call, writer.toString().replace(ToolBox.lineSeparator, "\n"));
    }

    public void printSources(List<ToolBox.JavaSource> sources) {
        for (ToolBox.JavaSource javaSource : sources) {
            echo(javaSource.getCharContent(true).toString());
        }
    }

    public void echo() {
        echo("");
    }

    public void echo(String output) {
        printf(output + "\n");
    }

    public void printf(String template, Object...args) {
        System.err.print(String.format(template, args).replace("\n", ToolBox.lineSeparator));
    }

    enum ClassType {
        A("A",
        "package pkg;\n" +
        "class A {\n" +
        "    static class Inner {}\n" +
        "    static Object field;\n" +
        "    static void method() {}\n" +
        "}\n"
        ),
        B("B",
        "package pkg;\n" +
        "interface B {\n" +
        "    static class Inner {}\n" +
        "    static Object field = null;\n" +
        "    static void method() {}\n" +
        "}\n"
        ),
        C("C",
        "package pkg;\n" +
        "enum C {field;\n" +
        "    static class Inner {}\n" +
        "    static void method() {}\n" +
        "}\n"
        ),
        D("D",
        "package pkg;\n" +
        "@interface D {\n" +
        "    static class Inner {}\n" +
        "    static Object field = null;\n" +
        "}\n"
        ),
        CHILD_A("ChildA",
        "package pkg;\n" +
        "class ChildA extends A {}\n",
        A);

        private final String className;
        private final String source;
        private final ClassType parentType;

        private ClassType(String className, String source) {
            this(className, source, null);
        }

        private ClassType(String className, String source, ClassType classType) {
            this.className = className;
            this.source = source;
            this.parentType = classType;
        }

        public String getClassName() {
            return className;
        }

        public List<ToolBox.JavaSource> getSources() {
            List<ToolBox.JavaSource> sourceList = new ArrayList<>();
            ClassType current = this;
            while (current != null) {
                sourceList.add(new ToolBox.JavaSource(current.className, current.source));
                current = current.parentType;
            }
            return sourceList;
        }
    }

    enum MemberType {
        CLASS("Inner", "Inner inner = null;"),
        FIELD("field", "Object o = field;"),
        METHOD("method", "method();"),
        STAR("*", ""),
        NOT_EXIST("NotExist", "");

        private final String memberType;
        private final String statement;

        private MemberType(String memberType, String statement) {
            this.memberType = memberType;
            this.statement = statement;
        }

        public String getStatement() {
            return statement;
        }

        public String getMemberType() {
            return memberType;
        }
    }

    enum ImportType {
        IMPORT("import pkg.#CLASS_NAME.#MEMBER_NAME;"),
        STATIC_IMPORT("import static pkg.#CLASS_NAME.#MEMBER_NAME;");

        private final String importType;

        private ImportType(String importType) {
            this.importType = importType;
        }

        public String generateImport(String className, String memberName) {
            return importType
                    .replace("#CLASS_NAME", className)
                    .replace("#MEMBER_NAME", memberName);
        }

        public boolean hasError(ClassType classType, MemberType memberType) {
            switch (memberType) {
                case FIELD:
                    return this != ImportType.STATIC_IMPORT;
                case METHOD:
                    return this != ImportType.STATIC_IMPORT || classType == ClassType.D;
                case NOT_EXIST:
                    return true;
                case CLASS:
                    return classType.parentType != null && this != STATIC_IMPORT;
                default:
                    return false;
            }
        }
    }
}
