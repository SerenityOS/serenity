/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.metadata.valuedescriptor;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Label;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test ValueDescriptor.getAnnotations()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.valuedescriptor.TestConstructor
 */
public class TestConstructor {

    public static void main(String[] args) throws Throwable {
        ValueDescriptor vdSimple = new ValueDescriptor(String.class, "message");
        Asserts.assertNull(vdSimple.getAnnotation(Label.class), "Expected getAnnotation()==null");
        Asserts.assertEquals(0, vdSimple.getAnnotationElements().size(), "Expected getAnnotations().size() == 0");

        // Add labelA and verify we can read it back
        List<AnnotationElement> annos = new ArrayList<>();
        AnnotationElement labelA = new AnnotationElement(Label.class, "labelA");
        annos.add(labelA);
        System.out.println("labelA.getClass()" + labelA.getClass());
        ValueDescriptor vdComplex = new ValueDescriptor(String.class, "message", annos);

        final Label outLabel = vdComplex.getAnnotation(Label.class);
        Asserts.assertFalse(outLabel == null, "getLabel(Label.class) was null");
        System.out.println("outLabel.value() = " + outLabel.value());

        // Get labelA from getAnnotations() list
        Asserts.assertEquals(1, vdComplex.getAnnotationElements().size(), "Expected getAnnotations().size() == 1");
        final AnnotationElement outAnnotation = vdComplex.getAnnotationElements().get(0);
        Asserts.assertNotNull(outAnnotation, "outAnnotation was null");
        System.out.printf("Annotation: %s = %s%n", outAnnotation.getTypeName(), outAnnotation.getValue("value"));
        Asserts.assertEquals(outAnnotation, labelA, "Expected firstAnnotation == labelA");

    }
}
