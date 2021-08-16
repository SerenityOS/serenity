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

== Description of tests for RuntimeVisibleAnnotations, RuntimeInVisibleAnnotations
RuntimeVisibleParameterAnnotations and RuntimeInvisibleParameterAnnotations attributes ==

* AnnotationsTestBase class is a base class for the Annotations attribute tests.
It contains some convenience methods which might be used in derived classes.

* ClassType is a enum which is used for convenience code generation (see TestCase).

* TestCase is a class which represent a test case. TestCase contains TestClassInfo,
which represents a class, TestMethodInfo, which represents a method, TestFieldInfo,
which represents a field, and TestParameterInfo, which represents a method's parameter.
The class is used as test case builder. For example, the following code creates
the top-level class Test with method classMethod() and local class Local.
Each program member is annotated by some annotation which is an instance of
TestAnnotationInfo (see TestAnnotationInfo):

    TestCase test = new TestCase(ClassType.CLASS, "Test", "public");
    test.clazz.addAnnotation(annotations);
    TestCase.TestMethodInfo classMethod = test.clazz.addMethod("classMethod()");
    classMethod.addAnnotation(annotation);
    TestCase.TestClassInfo localClassInClassMethod = classMethod.addLocalClass("Local");
    localClassInClassMethod.addAnnotation(annotations);

Let "annotations" be a list of annotations A, B(i = 2). Thus, a test will generate the
following code:

    @A
    @B(i = 2)
    public class Test {
        @A
        @B(i = 2)
        void classMethod() {
            @A
            @B(i = 2)
            class Local {
            }
        }
    }

Thus, TestCase contains information about structure of classes and golden data
about annotations. Thereafter, sources can be generated from this class representation
by calling method generateSource(). Enum ClassType is used to handle "class type specific"
code generation. For example, not-abstract method in a class always has body,
while not-static and not-default method in an interface does not.