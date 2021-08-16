/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333 7017446 8011537 8042470
 * @summary Unit test for java.nio.file.WatchService
 * @library ..
 * @run main Basic
 */

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for WatchService that exercises all methods in various scenarios.
 */

public class Basic {

    static void checkKey(WatchKey key, Path dir) {
        if (!key.isValid())
            throw new RuntimeException("Key is not valid");
        if (key.watchable() != dir)
            throw new RuntimeException("Unexpected watchable");
    }

    static void takeExpectedKey(WatchService watcher, WatchKey expected) {
        System.out.println("take events...");
        WatchKey key;
        try {
            key = watcher.take();
        } catch (InterruptedException x) {
            // not expected
            throw new RuntimeException(x);
        }
        if (key != expected)
            throw new RuntimeException("removed unexpected key");
    }

    static void checkExpectedEvent(Iterable<WatchEvent<?>> events,
                                   WatchEvent.Kind<?> expectedKind,
                                   Object expectedContext)
    {
        WatchEvent<?> event = events.iterator().next();
        System.out.format("got event: type=%s, count=%d, context=%s\n",
            event.kind(), event.count(), event.context());
        if (event.kind() != expectedKind)
            throw new RuntimeException("unexpected event");
        if (!expectedContext.equals(event.context()))
            throw new RuntimeException("unexpected context");
    }

    /**
     * Simple test of each of the standard events
     */
    static void testEvents(Path dir) throws IOException {
        System.out.println("-- Standard Events --");

        FileSystem fs = FileSystems.getDefault();
        Path name = fs.getPath("foo");

        try (WatchService watcher = fs.newWatchService()) {
            // --- ENTRY_CREATE ---

            // register for event
            System.out.format("register %s for ENTRY_CREATE\n", dir);
            WatchKey myKey = dir.register(watcher,
                new WatchEvent.Kind<?>[]{ ENTRY_CREATE });
            checkKey(myKey, dir);

            // create file
            Path file = dir.resolve("foo");
            System.out.format("create %s\n", file);
            Files.createFile(file);

            // remove key and check that we got the ENTRY_CREATE event
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(),
                StandardWatchEventKinds.ENTRY_CREATE, name);

            System.out.println("reset key");
            if (!myKey.reset())
                throw new RuntimeException("key has been cancalled");

            System.out.println("OKAY");

            // --- ENTRY_DELETE ---

            System.out.format("register %s for ENTRY_DELETE\n", dir);
            WatchKey deleteKey = dir.register(watcher,
                new WatchEvent.Kind<?>[]{ ENTRY_DELETE });
            if (deleteKey != myKey)
                throw new RuntimeException("register did not return existing key");
            checkKey(deleteKey, dir);

            System.out.format("delete %s\n", file);
            Files.delete(file);
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(),
                StandardWatchEventKinds.ENTRY_DELETE, name);

            System.out.println("reset key");
            if (!myKey.reset())
                throw new RuntimeException("key has been cancalled");

            System.out.println("OKAY");

            // create the file for the next test
            Files.createFile(file);

            // --- ENTRY_MODIFY ---

            System.out.format("register %s for ENTRY_MODIFY\n", dir);
            WatchKey newKey = dir.register(watcher,
                new WatchEvent.Kind<?>[]{ ENTRY_MODIFY });
            if (newKey != myKey)
                throw new RuntimeException("register did not return existing key");
            checkKey(newKey, dir);

