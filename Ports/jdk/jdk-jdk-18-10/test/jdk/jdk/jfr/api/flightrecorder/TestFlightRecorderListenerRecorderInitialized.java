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

package jdk.jfr.api.flightrecorder;
import static jdk.test.lib.Asserts.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestFlightRecorderListenerRecorderInitialized
 */
public class TestFlightRecorderListenerRecorderInitialized {

    /**
     * Utility class to wait/notify
     */
    private static class Signal {

        private static volatile boolean signalled;
        private static final Lock lock = new ReentrantLock();
        private static final Condition cond = lock.newCondition();

        private static void waitFor(long timeout, TimeUnit timeUnit) throws InterruptedException {
            try {
                lock.lock();
                if (!signalled) {
                    log("Waiting for FlightRecorder.recorderInitialized notification...");
                    cond.await(timeout, timeUnit);
                }
            } finally {
                lock.unlock();
            }
        }

        private static void signal() {
            try {
                lock.lock();
                signalled = true;
                cond.signalAll();
            } finally {
                lock.unlock();
            }
        }
    };

    public static void main(String[] args) throws Throwable {
        FlightRecorder.addListener(new FlightRecorderListener() {

            @Override
            public void recorderInitialized(FlightRecorder recorder) {
                log("Recorder initialized");
                Signal.signal();
            }

        });
        FlightRecorder.getFlightRecorder();

        Signal.waitFor(3, TimeUnit.SECONDS);

        assertTrue(Signal.signalled, "FlightRecorderListener.recorderInitialized has not been called");

    }

    private static void log(String s) {
        System.out.println(s);
    }
}
