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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.ContentType;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test ValueDescriptor.getContentType()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.valuedescriptor.TestValueDescriptorContentType
 */
public class TestValueDescriptorContentType {

    @MetadataDefinition
    @ContentType
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
    static public @interface Hawaiian {
    }

    @MetadataDefinition
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    static public @interface NotContentType {
    }

    @SuppressWarnings("unused")
    private static class AlohaEvent extends Event {
        @Hawaiian
        String greeting;

        String missing;

        @NotContentType
        String otherAnnotation;
    }

    public static void main(String[] args) throws Throwable {
        EventType type = EventType.getEventType(AlohaEvent.class);

        // check field annotation on event value
        ValueDescriptor filter = type.getField("greeting");
        Asserts.assertEquals(filter.getContentType(), Hawaiian.class.getName());

        // check field annotation with missing content type
        ValueDescriptor missing = type.getField("missing");
        Asserts.assertEquals(missing.getContentType(), null);

        // check field annotation with annotation but not content type
        ValueDescriptor otherAnnotation = type.getField("otherAnnotation");
        Asserts.assertEquals(otherAnnotation.getContentType(), null);
    }

}
