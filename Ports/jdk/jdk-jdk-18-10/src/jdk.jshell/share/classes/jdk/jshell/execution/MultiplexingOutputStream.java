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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Packetize an OutputStream, dividing it into named channels.
 *
 * @author Jan Lahoda
 */
class MultiplexingOutputStream extends OutputStream {

    private static final int PACKET_SIZE = 127;
    private final byte[] name;
    private final OutputStream delegate;

    MultiplexingOutputStream(String name, OutputStream delegate) {
        try {
            this.name = name.getBytes("UTF-8");
            this.delegate = delegate;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex); //should not happen
        }
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] {(byte) b});
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (delegate) {
            int i = 0;
            while (len > 0) {
                int size = Math.min(PACKET_SIZE, len);
                byte[] data = new byte[name.length + 1 + size + 1];
                data[0] = (byte) name.length; //assuming the len is small enough to fit into byte
                System.arraycopy(name, 0, data, 1, name.length);
                data[name.length + 1] = (byte) size;
                System.arraycopy(b, off + i, data, name.length + 2, size);
                delegate.write(data);
                i += size;
                len -= size;
            }
            delegate.flush();
        }
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
        delegate.close();
    }

}
