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

package jdk.jfr.api.recording.event;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @summary Enable, disable, enable event during recording.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestReEnableClass
 */
public class TestReEnableClass {

    public static void main(String[] args) throws Throwable {
        test(false);
        test(true);
    }

    // Loop and enable/disable events.
    // Verify recording only contains event created during enabled.
    private static void test(boolean isEnabled) throws Exception {
        System.out.println("Start with isEnabled = " + isEnabled);

        List<Integer> expectedIds = new ArrayList<>();
        Recording r = new Recording();
        SimpleEventHelper.enable(r, isEnabled);
        r.start();

        for (int i = 0; i < 10; ++i) {
            SimpleEventHelper.createEvent(i);
            if (isEnabled) {
                expectedIds.add(i);
            }
            isEnabled = !isEnabled;
            SimpleEventHelper.enable(r, isEnabled);
        }

        r.stop();
        SimpleEventHelper.createEvent(100);

        int[] ids = new int[expectedIds.size()];
        for (int i = 0; i < expectedIds.size(); ++i) {
            ids[i] = expectedIds.get(i);
        }
        SimpleEventHelper.verifyEvents(r, ids);

        r.close();
    }

}
