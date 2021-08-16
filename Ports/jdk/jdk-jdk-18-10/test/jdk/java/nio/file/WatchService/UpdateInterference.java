/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8145981
 * @summary LinuxWatchService sometimes reports inotify events against wrong directory
 * @run main UpdateInterference
 */
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static java.nio.file.StandardWatchEventKinds.*;

public class UpdateInterference {

    private static volatile boolean stop;

    public static void main(String[] args) throws IOException, InterruptedException {
        final Path root = Files.createTempDirectory("test");
        final Path foo = root.resolve("foo");
        final Path bar = root.resolve("bar");
        final Path baz = root.resolve("baz");

        Files.createDirectory(foo);
        Files.createDirectory(bar);
        Files.createDirectory(baz);

        try (final WatchService watcher = root.getFileSystem().newWatchService()) {
            final WatchKey fooKey = foo.register(watcher, ENTRY_CREATE);
            final WatchKey barKey = bar.register(watcher, ENTRY_CREATE);

            Thread t1 = null;
            Thread t2 = null;
            try {
                t1 = new Thread() {

                    @Override
                    public void run() {
                        while (!stop) {
                            try {
                                final Path temp = Files.createTempFile(foo, "temp", ".tmp");
                                Files.delete(temp);
                                Thread.sleep(10);
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };

                t2 = new Thread() {

                    @Override
                    public void run() {
                        WatchKey bazKeys[] = new WatchKey[32];
                        while (!stop) {
                            try {
                                for( int i = 0; i < bazKeys.length; i++) {
                                    bazKeys[i] = baz.register(watcher, ENTRY_CREATE);
                                }
                                for( int i = 0; i < bazKeys.length; i++) {
                                    bazKeys[i].cancel();
                                }
                                Thread.sleep(1);
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };

                t1.start();
                t2.start();

                long time = System.currentTimeMillis();
                while ((System.currentTimeMillis() - time) < 15000) {
                    final WatchKey key = watcher.poll(60, TimeUnit.SECONDS);
                    if (key == null) continue;

                    if (key != fooKey) {
                        List<WatchEvent<?>> pollEvents = key.pollEvents();
                        for (WatchEvent<?> watchEvent : pollEvents) {
                            System.out.println(watchEvent.count() + " " +
                                            watchEvent.kind() + " " +
                                            watchEvent.context());
                        }
                        throw new RuntimeException("Event received for unexpected key");
                    }
                    key.reset();
                }
            } finally {
                // the threads should stop before WatchService is closed
                // to avoid ClosedWatchServiceException
                stop = true;

                // wait for threads to finish
                if (t1 != null) {
                    t1.join();
                }

                if (t2 != null) {
                    t2.join();
                }
            }
        } finally {
            // clean up
            Files.delete(foo);
            Files.delete(bar);
            Files.delete(baz);
            Files.delete(root);
        }
    }
}

