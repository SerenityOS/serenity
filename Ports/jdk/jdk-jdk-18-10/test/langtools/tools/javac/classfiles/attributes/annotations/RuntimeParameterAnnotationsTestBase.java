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

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

public abstract class RuntimeParameterAnnotationsTestBase extends AnnotationsTestBase {

    @Override
    public void test(TestCase testCase, Map<String, ? extends JavaFileObject> classes)
            throws IOException, ConstantPoolException, Descriptor.InvalidDescriptor {
        for (Map.Entry<String, ? extends JavaFileObject> entry : classes.entrySet()) {
            ClassFile classFile = readClassFile(classes.get(entry.getKey()));
            Set<String> foundMethods = new HashSet<>();
            String className = classFile.getName();
            TestCase.TestClassInfo testClassInfo = testCase.classes.get(className);
            for (Method method : classFile.methods) {
                String methodName = method.getName(classFile.constant_pool) +
                        method.descriptor.getParameterTypes(classFile.constant_pool);
                if (methodName.startsWith("<init>")) {
                    methodName = methodName.replace("<init>", className);
                }
                foundMethods.add(methodName);
                echo("Testing method : " + methodName);

                TestCase.TestMethodInfo testMethod = testClassInfo.getTestMethodInfo(methodName);
                if (testMethod == null) {
                    continue;
                }
                testAttributes(testMethod, classFile, method);
            }
            checkContains(foundMethods, testClassInfo.methods.keySet(), "Methods in " + className);
        }
    }

    protected void testAttributes(
            TestCase.TestMethodInfo testMethod,
            ClassFile classFile,
            Method method) throws ConstantPoolException {
        List<Map<String, Annotation>> actualInvisible = collectAnnotations(
                classFile,
                testMethod,
                method,
                Attribute.RuntimeInvisibleParameterAnnotations);
        List<Map<String, Annotation>> actualVisible = collectAnnotations(
                classFile,
                testMethod,
                method,
                Attribute.RuntimeVisibleParameterAnnotations);

        List<TestCase.TestParameterInfo> parameters = testMethod.parameters;
        for (int i = 0; i < parameters.size(); ++i) {
            TestCase.TestParameterInfo parameter = parameters.get(i);
            checkEquals(actualInvisible.get(i).keySet(), parameter.getRuntimeInvisibleAnnotations(),
                    "RuntimeInvisibleParameterAnnotations");
            checkEquals(actualVisible.get(i).keySet(), parameter.getRuntimeVisibleAnnotations(),
                    "RuntimeVisibleParameterAnnotations");
        }

        for (int i = 0; i < parameters.size(); ++i) {
            TestCase.TestParameterInfo parameter = parameters.get(i);
            for (TestAnnotationInfo expectedAnnotation : parameter.annotations.values()) {
                RetentionPolicy policy = getRetentionPolicy(expectedAnnotation.annotationName);
                if (policy == RetentionPolicy.SOURCE) {
                    continue;
                }
                printf("Testing: isVisible: %s %s%n", policy.toString(), expectedAnnotation.annotationName);
                Annotation actualAnnotation =
                        (policy == RetentionPolicy.RUNTIME
                                ? actualVisible
                                : actualInvisible)
                                .get(i).get(expectedAnnotation.annotationName);
                if (checkNotNull(actualAnnotation, "Annotation is found : "
                        + expectedAnnotation.annotationName)) {
                    expectedAnnotation.testAnnotation(this, classFile,
                            actualAnnotation);
                }
            }
        }
    }

    private List<Map<String, Annotation>> collectAnnotations(
            ClassFile classFile,
            TestCase.TestMethodInfo testMethod,
            Method method,
            String attribute) throws ConstantPoolException {

        Attributes attributes = method.attributes;
        RuntimeParameterAnnotations_attribute attr = (RuntimeParameterAnnotations_attribute) attributes.get(attribute);

        List<Map<String, Annotation>> actualAnnotations = new ArrayList<>();
        RetentionPolicy policy = getRetentionPolicy(attribute);
        if (testMethod.isParameterAnnotated(policy)) {
            if (!checkNotNull(attr, "Attribute " + attribute + " must not be null")) {
                testMethod.parameters.forEach($ -> actualAnnotations.add(new HashMap<>()));
                return actualAnnotations;
            }
            for (Annotation[] anns : attr.parameter_annotations) {
                Map<String, Annotation> annotations = new HashMap<>();
                for (Annotation ann : anns) {
                    String name = classFile.constant_pool.getUTF8Value(ann.type_index);
                    annotations.put(name.substring(1, name.length() - 1), ann);
                }
                actualAnnotations.add(annotations);
            }
            checkEquals(countNumberOfAttributes(attributes.attrs,
                    getRetentionPolicy(attribute) == RetentionPolicy.RUNTIME
                            ? RuntimeVisibleParameterAnnotations_attribute.class
                            : RuntimeInvisibleParameterAnnotations_attribute.class),
                    1l,
                    String.format("Number of %s", attribute));
        } else {
            checkNull(attr, String.format("%s should be null", attribute));
            testMethod.parameters.forEach($ -> actualAnnotations.add(new HashMap<>()));
        }
        return actualAnnotations;
    }
}
