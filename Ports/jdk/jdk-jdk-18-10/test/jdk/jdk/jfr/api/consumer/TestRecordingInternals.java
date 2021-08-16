/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Tests that chunks are read in order and constant pools from multiple chunks can be read
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordingInternals
 */
public class TestRecordingInternals {

    public static void main(String[] args) throws Throwable {
        try (Recording continuous = new Recording()) {
            continuous.start();
            for (int i = 0; i < 3; i++) {
                // Each new recording will create a new chunk
                // with a new set of constant pools, i.e.
                // constant pools for threads and thread groups
                createProfilingRecording(i);
            }
            continuous.stop();
            int i = 0;
            for (RecordedEvent e : Events.fromRecording(continuous)) {
                Integer id = e.getValue("id");
                RecordedThread rt = e.getThread();
                Asserts.assertEquals(id.toString(), rt.getJavaName(), "Thread name should match id");
                Asserts.assertEquals(id.toString(), rt.getThreadGroup().getName(), "Thread group name should match id");
                Asserts.assertEquals(id, Integer.valueOf(i), "Chunks out of order");
                i++;
                System.out.println(i + " OK ");
            }
        }
    }

    private static void createProfilingRecording(int id) throws InterruptedException {
        try (Recording r = new Recording()) {
            r.start();
            ThreadGroup tg = new ThreadGroup(String.valueOf(id));
            Thread t = new Thread(tg, () -> {
                SimpleEvent event = new SimpleEvent();
                event.id = id;
                event.commit();
            }, String.valueOf(id));
            t.start();
            t.join();
            r.stop();
        }
    }
}
