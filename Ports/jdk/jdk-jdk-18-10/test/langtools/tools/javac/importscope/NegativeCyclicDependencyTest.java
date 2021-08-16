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
 * @bug 8064794
 * @summary The negative test against cyclic dependencies.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox NegativeCyclicDependencyTest
 * @run main NegativeCyclicDependencyTest
 */

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import toolbox.ToolBox;

/**
 * The test generates the following code:
 *
 * package pkg;
 * import pkg.B.InnerB;
 * class A extends InnerB {
 *    static class InnerA {}
 * }
 *
 * package pkg;
 * import pkg.A.InnerA;
 * class B extends InnerA {
 *     static class InnerB {}
 * }
 *
 * compiles and checks whether compilation fails with the correct message.
 * The test generates all possible combination of inheritance:
 *     1. A extends InnerB, B extends InnerA;
 *     2. InnerA extends InnerB, InnerB extends InnerA;
 *     3. A extends InnerB, InnerB extends InnerA;
 *     4. B extends InnerA, InnerA extends InnerB;
 *     5. A extends InnerA.
 * The test checks class, enum and interface as parent class, and checks all
 * possible import statements.
 */
public class NegativeCyclicDependencyTest {
    private final static String expectedErrorMessage =
            "\\w+:\\d+:\\d+: compiler.err.cyclic.inheritance: [\\w.]+\n1 error\n";

    private final static String[] sourceTemplatesA = {
            "package pkg;\n" +
            "#IMPORT_TYPE\n" +
            "#OUTER_CLASS A #INHERIT InnerB {#ENUM_SEMI\n" +
            "    static #INNER_CLASS InnerA {}\n" +
            "}",
            "package pkg;\n" +
            "#IMPORT_TYPE\n" +
            "#OUTER_CLASS A {#ENUM_SEMI\n" +
            "    static #INNER_CLASS InnerA #INHERIT InnerB {}\n" +
            "}"
    };

    private final static String[] sourceTemplatesB = {
            "package pkg;\n" +
            "#IMPORT_TYPE\n" +
            "#OUTER_CLASS B #INHERIT InnerA {#ENUM_SEMI\n" +
            "    static #INNER_CLASS InnerB {}\n" +
            "}",
            "package pkg;\n" +
            "#IMPORT_TYPE\n" +
            "#OUTER_CLASS B {#ENUM_SEMI\n" +
            "    static #INNER_CLASS InnerB #INHERIT InnerA {}\n" +
            "}"
    };

    private final static String sourceTemplate =
            "package pkg;\n" +
            "#IMPORT_TYPE\n" +
            "#OUTER_CLASS A #INHERIT InnerA {#ENUM_SEMI\n" +
            "    static #INNER_CLASS InnerA {}\n" +
            "}";

    public static void main(String[] args) {
        new NegativeCyclicDependencyTest().test();
    }

    public void test() {
        int passed = 0;
        List<TestCase> testCases = generateTestCases();
        for (TestCase testCase : testCases) {
            try {
                String output = compile(testCase.sources);
                if (!output.matches(testCase.expectedMessage)) {
                    reportFailure(testCase);
                    printf(String.format("Message: %s, does not match regexp: %s\n",
                            output, testCase.expectedMessage));
                } else {
                    ++passed;
                }
            } catch (RuntimeException e) {
                reportFailure(testCase);
                e.printStackTrace();
            }
        }
        String message = String.format(
                "Total test cases run: %d, passed: %d, failed: %d.",
                testCases.size(), passed, testCases.size() - passed);
        if (passed != testCases.size()) {
            throw new RuntimeException(message);
        }
        echo(message);
    }

    private void reportFailure(TestCase testCase) {
        echo("Test case failed.");
        for (ToolBox.JavaSource source : testCase.sources) {
            echo(source.getCharContent(true));
            echo();
        }
    }

    public List<TestCase> generateTestCases() {
        List<TestCase> testCases = generateTestCasesWithTwoClasses();
        testCases.addAll(generateTestCasesWithOneClass());
        return testCases;
    }

    private List<TestCase> generateTestCasesWithOneClass() {
        String importedClassName = "pkg.A.InnerA";
        List<TestCase> testCases = new ArrayList<>();
        for (ClassType outerClass : ClassType.values()) {
            for (ClassType innerClass : ClassType.values()) {
                if (!outerClass.canInherit(innerClass)) {
                    continue;
                }
                for (ImportType importType : ImportType.values()) {
                    String source = generateSource(
                            sourceTemplate,
                            outerClass,
                            innerClass,
                            outerClass.inheritedString(innerClass),
                            importType,
                            importedClassName);
                    testCases.add(new TestCase(expectedErrorMessage,
                            new ToolBox.JavaSource("A", source)));
                }
            }
        }
        return testCases;
    }

