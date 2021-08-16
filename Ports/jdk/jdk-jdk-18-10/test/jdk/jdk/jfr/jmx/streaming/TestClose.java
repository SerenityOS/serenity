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

import javax.management.MBeanServerConnection;

import jdk.jfr.Event;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Tests that a RemoteRecordingStream can be closed
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestClose
 */
public class TestClose {

    static class TestCloseEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        Path p = Files.createDirectory(Paths.get("test-close-" + System.currentTimeMillis()));

        RemoteRecordingStream e = new RemoteRecordingStream(conn, p);
        e.startAsync();
        // Produce enough to generate multiple chunks
        for (int i = 0; i < 200_000; i++) {
            TestCloseEvent event = new TestCloseEvent();
            event.commit();
        }
        e.onFlush(() -> {
            e.close(); // <- should clean up files.
        });
        e.awaitTermination();
        int count = 0;
        for (Object path : Files.list(p).toArray()) {
            System.out.println(path);
            count++;
        }
        if (count > 0) {
            throw new Exception("Expected repository to be empty");
        }
    }
}
