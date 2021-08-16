/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml.impl;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * UTF-8 transformed UCS-2 character stream reader.
 *
 * This reader converts UTF-8 transformed UCS-2 characters to Java characters.
 * The UCS-2 subset of UTF-8 transformation is described in RFC-2279 #2
 * "UTF-8 definition":
 *  0000 0000-0000 007F   0xxxxxxx
 *  0000 0080-0000 07FF   110xxxxx 10xxxxxx
 *  0000 0800-0000 FFFF   1110xxxx 10xxxxxx 10xxxxxx
 *
 * This reader will return incorrect last character on broken UTF-8 stream.
 */
public class ReaderUTF8 extends Reader {

    private InputStream is;

    /**
     * Constructor.
     *
     * @param is A byte input stream.
     */
    public ReaderUTF8(InputStream is) {
        this.is = is;
    }

    /**
     * Reads characters into a portion of an array.
     *
     * @param cbuf Destination buffer.
     * @param off Offset at which to start storing characters.
     * @param len Maximum number of characters to read.
     * @exception IOException If any IO errors occur.
     * @exception UnsupportedEncodingException If UCS-4 character occur in the stream.
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        int num = 0;
        int val;
        while (num < len) {
            if ((val = is.read()) < 0) {
                return (num != 0) ? num : -1;
            }
            switch (val & 0xf0) {
                case 0xc0:
                case 0xd0:
                    cbuf[off++] = (char) (((val & 0x1f) << 6) | (is.read() & 0x3f));
                    break;

                case 0xe0:
                    cbuf[off++] = (char) (((val & 0x0f) << 12)
                            | ((is.read() & 0x3f) << 6) | (is.read() & 0x3f));
                    break;

                case 0xf0:      // UCS-4 character
                    throw new UnsupportedEncodingException("UTF-32 (or UCS-4) encoding not supported.");

                default:
                    cbuf[off++] = (char) val;
                    break;
            }
            num++;
        }
        return num;
    }

    /**
     * Reads a single character.
     *
     * @return The character read, as an integer in the range 0 to 65535
     *  (0x00-0xffff), or -1 if the end of the stream has been reached.
     * @exception IOException If any IO errors occur.
     * @exception UnsupportedEncodingException If UCS-4 character occur in the stream.
     */
    public int read() throws IOException {
        int val;
        if ((val = is.read()) < 0) {
            return -1;
        }
        switch (val & 0xf0) {
            case 0xc0:
            case 0xd0:
                val = ((val & 0x1f) << 6) | (is.read() & 0x3f);
                break;

            case 0xe0:
                val = ((val & 0x0f) << 12)
                        | ((is.read() & 0x3f) << 6) | (is.read() & 0x3f);
                break;

            case 0xf0:  // UCS-4 character
                throw new UnsupportedEncodingException();

            default:
                break;
        }
        return val;
    }

    /**
     * Closes the stream.
     *
     * @exception IOException If any IO errors occur.
     */
    public void close() throws IOException {
        is.close();
    }
}
