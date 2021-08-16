/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6907760 6929532
 * @summary Tests WatchService behavior when lots of events are pending (use -Dseed=X to set PRNG seed)
 * @library ..
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/timeout=180 LotsOfEvents
 * @key randomness
 */

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.RandomFactory;

public class LotsOfEvents {

    private static final Random RAND = RandomFactory.getRandom();

    public static void main(String[] args) throws Exception {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            testOverflowEvent(dir);
            testModifyEventsQueuing(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }

    /**
     * Tests that OVERFLOW events are not retreived with other events.
     */
    static void testOverflowEvent(Path dir)
        throws IOException, InterruptedException
    {
        try (WatchService watcher = dir.getFileSystem().newWatchService()) {
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);

            // create a lot of files
            int n = 1024;
            Path[] files = new Path[n];
            for (int i=0; i<n; i++) {
                files[i] = Files.createFile(dir.resolve("foo" + i));
            }

            // give time for events to accumulate (improve chance of overflow)
            Thread.sleep(1000);

            // check that we see the create events (or overflow)
            drainAndCheckOverflowEvents(dir, watcher, ENTRY_CREATE, n);

            // delete the files
            for (int i=0; i<n; i++) {
                Files.delete(files[i]);
            }

            // give time for events to accumulate (improve chance of overflow)
            Thread.sleep(1000);

            // check that we see the delete events (or overflow)
            drainAndCheckOverflowEvents(dir, watcher, ENTRY_DELETE, n);
        }
    }

    static void drainAndCheckOverflowEvents(Path dir,
                                            WatchService watcher,
                                            WatchEvent.Kind<?> expectedKind,
                                            int count)
        throws IOException, InterruptedException
    {
        // wait for key to be signalled - the timeout is long to allow for
        // polling implementations
        WatchKey key = watcher.poll(15, TimeUnit.SECONDS);
        if (key != null && count == 0)
            throw new RuntimeException("Key was signalled (unexpected)");
        if (key == null && count > 0)
            throw new RuntimeException("Key not signalled (unexpected)");

        int nread = 0;
        boolean gotOverflow = false;
        while (key != null) {
            List<WatchEvent<?>> events = key.pollEvents();
            for (WatchEvent<?> event: events) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == expectedKind) {
                    // expected event kind
                    if (++nread > count)
                        throw new RuntimeException("More events than expected!!");
                } else if (kind == OVERFLOW) {
                    // overflow event should not be retrieved with other events
                    if (events.size() > 1)
                        throw new RuntimeException("Overflow retrieved with other events");
                    gotOverflow = true;
                } else {
                    throw new RuntimeException("Unexpected event '" + kind + "'");
                }
            }
            if (!key.reset())
                throw new RuntimeException("Key is no longer valid");
            key = watcher.poll(2, TimeUnit.SECONDS);
        }

        // check that all expected events were received or there was an overflow
        if (nread < count && !gotOverflow) {
            System.err.printf("Test directory %s contains %d files%n",
                dir, Files.list(dir).count());

            long timeBeforePoll = System.nanoTime();
            key = watcher.poll(15, TimeUnit.SECONDS);
            long timeAfterPoll = System.nanoTime();
            if (key == null) {
                System.err.println("key still null after extra polling");
            } else {
                List<WatchEvent<?>> events = key.pollEvents();
                System.err.printf("Retrieved key with %d events after %d ns%n",
                    events.size(), timeAfterPoll - timeBeforePoll);
            }

            throw new RuntimeException("Insufficient "
                + expectedKind.name() + "  events: expected "
                + count + ", received " + nread);
        }
    }

    /**
     * Tests that check that ENTRY_MODIFY events are queued efficiently
     */
    static void testModifyEventsQueuing(Path dir)
        throws IOException, InterruptedException
    {
        // this test uses a random number of files
        final int nfiles = 5 + RAND.nextInt(10);
        DirectoryEntry[] entries = new DirectoryEntry[nfiles];
        for (int i=0; i<nfiles; i++) {
            entries[i] = new DirectoryEntry(dir.resolve("foo" + i));

            // "some" of the files exist, some do not.
            entries[i].deleteIfExists();
            if (RAND.nextBoolean())
                entries[i].create();
        }

        try (WatchService watcher = dir.getFileSystem().newWatchService()) {
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            // do several rounds of noise and test
            for (int round=0; round<10; round++) {

                // make some noise!!!
                for (int i=0; i<100; i++) {
                    DirectoryEntry entry = entries[RAND.nextInt(nfiles)];
                    int action = RAND.nextInt(10);
                    switch (action) {
                        case 0 : entry.create(); break;
                        case 1 : entry.deleteIfExists(); break;
                        default: entry.modifyIfExists();
                    }
                }

                // process events and ensure that we don't get repeated modify
                // events for the same file.
                WatchKey key = watcher.poll(15, TimeUnit.SECONDS);
                while (key != null) {
                    Set<Path> modified = new HashSet<>();
                    for (WatchEvent<?> event: key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path file = (kind == OVERFLOW) ? null : (Path)event.context();
                        if (kind == ENTRY_MODIFY) {
                            boolean added = modified.add(file);
                            if (!added) {
                                throw new RuntimeException(
                                    "ENTRY_MODIFY events not queued efficiently");
                            }
                        } else {
                            if (file != null) modified.remove(file);
                        }
                    }
                    if (!key.reset())
                        throw new RuntimeException("Key is no longer valid");
                    key = watcher.poll(2, TimeUnit.SECONDS);
                }
            }
        }
    }

    static class DirectoryEntry {
        private final Path file;
        DirectoryEntry(Path file) {
            this.file = file;
        }
        void create() throws IOException {
            if (Files.notExists(file))
                Files.createFile(file);

        }
        void deleteIfExists() throws IOException {
            Files.deleteIfExists(file);
        }
        void modifyIfExists() throws IOException {
            if (Files.exists(file)) {
                try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
                    out.write("message".getBytes());
                }
            }
        }
    }

}
