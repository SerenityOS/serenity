/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jshell.execution;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Read from an InputStream which has been packetized and write its contents
 * to the named OutputStreams.
 *
 * @author Jan Lahoda
 * @see Util#demultiplexInput(java.io.InputStream, java.io.OutputStream, java.io.OutputStream, java.io.OutputStream...)
 */
class DemultiplexInput extends Thread {

    private final DataInputStream delegate;
    private final Map<String, OutputStream> io;
    private final Iterable<OutputStream> closeList;

    DemultiplexInput(InputStream input, Map<String, OutputStream> io, Iterable<OutputStream> closeList) {
        super("output reader");
        setDaemon(true);
        this.delegate = new DataInputStream(input);
        this.io = io;
        this.closeList = closeList;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int nameLen = delegate.read();
                if (nameLen == (-1)) {
                    break;
                }
                byte[] name = new byte[nameLen];
                DemultiplexInput.this.delegate.readFully(name);
                int dataLen = delegate.read();
                byte[] data = new byte[dataLen];
                DemultiplexInput.this.delegate.readFully(data);
                String chan = new String(name, "UTF-8");
                OutputStream out = io.get(chan);
                if (out == null) {
                    debug("Unexpected channel name: %s", chan);
                } else {
                    out.write(data);
                }
            }
        } catch (IOException ex) {
            debug(ex, "Failed reading output");
        } finally {
            for (OutputStream out : closeList) {
                try {
                    out.close();
                } catch (IOException ex) {
                    debug(ex, "Failed reading output");
                }
            }
        }
    }

    /**
     * Log debugging information. Arguments as for {@code printf}.
     *
     * @param format a format string as described in Format string syntax
     * @param args arguments referenced by the format specifiers in the format
     * string.
     */
    private void debug(String format, Object... args) {
        // Reserved for future logging
    }

    /**
     * Log a serious unexpected internal exception.
     *
     * @param ex the exception
     * @param where a description of the context of the exception
     */
    private void debug(Throwable ex, String where) {
        // Reserved for future logging
    }

}
