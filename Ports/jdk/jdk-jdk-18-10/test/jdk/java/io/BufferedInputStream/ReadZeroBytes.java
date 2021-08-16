/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
 * @bug 5041041
 * @summary Test BufferedInputStream read of zero byte array
 */

import java.io.*;

/**
 * This class tests to see if BufferedInputStream of zero length array
 * invokes the read method or not. Invoking read could block which is
 * incompatible behavior for zero length array.
 */
public class ReadZeroBytes {

    public static void main( String argv[] ) throws Exception {
        BufferedInputStream in = new BufferedInputStream(
            new ThrowingInputStream());
        in.read(new byte[0], 0, 0);
    }
}

class ThrowingInputStream extends InputStream {

    public ThrowingInputStream() {
    }
    public int read() throws IOException {
        return 0;
    }
    public int read(byte[] b, int off, int len) throws IOException {
        throw new RuntimeException("Read invoked for len == 0");
    }
}
