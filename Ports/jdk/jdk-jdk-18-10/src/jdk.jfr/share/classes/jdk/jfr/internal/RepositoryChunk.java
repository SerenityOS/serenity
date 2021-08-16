/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.Comparator;

import jdk.jfr.internal.SecuritySupport.SafePath;

final class RepositoryChunk {

    static final Comparator<RepositoryChunk> END_TIME_COMPARATOR = new Comparator<RepositoryChunk>() {
        @Override
        public int compare(RepositoryChunk c1, RepositoryChunk c2) {
            return c1.endTime.compareTo(c2.endTime);
        }
    };

    private final SafePath chunkFile;
    private final RandomAccessFile unFinishedRAF;

    private Instant endTime = null; // unfinished
    private Instant startTime;
    private int refCount = 0;
    private long size;

    RepositoryChunk(SafePath path) throws Exception {
        this.chunkFile = path;
        this.unFinishedRAF = SecuritySupport.createRandomAccessFile(chunkFile);
    }

    void finish(Instant endTime) {
        try {
            finishWithException(endTime);
        } catch (IOException e) {
            Logger.log(LogTag.JFR, LogLevel.ERROR, "Could not finish chunk. " + e.getClass() + " "+ e.getMessage());
        }
    }

    private void finishWithException(Instant endTime) throws IOException {
        unFinishedRAF.close();
        this.size = SecuritySupport.getFileSize(chunkFile);
        this.endTime = endTime;
        if (Logger.shouldLog(LogTag.JFR_SYSTEM, LogLevel.DEBUG)) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.DEBUG, "Chunk finished: " + chunkFile);
        }
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant timestamp) {
        this.startTime = timestamp;
    }

    public Instant getEndTime() {
        return endTime;
    }

    private void delete(SafePath f) {
        try {
            SecuritySupport.delete(f);
            if (Logger.shouldLog(LogTag.JFR, LogLevel.DEBUG)) {
                Logger.log(LogTag.JFR, LogLevel.DEBUG, "Repository chunk " + f + " deleted");
            }
        } catch (IOException e) {
            // Probably happens because file is being streamed
            // on Windows where files in use can't be removed.
            if (Logger.shouldLog(LogTag.JFR, LogLevel.DEBUG)) {
                Logger.log(LogTag.JFR, LogLevel.DEBUG, "Repository chunk " + f + " could not be deleted: " + e.getMessage());
            }
            if (f != null) {
                FilePurger.add(f);
            }
        }
    }

    private void destroy() {
        if (!isFinished()) {
            finish(Instant.MIN);
        }
         delete(chunkFile);
        try {
            unFinishedRAF.close();
        } catch (IOException e) {
            if (Logger.shouldLog(LogTag.JFR, LogLevel.ERROR)) {
                Logger.log(LogTag.JFR, LogLevel.ERROR, "Could not close random access file: " + chunkFile.toString() + ". File will not be deleted due to: " + e.getMessage());
            }
        }
    }

    public synchronized void use() {
        ++refCount;
        if (Logger.shouldLog(LogTag.JFR_SYSTEM, LogLevel.DEBUG)) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.DEBUG, "Use chunk " + toString() + " ref count now " + refCount);
        }
    }

    public synchronized void release() {
        --refCount;
        if (Logger.shouldLog(LogTag.JFR_SYSTEM, LogLevel.DEBUG)) {
            Logger.log(LogTag.JFR_SYSTEM, LogLevel.DEBUG, "Release chunk " + toString() + " ref count now " + refCount);
        }
        if (refCount == 0) {
            destroy();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void finalize() {
        boolean destroy = false;
        synchronized (this) {
            if (refCount > 0) {
                destroy = true;
            }
        }
        if (destroy) {
            destroy();
        }
    }

    public long getSize() {
        return size;
    }

    public boolean isFinished() {
        return endTime != null;
    }

    @Override
    public String toString() {
        return chunkFile.toString();
    }

    ReadableByteChannel newChannel() throws IOException {
        if (!isFinished()) {
            throw new IOException("Chunk not finished");
        }
        return ((SecuritySupport.newFileChannelToRead(chunkFile)));
    }

    public boolean inInterval(Instant startTime, Instant endTime) {
        if (startTime != null && getEndTime().isBefore(startTime)) {
            return false;
        }
        if (endTime != null && getStartTime().isAfter(endTime)) {
            return false;
        }
        return true;
    }

    public SafePath getFile() {
        return chunkFile;
    }
}
