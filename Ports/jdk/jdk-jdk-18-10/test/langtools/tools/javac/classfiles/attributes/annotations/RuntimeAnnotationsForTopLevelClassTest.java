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
 * @summary Tests the RuntimeVisibleAnnotations/RuntimeInvisibleAnnotations attribute.
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib /tools/javac/lib ../lib
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @build WorkAnnotations TestCase ClassType TestAnnotationInfo
 * @build RuntimeAnnotationsForTopLevelClassTest AnnotationsTestBase RuntimeAnnotationsTestBase
 * @run main RuntimeAnnotationsForTopLevelClassTest
 */

import java.util.ArrayList;
import java.util.List;

/**
 * The test checks that RuntimeVisibleAnnotationsAttribute and RuntimeInvisibleAnnotationsAttribute
 * are generated properly for top-level class (class, enum, annotation, interface),
 * for constructors (in enum and in class), for methods (abstract methods, static and default methods in interface),
 * for fields. The test checks both single and repeatable annotations.
 * In addition, all possible combinations of retention policies are tested.
 *
 * The test generates source code, compiles it and checks the byte code.
 *
 * See README.txt for more information.
 */
public class RuntimeAnnotationsForTopLevelClassTest extends RuntimeAnnotationsTestBase {

    @Override
    public List<TestCase> generateTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (List<TestAnnotationInfos> groupedAnnotations : groupAnnotations(getAllCombinationsOfAnnotations())) {
            for (ClassType classType : ClassType.values()) {
                TestCase test = new TestCase();
                for (int i = 0; i < groupedAnnotations.size(); ++i) {
                    TestAnnotationInfos annotations = groupedAnnotations.get(i);
                    TestCase.TestClassInfo clazz = test.addClassInfo(classType, "Test" + i);
                    annotations.annotate(clazz);

                    if (classType != ClassType.ENUM) {
                        TestCase.TestMethodInfo constructor = clazz.addMethodInfo("<init>()");
                        annotations.annotate(constructor);

                        TestCase.TestClassInfo localClass = constructor.addLocalClassInfo("Local1" + i);
                        annotations.annotate(localClass);
                    }
                    if (classType != ClassType.ANNOTATION) {
                        TestCase.TestMethodInfo staticClassMethod = clazz.addMethodInfo("staticClassMethod" + i + "()", "static");
                        annotations.annotate(staticClassMethod);

                        TestCase.TestClassInfo localClassInStaticMethod = staticClassMethod.addLocalClassInfo("Local2" + i);
                        annotations.annotate(localClassInStaticMethod);
                    }
                    TestCase.TestMethodInfo classMethod = clazz.addMethodInfo("classMethod" + i + "()");
                    annotations.annotate(classMethod);

                    TestCase.TestClassInfo localClassInClassMethod = classMethod.addLocalClassInfo("Local3" + i);
                    annotations.annotate(localClassInClassMethod);

                    TestCase.TestFieldInfo field = clazz.addFieldInfo("field" + i);
                    annotations.annotate(field);

                    TestCase.TestFieldInfo staticField = clazz.addFieldInfo("staticField" + i, "static");
                    annotations.annotate(staticField);
                }
                testCases.add(test);
            }
        }
        return testCases;
    }

    public static void main(String[] args) throws TestFailedException {
        new RuntimeAnnotationsForTopLevelClassTest().test();
    }
}
