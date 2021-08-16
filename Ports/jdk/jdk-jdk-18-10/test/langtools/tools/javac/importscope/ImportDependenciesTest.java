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
 * @summary The test checks dependencies through type parameters and implements/extends statements.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox ImportDependenciesTest
 * @run main ImportDependenciesTest
 */

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import toolbox.ToolBox;

/**
 * The test checks that code which contains dependencies through type parameters,
 * implements/extends statements compiles properly. All combinations of
 * import types are tested. In addition, the test checks various combinations
 * of classes.
 */
public class ImportDependenciesTest {

    private static final String sourceTemplate =
            "package pkg;\n" +
            "#IMPORT\n" +
            "public class Test {\n" +
            "    static #CLASS_TYPE InnerClass#TYPE_PARAMETER #PARENT {\n" +
            "        static class Inner1 {\n" +
            "        }\n" +
            "        interface Inner2 {\n" +
            "        }\n" +
            "        interface Inner3 {\n" +
            "        }\n" +
            "    }\n" +
            "    static class InnerClass1 {\n" +
            "        static class IInner1 {\n" +
            "        }\n" +
            "    }\n" +
            "    static class InnerInterface1 {\n" +
            "        interface IInner2 {\n" +
            "        }\n" +
            "    }\n" +
            "    static class InnerInterface2 {\n" +
            "        interface IInner3 {\n" +
            "        }\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) {
        new ImportDependenciesTest().test();
    }

    public void test() {
        List<List<InnerClass>> typeParameters = InnerClass.getAllCombinationsForTypeParameter();
        List<List<InnerClass>> parents = InnerClass.getAllCombinationsForInheritance();
        int passed = 0;
        int total = 0;
        for (ClassType classType : ClassType.values()) {
            for (List<InnerClass> parent : parents) {
                if (!classType.canBeInherited(parent)) {
                    continue;
                }
                for (List<InnerClass> typeParameter : typeParameters) {
                    List<InnerClass> innerClasses = new ArrayList<>(typeParameter);
                    innerClasses.addAll(parent);
                    for (ImportType importType : ImportType.values()) {
                        ++total;
                        String source = sourceTemplate
                                .replace("#IMPORT", importType.generateImports(innerClasses))
                                .replace("#CLASS_TYPE", classType.getClassType())
                                .replace("#TYPE_PARAMETER", generateTypeParameter(typeParameter))
                                .replace("#PARENT", classType.generateInheritanceString(parent));
                        CompilationResult result = compile(new ToolBox.JavaSource("pkg/Test.java", source));
                        if (!result.isSuccessful) {
                            echo("Compilation failed!");
                            echo(source);
                            echo(result.message);
                            echo();
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

    private String generateTypeParameter(List<InnerClass> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return String.format("<T extends %s>", typeParameters.stream()
                .map(InnerClass::getSimpleName)
                .collect(Collectors.joining(" & ")));
    }

    private static class CompilationResult {
        public final boolean isSuccessful;
        public final String message;

        public CompilationResult(boolean isSuccessful, String message) {
            this.isSuccessful = isSuccessful;
            this.message = message;
        }
    }

    private CompilationResult compile(ToolBox.JavaSource...sources) {
        StringWriter writer = new StringWriter();
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        Boolean call = jc.getTask(writer, null, null, null, null, Arrays.asList(sources)).call();
        return new CompilationResult(call, writer.toString().replace(ToolBox.lineSeparator, "\n"));
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

    enum ImportType {
        IMPORT("import"), STATIC_IMPORT("import static"),
        IMPORT_ON_DEMAND("import"), STATIC_IMPORT_ON_DEMAND("import static");

        private final String importType;
        private ImportType(String importType) {
            this.importType = importType;
        }

        private boolean isOnDemand() {
            return this == IMPORT_ON_DEMAND || this == STATIC_IMPORT_ON_DEMAND;
        }

        public String generateImports(List<InnerClass> innerClasses) {
            return innerClasses.stream()
                    .map(i -> isOnDemand() ? i.getPackageName() + ".*" : i.getCanonicalName())
                    .distinct()
                    .map(s -> String.format("%s %s;", importType, s))
                    .collect(Collectors.joining("\n"));
        }
    }

    enum ClassType {
        CLASS("class") {
            @Override
            public boolean canBeInherited(List<InnerClass> innerClasses) {
                return true;
            }

            @Override
            public String generateInheritanceString(List<InnerClass> innerClasses) {
                if (innerClasses.isEmpty()) {
                    return "";
                }
                StringBuilder sb = new StringBuilder();
                InnerClass firstClass = innerClasses.get(0);
                if (firstClass.isClass()) {
                    sb.append("extends ").append(firstClass.getSimpleName()).append(" ");
                }
                String str = innerClasses.stream()
                        .filter(x -> !x.isClass())
                        .map(InnerClass::getSimpleName)
                        .collect(Collectors.joining(", "));
                if (!str.isEmpty()) {
                    sb.append("implements ").append(str);
                }
                return sb.toString();
            }
        }, INTERFACE("interface") {
            @Override
            public boolean canBeInherited(List<InnerClass> innerClasses) {
                return !innerClasses.stream().anyMatch(InnerClass::isClass);
            }

            @Override
            public String generateInheritanceString(List<InnerClass> innerClasses) {
                if (innerClasses.isEmpty()) {
                    return "";
                }
                return "extends " + innerClasses.stream()
                        .map(InnerClass::getSimpleName)
                        .collect(Collectors.joining(", "));
            }
        };

        private final String classType;
        private ClassType(String classType) {
            this.classType = classType;
        }

        public String getClassType() {
            return classType;
        }

        public abstract boolean canBeInherited(List<InnerClass> innerClasses);

        public abstract String generateInheritanceString(List<InnerClass> innerClasses);
    }

    enum InnerClass {
        INNER_1("pkg.Test.InnerClass.Inner1", true),
        INNER_2("pkg.Test.InnerClass.Inner2", true),
        INNER_3("pkg.Test.InnerClass.Inner3", true),
        IINNER_1("pkg.Test.InnerClass1.IInner1", false),
        IINNER_2("pkg.Test.InnerInterface1.IInner2", false),
        IINNER_3("pkg.Test.InnerInterface2.IInner3", false);

        private final String canonicalName;
        private final boolean isForTypeParameter;

        private InnerClass(String canonicalName, boolean isForTypeParameter) {
            this.canonicalName = canonicalName;
            this.isForTypeParameter = isForTypeParameter;
        }

        private static List<List<InnerClass>> getAllCombinations(boolean isTypeParameter) {
            List<List<InnerClass>> result = new ArrayList<>();
            List<InnerClass> tmpl = Stream.of(InnerClass.values())
                    .filter(i -> i.isForTypeParameter() == isTypeParameter)
                    .collect(Collectors.toCollection(ArrayList::new));
            result.add(Arrays.asList());
            for (int i = 0; i < tmpl.size(); ++i) {
                result.add(Arrays.asList(tmpl.get(i)));
                for (int j = i + 1; j < tmpl.size(); ++j) {
                    result.add(Arrays.asList(tmpl.get(i), tmpl.get(j)));
                }
            }
            result.add(tmpl);
            return result;
        }

        public static List<List<InnerClass>> getAllCombinationsForTypeParameter() {
            return getAllCombinations(true);
        }

        public static List<List<InnerClass>> getAllCombinationsForInheritance() {
            return getAllCombinations(false);
        }

        public String getCanonicalName() {
            return canonicalName;
        }

        public String getSimpleName() {
            String cName = getCanonicalName();
            return cName.substring(cName.lastIndexOf('.') + 1);
        }

        public String getPackageName() {
            String cName = getCanonicalName();
            int dotIndex = cName.lastIndexOf('.');
            return dotIndex == -1 ? "" : cName.substring(0, dotIndex);
        }

        public boolean isClass() {
            return this == INNER_1 || this == IINNER_1;
        }
        private boolean isForTypeParameter() {
            return isForTypeParameter;
        }
    }
}
