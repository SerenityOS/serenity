/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx.streaming;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Tests that a RemoteRecordingStream can stream over multiple chunks
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestMultipleChunks
 */
public class TestMultipleChunks {

    static class Snake extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch latch = new CountDownLatch(5);
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (RemoteRecordingStream s = new RemoteRecordingStream(conn)) {
            s.onEvent(e -> latch.countDown());
            s.startAsync();
            for (int i = 0; i < 5; i++) {
                Snake snake = new Snake();
                snake.commit();
                rotate();
            }
        }
    }

    private static void rotate() {
        try (Recording r = new Recording()) {
            r.start();
        }
    }
}
