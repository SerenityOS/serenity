/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.metadata.settingdescriptor;

import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Description;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.Timespan;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test SettingDescriptor.getAnnotationElements()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.metadata.settingdescriptor.TestGetAnnotationElement
 */
public class TestGetAnnotationElement {

    public static void main(String[] args) throws Exception {
        EventType type = EventType.getEventType(CustomEvent.class);

        SettingDescriptor plain = Events.getSetting(type, "plain");
        Asserts.assertTrue(plain.getAnnotationElements().isEmpty());

        SettingDescriptor annotatedType = Events.getSetting(type, "annotatedType");
        for (AnnotationElement ae : annotatedType.getAnnotationElements()) {
            System.out.println(ae.getTypeName());
        }
        Asserts.assertTrue(annotatedType.getAnnotationElements().isEmpty());

        SettingDescriptor newName = Events.getSetting(type, "newName");
        List<AnnotationElement> ae = newName.getAnnotationElements();
        Asserts.assertEquals(ae.size(), 4);
        Asserts.assertEquals(ae.get(0).getTypeName(), Name.class.getName());
        Asserts.assertEquals(ae.get(1).getTypeName(), Label.class.getName());
        Asserts.assertEquals(ae.get(2).getTypeName(), Description.class.getName());
        Asserts.assertEquals(ae.get(3).getTypeName(), Timespan.class.getName());

        SettingDescriptor overridden = Events.getSetting(type, "overridden");
        Asserts.assertTrue(overridden.getAnnotationElements().isEmpty());

        CustomEvent.assertOnDisk((x, y) -> {
            List<AnnotationElement> a1 = x.getAnnotationElements();
            List<AnnotationElement> a2 = y.getAnnotationElements();
            if (a1.size() != a2.size()) {
                throw new RuntimeException("Not same number of annotation ekements on disk as in process");
            }
            for (int i = 0; i < a1.size(); i++) {
                if (!a1.get(i).getTypeName().equals(a2.get(i).getTypeName())) {
                    throw new RuntimeException("Type name of annotation elements in process doesn't match type name on disk");
                }
            }
            return 0;
        });
    }

}
