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
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
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
 * @run main/othervm jdk.jfr.api.metadata.annotations.TestLabel
 */
public class TestLabel {

    @MetadataDefinition
    @Label("Annotation Label")
    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnnotionWithLabel {
    }

    @AnnotionWithLabel
    @Label("Event Label")
    static class LabelEvent extends Event {
        @Label("Field Label")
        boolean labledField;

        @SettingDefinition
        @Label("Setting Label")
        boolean dummy(SimpleSetting ds) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        // Event
        EventType t = EventType.getEventType(LabelEvent.class);
        Asserts.assertEquals(t.getLabel(), "Event Label", "Incorrect label for event");

        // Field
        ValueDescriptor field = t.getField("labledField");
        Asserts.assertEquals(field.getLabel(), "Field Label", "Incorrect label for field");

        // Annotation
        AnnotationElement awl = Events.getAnnotationByName(t, AnnotionWithLabel.class.getName());
        Label label = awl.getAnnotation(Label.class);
        Asserts.assertEquals(label.value(), "Annotation Label", "Incorrect label for annotation");

        // Setting
        for (SettingDescriptor v: t.getSettingDescriptors()) {
            if (v.getName().equals("dummy")) {
                Label settingLabel = v.getAnnotation(Label.class);
                Asserts.assertEquals(settingLabel.value(), "Setting Label", "Incorrect label for setting");
            }
        }
    }
}
