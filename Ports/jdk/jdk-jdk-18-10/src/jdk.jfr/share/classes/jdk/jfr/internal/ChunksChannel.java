/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ChunksChannel implements ReadableByteChannel {
    private final Iterator<RepositoryChunk> chunks;
    private RepositoryChunk                 current;
    private ReadableByteChannel             channel;

    public ChunksChannel(List<RepositoryChunk> chunks) throws IOException {
        if (chunks.isEmpty()) {
            throw new FileNotFoundException("No chunks");
        }
        List<RepositoryChunk> l = new ArrayList<>(chunks.size());
        for (RepositoryChunk c : chunks) {
            c.use(); // keep alive while we're reading.
            l.add(c);
        }
        this.chunks = l.iterator();
        nextChannel();
    }

    private boolean nextChunk() {
        if (!chunks.hasNext()) {
            return false;
        }
        current = chunks.next();
        return true;
    }

    private boolean nextChannel() throws IOException {
        if (!nextChunk()) {
            return false;
        }

        channel = current.newChannel();
        return true;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        for (;;) {
            if (channel != null) {
                assert current != null;
                int r = channel.read(dst);
                if (r != -1) {
                    return r;
                }
                channel.close();
                current.release();
                channel = null;
                current = null;
            }
            if (!nextChannel()) {
                return -1;
            }
        }
    }

    public long transferTo(FileChannel out) throws IOException {
        long pos = 0;
        for (;;) {
            if (channel != null) {
                assert current != null;

                long rem = current.getSize();

                while (rem > 0) {
                    long n = Math.min(rem, 1024 * 1024);
                    long w = out.transferFrom(channel, pos, n);
                    pos += w;
                    rem -= w;
                }

                channel.close();
                current.release();

                channel = null;
                current = null;
            }
            if (!nextChannel()) {
                return pos;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        while (current != null) {
            current.release();
            current = null;
            if (!nextChunk()) {
                return;
            }
        }
    }

    @Override
    public boolean isOpen() {
        return channel != null;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
}
