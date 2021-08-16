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

package jdk.jfr.api.flightrecorder;

import static jdk.test.lib.Asserts.assertTrue;

import java.util.Set;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.SettingControl;
import jdk.jfr.SettingDefinition;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestSettingsControl
 */
public class TestSettingsControl {
    static class MySettingsControl extends SettingControl {

        public static boolean setWasCalled;

        private String value = "default";

        @Override
        public String combine(Set<String> values) {
           StringBuilder sb = new StringBuilder();
            for(String s : values) {
                sb.append(s).append(" ");
            }
            return sb.toString();
        }

        @Override
        public void setValue(String value) {
            setWasCalled = true;
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

    }

    static class MyCustomSettingEvent extends Event {
        @SettingDefinition
        boolean mySetting(MySettingsControl msc) {
            return true;
        }
    }

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();
        r.enable(MyCustomSettingEvent.class).with("mySetting", "myvalue");
        r.start();
        MyCustomSettingEvent e = new MyCustomSettingEvent();
        e.commit();
        r.stop();
        r.close();
        assertTrue(MySettingsControl.setWasCalled, "SettingControl.setValue was not called");
    }
}




