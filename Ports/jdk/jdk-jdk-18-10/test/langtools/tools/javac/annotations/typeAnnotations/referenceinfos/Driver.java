/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.TypeAnnotation.TargetType;

import static java.lang.String.format;

public class Driver {

    private static final PrintStream out = System.err;

    private final Object testObject;

    public Driver(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        testObject = clazz.newInstance();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length > 1)
            throw new IllegalArgumentException("Usage: java Driver <test-name>");
        String name = args[0];
        new Driver(Class.forName(name)).runDriver();
    }

    private final String[][] extraParamsCombinations = new String[][] {
        new String[] { },
        new String[] { "-g" },
    };

    private final String[] retentionPolicies = {RetentionPolicy.CLASS.toString(), RetentionPolicy.RUNTIME.toString()};

    protected void runDriver() {
        int passed = 0, failed = 0;
        Class<?> clazz = testObject.getClass();
        out.println("Tests for " + clazz.getName());

        // Find methods
        for (Method method : clazz.getMethods()) {
            try {
                Map<String, TypeAnnotation.Position> expected = expectedOf(method);
                if (expected == null)
                    continue;
                if (method.getReturnType() != String.class)
                    throw new IllegalArgumentException("Test method needs to return a string: " + method);

                String compact = (String) method.invoke(testObject);
                for (String retentionPolicy : retentionPolicies) {
                    String testClassName = getTestClassName(method, retentionPolicy);
                    String testClass = testClassOf(method, testClassName);
                    String fullFile = wrap(compact, new HashMap<String, String>() {{
                        put("%RETENTION_POLICY%", retentionPolicy);
                        put("%TEST_CLASS_NAME%", testClassName);
                    }});
                    for (String[] extraParams : extraParamsCombinations) {
                        try {
                            ClassFile cf = compileAndReturn(fullFile, testClass, extraParams);
                            List<TypeAnnotation> actual = ReferenceInfoUtil.extendedAnnotationsOf(cf);
                            ReferenceInfoUtil.compare(expected, actual, cf);
                            out.format("PASSED:  %s %s%n", testClassName, Arrays.toString(extraParams));
                            ++passed;
                        } catch (Throwable e) {
                            out.format("FAILED:  %s %s%n", testClassName, Arrays.toString(extraParams));
                            out.println(fullFile);
                            out.println("    " + e.toString());
                            e.printStackTrace(out);
                            ++failed;
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                out.println("FAILED:  " + method.getName());
                out.println("    " + e.toString());
                e.printStackTrace(out);
                ++failed;
            }
        }

        out.println();
        int total = passed + failed;
        out.println(total + " total tests: " + passed + " PASSED, " + failed + " FAILED");

        out.flush();

        if (failed != 0)
            throw new RuntimeException(failed + " tests failed");
    }

    private Map<String, TypeAnnotation.Position> expectedOf(Method m) {
        TADescription ta = m.getAnnotation(TADescription.class);
        TADescriptions tas = m.getAnnotation(TADescriptions.class);

        if (ta == null && tas == null)
            return null;

        Map<String, TypeAnnotation.Position> result =
            new HashMap<>();

        if (ta != null)
            result.putAll(expectedOf(ta));

        if (tas != null) {
            for (TADescription a : tas.value()) {
                result.putAll(expectedOf(a));
            }
        }

        return result;
    }

    private Map<String, TypeAnnotation.Position> expectedOf(TADescription d) {
        String annoName = d.annotation();

        TypeAnnotation.Position p = new TypeAnnotation.Position();
        p.type = d.type();
        if (d.offset() != NOT_SET)
            p.offset = d.offset();
        if (d.lvarOffset().length != 0)
            p.lvarOffset = d.lvarOffset();
        if (d.lvarLength().length != 0)
            p.lvarLength = d.lvarLength();
        if (d.lvarIndex().length != 0)
            p.lvarIndex = d.lvarIndex();
        if (d.boundIndex() != NOT_SET)
            p.bound_index = d.boundIndex();
        if (d.paramIndex() != NOT_SET)
            p.parameter_index = d.paramIndex();
        if (d.typeIndex() != NOT_SET)
            p.type_index = d.typeIndex();
        if (d.exceptionIndex() != NOT_SET)
            p.exception_index = d.exceptionIndex();
        if (d.genericLocation().length != 0) {
            p.location = TypeAnnotation.Position.getTypePathFromBinary(wrapIntArray(d.genericLocation()));
        }

        return Collections.singletonMap(annoName, p);
    }

    private List<Integer> wrapIntArray(int[] ints) {
        List<Integer> list = new ArrayList<>(ints.length);
        for (int i : ints)
            list.add(i);
        return list;
    }

    private String getTestClassName(Method m, String retentionPolicy) {
        return format("%s_%s_%s", testObject.getClass().getSimpleName(),
                m.getName(), retentionPolicy);
    }

    private String testClassOf(Method m, String testClassName) {
        TestClass tc = m.getAnnotation(TestClass.class);
        if (tc != null) {
            return tc.value().replace("%TEST_CLASS_NAME%", testClassName);
        } else {
            return testClassName;
        }
    }

    private ClassFile compileAndReturn(String fullFile, String testClass, String... extraParams) throws Exception {
        File source = writeTestFile(fullFile, testClass);
        File clazzFile = compileTestFile(source, testClass, extraParams);
        return ClassFile.read(clazzFile);
    }

    protected File writeTestFile(String fullFile, String testClass) throws IOException {
        File f = new File(getClassDir(), format("%s.java", testClass));
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
            out.println(fullFile);
            return f;
        }
    }

    private String getClassDir() {
        return System.getProperty("test.classes", Driver.class.getResource(".").getPath());
    }

    protected File compileTestFile(File f, String testClass, String... extraParams) {
        List<String> options = new ArrayList<>();
        options.addAll(Arrays.asList(extraParams));
        options.add(f.getPath());
        int rc = com.sun.tools.javac.Main.compile(options.toArray(new String[options.size()]));
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getParent() != null ? f.getParent() : "";
        return new File(path, format("%s.class", testClass));
    }

    private String wrap(String compact, Map<String, String> replacements) {
        StringBuilder sb = new StringBuilder();

        // Automatically import java.util
        sb.append("\nimport java.io.*;");
        sb.append("\nimport java.util.*;");
        sb.append("\nimport java.lang.annotation.*;");

        sb.append("\n\n");
        boolean isSnippet = !(compact.startsWith("class")
                              || compact.contains(" class"))
                            && !compact.contains("interface")
                            && !compact.contains("enum");
        if (isSnippet)
            sb.append("class %TEST_CLASS_NAME% {\n");

        sb.append(compact);
        sb.append("\n");

        if (isSnippet)
            sb.append("}\n\n");

        if (isSnippet) {
            // Have a few common nested types for testing
            sb.append("class Outer { class Inner {} class Middle { class MInner {} } }");
            sb.append("class SOuter { static class SInner {} }");
            sb.append("class GOuter<X, Y> { class GInner<X, Y> {} }");
        }

        // create A ... F annotation declarations
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface A {}");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface B {}");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface C {}");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface D {}");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface E {}");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface F {}");

        // create TA ... TF proper type annotations
        sb.append("\n");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                " @Retention(RetentionPolicy.%RETENTION_POLICY%)  @interface TA {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TB {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TC {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TD {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TE {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TF {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TG {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TH {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TI {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TJ {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TK {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TL {}");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface TM {}");

        // create RT?, RT?s for repeating type annotations
        sb.append("\n");
        sb.append("\n@Repeatable(RTAs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTA {}");
        sb.append("\n@Repeatable(RTBs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTB {}");
        sb.append("\n@Repeatable(RTCs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTC {}");
        sb.append("\n@Repeatable(RTDs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTD {}");
        sb.append("\n@Repeatable(RTEs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTE {}");
        sb.append("\n@Repeatable(RTFs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTF {}");
        sb.append("\n@Repeatable(RTGs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTG {}");
        sb.append("\n@Repeatable(RTHs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTH {}");
        sb.append("\n@Repeatable(RTIs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTI {}");
        sb.append("\n@Repeatable(RTJs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTJ {}");
        sb.append("\n@Repeatable(RTKs.class) @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTK {}");

        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTAs { RTA[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTBs { RTB[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTCs { RTC[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTDs { RTD[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTEs { RTE[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTFs { RTF[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTGs { RTG[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTHs { RTH[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTIs { RTI[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTJs { RTJ[] value(); }");
        sb.append("\n@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})" +
                "@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface RTKs { RTK[] value(); }");

        sb.append("\n@Target(value={ElementType.TYPE,ElementType.FIELD,ElementType.METHOD," +
                "ElementType.PARAMETER,ElementType.CONSTRUCTOR,ElementType.LOCAL_VARIABLE})");
        sb.append("\n@Retention(RetentionPolicy.%RETENTION_POLICY%) @interface Decl {}");

        return replaceAll(sb.toString(), replacements);
    }

    private String replaceAll(String src, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            src = src.replace(entry.getKey(), entry.getValue());
        }
        return src;
    }

    public static final int NOT_SET = -888;

}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(TADescriptions.class)
@interface TADescription {
    String annotation();

    TargetType type();
    int offset() default Driver.NOT_SET;
    int[] lvarOffset() default { };
    int[] lvarLength() default { };
    int[] lvarIndex() default { };
    int boundIndex() default Driver.NOT_SET;
    int paramIndex() default Driver.NOT_SET;
    int typeIndex() default Driver.NOT_SET;
    int exceptionIndex() default Driver.NOT_SET;

    int[] genericLocation() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescriptions {
    TADescription[] value() default {};
}

/**
 * The name of the class that should be analyzed.
 * Should only need to be provided when analyzing inner classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TestClass {
    String value();
}
