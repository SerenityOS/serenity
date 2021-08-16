/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4072173
   @summary Test BufferdInputStream close method */

import java.io.*;

/**
 * This class tests to see if BufferedInputStream closes
 * properly
 */

public class CloseStream {

    public static void main( String argv[] ) throws Exception {
        BufferedInputStream in = new BufferedInputStream(new MyInputStream());

        in.read();
        in.close();

        try {
            in.read(); // IOException should be thrown here
            throw new RuntimeException("No exception during read on closed stream");
        }
        catch (IOException e) {
            System.err.println("Test passed: IOException is thrown");
        }
    }
}

class MyInputStream extends InputStream {

    public MyInputStream() {
    }

    public void close() throws IOException {
        if (status == OPEN) {
            status = CLOSED;
        } else throw new IOException();
    }

    public int read() throws IOException {
        if (status == CLOSED)
            throw new IOException();
        return (byte)'a';
    }

    private final int OPEN = 1;
    private final int CLOSED = 2;
    private int status = OPEN;
}
