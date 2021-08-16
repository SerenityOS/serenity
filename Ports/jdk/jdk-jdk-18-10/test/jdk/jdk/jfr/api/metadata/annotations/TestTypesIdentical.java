/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.metadata.annotations;

import jdk.jfr.AnnotationElement;
import jdk.jfr.BooleanFlag;
import jdk.jfr.Category;
import jdk.jfr.ContentType;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Experimental;
import jdk.jfr.Frequency;
import jdk.jfr.Label;
import jdk.jfr.MemoryAddress;
import jdk.jfr.DataAmount;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.Percentage;
import jdk.jfr.Period;
import jdk.jfr.Registered;
import jdk.jfr.Relational;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.jfr.TransitionFrom;
import jdk.jfr.TransitionTo;
import jdk.jfr.Unsigned;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestTypesIdentical
 */
public class TestTypesIdentical {

    @MetadataDefinition
    @interface CustomAnnotation {
        String value();
    }

    private static Class<?>[] predefinedAnnotations = {
        Category.class, Enabled.class,  Frequency.class,  DataAmount.class,  Percentage.class,  StackTrace.class,  Timestamp.class,  Unsigned.class,
        ContentType.class,  Experimental.class,  Label.class, Registered.class, Period.class, Threshold.class,  TransitionFrom.class,
        Description.class, BooleanFlag.class,  MemoryAddress.class,  Name.class,  Relational.class, Timespan.class, TransitionTo.class
    };

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        for(Class<?> clz : predefinedAnnotations) {
            System.out.println("Testing class " + clz);
            assertTypeId((Class<? extends java.lang.annotation.Annotation>) clz);
        }
        assertTypeId(CustomAnnotation.class);
    }

    private static void assertTypeId(Class<? extends java.lang.annotation.Annotation> clz) {
        AnnotationElement a1, a2;
        try {
            a1 = new AnnotationElement(clz, "value");
            a2 = new AnnotationElement(clz, "value2");
        } catch(IllegalArgumentException x) {
            a1 = new AnnotationElement(clz);
            a2 = new AnnotationElement(clz);
        }
        Asserts.assertEquals(a1.getTypeId(), a2.getTypeId());
    }
}
