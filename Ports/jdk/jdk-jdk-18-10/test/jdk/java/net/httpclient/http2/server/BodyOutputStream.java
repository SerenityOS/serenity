/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.ByteBuffer;

import jdk.internal.net.http.frame.DataFrame;

/**
 * OutputStream. Incoming window updates handled by the main connection
 * reader thread.
 */
@SuppressWarnings({"rawtypes","unchecked"})
class BodyOutputStream extends OutputStream {
    final static byte[] EMPTY_BARRAY = new byte[0];

    final int streamid;
    int window;
    volatile boolean closed;
    boolean goodToGo = false; // not allowed to send until headers sent
    final Http2TestServerConnection conn;
    final Queue outputQ;

    BodyOutputStream(int streamid, int initialWindow, Http2TestServerConnection conn) {
        this.window = initialWindow;
        this.streamid = streamid;
        this.conn = conn;
        this.outputQ = conn.outputQ;
        conn.registerStreamWindowUpdater(streamid, this::updateWindow);
    }

    // called from connection reader thread as all incoming window
    // updates are handled there.
    synchronized void updateWindow(int update) {
        window += update;
        notifyAll();
    }

    void waitForWindow(int demand) throws InterruptedException {
        // first wait for the connection window
        conn.obtainConnectionWindow(demand);
        // now wait for the stream window
        synchronized (this) {
            while (demand > 0) {
                int n = Math.min(demand, window);
                demand -= n;
                window -= n;
                if (demand > 0) {
                    wait();
                }
            }
        }
    }

    void goodToGo() {
        goodToGo = true;
    }

    @Override
    public void write(byte[] buf, int offset, int len) throws IOException {
        if (closed) {
            throw new IOException("closed");
        }

        if (!goodToGo) {
            throw new IllegalStateException("sendResponseHeaders must be called first");
        }
        try {
            int max = conn.getMaxFrameSize();
            while (len > 0) {
                int n = len > max ? max : len;
                waitForWindow(n);
                send(buf, offset, n, 0);
                offset += n;
                len -= n;
            }
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    private void send(byte[] buf, int offset, int len, int flags) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.put(buf, offset, len);
        buffer.flip();
        assert streamid != 0;
        DataFrame df = new DataFrame(streamid, flags, buffer);
        outputQ.put(df);
    }

    byte[] one = new byte[1];

    @Override
    public void write(int b) throws IOException {
        one[0] = (byte) b;
        write(one, 0, 1);
    }

    void closeInternal() {
        closed = true;
    }

    @Override
    public void close() {
        if (closed) return;
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        try {
            send(EMPTY_BARRAY, 0, 0, DataFrame.END_STREAM);
        } catch (IOException ex) {
            System.err.println("TestServer: OutputStream.close exception: " + ex);
            ex.printStackTrace();
        }
    }
}
