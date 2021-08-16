/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * IOUtils: A collection of IO-related public static methods.
 */

package sun.security.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils {

    /**
     * Read exactly {@code length} of bytes from {@code in}.
     *
     * <p> Note that this method is safe to be called with unknown large
     * {@code length} argument. The memory used is proportional to the
     * actual bytes available. An exception is thrown if there are not
     * enough bytes in the stream.
     *
     * @param is input stream, must not be null
     * @param length number of bytes to read
     * @return bytes read
     * @throws EOFException if there are not enough bytes in the stream
     * @throws IOException if an I/O error occurs or {@code length} is negative
     * @throws OutOfMemoryError if an array of the required size cannot be
     *         allocated.
     */
    public static byte[] readExactlyNBytes(InputStream is, int length)
            throws IOException {
        if (length < 0) {
            throw new IOException("length cannot be negative: " + length);
        }
        byte[] data = is.readNBytes(length);
        if (data.length < length) {
            throw new EOFException();
        }
        return data;
    }
}
