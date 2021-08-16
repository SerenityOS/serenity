/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.net.www.http;

import java.io.*;

import sun.net.ProgressSource;
import sun.net.www.MeteredStream;
import jdk.internal.misc.InnocuousThread;

/**
 * A stream that has the property of being able to be kept alive for
 * multiple downloads from the same server.
 *
 * @author Stephen R. Pietrowicz (NCSA)
 * @author Dave Brown
 */
public
class KeepAliveStream extends MeteredStream implements Hurryable {

    // instance variables
    HttpClient hc;

    boolean hurried;

    // has this KeepAliveStream been put on the queue for asynchronous cleanup.
    // This flag is read from within KeepAliveCleanerEntry outside of any lock.
    protected volatile boolean queuedForCleanup = false;

    private static final KeepAliveStreamCleaner queue = new KeepAliveStreamCleaner();
    private static Thread cleanerThread; // null

    /**
     * Constructor
     */
    public KeepAliveStream(InputStream is, ProgressSource pi, long expected, HttpClient hc)  {
        super(is, pi, expected);
        this.hc = hc;
    }

    /**
     * Attempt to cache this connection
     */
    public void close() throws IOException  {
        // If the inputstream is queued for cleanup, just return.
        if (queuedForCleanup) return;

        // Skip past the data that's left in the Inputstream because
        // some sort of error may have occurred.
        // Do this ONLY if the skip won't block. The stream may have
        // been closed at the beginning of a big file and we don't want
        // to hang around for nothing. So if we can't skip without blocking
        // we just close the socket and, therefore, terminate the keepAlive
        // NOTE: Don't close super class
        // For consistency, access to `expected` and `count` should be
        // protected by readLock
        lock();
        try {
            // If the inputstream is closed already, or if this stream
            // has already been queued for cleanup, just return.
            if (closed || queuedForCleanup) return;
            try {
                if (expected > count) {
                    long nskip = expected - count;
                    if (nskip <= available()) {
                        do {
                        } while ((nskip = (expected - count)) > 0L
                                && skip(Math.min(nskip, available())) > 0L);
                    } else if (expected <= KeepAliveStreamCleaner.MAX_DATA_REMAINING && !hurried) {
                        //put this KeepAliveStream on the queue so that the data remaining
                        //on the socket can be cleanup asyncronously.
                        queueForCleanup(new KeepAliveCleanerEntry(this, hc));
                    } else {
                        hc.closeServer();
                    }
                }
                if (!closed && !hurried && !queuedForCleanup) {
                    hc.finished();
                }
            } finally {
                if (pi != null)
                    pi.finishTracking();

                if (!queuedForCleanup) {
                    // nulling out the underlying inputstream as well as
                    // httpClient to let gc collect the memories faster
                    in = null;
                    hc = null;
                    closed = true;
                }
            }
        } finally {
            unlock();
        }
    }

    /* we explicitly do not support mark/reset */

    public boolean markSupported()  {
        return false;
    }

    public void mark(int limit) {}

    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public boolean hurry() {
        lock();
        try {
            /* CASE 0: we're actually already done */
            if (closed || count >= expected) {
                return false;
            } else if (in.available() < (expected - count)) {
                /* CASE I: can't meet the demand */
                return false;
            } else {
                /* CASE II: fill our internal buffer
                 * Remind: possibly check memory here
                 */
                int size = (int) (expected - count);
                byte[] buf = new byte[size];
                DataInputStream dis = new DataInputStream(in);
                dis.readFully(buf);
                in = new ByteArrayInputStream(buf);
                hurried = true;
                return true;
            }
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        } finally {
            unlock();
        }
    }

    @SuppressWarnings("removal")
    private static void queueForCleanup(KeepAliveCleanerEntry kace) {
        queue.lock();
        try {
            if(!kace.getQueuedForCleanup()) {
                if (!queue.offer(kace)) {
                    kace.getHttpClient().closeServer();
                    return;
                }

                kace.setQueuedForCleanup();
                queue.signalAll();
            }

            boolean startCleanupThread = (cleanerThread == null);
            if (!startCleanupThread) {
                if (!cleanerThread.isAlive()) {
                    startCleanupThread = true;
                }
            }

            if (startCleanupThread) {
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        cleanerThread = InnocuousThread.newSystemThread("Keep-Alive-SocketCleaner", queue);
                        cleanerThread.setDaemon(true);
                        cleanerThread.setPriority(Thread.MAX_PRIORITY - 2);
                        cleanerThread.start();
                        return null;
                    }
                });
            }
        } finally {
            queue.unlock();
        }
    }

    // Only called from KeepAliveStreamCleaner
    protected long remainingToRead() {
        assert isLockHeldByCurrentThread();
        return expected - count;
    }

    // Only called from KeepAliveStreamCleaner
    protected void setClosed() {
        assert isLockHeldByCurrentThread();
        in = null;
        hc = null;
        closed = true;
    }
}
