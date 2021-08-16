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
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @summary Purpose of this test is to run leak profiler without command line tweaks or WhiteBox hacks until we succeed
 * @run main/othervm jdk.jfr.event.oldobject.TestSanityDefault
 */
public class TestSanityDefault {

    static private class FindMe {
    }

    public static List<FindMe> list = new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        // Should not use WhiteBox API, we want to execute actual code paths

        // Trigger c2 compilation, so we get sample
        for (long i = 0; i < 100_000_000; i++) {
            allocateFindMe(true);
        }


        // It's hard to get samples with interpreter / C1 so loop until we do
        while (true) {
            try (Recording r = new Recording()) {
                r.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
                r.start();
                allocateFindMe(false);
                System.gc();
                r.stop();
                List<RecordedEvent> events = Events.fromRecording(r);
                if (OldObjects.countMatchingEvents(events, FindMe.class, null, null, -1, "allocateFindMe") > 0) {
                    return;
                }
                // no events found, retry
            }
        }
    }

    public static void allocateFindMe(boolean doNothing) {
        if (doNothing) {
            return;
        }
        for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
            // Purposely don't allocate array, so we at least
            // in one old-object test check an ordinary object.
            list.add(new FindMe());
        }
    }
}
