/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.management.jfr;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Queue;

import jdk.management.jfr.DiskRepository.DiskChunk;

final class FileDump {
    private final Queue<DiskChunk> chunks = new ArrayDeque<>();
    private final long stopTimeMillis;
    private boolean complete;

    FileDump(long stopTimeMillis) {
        this.stopTimeMillis = stopTimeMillis;
    }

    public synchronized void add(DiskChunk dc) {
        if (isComplete()) {
            return;
        }
        dc.acquire();
        chunks.add(dc);
        long endMillis = dc.endTimeNanos / 1_000_000;
        if (endMillis >= stopTimeMillis) {
            setComplete();
        }
    }

    public synchronized boolean isComplete() {
        return complete;
    }

    public synchronized void setComplete() {
        complete = true;
        this.notifyAll();
    }

    public synchronized void close() {
        for (DiskChunk dc : chunks) {
            dc.release();
        }
        chunks.clear();
        complete = true;
    }

    private DiskChunk oldestChunk() throws InterruptedException {
        while (true) {
            synchronized (this) {
                if (!chunks.isEmpty()) {
                    return chunks.poll();
                }
                if (complete) {
                    return null;
                }
                this.wait();
            }
        }
    }

    public void write(Path path) throws IOException, InterruptedException {
        try (FileChannel out = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            DiskChunk chunk = null;
            while ((chunk = oldestChunk()) != null) {
                try (FileChannel in = FileChannel.open(chunk.path(), StandardOpenOption.READ)) {
                    in.transferTo(0, in.size(), out);
                }
            }
        } finally {
            close();
        }
    }
}