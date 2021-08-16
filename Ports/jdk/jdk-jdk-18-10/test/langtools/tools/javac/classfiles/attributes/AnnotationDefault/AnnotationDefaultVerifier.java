/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.classfile.AnnotationDefault_attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AnnotationDefaultVerifier {

    private final Map<Integer, TestElementValue> verifiers;

    public AnnotationDefaultVerifier() {
        this.verifiers = new HashMap<>();
        verifiers.put((int) 'B', new TestIntegerElementValue());
        verifiers.put((int) 'C', new TestIntegerElementValue());
        verifiers.put((int) 'D', new TestDoubleElementValue());
        verifiers.put((int) 'F', new TestFloatElementValue());
        verifiers.put((int) 'I', new TestIntegerElementValue());
        verifiers.put((int) 'J', new TestLongElementValue());
        verifiers.put((int) 'S', new TestIntegerElementValue());
        verifiers.put((int) 'Z', new TestIntegerElementValue());
        verifiers.put((int) 's', new TestStringElementValue());
        verifiers.put((int) 'e', new TestEnumElementValue());
        verifiers.put((int) 'c', new TestClassElementValue());
        verifiers.put((int) '[', new TestArrayElementValue());
        verifiers.put((int) '@', new TestAnnotationElementValue());
    }

    public void testLength(int tag, TestResult testResult, AnnotationDefault_attribute attr) {
        verifiers.get(tag).testLength(testResult, attr);
    }

    public void testElementValue(int tag, TestResult testResult, ClassFile classFile,
                                 Annotation.element_value element_value, String[] values)
            throws ConstantPool.UnexpectedEntry, ConstantPool.InvalidIndex {
        get(tag).testElementValue(testResult, classFile, element_value, values);
    }

    private TestElementValue get(int tag) {
        TestElementValue ev = verifiers.get(tag);
        if (ev == null) {
            throw new IllegalArgumentException("Unknown tag : " + (char) tag);
        }
        return ev;
    }

    private abstract class TestElementValue {
        public void testLength(TestResult testCase, AnnotationDefault_attribute attr) {
            testCase.checkEquals(attr.attribute_length, 1 + attr.default_value.length(),
                    "attribute_length");
        }

        public String[] getValues(String[] values, int index, int length) {
            return Arrays.copyOfRange(values, index, index + length);
        }

        public int getLength() {
            return 1;
        }

        public abstract void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values)
                throws ConstantPool.InvalidIndex, ConstantPool.UnexpectedEntry;
    }

    private class TestIntegerElementValue extends TestElementValue {

        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values) throws ConstantPool.InvalidIndex {
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Integer_info info =
                    (ConstantPool.CONSTANT_Integer_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testCase.checkEquals(info.value, Integer.parseInt(values[0]), "const_value_index");
        }
    }

    private class TestLongElementValue extends TestElementValue {
        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values) throws ConstantPool.InvalidIndex {
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Long_info info =
                    (ConstantPool.CONSTANT_Long_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testCase.checkEquals(info.value, Long.parseLong(values[0]), "const_value_index");
        }
    }

    private class TestFloatElementValue extends TestElementValue {
        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values) throws ConstantPool.InvalidIndex {
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Float_info info =
                    (ConstantPool.CONSTANT_Float_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testCase.checkEquals(info.value, Float.parseFloat(values[0]), "const_value_index");
        }
    }

    private class TestDoubleElementValue extends TestElementValue {
        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values) throws ConstantPool.InvalidIndex {
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Double_info info =
                    (ConstantPool.CONSTANT_Double_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testCase.checkEquals(info.value, Double.parseDouble(values[0]), "const_value_index");
        }
    }

    private class TestStringElementValue extends TestElementValue {
        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values) throws ConstantPool.InvalidIndex {
            Annotation.Primitive_element_value ev =
                    (Annotation.Primitive_element_value) element_value;
            ConstantPool.CONSTANT_Utf8_info info =
                    (ConstantPool.CONSTANT_Utf8_info)
                            classFile.constant_pool.get(ev.const_value_index);
            testCase.checkEquals(info.value, values[0], "const_value_index");
        }
    }

    private class TestEnumElementValue extends TestElementValue {

        @Override
        public int getLength() {
            return 2;
        }

        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values)
                throws ConstantPool.InvalidIndex, ConstantPool.UnexpectedEntry {
            Annotation.Enum_element_value ev = (Annotation.Enum_element_value) element_value;
            testCase.checkEquals(classFile.constant_pool.getUTF8Info(ev.type_name_index).value,
                    values[0], "type_name_index");
            testCase.checkEquals(classFile.constant_pool.getUTF8Info(ev.const_name_index).value,
                    values[1], "const_name_index");
        }
    }

    private class TestClassElementValue extends TestElementValue {
        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values)
                throws ConstantPool.InvalidIndex, ConstantPool.UnexpectedEntry {
            Annotation.Class_element_value ev = (Annotation.Class_element_value) element_value;
            testCase.checkEquals(
                    classFile.constant_pool.getUTF8Info(ev.class_info_index).value,
                    values[0], "class_info_index");
        }
    }

    private class TestAnnotationElementValue extends TestElementValue {
        @Override
        public void testLength(TestResult testCase, AnnotationDefault_attribute attr) {
            // Suppress, since it is hard to test the length of this kind of element values.
        }

        @Override
        public int getLength() {
            // Expected that the test uses DefaultAnnotation
            // tag (1 byte) + annotation_value (2 bytes) which contains const_value
            return 3;
        }

        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values)
                throws ConstantPool.InvalidIndex, ConstantPool.UnexpectedEntry {
            Annotation ev = ((Annotation.Annotation_element_value) element_value)
                    .annotation_value;
            testCase.checkEquals(
                    classFile.constant_pool.getUTF8Info(ev.type_index).value,
                    values[0],
                    "type_index");
            for (int i = 0; i < ev.num_element_value_pairs; ++i) {
                Annotation.element_value_pair pair = ev.element_value_pairs[i];
                testCase.checkEquals(
                        classFile.constant_pool.getUTF8Info(pair.element_name_index).value,
                        values[2 * i + 1],
                        "element_name_index");
                TestElementValue testElementValue = verifiers.get(pair.value.tag);
                testElementValue.testElementValue(
                        testCase,
                        classFile,
                        pair.value,
                        new String[]{values[2 * i + 2]});
            }
        }
    }

    private class TestArrayElementValue extends TestElementValue {
        @Override
        public void testLength(TestResult testCase, AnnotationDefault_attribute attr) {
            Annotation.Array_element_value ev =
                    (Annotation.Array_element_value) attr.default_value;
            int sizeOfTag = ev.values[0].tag == 'e' ? 0 : 1;
            // tag (1 byte) + array header (2 byte) + length of entries
            testCase.checkEquals(attr.attribute_length, 1 + 2 +
                    (sizeOfTag + ev.length() / ev.num_values) * ev.num_values, "attribute_length");
        }

        @Override
        public void testElementValue(
                TestResult testCase,
                ClassFile classFile,
                Annotation.element_value element_value,
                String[] values)
                throws ConstantPool.InvalidIndex, ConstantPool.UnexpectedEntry {
            Annotation.Array_element_value ev =
                    (Annotation.Array_element_value) element_value;
            int index = 0;
            for (int i = 0; i < ev.num_values; ++i) {
                TestElementValue testElementValue = verifiers.get(ev.values[i].tag);
                int length = testElementValue.getLength();
                testElementValue.testElementValue(
                        testCase,
                        classFile,
                        ev.values[i],
                        testElementValue.getValues(values, index, length));
                index += length;
            }
        }
    }
}
