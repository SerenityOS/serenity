/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * a class which allows the caller to write an indefinite
 * number of bytes to an underlying stream , but without using
 * chunked encoding. Used for http/1.0 clients only
 * The underlying connection needs to be closed afterwards.
 */

class UndefLengthOutputStream extends FilterOutputStream
{
    private boolean closed = false;
    ExchangeImpl t;

    UndefLengthOutputStream (ExchangeImpl t, OutputStream src) {
        super (src);
        this.t = t;
    }

    public void write (int b) throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        out.write(b);
    }

    public void write (byte[]b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException ("stream closed");
        }
        out.write(b, off, len);
    }

    public void close () throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        flush();
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
