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

package jdk.jfr.event.oldobject;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestListenerLeak
 */
public class TestListenerLeak {

    private interface TestListener {
        void onListen();
    }

    static class Stuff {
    }

    static class ListenerThread extends Thread {

        private List<Stuff[]> stuff;

        public ListenerThread(List<Stuff[]> stuff) {
            this.stuff = stuff;
        }

        public void run() {
            listener.add(new TestListener() {
                @Override
                public void onListen() {
                    System.out.println(stuff);
                }
            });
        }
    }

    private static List<TestListener> listener = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording r = new Recording()) {
            r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
            r.start();
            listenerLeak();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            if (OldObjects.countMatchingEvents(events, Stuff[].class, null, null, -1, "listenerLeak") == 0) {
                throw new Exception("Could not find leak with " + Stuff[].class);
            }
        }
    }

    private static void listenerLeak() throws InterruptedException {
        List<Stuff[]> stuff = new ArrayList<>(OldObjects.MIN_SIZE);
        for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
            // Allocate array to trigger sampling code path for interpreter / c1
            stuff.add(new Stuff[0]);
        }

        ListenerThread t = new ListenerThread(stuff);
        t.start();
        t.join();
    }

}
