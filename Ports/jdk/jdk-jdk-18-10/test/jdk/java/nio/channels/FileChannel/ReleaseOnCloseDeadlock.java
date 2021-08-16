/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6543863 6842687
 * @summary Try to cause a deadlock between (Asynchronous)FileChannel.close
 *   and FileLock.release
 */

import java.io.*;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class ReleaseOnCloseDeadlock {
    private static final int LOCK_COUNT = 1024;

    public static void main(String[] args) throws IOException {
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        try {
            for (int i=0; i<100; i++) {
                test(blah.toPath());
            }
        } finally {
            blah.delete();
        }
    }

    static void test(Path file) throws IOException {
        FileLock[] locks = new FileLock[LOCK_COUNT];

        FileChannel fc = FileChannel.open(file, READ, WRITE);
        for (int i=0; i<LOCK_COUNT; i++) {
            locks[i] = fc.lock(i, 1, true);
        }
        tryToDeadlock(fc, locks);

        AsynchronousFileChannel ch = AsynchronousFileChannel.open(file, READ, WRITE);
        for (int i=0; i<LOCK_COUNT; i++) {
            try {
                locks[i] = ch.lock(i, 1, true).get();
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            } catch (ExecutionException x) {
                throw new RuntimeException(x);
            }
        }
        tryToDeadlock(ch, locks);
    }

    static void tryToDeadlock(final Channel channel, FileLock[] locks)
        throws IOException
    {
        // start thread to close the file (and invalidate the locks)
        Thread closer = new Thread(
            new Runnable() {
                public void run() {
                    try {
                        channel.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                }});
        closer.start();

        // release the locks explicitly
        for (int i=0; i<locks.length; i++) {
            try {
                locks[i].release();
            } catch (ClosedChannelException ignore) { }
        }

        // we are done when closer has terminated
        while (closer.isAlive()) {
            try {
                closer.join();
            } catch (InterruptedException ignore) { }
        }
    }
}
