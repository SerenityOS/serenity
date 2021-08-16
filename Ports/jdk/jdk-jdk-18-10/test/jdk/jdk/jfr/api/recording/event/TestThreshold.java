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

import java.time.Duration;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test event threshold.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestThreshold
 */
public class TestThreshold {

    public static void main(String[] args) throws Throwable {
        Recording r = new Recording();

        r.start();
        r.enable(MyEvent.class).withThreshold(Duration.ofMillis(500));
        createEvent(0, 100);
        createEvent(1, 600);
        createEvent(2, 100);
        r.stop();

        List<RecordedEvent> events = Events.fromRecording(r);
        Asserts.assertTrue(1 <= events.size(), "Should get at most 1 event");
        r.close();
    }

    private static void createEvent(int id, long duration) throws Exception {
        MyEvent event = new MyEvent(id);
        long start = System.currentTimeMillis();

        event.begin();
        sleepUntil(start + duration);
        event.end();

        long actualDuration = System.currentTimeMillis() - start;
        System.out.printf("targetDuration=%d, actualDuration=%d, shouldCommit=%b%n",
        duration, actualDuration, event.shouldCommit());
        event.commit();
    }

    private static void sleepUntil(long endTime) throws Exception {
        long sleepTime = endTime - System.currentTimeMillis();
        while(sleepTime > 0) {
            Thread.sleep(sleepTime);
            sleepTime = endTime - System.currentTimeMillis();
        }
    }

    static class MyEvent extends Event {
        public int id;
        public MyEvent(int id) {
            this.id = id;
        }
    }

}
