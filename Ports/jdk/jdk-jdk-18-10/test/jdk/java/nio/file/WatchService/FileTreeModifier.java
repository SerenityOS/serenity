/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6838333
 * @summary Sanity test for JDK-specific FILE_TREE watch event modifier
 * @library ..
 * @modules jdk.unsupported
 */

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import static com.sun.nio.file.ExtendedWatchEventModifier.*;

public class FileTreeModifier {

    static void checkExpectedEvent(WatchService watcher,
                                   WatchEvent.Kind<?> expectedType,
                                   Object expectedContext)
    {
        WatchKey key;
        try {
            key = watcher.take();
        } catch (InterruptedException x) {
            // should not happen
            throw new RuntimeException(x);
        }
        WatchEvent<?> event = key.pollEvents().iterator().next();
        System.out.format("Event: type=%s, count=%d, context=%s\n",
            event.kind(), event.count(), event.context());
        if (event.kind() != expectedType)
            throw new RuntimeException("unexpected event");
        if (!expectedContext.equals(event.context()))
            throw new RuntimeException("unexpected context");
    }

    static void doTest(Path top) throws IOException {
        FileSystem fs = top.getFileSystem();
        WatchService watcher = fs.newWatchService();

        // create directories
        Path subdir = Files.createDirectories(top.resolve("a").resolve("b").resolve("c"));

        // Test ENTRY_CREATE with FILE_TREE modifier.

        WatchKey key = top.register(watcher,
            new WatchEvent.Kind<?>[]{ ENTRY_CREATE }, FILE_TREE);

        // create file in a/b/c and check we get create event
        Path file = Files.createFile(subdir.resolve("foo"));
        checkExpectedEvent(watcher, ENTRY_CREATE, top.relativize(file));
        key.reset();

        // Test ENTRY_DELETE with FILE_TREE modifier.

        WatchKey k = top.register(watcher,
            new WatchEvent.Kind<?>[]{ ENTRY_DELETE }, FILE_TREE);
        if (k != key)
            throw new RuntimeException("Existing key not returned");

        // delete a/b/c/foo and check we get delete event
        Files.delete(file);
        checkExpectedEvent(watcher, ENTRY_DELETE, top.relativize(file));
        key.reset();

        // Test changing registration to ENTRY_CREATE without modifier

        k = top.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_CREATE });
        if (k != key)
            throw new RuntimeException("Existing key not returned");

        // create a/b/c/foo
        Files.createFile(file);

        // check that key is not queued
        WatchKey nextKey;
        try {
            nextKey = watcher.poll(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
        if (nextKey != null)
            throw new RuntimeException("WatchKey not expected to be polled");

        // create bar and check we get create event
        file = Files.createFile(top.resolve("bar"));
        checkExpectedEvent(watcher, ENTRY_CREATE, top.relativize(file));
        key.reset();

        // Test changing registration to <all> with FILE_TREE modifier

        k = top.register(watcher,
            new WatchEvent.Kind<?>[]{ ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY },
            FILE_TREE);
        if (k != key)
            throw new RuntimeException("Existing key not returned");

        // modify bar and check we get modify event
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write("Double shot expresso please".getBytes("UTF-8"));
        }
        checkExpectedEvent(watcher, ENTRY_MODIFY, top.relativize(file));
        key.reset();
    }


    public static void main(String[] args) throws IOException {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("This is Windows-only test at this time!");
            return;
        }

        Path dir = TestUtil.createTemporaryDirectory();
        try {
            doTest(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}
