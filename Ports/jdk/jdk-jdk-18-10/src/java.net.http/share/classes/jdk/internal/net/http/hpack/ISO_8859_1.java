/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import java.io.IOException;
import java.nio.ByteBuffer;

//
// Custom implementation of ISO/IEC 8859-1:1998
//
// The rationale behind this is not to deal with CharsetEncoder/CharsetDecoder,
// basically because it would require wrapping every single CharSequence into a
// CharBuffer and then copying it back.
//
// But why not to give a CharBuffer instead of Appendable? Because I can choose
// an Appendable (e.g. StringBuilder) that adjusts its length when needed and
// therefore not to deal with pre-sized CharBuffers or copying.
//
// The encoding is simple and well known: 1 byte <-> 1 char
//
final class ISO_8859_1 {

    private ISO_8859_1() { }

    public static final class Reader {

        private final HPACK.BufferUpdateConsumer UPDATER =
                (buf, bufLen) -> {
                    buffer = buf;
                    bufferLen = bufLen;
                };

        private long buffer;
        private int bufferLen;

        public void read(ByteBuffer source, Appendable destination)
                throws IOException
        {
            while (true) {
                int nBytes = HPACK.read(source, buffer, bufferLen, UPDATER);
                if (nBytes == 0) {
                    return;
                }
                assert bufferLen % 8 == 0 : bufferLen;
                while (bufferLen > 0) {
                    char c = (char) (buffer >>> 56);
                    try {
                        destination.append(c);
                    } catch (IOException e) {
                        throw new IOException(
                                "Error appending to the destination", e);
                    }
                    buffer <<= 8;
                    bufferLen -= 8;
                }
            }
        }

        public Reader reset() {
            buffer = 0;
            bufferLen = 0;
            return this;
        }
    }

    public static final class Writer {

        private final HPACK.BufferUpdateConsumer UPDATER =
                (buf, bufLen) -> {
                    buffer = buf;
                    bufferLen = bufLen;
                };

        private CharSequence source;
        private int pos;
        private int end;
        private long buffer;
        private int bufferLen;

        public Writer configure(CharSequence source, int start, int end) {
            this.source = source;
            this.pos = start;
            this.end = end;
            return this;
        }

        public boolean write(ByteBuffer destination) {
            while (true) {
                while (true) { // stuff codes into long
                    if (pos >= end) {
                        break;
                    }
                    char c = source.charAt(pos);
                    if (c > 255) {
                        throw new IllegalArgumentException(Integer.toString((int) c));
                    }
                    if (bufferLen <= 56) {
                        buffer |= (((long) c) << (56 - bufferLen)); // append
                        bufferLen += 8;
                        pos++;
                    } else {
                        break;
                    }
                }
                if (bufferLen == 0) {
                    return true;
                }
                int nBytes = HPACK.write(buffer, bufferLen, UPDATER, destination);
                if (nBytes == 0) {
                    return false;
                }
            }
        }

        public Writer reset() {
            source = null;
            pos = -1;
            end = -1;
            buffer = 0;
            bufferLen = 0;
            return this;
        }
    }
}
