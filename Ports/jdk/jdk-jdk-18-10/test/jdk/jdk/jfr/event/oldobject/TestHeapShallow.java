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
import jdk.jfr.internal.test.WhiteBox;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal.test
 * @run main/othervm -XX:TLABSize=2k jdk.jfr.event.oldobject.TestHeapShallow
 */
public class TestHeapShallow {
    private final static long LARGE_OBJECT_FACTOR = 509; // prime number

    static class LeakObject {
        Object object = null;

        public LeakObject(Object object) {
            this.object = object;
        }
    }

    public static ArrayList<LeakObject> leak =  new ArrayList<>(OldObjects.MIN_SIZE);

    public static void main(String[] args) throws Exception {
        WhiteBox.setWriteAllObjectSamples(true);

        try (Recording recording = new Recording()) {
            recording.enable(EventNames.OldObjectSample).withStackTrace().with("cutoff", "infinity");
            recording.start();

            addObjectsToShallowArray(leak);
            recording.stop();
            if (OldObjects.countMatchingEvents(recording, "addObjectsToShallowArray", byte[].class, "object", LeakObject.class, -1) < 1) {
                throw new Exception("Could not find shallow leak object");
            }
        }
    }

    private static void addObjectsToShallowArray(List<LeakObject> list) {
        for (int i = 0; i < OldObjects.MIN_SIZE; i++) {
            if (i % LARGE_OBJECT_FACTOR == 0) {
                // Triggers allocation outside TLAB path
                list.add(new LeakObject(new byte[16384]));
            } else {
                // Triggers allocation in TLAB path
                list.add(new LeakObject(new byte[10]));
            }
        }
    }
}