            System.out.format("update: %s\n", file);
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
                out.write("I am a small file".getBytes("UTF-8"));
            }

            // remove key and check that we got the ENTRY_MODIFY event
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(),
                StandardWatchEventKinds.ENTRY_MODIFY, name);
            System.out.println("OKAY");

            // done
            Files.delete(file);
        }
    }

    /**
     * Check that a cancelled key will never be queued
     */
    static void testCancel(Path dir) throws IOException {
        System.out.println("-- Cancel --");

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

            System.out.format("register %s for events\n", dir);
            WatchKey myKey = dir.register(watcher,
                new WatchEvent.Kind<?>[]{ ENTRY_CREATE });
            checkKey(myKey, dir);

            System.out.println("cancel key");
            myKey.cancel();

            // create a file in the directory
            Path file = dir.resolve("mars");
            System.out.format("create: %s\n", file);
            Files.createFile(file);

            // poll for keys - there will be none
            System.out.println("poll...");
            try {
                WatchKey key = watcher.poll(3000, TimeUnit.MILLISECONDS);
                if (key != null)
                    throw new RuntimeException("key should not be queued");
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }

            // done
            Files.delete(file);

            System.out.println("OKAY");
        }
    }

    /**
     * Check that deleting a registered directory causes the key to be
     * cancelled and queued.
     */
    static void testAutomaticCancel(Path dir) throws IOException {
        System.out.println("-- Automatic Cancel --");

        Path subdir = Files.createDirectory(dir.resolve("bar"));

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

            System.out.format("register %s for events\n", subdir);
            WatchKey myKey = subdir.register(watcher,
                new WatchEvent.Kind<?>[]{ ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY });

            System.out.format("delete: %s\n", subdir);
            Files.delete(subdir);
            takeExpectedKey(watcher, myKey);

            System.out.println("reset key");
            if (myKey.reset())
                throw new RuntimeException("Key was not cancelled");
            if (myKey.isValid())
                throw new RuntimeException("Key is still valid");

            System.out.println("OKAY");

        }
    }

    /**
     * Asynchronous close of watcher causes blocked threads to wakeup
     */
    static void testWakeup(Path dir) throws IOException {
        System.out.println("-- Wakeup Tests --");
        final WatchService watcher = FileSystems.getDefault().newWatchService();
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                    System.out.println("close WatchService...");
                    watcher.close();
                } catch (InterruptedException x) {
                    x.printStackTrace();
                } catch (IOException x) {
                    x.printStackTrace();
                }
            }
        };

        // start thread to close watch service after delay
        new Thread(r).start();

        try {
            System.out.println("take...");
            watcher.take();
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (InterruptedException x) {
            throw new RuntimeException(x);
        } catch (ClosedWatchServiceException  x) {
            System.out.println("ClosedWatchServiceException thrown");
        }

        System.out.println("OKAY");
    }

    /**
     * Simple test to check exceptions and other cases
     */
    @SuppressWarnings("unchecked")
    static void testExceptions(Path dir) throws IOException {
        System.out.println("-- Exceptions and other simple tests --");

        WatchService watcher = FileSystems.getDefault().newWatchService();
        try {

            // Poll tests

            WatchKey key;
            System.out.println("poll...");
            key = watcher.poll();
            if (key != null)
                throw new RuntimeException("no keys registered");

            System.out.println("poll with timeout...");
            try {
                long start = System.nanoTime();
                key = watcher.poll(3000, TimeUnit.MILLISECONDS);
                if (key != null)
                    throw new RuntimeException("no keys registered");
                long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                if (waited < 2900)
                    throw new RuntimeException("poll was too short");
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }

            // IllegalArgumentException
            System.out.println("IllegalArgumentException tests...");
            try {
                dir.register(watcher /*empty event list*/);
                throw new RuntimeException("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException x) {
            }
            try {
                // OVERFLOW is ignored so this is equivalent to the empty set
                dir.register(watcher, OVERFLOW);
                throw new RuntimeException("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException x) {
            }
            try {
                // OVERFLOW is ignored even if specified multiple times
                dir.register(watcher, OVERFLOW, OVERFLOW);
                throw new RuntimeException("IllegalArgumentException not thrown");
            } catch (IllegalArgumentException x) {
            }

            // UnsupportedOperationException
            try {
                dir.register(watcher,
                             new WatchEvent.Kind<Object>() {
                                @Override public String name() { return "custom"; }
                                @Override public Class<Object> type() { return Object.class; }
                             });
                throw new RuntimeException("UnsupportedOperationException not thrown");
            } catch (UnsupportedOperationException x) {
            }
            try {
                dir.register(watcher,
                             new WatchEvent.Kind<?>[]{ ENTRY_CREATE },
                             new WatchEvent.Modifier() {
                                 @Override public String name() { return "custom"; }
                             });
                throw new RuntimeException("UnsupportedOperationException not thrown");
            } catch (UnsupportedOperationException x) {
            }

            // NullPointerException
            System.out.println("NullPointerException tests...");
            try {
                dir.register(null, ENTRY_CREATE);
                throw new RuntimeException("NullPointerException not thrown");
            } catch (NullPointerException x) {
            }
            try {
                dir.register(watcher, new WatchEvent.Kind<?>[]{ null });
                throw new RuntimeException("NullPointerException not thrown");
            } catch (NullPointerException x) {
            }
            try {
                dir.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_CREATE },
                    (WatchEvent.Modifier)null);
                throw new RuntimeException("NullPointerException not thrown");
            } catch (NullPointerException x) {
            }
        } finally {
            watcher.close();
        }

        // -- ClosedWatchServiceException --

        System.out.println("ClosedWatchServiceException tests...");

        try {
            watcher.poll();
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (ClosedWatchServiceException  x) {
        }

        // assume that poll throws exception immediately
        long start = System.nanoTime();
        try {
            watcher.poll(10000, TimeUnit.MILLISECONDS);
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (InterruptedException x) {
            throw new RuntimeException(x);
        } catch (ClosedWatchServiceException  x) {
            long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (waited > 5000)
                throw new RuntimeException("poll was too long");
        }

        try {
            watcher.take();
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (InterruptedException x) {
            throw new RuntimeException(x);
        } catch (ClosedWatchServiceException  x) {
        }

        try {
            dir.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_CREATE });
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (ClosedWatchServiceException  x) {
        }

        System.out.println("OKAY");
    }

    /**
     * Test that directory can be registered with more than one watch service
     * and that events don't interfere with each other
     */
    static void testTwoWatchers(Path dir) throws IOException {
        System.out.println("-- Two watchers test --");

        FileSystem fs = FileSystems.getDefault();
        WatchService watcher1 = fs.newWatchService();
        WatchService watcher2 = fs.newWatchService();
        try {
            Path name1 = fs.getPath("gus1");
            Path name2 = fs.getPath("gus2");

            // create gus1
            Path file1 = dir.resolve(name1);
            System.out.format("create %s\n", file1);
            Files.createFile(file1);

            // register with both watch services (different events)
            System.out.println("register for different events");
            WatchKey key1 = dir.register(watcher1,
                new WatchEvent.Kind<?>[]{ ENTRY_CREATE });
            WatchKey key2 = dir.register(watcher2,
                new WatchEvent.Kind<?>[]{ ENTRY_DELETE });

            if (key1 == key2)
                throw new RuntimeException("keys should be different");

            // create gus2
            Path file2 = dir.resolve(name2);
            System.out.format("create %s\n", file2);
            Files.createFile(file2);

            // check that key1 got ENTRY_CREATE
            takeExpectedKey(watcher1, key1);
            checkExpectedEvent(key1.pollEvents(),
                StandardWatchEventKinds.ENTRY_CREATE, name2);

            // check that key2 got zero events
            WatchKey key = watcher2.poll();
            if (key != null)
                throw new RuntimeException("key not expected");

            // delete gus1
            Files.delete(file1);

            // check that key2 got ENTRY_DELETE
            takeExpectedKey(watcher2, key2);
            checkExpectedEvent(key2.pollEvents(),
                StandardWatchEventKinds.ENTRY_DELETE, name1);

            // check that key1 got zero events
            key = watcher1.poll();
            if (key != null)
                throw new RuntimeException("key not expected");

            // reset for next test
            key1.reset();
            key2.reset();

            // change registration with watcher2 so that they are both
            // registered for the same event
            System.out.println("register for same event");
            key2 = dir.register(watcher2, new WatchEvent.Kind<?>[]{ ENTRY_CREATE });

            // create file and key2 should be queued
            System.out.format("create %s\n", file1);
            Files.createFile(file1);
            takeExpectedKey(watcher2, key2);
            checkExpectedEvent(key2.pollEvents(),
                StandardWatchEventKinds.ENTRY_CREATE, name1);

            System.out.println("OKAY");

        } finally {
            watcher2.close();
            watcher1.close();
        }
    }

    /**
     * Test that thread interruped status is preserved upon a call
     * to register()
     */
    static void testThreadInterrupt(Path dir) throws IOException {
        System.out.println("-- Thread interrupted status test --");

        FileSystem fs = FileSystems.getDefault();
        Thread curr = Thread.currentThread();
        try (WatchService watcher = fs.newWatchService()) {
            System.out.println("interrupting current thread");
            curr.interrupt();
            dir.register(watcher, ENTRY_CREATE);
            if (!curr.isInterrupted())
                throw new RuntimeException("thread should remain interrupted");
            System.out.println("current thread is still interrupted");
            System.out.println("OKAY");
        } finally {
            curr.interrupted();
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {

            testEvents(dir);
            testCancel(dir);
            testAutomaticCancel(dir);
            testWakeup(dir);
            testExceptions(dir);
            testTwoWatchers(dir);
            testThreadInterrupt(dir);

        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
