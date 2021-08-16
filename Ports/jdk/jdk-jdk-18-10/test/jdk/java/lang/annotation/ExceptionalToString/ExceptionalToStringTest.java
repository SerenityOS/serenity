/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5040830
 * @summary Verify information annotation strings with exception proxies
 * @compile -sourcepath version1 version1/Fleeting.java  version1/Utopia.java version1/DangerousAnnotation.java
 * @compile  AnnotationHost.java
 * @build ExceptionalToStringTest
 * @run main ExceptionalToStringTest
 * @clean Utopia DangerousAnnotation
 * @compile -sourcepath version2 -implicit:none version2/Utopia.java
 * @compile -sourcepath version2 -implicit:none version2/DangerousAnnotation.java
 * @clean Fleeting
 * @run main ExceptionalToStringTest
 */

/**
 * There are three potential exception conditions which can occur
 * reading annotations:
 *
 * EnumConstantNotPresentException - "Thrown when an application tries
 * to access an enum constant by name and the enum type contains no
 * constant with the specified name."
 *
 * AnnotationTypeMismatchException - "Thrown to indicate that a
 * program has attempted to access an element of an annotation whose
 * type has changed after the annotation was compiled (or serialized)"
 *
 * TypeNotPresentException - "Thrown when an application tries to
 * access a type using a string representing the type's name, but no
 * definition for the type with the specified name can be found."
 *
 * The test reads an annotation, DangerousAnnotation, which can
 * display all three pathologies. The pathologies are <em>not</em>
 * present when the version1 sources are used but are present with the
 * version2 sources. The version2 sources remove an enum constant
 * (EnumConstantNotPresentException), change the return type of an
 * annotation method (AnnotationTypeMismatchException), and remove a
 * type whose Class literal is referenced (TypeNotPresentException).
 */
public class ExceptionalToStringTest {
    public static void main(String... args) {
        String annotationAsString = AnnotationHost.class.getAnnotation(DangerousAnnotation.class).toString();

        // Verify no occurrence of "ExceptionProxy" in the string.
        if (annotationAsString.indexOf("ExceptionProxy") != -1) {
            throw new RuntimeException();
        }
    }
}