    private List<TestCase> generateTestCasesWithTwoClasses() {
        String importedClassName1 = "pkg.A.InnerA";
        String importedClassName2 = "pkg.B.InnerB";
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < sourceTemplatesA.length; ++i) {
            for (int j = 0; j < sourceTemplatesB.length; ++j) {
                for (ClassType outerClass1 : ClassType.values()) {
                    for (ClassType outerClass2 : ClassType.values()) {
                        for (ClassType innerClass1 : ClassType.values()) {
                            for (ClassType innerClass2 : ClassType.values()) {
                                ClassType childClass1 = i == 0 ? outerClass1 : innerClass1;
                                ClassType childClass2 = j == 0 ? outerClass2 : innerClass2;
                                if (!childClass1.canInherit(innerClass2) ||
                                        !childClass2.canInherit(innerClass1)) {
                                    continue;
                                }
                                for (ImportType importType1 : ImportType.values()) {
                                    for (ImportType importType2 : ImportType.values()) {
                                        String sourceA = generateSource(
                                                sourceTemplatesA[i],
                                                outerClass1,
                                                innerClass1,
                                                childClass1.inheritedString(innerClass2),
                                                importType1,
                                                importedClassName2);
                                        String sourceB = generateSource(
                                                sourceTemplatesB[j],
                                                outerClass2,
                                                innerClass2,
                                                childClass2.inheritedString(innerClass1),
                                                importType2,
                                                importedClassName1);
                                        testCases.add(new TestCase(expectedErrorMessage,
                                                new ToolBox.JavaSource("A", sourceA),
                                                new ToolBox.JavaSource("B", sourceB)));
                                        testCases.add(new TestCase(expectedErrorMessage,
                                                new ToolBox.JavaSource("B", sourceB),
                                                new ToolBox.JavaSource("A", sourceA)));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return testCases;
    }

    public String generateSource(String template,
                                 ClassType outerClass,
                                 ClassType innerClass,
                                 String inheritString,
                                 ImportType importType,
                                 String innerClassName) {
        return template
                .replace("#OUTER_CLASS", outerClass.getType())
                .replace("#INNER_CLASS", innerClass.getType())
                .replace("#INHERIT", inheritString)
                .replace("#IMPORT_TYPE", importType.getImport(innerClassName))
                .replace("#ENUM_SEMI", outerClass == ClassType.ENUM ? ";" : "");
    }

    /**
     * Compiles sources with -XDrawDiagnostics flag and
     * returns the output of compilation.
     *
     * @param sources sources
     * @return the result of compilation
     */
    private String compile(ToolBox.JavaSource...sources) {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StringWriter writer = new StringWriter();
        JavaCompiler.CompilationTask ct = jc.getTask(writer, null, null,
                Arrays.asList("-XDrawDiagnostics"),
                null, Arrays.asList(sources));
        if (ct.call()) {
            throw new RuntimeException("Expected compilation failure.");
        }
        return writer.toString().replace(ToolBox.lineSeparator, "\n");
    }

    public void echo() {
        echo("");
    }

    public void echo(CharSequence message) {
        echo(message.toString());
    }

    public void echo(String message) {
        printf(message + "\n");
    }

    public void printf(String template, Object...args) {
        System.err.print(String.format(template, args).replace("\n", ToolBox.lineSeparator));
    }

    /**
     * The class represents a test case.
     */
    public static class TestCase {
        public final ToolBox.JavaSource[] sources;
        public final String expectedMessage;

        public TestCase(String expectedMessage, ToolBox.JavaSource...sources) {
            this.sources = sources;
            this.expectedMessage = expectedMessage;
        }
    }

    /**
     * The enum represents all possible imports.
     */
    public enum ImportType {
        SINGLE_IMPORT("import %s;"),
        IMPORT_ON_DEMAND("import %s.*;"),
        SINGLE_STATIC_IMPORT("import static %s;"),
        STATIC_IMPORT_ON_DEMAND("import static %s.*;");

        private final String type;

        private ImportType(String type) {
            this.type = type;
        }

        public String getImport(String className) {
            if (this == ImportType.IMPORT_ON_DEMAND || this == ImportType.STATIC_IMPORT_ON_DEMAND) {
                int lastDot = className.lastIndexOf('.');
                className = className.substring(0, lastDot);
            }
            return String.format(type, className);
        }
    }

    /**
     * The enum represents all possible class types that can be used in
     * inheritance.
     */
    public enum ClassType {
        CLASS("class"), INTERFACE("interface"), ENUM("enum");

        public boolean canInherit(ClassType innerClass) {
            return innerClass != ENUM && !(this == ENUM && innerClass == ClassType.CLASS
                    || this == INTERFACE && innerClass == ClassType.CLASS);
        }

        public String inheritedString(ClassType innerClass) {
            if (!canInherit(innerClass)) {
                throw new IllegalArgumentException(String.format("%s cannot inherit %s", this, innerClass));
            }
            return this == innerClass ? "extends" : "implements";
        }

        private final String type;

        private ClassType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
