/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222430
 * @summary Test various predicates of ElementKind.
 */

import java.util.Set;
import java.util.function.Predicate;
import javax.lang.model.element.ElementKind;

/**
 * Test the isClass, isField, and isInterface predicates of ElementKind.
 */
public class TestElementKindPredicates {
    public static void main(String... args) {
        Set<ElementKind> ALL_KINDS = Set.of(ElementKind.values());

        // isClass: Returns true if this is a kind of class: either CLASS or ENUM.
        test(ALL_KINDS,
             (ElementKind k) -> Set.of(ElementKind.CLASS,
                                       ElementKind.ENUM,
                                       ElementKind.RECORD).contains(k),
             (ElementKind k) -> k.isClass(), "isClass");

        // isField: Returns true if this is a kind of field: either FIELD or ENUM_CONSTANT.
        test(ALL_KINDS,
             (ElementKind k) -> Set.of(ElementKind.FIELD,
                                       ElementKind.ENUM_CONSTANT).contains(k),
             (ElementKind k) -> k.isField(), "isField");

        // isInterface: Returns true if this is a kind of interface: either INTERFACE or ANNOTATION_TYPE.
        test(ALL_KINDS,
             (ElementKind k) -> Set.of(ElementKind.INTERFACE,
                                       ElementKind.ANNOTATION_TYPE).contains(k),
             (ElementKind k) -> k.isInterface(), "isInterface");
    }

    private static void test(Set<ElementKind> kinds,
                             Predicate<ElementKind> expectedPred,
                             Predicate<ElementKind> actualPred,
                             String errorMessage) {
        for(ElementKind kind : kinds) {
            boolean expected = expectedPred.test(kind);
            boolean actual = actualPred.test(kind);

            if (expected != actual) {
                throw new RuntimeException("Error testing ElementKind." + errorMessage + "(" +  kind +
                                           "):\texpected " + expected + "\tgot " + actual);
            }
        }
    }
}
