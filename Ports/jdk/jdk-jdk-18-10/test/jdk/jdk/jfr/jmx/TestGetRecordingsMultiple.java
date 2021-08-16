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

import java.util.ArrayList;
import java.util.List;

import jdk.management.jfr.RecordingInfo;
import jdk.test.lib.Asserts;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestGetRecordingsMultiple
 */
public class TestGetRecordingsMultiple {

    private static class TestRecording {
        long id;
        boolean isClosed;
        public TestRecording(long id) {
            this.id = id;
            isClosed = false;
        }
    }

    public static void main(String[] args) throws Throwable {
        List<TestRecording> testRecordings = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            verifyExistingRecordings(testRecordings);
            testRecordings.add(createRecording());
            if (i >= 1) {
                startRecording(testRecordings.get(i-1));
            }
            if (i >= 2) {
                stopRecording(testRecordings.get(i-2));
            }
            if (i >= 3) {
                closeRecording(testRecordings.get(i-3));
            }
        }
        verifyExistingRecordings(testRecordings);

        for (TestRecording r : testRecordings) {
            if (!r.isClosed) {
                closeRecording(r);
            }
        }
        verifyExistingRecordings(testRecordings);
    }

    // Verify that all active recordings are found, but no closed recordings.
    private static void verifyExistingRecordings(List<TestRecording> testRecordings) {
        for (TestRecording testRecording : testRecordings) {
            RecordingInfo r = findRecording(testRecording);
            if (r != null) {
                Asserts.assertFalse(testRecording.isClosed, "Found closed recording with id " + testRecording.id);
                System.out.printf("Recording %d: %s%n", r.getId(), r.getState());
            } else {
                Asserts.assertTrue(testRecording.isClosed, "Missing recording with id " + testRecording.id);
                System.out.printf("Recording %d: CLOSED%n", testRecording.id);
            }
        }
    }

    private static RecordingInfo findRecording(TestRecording testRecording) {
        for (RecordingInfo r : JmxHelper.getFlighteRecorderMXBean().getRecordings()) {
            if (r.getId() == testRecording.id) {
                return r;
            }
        }
        return null;
    }

    private static TestRecording createRecording() {
        long id = JmxHelper.getFlighteRecorderMXBean().newRecording();
        System.out.println("created recording " + id);
        return new TestRecording(id);
    }

    private static void startRecording(TestRecording rec) {
        System.out.println("starting recording " + rec.id);
        JmxHelper.getFlighteRecorderMXBean().startRecording(rec.id);
    }

    private static void stopRecording(TestRecording rec) {
        System.out.println("stopping recording " + rec.id);
        JmxHelper.getFlighteRecorderMXBean().stopRecording(rec.id);
    }

    private static void closeRecording(TestRecording rec) throws Exception {
        System.out.println("closing recording " + rec.id);
        JmxHelper.getFlighteRecorderMXBean().closeRecording(rec.id);
        rec.isClosed = true;

    }
}
