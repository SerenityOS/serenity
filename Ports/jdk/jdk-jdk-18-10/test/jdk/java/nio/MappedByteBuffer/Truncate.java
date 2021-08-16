/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6934977
 * @summary Test MappedByteBuffer operations after mapped bye buffer becomes
 *   inaccessible
 * @run main/othervm Truncate
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.Callable;

public class Truncate {

    static final long INITIAL_FILE_SIZE   = 32000L;
    static final long TRUNCATED_FILE_SIZE =   512L;

    public static void main(String[] args) throws Exception {
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();

        final FileChannel fc = new RandomAccessFile(blah, "rw").getChannel();
        fc.position(INITIAL_FILE_SIZE).write(ByteBuffer.wrap("THE END".getBytes()));
        final MappedByteBuffer mbb =
            fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size());
        boolean truncated;
        try {
            fc.truncate(TRUNCATED_FILE_SIZE);
            truncated = true;
        } catch (IOException ioe) {
            // probably on Windows where a file cannot be truncated when
            // there is a file mapping.
            truncated = false;
        }
        if (truncated) {
            // Test 1: access region that is no longer accessible
            execute(new Callable<Void>() {
                public Void call() {
                    mbb.get((int)TRUNCATED_FILE_SIZE + 1);
                    mbb.put((int)TRUNCATED_FILE_SIZE + 2, (byte)123);
                    return null;
                }
            });
            // Test 2: load buffer into memory
            execute(new Callable<Void>() {
                public Void call() throws IOException {
                    mbb.load();
                    return null;
                }
            });
        }
        fc.close();
    }

    // Runs the given task in its own thread. If operating correcting the
    // the thread will terminate with an InternalError as the mapped buffer
    // is inaccessible.
    static void execute(final Callable<?> c) {
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Object ignore = c.call();
                } catch (Exception ignore) {
                }
            }
        };
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
        t.start();
        try { t.join(); } catch (InterruptedException ignore) { }
    }
}
