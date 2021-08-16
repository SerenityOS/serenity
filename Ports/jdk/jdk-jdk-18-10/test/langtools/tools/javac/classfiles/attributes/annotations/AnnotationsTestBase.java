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

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class AnnotationsTestBase extends TestResult {

    /**
     * Element values which are used in generation of annotations.
     */
    private static final TestAnnotationInfo.Pair[] elementValues = {
            new TestAnnotationInfo.Pair("booleanValue", new TestAnnotationInfo.TestBooleanElementValue(true)),
            new TestAnnotationInfo.Pair("byteValue", new TestAnnotationInfo.TestIntegerElementValue('B', 83)),
            new TestAnnotationInfo.Pair("charValue", new TestAnnotationInfo.TestCharElementValue('H')),
            new TestAnnotationInfo.Pair("shortValue", new TestAnnotationInfo.TestIntegerElementValue('S', 14)),
            new TestAnnotationInfo.Pair("intValue", new TestAnnotationInfo.TestIntegerElementValue('I', 18)),
            new TestAnnotationInfo.Pair("longValue", new TestAnnotationInfo.TestLongElementValue(14)),
            new TestAnnotationInfo.Pair("floatValue", new TestAnnotationInfo.TestFloatElementValue(-1)),
            new TestAnnotationInfo.Pair("doubleValue", new TestAnnotationInfo.TestDoubleElementValue(-83)),
            new TestAnnotationInfo.Pair("stringValue", new TestAnnotationInfo.TestStringElementValue("///")),
            new TestAnnotationInfo.Pair("arrayValue1", new TestAnnotationInfo.TestArrayElementValue(
                    new TestAnnotationInfo.TestIntegerElementValue('I', 1),
                    new TestAnnotationInfo.TestIntegerElementValue('I', 4),
                    new TestAnnotationInfo.TestIntegerElementValue('I', 8),
                    new TestAnnotationInfo.TestIntegerElementValue('I', 3))),
            new TestAnnotationInfo.Pair("arrayValue2", new TestAnnotationInfo.TestArrayElementValue(
                    new TestAnnotationInfo.TestStringElementValue("AAA"),
                    new TestAnnotationInfo.TestStringElementValue("BBB"))),
            new TestAnnotationInfo.Pair("enumValue", new TestAnnotationInfo.TestEnumElementValue("EnumValue", "VALUE2")),
            new TestAnnotationInfo.Pair("classValue1", new TestAnnotationInfo.TestClassElementValue("void.class")),
            new TestAnnotationInfo.Pair("classValue2", new TestAnnotationInfo.TestClassElementValue("Character.class")),
            new TestAnnotationInfo.Pair("annoValue", new TestAnnotationInfo.TestAnnotationElementValue("AnnotationValue",
                    new TestAnnotationInfo("AnnotationValue", RetentionPolicy.CLASS,
                            new TestAnnotationInfo.Pair("stringValue",
                                    new TestAnnotationInfo.TestStringElementValue("StringValue1"))))),
            new TestAnnotationInfo.Pair("annoArrayValue", new TestAnnotationInfo.TestArrayElementValue(
                    new TestAnnotationInfo.TestAnnotationElementValue("AnnotationValue",
                            new TestAnnotationInfo("AnnotationValue", RetentionPolicy.CLASS,
                                new TestAnnotationInfo.Pair("stringValue",
                                        new TestAnnotationInfo.TestStringElementValue("StringValue1")))),
                    new TestAnnotationInfo.TestAnnotationElementValue("AnnotationValue",
                            new TestAnnotationInfo("AnnotationValue", RetentionPolicy.CLASS,
                                    new TestAnnotationInfo.Pair("stringValue",
                                            new TestAnnotationInfo.TestStringElementValue("StringValue1"))))))
    };

    /**
     * Masks which are used in generation of annotations.
     * E.g. mask 0 corresponds to an annotation without element values.
     */
    private static final int[] elementValuesCombinations;

    static {
        List<Integer> combinations = new ArrayList<>();
        combinations.add(0);
        for (int i = 0; i < elementValues.length; ++i) {
            combinations.add(1 << i);
        }
        // pairs int value and another value
        for (int i = 0; i < elementValues.length; ++i) {
            combinations.add((1 << 5) | (1 << i));
        }
        combinations.add((1 << elementValues.length) - 1);
        elementValuesCombinations = combinations.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Method generates a list of test cases.
     * Method is called in the method {@code call()}.
     *
     * @return a list of test cases
     */
    public abstract List<TestCase> generateTestCases();

    public abstract void test(TestCase testCase, Map<String, ? extends JavaFileObject> classes)
            throws IOException, ConstantPoolException, Descriptor.InvalidDescriptor;

    /**
     * The method is used to create a repeatable annotation.
     */
    private TestAnnotationInfo createSomeAnnotation(String annotationName) {
        return new TestAnnotationInfo(annotationName, getRetentionPolicy(annotationName),
                new TestAnnotationInfo.Pair("booleanValue",
                        new TestAnnotationInfo.TestBooleanElementValue(true)),
                new TestAnnotationInfo.Pair("intValue",
                        new TestAnnotationInfo.TestIntegerElementValue('I', 1)),
                new TestAnnotationInfo.Pair("enumValue",
                        new TestAnnotationInfo.TestEnumElementValue("EnumValue", "VALUE1")));
    }

    private TestAnnotationInfo getAnnotationByMask(String annotationName, int mask) {
        List<TestAnnotationInfo.Pair> pairs = new ArrayList<>();
        for (int i = 0; i < elementValues.length; ++i) {
            if ((mask & (1 << i)) != 0) {
                pairs.add(elementValues[i]);
            }
        }
        return new TestAnnotationInfo(
                annotationName,
                getRetentionPolicy(annotationName),
                pairs.toArray(new TestAnnotationInfo.Pair[pairs.size()]));
    }

    /**
     * Class represents annotations which will be applied to one method.
     */
    public static class TestAnnotationInfos {
        public final List<TestAnnotationInfo> annotations;

        public TestAnnotationInfos(List<TestAnnotationInfo> a) {
            this.annotations = a;
        }

        public void annotate(TestCase.TestMemberInfo memberInfo) {
            annotations.forEach(memberInfo::addAnnotation);
        }
    }

    /**
     * Convenience method to group test cases.
     * Increases speed of tests.
     */
    public List<List<TestAnnotationInfos>> groupAnnotations(List<TestAnnotationInfos> annotations) {
        List<List<TestAnnotationInfos>> groupedAnnotations = new ArrayList<>();
        int size = 32;
        List<TestAnnotationInfos> current = null;
        for (TestAnnotationInfos infos : annotations) {
            if (current == null || current.size() == size) {
                current = new ArrayList<>();
                groupedAnnotations.add(current);
            }
            current.add(infos);
        }
        return groupedAnnotations;
    }

    public List<TestAnnotationInfos> getAllCombinationsOfAnnotations() {
        List<TestAnnotationInfos> combinations = new ArrayList<>();
        for (Annotations annotationName1 : Annotations.values()) {
            List<TestAnnotationInfo> list = IntStream.of(elementValuesCombinations)
                    .mapToObj(e -> getAnnotationByMask(annotationName1.getAnnotationName(), e))
                    .collect(Collectors.toList());
            // add cases with a single annotation
            combinations.addAll(list.stream()
                    .map(Collections::singletonList)
                    .map(TestAnnotationInfos::new)
                    .collect(Collectors.toList()));

            // add cases with a repeatable annotation
            for (Annotations annotationName2 : Annotations.values()) {
                if (annotationName1 == annotationName2 && !annotationName1.isRepeatable()) {
                    continue;
                }
                TestAnnotationInfo annotation2 = createSomeAnnotation(annotationName2.getAnnotationName());
                for (TestAnnotationInfo annotation1 : list) {
                    List<TestAnnotationInfo> list1 = new ArrayList<>();
                    Collections.addAll(list1, annotation1, annotation2);
                    combinations.add(new TestAnnotationInfos(list1));
                }
            }
        }
        return combinations;
    }

    protected RetentionPolicy getRetentionPolicy(String name) {
        if (name.contains("Visible")) {
            return RetentionPolicy.RUNTIME;
        } else if (name.contains("Invisible")) {
            return RetentionPolicy.CLASS;
        }
        throw new IllegalArgumentException(name);
    }

    protected long countNumberOfAttributes(Attribute[] attrs,
                                               Class<? extends Attribute> clazz) {
        return Stream.of(attrs)
                .filter(clazz::isInstance)
                .count();
    }

    public void test() throws TestFailedException {
        try {
            List<TestCase> testCases = generateTestCases();
            for (int i = 0; i < testCases.size(); ++i) {
                TestCase testCase = testCases.get(i);
                String source = testCase.generateSource();
                Path sourceFile = Paths.get(getClass().getSimpleName() + i + ".java");
                addTestCase(sourceFile.toAbsolutePath().toString());
                writeToFileIfEnabled(sourceFile, source);
                echo("Testing: " + sourceFile.toString());
                try {
                    test(testCase, compile(source).getClasses());
                } catch (Exception e) {
                    addFailure(e);
                }
            }
        } catch (RuntimeException | IOException e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    public enum Annotations {
        RUNTIME_INVISIBLE_REPEATABLE("RuntimeInvisibleRepeatable", true),
        RUNTIME_INVISIBLE_NOT_REPEATABLE("RuntimeInvisibleNotRepeatable", false),
        RUNTIME_VISIBLE_REPEATABLE("RuntimeVisibleRepeatable", true),
        RUNTIME_VISIBLE_NOT_REPEATABLE("RuntimeVisibleNotRepeatable", false);

        private final String annotationName;
        private final boolean isRepeatable;

        Annotations(String annotationName, boolean isRepeatable) {
            this.annotationName = annotationName;
            this.isRepeatable = isRepeatable;
        }

        public String getAnnotationName() {
            return annotationName;
        }

        public boolean isRepeatable() {
            return isRepeatable;
        }
    }
}
