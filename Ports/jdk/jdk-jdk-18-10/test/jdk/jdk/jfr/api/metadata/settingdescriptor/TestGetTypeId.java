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

import jdk.jfr.EventType;
import jdk.jfr.SettingDescriptor;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test SettingDescriptor.getTypeId()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.metadata.settingdescriptor.TestGetTypeId
 */
public class TestGetTypeId {

    public static void main(String[] args) throws Exception {
        EventType type = EventType.getEventType(CustomEvent.class);

        SettingDescriptor plain = Events.getSetting(type, "plain");
        long plainId = plain.getTypeId();
        Asserts.assertGreaterThan(plainId, 0L);

        SettingDescriptor annotatedType = Events.getSetting(type, "annotatedType");
        long annotatedId = annotatedType.getTypeId();
        Asserts.assertGreaterThan(annotatedId, 0L);

        Asserts.assertNotEquals(annotatedId, plainId);

        SettingDescriptor newName = Events.getSetting(type, "newName");
        Asserts.assertEquals(newName.getTypeId(), annotatedId);

        SettingDescriptor overridden = Events.getSetting(type, "overridden");
        Asserts.assertEquals(overridden.getTypeId(), plainId);

        SettingDescriptor protectedBase = Events.getSetting(type, "protectedBase");
        Asserts.assertEquals(protectedBase.getTypeId(), plainId);

        SettingDescriptor publicBase = Events.getSetting(type, "publicBase");
        Asserts.assertEquals(publicBase.getTypeId(), annotatedId);

        SettingDescriptor packageProtectedBase = Events.getSetting(type, "packageProtectedBase");
        Asserts.assertEquals(packageProtectedBase.getTypeId(), plainId);

        CustomEvent.assertOnDisk((x, y) -> Long.compare(x.getTypeId(), y.getTypeId()));
    }

}
