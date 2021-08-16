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
import java.util.List;

import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.frame.DataFrame;
import jdk.internal.net.http.frame.Http2Frame;
import jdk.internal.net.http.frame.ResetFrame;

/**
 * InputStream reads frames off stream q and supplies read demand from any
 * DataFrames it finds. Window updates are sent back on the connections send
 * q.
 */
class BodyInputStream extends InputStream {

    final Queue<Http2Frame> q;
    final int streamid;
    boolean closed;
    boolean eof;
    final Http2TestServerConnection conn;

    @SuppressWarnings({"rawtypes","unchecked"})
    BodyInputStream(Queue q, int streamid, Http2TestServerConnection conn) {
        this.q = q;
        this.streamid = streamid;
        this.conn = conn;
    }

    DataFrame df;
    ByteBuffer[] buffers;
    ByteBuffer buffer;
    int nextIndex = -1;

    private DataFrame getData() throws IOException {
        if (eof) {
            return null;
        }
        Http2Frame frame;
        do {
            frame = q.take();
            if (frame == null) return null; // closed/eof before receiving data.
            // ignoring others for now Wupdates handled elsewhere
            if (frame.type() != DataFrame.TYPE) {
                System.out.println("Ignoring " + frame.toString() + " CHECK THIS");
            }
        } while (frame.type() != DataFrame.TYPE);
        df = (DataFrame) frame;
        int len = df.payloadLength();
        eof = frame.getFlag(DataFrame.END_STREAM);
        // acknowledge
        conn.sendWindowUpdates(len, streamid);
        return (DataFrame) frame;
    }

    // null return means EOF
    private ByteBuffer getBuffer() throws IOException {
        if (buffer == null || !buffer.hasRemaining()) {
            if (nextIndex == -1 || nextIndex == buffers.length) {
                DataFrame df = getData();
                if (df == null) {
                    return null;
                }
                List<ByteBuffer> data = df.getData();
                long len = Utils.remaining(data);
                if ((len == 0) && eof) {
                    return null;
                }

                buffers = data.toArray(Utils.EMPTY_BB_ARRAY);
                nextIndex = 0;
            }
            buffer = buffers[nextIndex++];
        }
        return buffer;
    }

    @Override
    public int read(byte[] buf, int offset, int length) throws IOException {
        if (closed) {
            throw new IOException("closed");
        }
        ByteBuffer b = getBuffer();
        if (b == null) {
            return -1;
        }
        int remaining = b.remaining();
        if (remaining < length) {
            length = remaining;
        }
        b.get(buf, offset, length);
        return length;
    }

    byte[] one = new byte[1];

    @Override
    public int read() throws IOException {
        int c = read(one, 0, 1);
        if (c == -1) {
            return -1;
        }
        return one[0] & 0xFF;
    }

    @Override
    public void close() {
        // TODO reset this stream
        closed = true;
    }
}
