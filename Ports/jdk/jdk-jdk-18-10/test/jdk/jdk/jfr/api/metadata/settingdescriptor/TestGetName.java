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
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test SettingDescriptor.getName()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.metadata.settingdescriptor.TestGetName
 */
public class TestGetName {

    public static void main(String[] args) throws Exception {
        EventType type = EventType.getEventType(CustomEvent.class);

        // subclass
        Events.getSetting(type, "plain");
        Events.getSetting(type, "annotatedType");
        Events.getSetting(type, "newName");
        Events.getSetting(type, "overridden");
        // base class
        Events.getSetting(type, "overridden");
        Events.getSetting(type, "protectedBase");
        Events.getSetting(type, "publicBase");
        Events.getSetting(type, "packageProtectedBase");

        int defaultNumberOfSettings = 3; // Enabled , Stack Trace, Threshold
        if (type.getSettingDescriptors().size() != 8 + defaultNumberOfSettings) {
            for (SettingDescriptor s : type.getSettingDescriptors()) {
                System.out.println(s.getName());
            }
            throw new Exception("Wrong number of settings");
        }

        CustomEvent.assertOnDisk((x, y) -> x.getName().compareTo(y.getName()));
    }
}
