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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;
import jdk.jfr.SettingDefinition;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleSetting;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestName
 */
public class TestName {

    @MetadataDefinition
    @Name("com.oracle.TestAnnotation")
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface NamedAnnotation {
    }

    @NamedAnnotation
    @Name("com.oracle.TestEvent")
    static class NamedEvent extends Event {
        @Name("testField")
        boolean namedField;

        @SettingDefinition
        @Name("name")
        boolean dummy(SimpleSetting ds) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        EventType t = EventType.getEventType(NamedEvent.class);
        ValueDescriptor testField = t.getField("testField");
        SettingDescriptor setting = getSetting(t, "name");
        AnnotationElement a = Events.getAnnotationByName(t, "com.oracle.TestAnnotation");

        // Check that names are overridden
        Asserts.assertNotNull(testField, "Can't find expected field testField");
        Asserts.assertEquals(t.getName(), "com.oracle.TestEvent", "Incorrect name for event");
        Asserts.assertEquals(a.getTypeName(), "com.oracle.TestAnnotation", "Incorrect name for annotation");
        Asserts.assertEquals(a.getTypeName(), "com.oracle.TestAnnotation", "Incorrect name for annotation");
        Asserts.assertEquals(setting.getName(), "name", "Incorrect name for setting");

        // Check that @Name is persisted
        assertAnnotation(t.getAnnotation(Name.class), "@Name should be persisted on event");
        assertAnnotation(testField.getAnnotation(Name.class), "@Name should be persisted on field");
        assertAnnotation(a.getAnnotation(Name.class), "@Name should be persisted on annotations");
        assertAnnotation(setting.getAnnotation(Name.class), "@Name should be persisted on setting");
    }

    // Can't use assert since the use toString on the object which doesn't work well JFR proxies.
    private static void assertAnnotation(Object annotation,String message) throws Exception {
       if (annotation == null) {
           throw new Exception(message);
       }
    }

    private static SettingDescriptor getSetting(EventType t, String name) {
        for (SettingDescriptor v : t.getSettingDescriptors()) {
            if (v.getName().equals(name)) {
                return v;
            }
        }
        Asserts.fail("Could not find setting with name " + name);
        return null;
    }
}
