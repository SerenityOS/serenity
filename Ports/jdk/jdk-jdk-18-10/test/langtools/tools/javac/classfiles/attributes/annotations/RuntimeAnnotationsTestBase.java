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

import com.sun.tools.classfile.*;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.function.Supplier;

import javax.tools.JavaFileObject;

public abstract class RuntimeAnnotationsTestBase extends AnnotationsTestBase {

    @Override
    public void test(TestCase testCase, Map<String, ? extends JavaFileObject> classes)
            throws IOException, ConstantPoolException, Descriptor.InvalidDescriptor {
        for (Map.Entry<String, ? extends JavaFileObject> entry : classes.entrySet()) {
            String className = entry.getKey();
            TestCase.TestClassInfo clazz = testCase.getTestClassInfo(className);
            echo("Testing class : " + className);
            ClassFile classFile = readClassFile(entry.getValue());

            testAttributes(clazz, classFile, () -> classFile.attributes);

            testMethods(clazz, classFile);

            testFields(clazz, classFile);
        }
    }

    private void testMethods(TestCase.TestClassInfo clazz, ClassFile classFile)
            throws ConstantPoolException, Descriptor.InvalidDescriptor {
        String className = clazz.getName();
        Set<String> foundMethods = new HashSet<>();
        for (Method method : classFile.methods) {
            String methodName = method.getName(classFile.constant_pool) +
                    method.descriptor.getParameterTypes(classFile.constant_pool);
            if (methodName.startsWith("<init>")) {
                String constructorName = className.replaceAll(".*\\$", "");
                methodName = methodName.replace("<init>", constructorName);
            }
            echo("Testing method : " + methodName);

            TestCase.TestMethodInfo testMethod = clazz.getTestMethodInfo(methodName);
            foundMethods.add(methodName);
            if (testMethod == null) {
                continue;
            }
            testAttributes(testMethod, classFile, () -> method.attributes);
        }
        checkContains(foundMethods, clazz.methods.keySet(), "Methods in class : " + className);
    }

    private void testFields(TestCase.TestClassInfo clazz, ClassFile classFile)
            throws ConstantPoolException {
        Set<String> foundFields = new HashSet<>();
        for (Field field : classFile.fields) {
            String fieldName = field.getName(classFile.constant_pool);
            echo("Testing field : " + fieldName);

            TestCase.TestFieldInfo testField = clazz.getTestFieldInfo(fieldName);
            foundFields.add(fieldName);
            if (testField == null) {
                continue;
            }
            testAttributes(testField, classFile, () -> field.attributes);
        }
        checkContains(foundFields, clazz.fields.keySet(), "Fields in class : " + clazz.getName());
    }

    private void testAttributes(
            TestCase.TestMemberInfo member,
            ClassFile classFile,
            Supplier<Attributes> attributes)
            throws ConstantPoolException {
        Map<String, Annotation> actualInvisible = collectAnnotations(
                classFile,
                member,
                attributes.get(),
                Attribute.RuntimeInvisibleAnnotations);
        Map<String, Annotation> actualVisible = collectAnnotations(
                classFile,
                member,
                attributes.get(),
                Attribute.RuntimeVisibleAnnotations);

        checkEquals(actualInvisible.keySet(),
                member.getRuntimeInvisibleAnnotations(), "RuntimeInvisibleAnnotations");
        checkEquals(actualVisible.keySet(),
                member.getRuntimeVisibleAnnotations(), "RuntimeVisibleAnnotations");

        for (TestAnnotationInfo expectedAnnotation : member.annotations.values()) {
            RetentionPolicy policy = getRetentionPolicy(expectedAnnotation.annotationName);
            if (policy == RetentionPolicy.SOURCE) {
                continue;
            }
            printf("Testing: isVisible: %s %s%n", policy.toString(), expectedAnnotation.annotationName);
            Annotation actualAnnotation =
                    (policy == RetentionPolicy.RUNTIME ? actualVisible : actualInvisible)
                            .get(expectedAnnotation.annotationName);
            if (checkNotNull(actualAnnotation, "Annotation is found : "
                    + expectedAnnotation.annotationName)) {
                expectedAnnotation.testAnnotation(this, classFile, actualAnnotation);
            }
        }
    }

    private Map<String, Annotation> collectAnnotations(
            ClassFile classFile,
            TestCase.TestMemberInfo member,
            Attributes attributes,
            String attribute) throws ConstantPoolException {

        RuntimeAnnotations_attribute attr = (RuntimeAnnotations_attribute) attributes.get(attribute);
        Map<String, Annotation> actualAnnotations = new HashMap<>();
        RetentionPolicy policy = getRetentionPolicy(attribute);
        if (member.isAnnotated(policy)) {
            if (!checkNotNull(attr, String.format("%s should be not null value", attribute))) {
                // test case failed, stop checking
                return actualAnnotations;
            }
            for (Annotation ann : attr.annotations) {
                String name = classFile.constant_pool.getUTF8Value(ann.type_index);
                actualAnnotations.put(name.substring(1, name.length() - 1), ann);
            }
            checkEquals(countNumberOfAttributes(attributes.attrs,
                    getRetentionPolicy(attribute) == RetentionPolicy.RUNTIME
                            ? RuntimeVisibleAnnotations_attribute.class
                            : RuntimeInvisibleAnnotations_attribute.class),
                    1l,
                    String.format("Number of %s", attribute));
        } else {
            checkNull(attr, String.format("%s should be null", attribute));
        }
        return actualAnnotations;
    }
}
