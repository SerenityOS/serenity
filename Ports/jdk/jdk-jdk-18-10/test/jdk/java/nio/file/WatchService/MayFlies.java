/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 7164570 7191467
 * @summary Test that CREATE and DELETE events are paired for very
 *     short lived files
 * @library ..
 * @run main MayFlies
 * @key randomness
 */

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.*;
import java.util.concurrent.*;

public class MayFlies {

    static volatile boolean stopped;

    static volatile boolean failure;

    /**
     * Continuously creates short-lived files in a directory until {@code
     * stopped} is set to {@code true}.
     */
    static class MayFlyHatcher implements Runnable {
        static final Random rand = new Random();

        private final Path dir;
        private final String prefix;

        private MayFlyHatcher(Path dir, String prefix) {
            this.dir = dir;
            this.prefix = prefix;
        }

        static void start(Path dir, String prefix) {
            MayFlyHatcher hatcher = new MayFlyHatcher(dir, prefix);
            new Thread(hatcher).start();
        }

        public void run() {
            try {
                int n = 0;
                while (!stopped) {
                    Path name = dir.resolve(prefix + (++n));
                    Files.createFile(name);
                    if (rand.nextBoolean())
                        Thread.sleep(rand.nextInt(500));
                    Files.delete(name);
                    Thread.sleep(rand.nextInt(100));
                }
                System.out.format("%d %ss hatched%n", n, prefix);
            } catch (Exception x) {
                failure = true;
                x.printStackTrace();
            }
        }
    }

    /**
     * Test phases.
     */
    static enum Phase {
        /**
         * Short-lived files are being created
         */
        RUNNING,
        /**
         * Draining the final events
         */
        FINISHING,
        /**
         * No more events or overflow detected
         */
        FINISHED
    };


    public static void main(String[] args) throws Exception {

        // schedules file creation to stop after 10 seconds
        ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();
        pool.schedule(
            new Runnable() { public void run() { stopped = true; }},
            10, TimeUnit.SECONDS);

        Path dir = TestUtil.createTemporaryDirectory();

        Set<Path> entries = new HashSet<>();
        int nCreateEvents = 0;
        boolean overflow = false;

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);

            // start hatching Mayflies
            MayFlyHatcher.start(dir, "clinger");
            MayFlyHatcher.start(dir, "crawler");
            MayFlyHatcher.start(dir, "burrower");
            MayFlyHatcher.start(dir, "swimmer");

            Phase phase = Phase.RUNNING;
            while (phase != Phase.FINISHED) {
                // during the running phase then poll for 1 second.
                // once the file creation has stopped then move to the finishing
                // phase where we do a long poll to ensure that all events have
                // been read.
                int time = (phase == Phase.RUNNING) ? 1 : 15;
                key = watcher.poll(time, TimeUnit.SECONDS);
                if (key == null) {
                    if (phase == Phase.RUNNING && stopped)
                        phase = Phase.FINISHING;
                    else if (phase == Phase.FINISHING)
                        phase = Phase.FINISHED;
                } else {
                    // process events
                    for (WatchEvent<?> event: key.pollEvents()) {
                        if (event.kind() == ENTRY_CREATE) {
                            Path name = (Path)event.context();
                            boolean added = entries.add(name);
                            if (!added)
                                throw new RuntimeException("Duplicate ENTRY_CREATE event");
                            nCreateEvents++;
                        } else if (event.kind() == ENTRY_DELETE) {
                            Path name = (Path)event.context();
                            boolean removed = entries.remove(name);
                            if (!removed)
                                throw new RuntimeException("ENTRY_DELETE event without ENTRY_CREATE event");
                        } else if (event.kind() == OVERFLOW) {
                            overflow = true;
                            phase = Phase.FINISHED;
                        } else {
                            throw new RuntimeException("Unexpected event: " + event.kind());
                        }
                    }
                    key.reset();
                }
            }

            System.out.format("%d ENTRY_CREATE events read%n", nCreateEvents);

            // there should be a DELETE event for each CREATE event and so the
            // entries set should be empty.
            if (!overflow && !entries.isEmpty())
                throw new RuntimeException("Missed " + entries.size() + " DELETE event(s)");


        } finally {
            try {
                TestUtil.removeAll(dir);
            } finally {
                pool.shutdown();
            }
        }

        if (failure)
            throw new RuntimeException("Test failed - see log file for details");
    }
}
