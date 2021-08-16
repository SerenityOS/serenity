/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.log;

import java.io.*;

public
class LogInputStream extends InputStream {
    private InputStream in;
    private int length;

    /**
     * Creates a log input file with the specified system dependent
     * file descriptor.
     * @param in the system dependent file descriptor
     * @param length the total number of bytes allowed to be read
     * @exception IOException If an I/O error has occurred.
     */
    public LogInputStream(InputStream in, int length) throws IOException {
        this.in = in;
        this.length = length;
    }

    /**
     * Reads a byte of data. This method will block if no input is
     * available.
     * @return  the byte read, or -1 if the end of the log or end of the
     *          stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    public int read() throws IOException {
        if (length == 0)
            return -1;
        int c = in.read();
        length = (c != -1) ? length - 1 : 0;
        return c;
    }

    /**
     * Reads data into an array of bytes.
     * This method blocks until some input is available.
     * @param b the buffer into which the data is read
     * @return  the actual number of bytes read, or -1 if the end of the log
     *          or end of the stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads data into an array of bytes.
     * This method blocks until some input is available.
     * @param b the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return  the actual number of bytes read, or -1 if the end of the log or
     *          end of the stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (length == 0)
            return -1;
        len = (length < len) ? length : len;
        int n = in.read(b, off, len);
        length = (n != -1) ? length - n : 0;
        return n;
    }

    /**
     * Skips n bytes of input.
     * @param n the number of bytes to be skipped
     * @return  the actual number of bytes skipped.
     * @exception IOException If an I/O error has occurred.
     */
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE)
            throw new IOException("Too many bytes to skip - " + n);
        if (length == 0)
            return 0;
        n = (length < n) ? length : n;
        n = in.skip(n);
        length -= n;
        return n;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     * @return  the number of available bytes, which is initially
     *          equal to the file size.
     */
    public int available() throws IOException {
        int avail = in.available();
        return (length < avail) ? length : avail;
    }

    /**
     * Closes the input stream.  No further input can be read.
     * the stream.
     */
    public void close() {
        length = 0;
    }

    /**
     * Closes the stream when garbage is collected.
     */
    @SuppressWarnings("deprecation")
    protected void finalize() throws IOException {
        close();
    }
}
