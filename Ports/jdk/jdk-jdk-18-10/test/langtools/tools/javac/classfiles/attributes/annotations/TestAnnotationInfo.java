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

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;

import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.stream.Collectors;

public class TestAnnotationInfo {
    public final String annotationName;
    public final RetentionPolicy policy;
    public final boolean isContainer;
    public final List<Pair> elementValues;

    public TestAnnotationInfo(String typeIndexName, RetentionPolicy policy, Pair... values) {
        this(typeIndexName, policy, false, values);
    }

    public TestAnnotationInfo(String typeIndexName, RetentionPolicy policy, boolean isRepeatable, Pair... values) {
        this.annotationName = typeIndexName;
        this.policy = policy;
        this.isContainer = isRepeatable;
        elementValues = Arrays.asList(values);
    }

    public void testAnnotation(TestResult testResult, ClassFile classFile, Annotation annotation)
            throws ConstantPoolException {
        testResult.checkEquals(classFile.constant_pool.getUTF8Value(annotation.type_index),
                String.format("L%s;", annotationName), "Testing annotation name : " + annotationName);
        testResult.checkEquals(annotation.num_element_value_pairs,
                elementValues.size(), "Number of element values");
        if (!testResult.checkEquals(annotation.num_element_value_pairs, elementValues.size(),
                "Number of element value pairs")) {
            return;
        }
        for (int i = 0; i < annotation.num_element_value_pairs; ++i) {
            Annotation.element_value_pair pair = annotation.element_value_pairs[i];
            testResult.checkEquals(classFile.constant_pool.getUTF8Value(pair.element_name_index),
                    elementValues.get(i).elementName, "element_name_index : " + elementValues.get(i).elementName);
            elementValues.get(i).elementValue.testElementValue(testResult, classFile, pair.value);
        }
    }

