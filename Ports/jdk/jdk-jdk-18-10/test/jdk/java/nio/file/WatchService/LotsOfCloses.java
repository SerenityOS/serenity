/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148192
 * @summary Invoking close asynchronously can cause register to fail with
 *     an IOException rather than a ClosedWatchServiceException
 * @run main LotsOfCloses
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LotsOfCloses {

    private static final Random RAND = new Random();

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("tmp");
        ExecutorService pool = Executors.newCachedThreadPool();
        try {

            // run the test repeatedly
            long start = System.currentTimeMillis();
            while ((System.currentTimeMillis() - start) < 5000) {
                test(dir, pool);
            }

        } finally {
            pool.shutdown();
        }

    }

    /**
     * Create a WatchService to watch for changes in the given directory
     * and then attempt to close the WatchService and change a registration
     * at the same time.
     */
    static void test(Path dir, ExecutorService pool) throws Exception {
        WatchService watcher = FileSystems.getDefault().newWatchService();

        // initial registration
        dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

        // submit tasks to close the WatchService and update the registration
        Future<Void> closeResult;
        Future<Boolean> registerResult;

        if (RAND.nextBoolean()) {
            closeResult = pool.submit(newCloserTask(watcher));
            registerResult = pool.submit(newRegisterTask(watcher, dir));
        } else {
            registerResult = pool.submit(newRegisterTask(watcher, dir));
            closeResult = pool.submit(newCloserTask(watcher));
        }

        closeResult.get();
        registerResult.get();

    }

    /**
     * Returns a task that closes the given WatchService.
     */
    static Callable<Void> newCloserTask(WatchService watcher) {
        return () ->  {
            try {
                watcher.close();
                return null;
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }

    /**
     * Returns a task that updates the registration of a directory with
     * a WatchService.
     */
    static Callable<Boolean> newRegisterTask(WatchService watcher, Path dir) {
        return () -> {
            try {
                dir.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
                return true;
            } catch (ClosedWatchServiceException e) {
                return false;
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        };
    }
}
