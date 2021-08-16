/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.allocation;

import static java.lang.Math.floor;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test that event is triggered when an object is allocated in a new TLAB.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -XX:+UseTLAB -XX:TLABSize=100k -XX:-ResizeTLAB -XX:TLABRefillWasteFraction=1 jdk.jfr.event.allocation.TestObjectAllocationInNewTLABEvent
 * @run main/othervm -XX:+UseTLAB -XX:TLABSize=100k -XX:-ResizeTLAB -XX:TLABRefillWasteFraction=1 -Xint jdk.jfr.event.allocation.TestObjectAllocationInNewTLABEvent
 */

/**
 * Test that when an object is allocated in a new Thread Local Allocation Buffer (TLAB)
 * an event will be triggered. The test is done for default and interpreted mode (-Xint).
 *
 * To force objects to be allocated in a new TLAB:
 *      the size of TLAB is set to 100k (-XX:TLABSize=100k);
 *      the size of allocated objects is set to 100k minus 16 bytes overhead;
 *      max TLAB waste at refill is set to minimum (-XX:TLABRefillWasteFraction=1),
 *          to provoke a new TLAB creation.
 */
public class TestObjectAllocationInNewTLABEvent {
    private final static String EVENT_NAME = EventNames.ObjectAllocationInNewTLAB;

    private static final int BYTE_ARRAY_OVERHEAD = 16; // Extra bytes used by a byte array.
    private static final int OBJECT_SIZE  = 100 * 1024;
    private static final int OBJECT_SIZE_ALT = OBJECT_SIZE + 8; // Object size in case of disabled CompressedOops.
    private static final int OBJECTS_TO_ALLOCATE = 100;
    private static final String BYTE_ARRAY_CLASS_NAME = new byte[0].getClass().getName();
    private static final int INITIAL_TLAB_SIZE = 100 * 1024;
    private static int countAllTlabs; // Count all matching tlab allocations.
    private static int countFullTlabs; // Count matching tlab allocations with full tlab size.

    // Make sure allocation isn't dead code eliminated.
    public static byte[] tmp;

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        System.gc();
        allocate();
        recording.stop();
        verifyRecording(recording);
        int minCount = (int) floor(OBJECTS_TO_ALLOCATE * 0.80);
        Asserts.assertGreaterThanOrEqual(countAllTlabs, minCount, "Too few tlab objects allocated");
        Asserts.assertLessThanOrEqual(countAllTlabs, OBJECTS_TO_ALLOCATE, "Too many tlab objects allocated");

        // For most GCs we expect the size of each tlab to be
        // INITIAL_TLAB_SIZE + ALLOCATION_SIZE, but that is not always true for G1.
        // G1 may use a smaller tlab size if the full tlab does not fit in the
        // selected memory region.
        //
        // For example, if a G1 memory region has room for 4.7 tlabs,
        // then the first 4 tlabs will have the expected size,
        // but the fifth tlab would only have a size of 0.7*expected.
        //
        // It is only the last tlab in each region that has a smaller size.
        // This means that at least 50% of the allocated tlabs should
        // have the expected size (1 full tlab, and 1 fractional tlab).
        Asserts.assertGreaterThanOrEqual(2*countFullTlabs, countAllTlabs, "Too many fractional tlabs.");
    }

    private static void allocate() {
        for (int i = 0; i < OBJECTS_TO_ALLOCATE; ++i) {
            tmp = new byte[OBJECT_SIZE - BYTE_ARRAY_OVERHEAD];
        }
    }

    private static void verifyRecording(Recording recording) throws Exception {
        for (RecordedEvent event : Events.fromRecording(recording)) {
            verify(event);
        }
    }

    private static void verify(RecordedEvent event) {
        if (Thread.currentThread().getId() != event.getThread().getJavaThreadId()) {
            return;
        }
        long allocationSize = Events.assertField(event, "allocationSize").atLeast(1L).getValue();
        long tlabSize = Events.assertField(event, "tlabSize").atLeast(allocationSize).getValue();
        String className = Events.assertField(event, "objectClass.name").notEmpty().getValue();
        if (className.equals(BYTE_ARRAY_CLASS_NAME) && (allocationSize == OBJECT_SIZE || allocationSize == OBJECT_SIZE_ALT)) {
            countAllTlabs++;
            if (tlabSize == INITIAL_TLAB_SIZE + OBJECT_SIZE || tlabSize == INITIAL_TLAB_SIZE + OBJECT_SIZE_ALT) {
                countFullTlabs++;
            }
        }
    }
}
