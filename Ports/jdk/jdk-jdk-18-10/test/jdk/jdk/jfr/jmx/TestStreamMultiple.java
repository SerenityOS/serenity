/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx;

import java.util.List;

import jdk.jfr.consumer.RecordedEvent;
import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.SimpleEventHelper;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestStreamMultiple
 */
public class TestStreamMultiple {

    public static void main(String[] args) throws Exception {
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();

        SimpleEventHelper.createEvent(0); // No recordings

        long recIdA = bean.newRecording();

        bean.startRecording(recIdA);
        SimpleEventHelper.createEvent(1); // recA

        long recIdB = bean.newRecording();
        Asserts.assertNotEquals(recIdA, recIdB, "Recording Ids should be unique");
        bean.startRecording(recIdB);
        SimpleEventHelper.createEvent(2); // recA and recB

        bean.stopRecording(recIdA);
        SimpleEventHelper.createEvent(3); // recB

        bean.stopRecording(recIdB);
        SimpleEventHelper.createEvent(4); // No recordings

        // Check recA
        long streamIdA = bean.openStream(recIdA, null);
        List<RecordedEvent> events = JmxHelper.parseStream(streamIdA, bean);
        SimpleEventHelper.verifyContains(events, 1, 2);
        SimpleEventHelper.verifyNotContains(events, 0, 3, 4);
        bean.closeStream(streamIdA);
        // check recB
        long streamIdB = bean.openStream(recIdB, null);
        events = JmxHelper.parseStream(streamIdB, bean);
        SimpleEventHelper.verifyContains(events, 2, 3);
        SimpleEventHelper.verifyNotContains(events, 0, 1, 4);
        bean.closeStream(streamIdB);

        bean.closeRecording(recIdA);
        bean.closeRecording(recIdB);
    }
}
