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
 * @requires vm.gc.G1
 * @summary Test leak profiler with G1 GC
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm  -XX:TLABSize=2k -XX:+UseG1GC jdk.jfr.event.oldobject.TestG1
 */
public class TestG1 {

    static private class FindMe {
    }

    public static List<FindMe[]> list = new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        while (true) {
            try (Recording r = new Recording()) {
                r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
                r.start();
                allocateFindMe();
                System.gc();
                r.stop();
                List<RecordedEvent> events = Events.fromRecording(r);
                System.out.println(events);
                if (OldObjects.countMatchingEvents(events, FindMe[].class, null, null, -1, "allocateFindMe") > 0) {
                    return;
                }
                System.out.println("Could not find leaking object, retrying...");
            }
            list.clear();
        }
    }

    public static void allocateFindMe() {
        for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
            // Allocate array to trigger sampling code path for interpreter / c1
            list.add(new FindMe[0]);
        }
    }

}
