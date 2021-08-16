/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.io;


/**
 * Piped character-output streams.
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class PipedWriter extends Writer {

    /* REMIND: identification of the read and write sides needs to be
       more sophisticated.  Either using thread groups (but what about
       pipes within a thread?) or using finalization (but it may be a
       long time until the next GC). */
    private PipedReader sink;

    /* This flag records the open status of this particular writer. It
     * is independent of the status flags defined in PipedReader. It is
     * used to do a sanity check on connect.
     */
    private boolean closed = false;

    /**
     * Creates a piped writer connected to the specified piped
     * reader. Data characters written to this stream will then be
     * available as input from {@code snk}.
     *
     * @param      snk   The piped reader to connect to.
     * @throws     IOException  if an I/O error occurs.
     */
    public PipedWriter(PipedReader snk)  throws IOException {
        connect(snk);
    }

    /**
     * Creates a piped writer that is not yet connected to a
     * piped reader. It must be connected to a piped reader,
     * either by the receiver or the sender, before being used.
     *
     * @see     java.io.PipedReader#connect(java.io.PipedWriter)
     * @see     java.io.PipedWriter#connect(java.io.PipedReader)
     */
    public PipedWriter() {
    }

    /**
     * Connects this piped writer to a receiver. If this object
     * is already connected to some other piped reader, an
     * {@code IOException} is thrown.
     * <p>
     * If {@code snk} is an unconnected piped reader and
     * {@code src} is an unconnected piped writer, they may
     * be connected by either the call:
     * <blockquote><pre>
     * src.connect(snk)</pre></blockquote>
     * or the call:
     * <blockquote><pre>
     * snk.connect(src)</pre></blockquote>
     * The two calls have the same effect.
     *
     * @param      snk   the piped reader to connect to.
     * @throws     IOException  if an I/O error occurs.
     */
    public synchronized void connect(PipedReader snk) throws IOException {
        if (snk == null) {
            throw new NullPointerException();
        } else if (sink != null || snk.connected) {
            throw new IOException("Already connected");
        } else if (snk.closedByReader || closed) {
            throw new IOException("Pipe closed");
        }

        sink = snk;
        snk.in = -1;
        snk.out = 0;
        snk.connected = true;
    }

    /**
     * Writes the specified {@code char} to the piped output stream.
     * If a thread was reading data characters from the connected piped input
     * stream, but the thread is no longer alive, then an
     * {@code IOException} is thrown.
     * <p>
     * Implements the {@code write} method of {@code Writer}.
     *
     * @param   c   the {@code char} to be written.
     * @throws  IOException  if the pipe is
     *          <a href=PipedOutputStream.html#BROKEN> {@code broken}</a>,
     *          {@link #connect(java.io.PipedReader) unconnected}, closed
     *          or an I/O error occurs.
     */
    public void write(int c)  throws IOException {
        if (sink == null) {
            throw new IOException("Pipe not connected");
        }
        sink.receive(c);
    }

    /**
     * Writes {@code len} characters from the specified character array
     * starting at offset {@code off} to this piped output stream.
     * This method blocks until all the characters are written to the output
     * stream.
     * If a thread was reading data characters from the connected piped input
     * stream, but the thread is no longer alive, then an
     * {@code IOException} is thrown.
     *
     * @param   cbuf  the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of characters to write.
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code off} is negative, or {@code len} is negative,
     *          or {@code off + len} is negative or greater than the length
     *          of the given array
     *
     * @throws  IOException  if the pipe is
     *          <a href=PipedOutputStream.html#BROKEN>{@code broken}</a>,
     *          {@link #connect(java.io.PipedReader) unconnected}, closed
     *          or an I/O error occurs.
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        if (sink == null) {
            throw new IOException("Pipe not connected");
        } else if ((off | len | (off + len) | (cbuf.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        }
        sink.receive(cbuf, off, len);
    }

    /**
     * Flushes this output stream and forces any buffered output characters
     * to be written out.
     * This will notify any readers that characters are waiting in the pipe.
     *
     * @throws     IOException  if the pipe is closed, or an I/O error occurs.
     */
    public synchronized void flush() throws IOException {
        if (sink != null) {
            if (sink.closedByReader || closed) {
                throw new IOException("Pipe closed");
            }
            synchronized (sink) {
                sink.notifyAll();
            }
        }
    }

    /**
     * Closes this piped output stream and releases any system resources
     * associated with this stream. This stream may no longer be used for
     * writing characters.
     *
     * @throws     IOException  if an I/O error occurs.
     */
    public void close()  throws IOException {
        closed = true;
        if (sink != null) {
            sink.receivedLast();
        }
    }
}
