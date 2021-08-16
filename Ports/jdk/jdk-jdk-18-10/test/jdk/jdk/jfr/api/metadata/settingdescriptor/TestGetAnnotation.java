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

import jdk.jfr.Description;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.SettingDescriptor;
import jdk.jfr.Timespan;
import jdk.jfr.Timestamp;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test SettingDescriptor.getAnnotation();
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.metadata.settingdescriptor.TestGetAnnotation
 */
public class TestGetAnnotation {

    public static void main(String[] args) throws Exception {
        EventType type = EventType.getEventType(CustomEvent.class);

        SettingDescriptor annotatedType = Events.getSetting(type, "annotatedType");
        Label al = annotatedType.getAnnotation(Label.class);
        Asserts.assertNull(al); // we should not inherit annotation from type

        Description ad = annotatedType.getAnnotation(Description.class);
        Asserts.assertNull(ad); // we should not inherit annotation from type

        Timestamp at = annotatedType.getAnnotation(Timestamp.class);
        Asserts.assertNull(at); // we should not inherit annotation from type

        SettingDescriptor newName = Events.getSetting(type, "newName");
        Label nl = newName.getAnnotation(Label.class);
        Asserts.assertEquals(nl.value(), "Annotated Method");

        Description nd = newName.getAnnotation(Description.class);
        Asserts.assertEquals(nd.value(), "Description of an annotated method");

        Timespan nt = newName.getAnnotation(Timespan.class);
        Asserts.assertEquals(nt.value(), Timespan.NANOSECONDS);
    }

}
