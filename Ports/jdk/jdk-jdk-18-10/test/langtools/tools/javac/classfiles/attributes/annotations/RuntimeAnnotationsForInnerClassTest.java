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
 * @build RuntimeAnnotationsForInnerClassTest AnnotationsTestBase RuntimeAnnotationsTestBase
 * @run main RuntimeAnnotationsForInnerClassTest
 */

import java.util.ArrayList;
import java.util.List;

/**
 * The test checks that RuntimeVisibleAnnotationsAttribute and RuntimeInvisibleAnnotationsAttribute
 * are generated properly for inner classes, for constructors, for methods, for fields.
 * The test checks both single and repeatable annotations. In addition, all possible combinations
 * of retention policies are tested.
 *
 * The test generates source code, compiles it and checks the byte code.
 *
 * See README.txt for more information.
 */
public class RuntimeAnnotationsForInnerClassTest extends RuntimeAnnotationsTestBase {
    @Override
    public List<TestCase> generateTestCases() {
        List<TestCase> testCases = new ArrayList<>();
        for (List<TestAnnotationInfos> groupedAnnotations : groupAnnotations(getAllCombinationsOfAnnotations())) {
            for (ClassType outerClassType : ClassType.values()) {
                TestCase test = new TestCase();
                TestCase.TestClassInfo outerClazz = test.addClassInfo(outerClassType, "Test");
                for (int i = 0; i < groupedAnnotations.size(); ++i) {
                    TestAnnotationInfos annotations = groupedAnnotations.get(i);
                    TestCase.TestClassInfo clazz = outerClazz.addInnerClassInfo(ClassType.CLASS, "InnerClass" + i, "static");
                    annotations.annotate(clazz);

                    TestCase.TestMethodInfo constructor = clazz.addMethodInfo("<init>()");
                    annotations.annotate(constructor);

                    TestCase.TestClassInfo localClassInConstructor = constructor.addLocalClassInfo("Local1" + i);
                    annotations.annotate(localClassInConstructor);

                    TestCase.TestMethodInfo innerClazzMethod = clazz.addMethodInfo("innerClassMethod" + i + "()");
                    annotations.annotate(innerClazzMethod);

                    TestCase.TestClassInfo localClassInClassMethod = innerClazzMethod.addLocalClassInfo("Local2" + i);
                    annotations.annotate(localClassInClassMethod);

                    TestCase.TestMethodInfo innerStaticClazzMethod = clazz.addMethodInfo("innerStaticClassMethod" + i + "()", "static");
                    annotations.annotate(innerStaticClazzMethod);

                    TestCase.TestClassInfo localClassInStaticMethod = innerStaticClazzMethod.addLocalClassInfo("Local3" + i);
                    annotations.annotate(localClassInStaticMethod);
                }
                testCases.add(test);
            }
        }
        return testCases;
    }

    public static void main(String[] args) throws TestFailedException {
        new RuntimeAnnotationsForInnerClassTest().test();
    }
}
