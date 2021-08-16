/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.http;
import java.io.*;

/**
 * A Simple FilterInputStream subclass to capture HTTP traffic.
 * Every byte read is also passed to the HttpCapture class.
 *
 * @author jccollet
 */
public class HttpCaptureInputStream extends FilterInputStream {
    private HttpCapture capture = null;

    public HttpCaptureInputStream(InputStream in, HttpCapture cap) {
        super(in);
        capture = cap;
    }

    @Override
    public int read() throws IOException {
        int i = super.read();
        capture.received(i);
        return i;
    }

    @Override
    public void close() throws IOException {
        try {
            capture.flush();
        } catch (IOException iOException) {
        }
        super.close();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int ret = super.read(b);
        for (int i = 0; i < ret; i++) {
            capture.received(b[i]);
        }
        return ret;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = super.read(b, off, len);
        for (int i = 0; i < ret; i++) {
            capture.received(b[off+i]);
        }
        return ret;
    }
}
