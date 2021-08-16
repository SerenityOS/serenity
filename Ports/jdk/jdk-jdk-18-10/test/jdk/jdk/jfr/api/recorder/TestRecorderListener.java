/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.recorder;

import java.util.concurrent.CountDownLatch;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;

/**
 * @test TestRecorderListener
 *
 * @key jfr
 * @requires vm.hasJFR
 * @run main/othervm jdk.jfr.api.recorder.TestRecorderListener
 */
public class TestRecorderListener {

    static class Listener implements FlightRecorderListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final RecordingState waitFor;

        public Listener(RecordingState state) {
            waitFor = state;
        }

        @Override
        public void recordingStateChanged(Recording recording) {
            System.out.println("Listener: recording=" + recording.getName() + " state=" + recording.getState());
            RecordingState rs = recording.getState();
            if (rs == waitFor) {
                latch.countDown();
            }
        }

        public void waitFor() throws InterruptedException {
            latch.await();
        }
    }

    public static void main(String... args) throws Exception {
        Listener recordingListener = new Listener(RecordingState.RUNNING);
        FlightRecorder.addListener(recordingListener);

        Listener stoppedListener = new Listener(RecordingState.STOPPED);
        FlightRecorder.addListener(stoppedListener);

        Listener finishedListener = new Listener(RecordingState.CLOSED);
        FlightRecorder.addListener(finishedListener);

        Recording recording = new Recording();
        if (recording.getState() != RecordingState.NEW) {
            recording.close();
            throw new Exception("New recording should be in NEW state");
        }

        recording.start();
        recordingListener.waitFor();

        recording.stop();
        stoppedListener.waitFor();

        recording.close();
        finishedListener.waitFor();

        testDefaultrecordingStateChangedListener();

    }

    private static class DummyListener implements FlightRecorderListener {

    }

    private static void testDefaultrecordingStateChangedListener() {
        FlightRecorder.addListener(new DummyListener());
        Recording recording = new Recording();
        recording.start();
        recording.stop();
        recording.close();
    }
}
