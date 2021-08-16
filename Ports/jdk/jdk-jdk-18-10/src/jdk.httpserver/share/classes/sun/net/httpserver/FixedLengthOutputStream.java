/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.spi.*;

/**
 * a class which allows the caller to write up to a defined
 * number of bytes to an underlying stream. The caller *must*
 * write the pre-defined number or else an exception will be thrown
 * and the whole request aborted.
 * normal close() does not close the underlying stream
 */

class FixedLengthOutputStream extends FilterOutputStream
{
    private long remaining;
    private boolean eof = false;
    private boolean closed = false;
    ExchangeImpl t;

    FixedLengthOutputStream (ExchangeImpl t, OutputStream src, long len) {
        super (src);
        this.t = t;
        this.remaining = len;
    }

    public void write (int b) throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        eof = (remaining == 0);
        if (eof) {
            throw new StreamClosedException();
        }
        out.write(b);
        remaining --;
    }

    public void write (byte[]b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        eof = (remaining == 0);
        if (eof) {
            throw new StreamClosedException();
        }
        if (len > remaining) {
            // stream is still open, caller can retry
            throw new IOException ("too many bytes to write to stream");
        }
        out.write(b, off, len);
        remaining -= len;
    }

    public void close () throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (remaining > 0) {
            t.close();
            throw new IOException ("insufficient bytes written to stream");
        }
        flush();
        eof = true;
        LeftOverInputStream is = t.getOriginalInputStream();
        if (!is.isClosed()) {
            try {
                is.close();
            } catch (IOException e) {}
        }
        WriteFinishedEvent e = new WriteFinishedEvent (t);
        t.getHttpContext().getServerImpl().addEvent (e);
    }

    // flush is a pass-through
}
