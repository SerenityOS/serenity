/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044411
 * @summary Tests the RuntimeParameterVisibleAnnotations/RuntimeParameterInvisibleAnnotations attribute.
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib /tools/javac/lib ../lib
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build WorkAnnotations TestCase ClassType TestAnnotationInfo
 * @build RuntimeParameterAnnotationsTest AnnotationsTestBase RuntimeParameterAnnotationsTestBase
 * @run main RuntimeParameterAnnotationsTest
 */

import java.util.ArrayList;
import java.util.List;

/**
 * RuntimeParameterAnnotationsTest is a test which checks that RuntimeVisibleParameterAnnotationsAttribute
 * and RuntimeInvisibleParameterAnnotationsAttribute are generated properly for constructors,
 * for static and abstract methods of class, for abstract, default and static methods of interface.
 * The test checks both single and repeatable annotations.
 * All possible combinations of retention policies are tested.
 *
 * The test generates source code, compiles it and checks the byte code.
 *
 * See README.txt for more information.
 */
public class RuntimeParameterAnnotationsTest extends RuntimeParameterAnnotationsTestBase {

    @Override
    public List<TestCase> generateTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (List<TestAnnotationInfos> groupedAnnotations : groupAnnotations(getAllCombinationsOfAnnotations())) {
            for (ClassType classType : new ClassType[]{ClassType.CLASS, ClassType.INTERFACE}) {
                TestCase test = new TestCase();
                for (int i = 0; i < groupedAnnotations.size(); i++) {
                    TestAnnotationInfos annotations = groupedAnnotations.get(i);
                    TestCase.TestClassInfo clazz = test.addClassInfo(classType, "Test" + i, "abstract");

                    initMethod(annotations, clazz, "<init>");

                    initMethod(annotations, clazz, "method1");

                    initMethod(annotations, clazz, "method2",
                            classType == ClassType.CLASS ? "abstract" : "default");

                    initMethod(annotations, clazz, "staticMethod", "static");
                }
                testCases.add(test);
            }
        }
        return testCases;
    }

    /**
     * Adds a method to the {@code testCase} with {@code methodName}.
     *
     * @param annotations a list of annotations
     * @param clazz a test class
     * @param methodName a method name
     * @param mods an array of modifiers
     */
    private void initMethod(TestAnnotationInfos annotations, TestCase.TestClassInfo clazz, String methodName, String...mods) {
        String methodDescriptor = methodName + "(int, double, java.lang.String)";
        TestCase.TestMethodInfo method = clazz.addMethodInfo(methodDescriptor, mods);
        TestCase.TestParameterInfo p1 = method.addParameter("int", "a");
        annotations.annotate(p1);
        method.addParameter("double", "b");
        TestCase.TestParameterInfo p3 = method.addParameter("String", "c");
        annotations.annotate(p3);
    }

    public static void main(String[] args) throws TestFailedException {
        new RuntimeParameterAnnotationsTest().test();
    }
}
