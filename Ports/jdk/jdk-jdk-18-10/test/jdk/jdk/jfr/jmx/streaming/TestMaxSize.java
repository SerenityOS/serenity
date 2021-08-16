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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerConnection;

import jdk.jfr.Event;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Tests that max size can be set for a RemoteRecordingStream
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestMaxSize
 */
public class TestMaxSize {

    static class Monkey extends Event {
    }

    public static void main(String... args) throws Exception {
        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        Path dir = Files.createDirectories(Paths.get("max-size-" + System.currentTimeMillis()));
        System.out.println(dir);
        AtomicBoolean finished = new AtomicBoolean();
        try (RemoteRecordingStream e = new RemoteRecordingStream(conn, dir)) {
            e.startAsync();
            e.onEvent(ev -> {
                if (finished.get()) {
                    return;
                }
                // Consume some events, but give back control
                // to stream so it can be closed.
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    // ignore
                }
            });
            while (directorySize(dir) < 50_000_000) {
                emitEvents(500_000);
            }
            e.setMaxSize(1_000_000);
            long count = fileCount(dir);
            if (count > 2) {
                // Two chunks can happen when header of new chunk is written and previous
                // chunk is not finalized.
                throw new Exception("Expected only one or two chunks with setMaxSize(1_000_000). Found " + count);
            }
            finished.set(true);
        }
    }

    private static void emitEvents(int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            Monkey m = new Monkey();
            m.commit();
        }
        System.out.println("Emitted " + count + " events");
        Thread.sleep(1000);
    }

    private static int fileCount(Path dir) throws IOException {
        System.out.println("Files:");
        AtomicInteger count = new AtomicInteger();
        Files.list(dir).forEach(p -> {
            System.out.println(p);
            count.incrementAndGet();
        });
        return count.get();
    }

    private static long directorySize(Path dir) throws IOException {
        long p = Files.list(dir).mapToLong(f -> {
            try {
                return Files.size(f);
            } catch (IOException e) {
                return 0;
            }
        }).sum();
        System.out.println("Directory size: " + p);
        return p;
    }
}
