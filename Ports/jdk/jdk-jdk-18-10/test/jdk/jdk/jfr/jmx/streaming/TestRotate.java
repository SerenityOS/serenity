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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Tests that streaming can work over chunk rotations
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -Xlog:jfr=debug jdk.jfr.jmx.streaming.TestRotate
 */
public class TestRotate {

    @StackTrace(false)
    static class TestRotateEvent extends Event {
        int value;
    }

    public static void main(String... args) throws Exception {
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        Path p = Files.createDirectory(Paths.get("test-stream-rotate-" + System.currentTimeMillis()));
        CountDownLatch latch = new CountDownLatch(100);
        try (RemoteRecordingStream r = new RemoteRecordingStream(conn, p)) {
            r.onEvent(e -> {
                System.out.println(e);
                latch.countDown();
            });
            r.startAsync();
            for (int i = 1; i <= 100; i++) {
                TestRotateEvent e = new TestRotateEvent();
                e.value = i;
                e.commit();
                if (i % 30 == 0) {
                    rotate();
                }
                Thread.sleep(10);
            }
            System.out.println("Events generated. Awaiting consumption");
            latch.await();
        }
    }

    private static void rotate() {
        try (Recording r = new Recording()) {
            r.start();
        }
    }
}