    @Override
    public String toString() {
        return String.format("@%s(%s)", annotationName,
                elementValues.stream()
                        .map(Pair::toString)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(", ")));
    }

    public static class Pair {
        public final String elementName;
        public final TestElementValue elementValue;

        public Pair(String elementName, TestElementValue elementValue) {
            this.elementName = elementName;
            this.elementValue = elementValue;
        }

        @Override
        public String toString() {
            return elementName + "=" + elementValue;
        }
    }

    public static abstract class TestElementValue {
        private final int tag;

        public TestElementValue(int tag) {
            this.tag = tag;
        }

        public void testTag(TestResult testCase, int actualTag) {
            testCase.checkEquals(actualTag, tag, "tag " + (char) tag);
        }

        public abstract void testElementValue(TestResult testResult,
                                              ClassFile classFile,
                                              Annotation.element_value element_value)
                throws ConstantPoolException;
    }

    public static class TestIntegerElementValue extends TestElementValue {
        private final int value;

        public TestIntegerElementValue(int tag, int value) {
            super(tag);
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Integer_info info =
                    (ConstantPool.CONSTANT_Integer_info) classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value, "const_value_index : " + value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class TestBooleanElementValue extends TestElementValue {
        private final boolean value;

        public TestBooleanElementValue(boolean value) {
            super('Z');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Integer_info info =
                    (ConstantPool.CONSTANT_Integer_info) classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value ? 1 : 0, "const_value_index : " + value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class TestCharElementValue extends TestElementValue {
        private final char value;

        public TestCharElementValue(char value) {
            super('C');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Integer_info info =
                    (ConstantPool.CONSTANT_Integer_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, (int) value, "const_value_index : " + value);
        }

        @Override
        public String toString() {
            return String.format("\'%c\'", value);
        }
    }

    public static class TestLongElementValue extends TestElementValue {
        private final long value;

        public TestLongElementValue(long value) {
            super('J');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPool.InvalidIndex {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Long_info info =
                    (ConstantPool.CONSTANT_Long_info) classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value, "const_value_index");
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class TestFloatElementValue extends TestElementValue {
        private final float value;

        public TestFloatElementValue(float value) {
            super('F');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPool.InvalidIndex {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Float_info info =
                    (ConstantPool.CONSTANT_Float_info) classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value, "const_value_index");
        }

        @Override
        public String toString() {
            return String.valueOf(value) + "f";
        }
    }

    public static class TestDoubleElementValue extends TestElementValue {
        private final double value;

        public TestDoubleElementValue(double value) {
            super('D');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Double_info info = (ConstantPool.CONSTANT_Double_info)
                    classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value, "const_value_index");
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class TestStringElementValue extends TestElementValue {
        private final String value;

        public TestStringElementValue(String value) {
            super('s');
            this.value = value;
        }

        @Override
        public void testElementValue(TestResult testResult,
                                     ClassFile classFile,
                                     Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Utf8_info info =
                    (ConstantPool.CONSTANT_Utf8_info) classFile.constant_pool.get(ev.const_value_index);
            testResult.checkEquals(info.value, value, "const_value_index");
        }

        @Override
        public String toString() {
            return String.format("\"%s\"", value);
        }
    }

    public static class TestEnumElementValue extends TestElementValue {
        private final String typeName;
        private final String constName;

        public TestEnumElementValue(String typeName, String constName) {
            super('e');
            this.typeName = typeName;
            this.constName = constName;
        }

        @Override
        public void testElementValue(
                TestResult testResult,
                ClassFile classFile,
                Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Enum_element_value ev = (Annotation.Enum_element_value) element_value;
            testResult.checkEquals(classFile.constant_pool.getUTF8Info(ev.type_name_index).value,
                    String.format("L%s;", typeName), "type_name_index");
            testResult.checkEquals(classFile.constant_pool.getUTF8Info(ev.const_name_index).value,
                    constName, "const_name_index");
        }

        @Override
        public String toString() {
            return typeName + "." + constName;
        }
    }

    public static class TestClassElementValue extends TestElementValue {
        private final String className;

        private final static Map<String, String> mappedClassName;

        static {
            mappedClassName = new HashMap<>();
            mappedClassName.put("void", "V");
            mappedClassName.put("char", "C");
            mappedClassName.put("byte", "B");
            mappedClassName.put("short", "S");
            mappedClassName.put("int", "I");
            mappedClassName.put("long", "J");
            mappedClassName.put("float", "F");
            mappedClassName.put("double", "D");
        }

        public TestClassElementValue(String className) {
            super('c');
            this.className = className;
        }

        @Override
        public void testElementValue(
                TestResult testResult,
                ClassFile classFile,
                Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Class_element_value ev = (Annotation.Class_element_value) element_value;
            String expectedClassName = className.replace(".class", "");
            expectedClassName = mappedClassName.getOrDefault(expectedClassName,
                    String.format("Ljava/lang/%s;", expectedClassName));
            testResult.checkEquals(
                    classFile.constant_pool.getUTF8Info(ev.class_info_index).value,
                    expectedClassName, "class_info_index : " + expectedClassName);
        }

        @Override
        public String toString() {
            return className;
        }
    }

    public static class TestArrayElementValue extends TestElementValue {
        public final List<TestElementValue> values;

        public TestArrayElementValue(TestElementValue...values) {
            super('[');
            this.values = new ArrayList<>(Arrays.asList(values));
        }

        @Override
        public void testElementValue(
                TestResult testResult,
                ClassFile classFile,
                Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation.Array_element_value ev = (Annotation.Array_element_value) element_value;

            for (int i = 0; i < values.size(); ++i) {
                values.get(i).testElementValue(testResult, classFile, ev.values[i]);
            }
        }

        @Override
        public String toString() {
            return values.stream()
                    .map(TestElementValue::toString)
                    .collect(Collectors.joining(", ", "{", "}"));
        }
    }

    public static class TestAnnotationElementValue extends TestElementValue {
        private final String annotationName;
        private final TestAnnotationInfo annotation;

        public TestAnnotationElementValue(String className, TestAnnotationInfo annotation) {
            super('@');
            this.annotationName = className;
            this.annotation = annotation;
        }

        @Override
        public void testElementValue(
                TestResult testResult,
                ClassFile classFile,
                Annotation.element_value element_value)
                throws ConstantPoolException {
            testTag(testResult, element_value.tag);
            Annotation ev = ((Annotation.Annotation_element_value) element_value).annotation_value;
            testResult.checkEquals(
                    classFile.constant_pool.getUTF8Info(ev.type_index).value,
                    String.format("L%s;", annotationName),
                    "type_index");
            for (int i = 0; i < ev.num_element_value_pairs; ++i) {
                Annotation.element_value_pair pair = ev.element_value_pairs[i];
                Pair expectedPair = annotation.elementValues.get(i);
                expectedPair.elementValue.testElementValue(testResult, classFile, pair.value);
                testResult.checkEquals(
                        classFile.constant_pool.getUTF8Info(pair.element_name_index).value,
                        expectedPair.elementName,
                        "element_name_index");
            }
        }

        @Override
        public String toString() {
            return annotation.toString();
        }
    }
}