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
package jdk.jfr.api.consumer;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThreadGroup;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Tests getParent method in RecordedThreadGroup
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedThreadGroupParent
 */
public class TestRecordedThreadGroupParent {

    public static void main(String[] args) throws Exception {
        ThreadGroup beforeStartGroup = new ThreadGroup(new ThreadGroup(new ThreadGroup("Grandfather"), "Father"), "Son");
        Thread beforeThread = new Thread(beforeStartGroup, () -> new SimpleEvent().commit(), "Before Recording Start");

        try (Recording r = new Recording()) {
            r.enable(SimpleEvent.class).withoutStackTrace();
            r.start();
            beforeThread.start();
            ThreadGroup afterStartGroup = new ThreadGroup(new ThreadGroup(new ThreadGroup("Grandmother"), "Mother"), "Daughter");
            Thread afterThread = new Thread(afterStartGroup, () -> new SimpleEvent().commit(), "After Recording Start");
            afterThread.start();

            beforeThread.join();
            afterThread.join();

            r.stop();

            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            for (RecordedEvent e : events) {
                System.out.println(e);
                switch (e.getThread().getJavaName()) {
                case "Before Recording Start":
                    assetrThreadGroupParents(beforeStartGroup, e.getThread().getThreadGroup());
                    break;
                case "After Recording Start":
                    assetrThreadGroupParents(afterStartGroup, e.getThread().getThreadGroup());
                    break;
                default:
                    Asserts.fail("Unexpected thread found "  + e.getThread().getJavaName());
                }
            }
        }
    }

    private static void assetrThreadGroupParents(ThreadGroup realGroup, RecordedThreadGroup recordedGroup) {
        if (recordedGroup == null && realGroup == null) {
            return; // root
        }
        Asserts.assertNotNull(recordedGroup, "Parent thread group should not be null");
        Asserts.assertEquals(realGroup.getName(), recordedGroup.getName(), "Parent thread group names don't match");
        assetrThreadGroupParents(realGroup.getParent(), recordedGroup.getParent());
    }
}
