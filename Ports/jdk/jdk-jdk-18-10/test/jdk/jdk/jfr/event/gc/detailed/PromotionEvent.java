/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.detailed;

import static jdk.test.lib.Asserts.assertEquals;
import static jdk.test.lib.Asserts.assertNotEquals;
import static jdk.test.lib.Asserts.assertNotNull;
import static jdk.test.lib.Asserts.assertTrue;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * This is a base class for testing Promotion Events
 *
 * See TestPromotionEventWith* for actual test classes. Tests must set
 * -XX:MaxTenuringThreshold=5 -XX:InitialTenuringThreshold=5
 *
 * @author Staffan Friberg
 */
public class PromotionEvent {

    private final static String PROMOTION_IN_NEW_PLAB_NAME = EventNames.PromoteObjectInNewPLAB;
    private final static String PROMOTION_OUTSIDE_PLAB_NAME = EventNames.PromoteObjectOutsidePLAB;

    // This value needs to match the command line option set above
    private final static int MAX_TENURING_THRESHOLD = 5;

    // Keep track of the collection count just before and after JFR recording
    private static int startGCCount = 0;

    // Dummy objects to keep things alive and assure allocation happens
    public static Object dummy;
    public static Object[] keepAlive = new Object[128];
    public static Object[] age = new Object[128];

    public static void test() throws Exception {
        GarbageCollectorMXBean ycBean = null;

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if ("PS Scavenge".equals(gcBean.getName())
                    || "G1 Young Generation".equals(gcBean.getName())) {
                ycBean = gcBean;
            }

            if (ycBean != null) {
                break;
            }
        }

        if (ycBean == null) {
            assertNotNull(ycBean, "Test failed since the MXBean for the Young Collector could not be found.");
            return; // To remove IDE warning
        }

        System.gc(); // Clear nusery before recording

        // Get total GC count before recording
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            startGCCount += gcBean.getCollectionCount();
        }

        Recording recording = new Recording();
        recording.enable(PROMOTION_IN_NEW_PLAB_NAME);
        recording.enable(PROMOTION_OUTSIDE_PLAB_NAME);
        recording.start();

        byte[] largeBytes = new byte[1024 * 10];
        byte[] smallBytes = new byte[64];

        // Some large strings to keep alive for tenuring
        for (int i = 0; i < keepAlive.length / 2; i++) {
            ThreadLocalRandom.current().nextBytes(largeBytes);
            keepAlive[i] = new String(largeBytes);
        }

        // Some small strings to keep alive for tenuring
        for (int i = keepAlive.length / 2; i < keepAlive.length; i++) {
            ThreadLocalRandom.current().nextBytes(smallBytes);
            keepAlive[i] = new String(smallBytes);
        }

        // Allocate temp data to force GCs until we have promoted the live data
        for (int gcCount = 0; gcCount < MAX_TENURING_THRESHOLD * 2; gcCount++) {
            long currentGCCount = ycBean.getCollectionCount();

            // some large strings to age
            for (int i = 0; i < age.length / 2; i++) {
                ThreadLocalRandom.current().nextBytes(largeBytes);
                age[i] = new String(largeBytes);
            }

            // Some small strings to age
            for (int i = age.length / 2; i < age.length; i++) {
                ThreadLocalRandom.current().nextBytes(smallBytes);
                age[i] = new String(smallBytes);
            }

            while (ycBean.getCollectionCount() <= currentGCCount + 3) {
                ThreadLocalRandom.current().nextBytes(smallBytes);
                dummy = new String(smallBytes);
            }
        }

        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);

        verifyPromotionSampleEvents(events);

        recording.close();
    }

    private static void verifyPromotionSampleEvents(List<RecordedEvent> events)
            throws Exception {

        boolean objectWasPromotedInNewPLAB = false;
        boolean objectPromotedInNewPLABWasAged = false;
        boolean objectPromotedInNewPLABWasTenured = false;
        boolean objectWasPromotedOutsidePLAB = false;
        boolean objectPromotedOutsidePLABWasAged = false;
        boolean objectPromotedOutsidePLABWasTenured = false;

        Events.hasEvents(events);

        for (RecordedEvent event : events) {
            // Read all common fields
            Events.assertField(event, "gcId").atLeast(startGCCount).getValue();
            String className = (event.getEventType()).getName().toString();
            Events.assertField(event, "tenuringAge").atLeast(0).atMost(MAX_TENURING_THRESHOLD).getValue();
            Boolean tenured = Events.assertField(event, "tenured").getValue();
            Long objectSize = Events.assertField(event, "objectSize").above(0L).getValue();

            // Verify Class Name
            assertNotNull(className, "Class name is null. Event: " + event);
            assertNotEquals(className.length(), 0, "Class name is of zero length. Event: " + event);

            // Verify PLAB size and direct allocation
            if (PROMOTION_IN_NEW_PLAB_NAME.equals(event.getEventType().getName())) {
                // Read event specific fields
                Long plabSize = Events.assertField(event, "plabSize").above(0L).getValue();
                assertTrue(plabSize >= objectSize, "PLAB size is smaller than object size. Event: " + event);
                objectWasPromotedInNewPLAB = true;
                // Verify tenured is hard to do as objects might be tenured earlier than the max threshold
                // but at least verify that we got the field set at least once during the test
                if (tenured) {
                    objectPromotedInNewPLABWasTenured = true;
                } else {
                    objectPromotedInNewPLABWasAged = true;
                }
            } else if (PROMOTION_OUTSIDE_PLAB_NAME.equals(event.getEventType().getName())) {
                objectWasPromotedOutsidePLAB = true;
                // Verify tenured is hard to do as objects might be tenured earlier than the max threshold
                // but at least verify that we got the field set at least once during the test
                if (tenured) {
                    objectPromotedOutsidePLABWasTenured = true;
                } else {
                    objectPromotedOutsidePLABWasAged = true;
                }
            } else {
                assertEquals(event.getEventType().getName(), "Unreachable...", "Got wrong type of event " + event);
            }

        }

        // Verify that at least one event of these types occured during test
        assertTrue(objectWasPromotedInNewPLAB, "No object in new plab was promoted in test");
        assertTrue(objectPromotedInNewPLABWasAged, "No object in new plab was aged in test");
        assertTrue(objectPromotedInNewPLABWasTenured, "No object in new plab was tenured in test");
        assertTrue(objectWasPromotedOutsidePLAB, "No object outside plab was promoted in test");
        assertTrue(objectPromotedOutsidePLABWasAged, "No object outside plab was aged in test");
        assertTrue(objectPromotedOutsidePLABWasTenured, "No object outside plab was tenured in test");
    }
}
