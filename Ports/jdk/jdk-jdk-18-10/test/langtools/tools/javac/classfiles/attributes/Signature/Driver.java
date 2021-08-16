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
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Method;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The main class of Signature tests.
 * Driver reads golden data of each class member that must have a Signature attribute,
 * after that the class compares expected data with actual one.
 *
 * Example of usage Driver:
 * java Driver Test
 *
 * Each member of the class Test should have @ExpectedSignature annotations
 * if it must have the Signature attribute. Anonymous class cannot be annotated.
 * So its enclosing class should be annotated and method isAnonymous
 * of ExpectedSignature must return true.
 */
public class Driver extends TestResult {

    private final static String ACC_BRIDGE = "ACC_BRIDGE";

    private final String topLevelClassName;
    private final File[] files;

    public Driver(String topLevelClassName) {
        this.topLevelClassName = topLevelClassName;
        // Get top level class and all inner classes.
        FilenameFilter filter = (dir, file) ->
                file.equals(topLevelClassName + ".class")
                        || file.matches(topLevelClassName + "\\$.*\\.class");
        files = getClassDir().listFiles(filter);
    }

    private boolean isAnonymous(String className) {
        return className.matches(".*\\$\\d+$");
    }

    private Class<?> getEnclosingClass(String className) throws ClassNotFoundException {
        return Class.forName(className.replaceFirst("\\$\\d+$", ""));
    }

    private ExpectedSignature getExpectedClassSignature(String className, Class<?> clazz)
            throws ClassNotFoundException {
        // anonymous class cannot be annotated, so information about anonymous class
        // is located in its enclosing class.
        boolean isAnonymous = isAnonymous(className);
        clazz = isAnonymous ? getEnclosingClass(className) : clazz;
        return Stream.of(clazz.getAnnotationsByType(ExpectedSignature.class))
                .filter(s -> s.isAnonymous() == isAnonymous)
                .collect(Collectors.toMap(ExpectedSignature::descriptor, Function.identity()))
                .get(className);
    }

    // Class.getName() cannot be used here, because the method can rely on signature attribute.
    private Map<String, ExpectedSignature> getClassExpectedSignature(String className, Class<?> clazz)
            throws ClassNotFoundException {
        Map<String, ExpectedSignature> classSignatures = new HashMap<>();
        ExpectedSignature classSignature = getExpectedClassSignature(className, clazz);
        if (classSignature != null) {
            classSignatures.put(className, classSignature);
        }
        return classSignatures;
    }

    private Map<String, ExpectedSignature> getExpectedExecutableSignatures(Executable[] executables,
                                                                           Predicate<Executable> filterBridge) {
        return Stream.of(executables)
                .filter(filterBridge)
                .map(e -> e.getAnnotation(ExpectedSignature.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ExpectedSignature::descriptor, Function.identity()));
    }

    private Map<String, ExpectedSignature> getExpectedMethodSignatures(Class<?> clazz) {
        Map<String, ExpectedSignature> methodSignatures =
                getExpectedExecutableSignatures(clazz.getDeclaredMethods(),
                        m -> !((java.lang.reflect.Method) m).isBridge());
        methodSignatures.putAll(
                getExpectedExecutableSignatures(clazz.getDeclaredConstructors(),
                        m -> true));
        return methodSignatures;
    }

    private Map<String, ExpectedSignature> getExpectedFieldSignatures(Class<?> clazz) {
        return Stream.of(clazz.getDeclaredFields())
                .map(f -> f.getAnnotation(ExpectedSignature.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ExpectedSignature::descriptor, Function.identity()));
    }

    public void test() throws TestFailedException {
        try {
            addTestCase("Source is " + topLevelClassName + ".java");
            assertTrue(files.length > 0, "No class files found");
            for (File file : files) {
                try {
                    String className = file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    printf("Testing class %s\n", className);
                    ClassFile classFile = readClassFile(file);

                    // test class signature
                    testAttribute(
                            className,
                            classFile,
                            () -> (Signature_attribute) classFile.getAttribute(Attribute.Signature),
                            getClassExpectedSignature(className, clazz).get(className));

                    testFields(getExpectedFieldSignatures(clazz), classFile);

                    testMethods(getExpectedMethodSignatures(clazz), classFile);
                } catch (Exception e) {
                    addFailure(e);
                }
            }
        } catch (Exception e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    private void checkAllMembersFound(Set<String> found, Map<String, ExpectedSignature> signatures, String message) {
        if (signatures != null) {
            checkContains(found,
                    signatures.values().stream()
                            .map(ExpectedSignature::descriptor)
                            .collect(Collectors.toSet()),
                    message);
        }
    }

    private void testMethods(Map<String, ExpectedSignature> expectedSignatures, ClassFile classFile)
            throws ConstantPoolException, Descriptor.InvalidDescriptor {
        String className = classFile.getName();
        Set<String> foundMethods = new HashSet<>();
        for (Method method : classFile.methods) {
            String methodName = getMethodName(classFile, method);
            printf("Testing method %s\n", methodName);
            if (method.access_flags.getMethodFlags().contains(ACC_BRIDGE)) {
                printf("Bridge method is skipped : %s\n", methodName);
                continue;
            }
            testAttribute(
                    methodName,
                    classFile,
                    () -> (Signature_attribute) method.attributes.get(Attribute.Signature),
                    expectedSignatures.get(methodName));
            foundMethods.add(methodName);
        }
        checkAllMembersFound(foundMethods, expectedSignatures,
                "Checking that all methods of class " + className + " with Signature attribute found");
    }

    private String getMethodName(ClassFile classFile, Method method)
            throws ConstantPoolException, Descriptor.InvalidDescriptor {
        return String.format("%s%s",
                method.getName(classFile.constant_pool),
                method.descriptor.getParameterTypes(classFile.constant_pool));
    }

    private void testFields(Map<String, ExpectedSignature> expectedSignatures, ClassFile classFile)
            throws ConstantPoolException {
        String className = classFile.getName();
        Set<String> foundFields = new HashSet<>();
        for (Field field : classFile.fields) {
            String fieldName = field.getName(classFile.constant_pool);
            printf("Testing field %s\n", fieldName);
            testAttribute(
                    fieldName,
                    classFile,
                    () -> (Signature_attribute) field.attributes.get(Attribute.Signature),
                    expectedSignatures.get(fieldName));
            foundFields.add(fieldName);
        }
        checkAllMembersFound(foundFields, expectedSignatures,
                "Checking that all fields of class " + className + " with Signature attribute found");
    }

    private void testAttribute(
            String memberName,
            ClassFile classFile,
            Supplier<Signature_attribute> sup,
            ExpectedSignature expectedSignature)
            throws ConstantPoolException {

        Signature_attribute attribute = sup.get();
        if (expectedSignature != null && checkNotNull(attribute, memberName + " must have attribute")) {
            checkEquals(classFile.constant_pool.getUTF8Value(attribute.attribute_name_index),
                    "Signature", "Attribute's name : " + memberName);
            checkEquals(attribute.attribute_length, 2, "Attribute's length : " + memberName);
            checkEquals(attribute.getSignature(classFile.constant_pool),
                    expectedSignature.signature(),
                    "Testing signature of : " + memberName);
        } else {
            checkNull(attribute, memberName + " must not have attribute");
        }
    }

    public static void main(String[] args) throws TestFailedException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: Driver <class-name>");
        }
        new Driver(args[0]).test();
    }
}