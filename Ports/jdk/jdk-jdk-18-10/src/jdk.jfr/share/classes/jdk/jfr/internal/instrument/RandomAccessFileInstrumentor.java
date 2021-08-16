/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.instrument;

import java.io.IOException;

import jdk.jfr.events.Handlers;
import jdk.jfr.internal.handlers.EventHandler;

/**
 * See {@link JITracer} for an explanation of this code.
 */
@JIInstrumentationTarget("java.io.RandomAccessFile")
final class RandomAccessFileInstrumentor {

    private RandomAccessFileInstrumentor() {
    }

    private String path;

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int read() throws IOException {
        EventHandler handler = Handlers.FILE_READ;
        if (!handler.isEnabled()) {
            return read();
        }
        int result = 0;
        long bytesRead = 0;
        boolean endOfFile = false;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            result = read();
            if (result < 0) {
                endOfFile = true;
            } else {
                bytesRead = 1;
            }
        } finally {
            long duration = EventHandler.timestamp() - start;
            if (handler.shouldCommit(duration)) {
                handler.write(start, duration, path, bytesRead, endOfFile);
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int read(byte b[]) throws IOException {
        EventHandler handler = Handlers.FILE_READ;
        if (!handler.isEnabled()) {
            return read(b);
        }
        int bytesRead = 0;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            bytesRead = read(b);
        } finally {
            long duration = EventHandler.timestamp() - start;
            if (handler.shouldCommit(duration)) {
                if (bytesRead < 0) {
                    handler.write(start, duration, path, 0L, true);
                } else {
                    handler.write(start, duration, path, bytesRead, false);
                }
            }
        }
        return bytesRead;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public int read(byte b[], int off, int len) throws IOException {
        EventHandler handler = Handlers.FILE_READ;
        if (!handler.isEnabled()) {
            return read(b, off, len);
        }
        int bytesRead = 0;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            bytesRead = read(b, off, len);
        } finally {
            long duration = EventHandler.timestamp() - start;
            if (handler.shouldCommit(duration)) {
                if (bytesRead < 0) {
                    handler.write(start, duration, path, 0L, true);
                } else {
                    handler.write(start, duration, path, bytesRead, false);
                }
            }
        }
        return bytesRead;
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public void write(int b) throws IOException {
        EventHandler handler = Handlers.FILE_WRITE;
        if (!handler.isEnabled()) {
            write(b);
            return;
        }
        long bytesWritten = 0;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            write(b);
            bytesWritten = 1;
        } finally {
            long duration = EventHandler.timestamp() - start;
            if (handler.shouldCommit(duration)) {
                handler.write(start, duration, path, bytesWritten);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public void write(byte b[]) throws IOException {
        EventHandler handler = Handlers.FILE_WRITE;
        if (!handler.isEnabled()) {
            write(b);
            return;
        }
        long bytesWritten = 0;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            write(b);
            bytesWritten = b.length;
        } finally {
            long duration = EventHandler.timestamp();
            if (handler.shouldCommit(duration)) {
                handler.write(start, duration, path, bytesWritten);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @JIInstrumentationMethod
    public void write(byte b[], int off, int len) throws IOException {
        EventHandler handler = Handlers.FILE_WRITE;
        if (!handler.isEnabled()) {
            write(b, off, len);
            return;
        }
        long bytesWritten = 0;
        long start = 0;
        try {
            start = EventHandler.timestamp();
            write(b, off, len);
            bytesWritten = len;
        } finally {
            long duration = EventHandler.timestamp() - start;
            if (handler.shouldCommit(duration)) {
                handler.write(start, duration, path, bytesWritten);
            }
        }
    }
}
