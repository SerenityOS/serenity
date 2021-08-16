/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that if InputStream.read returns 0 we throw an exception.
 * @bug 4684515
 */

import java.io.*;

public class ReadZero {

    public static void main(String [] args) throws IOException {
        ReadZero r = new ReadZero();
        r.testInputStream();
    }

    private void testInputStream() throws IOException {
        File f = new File(System.getProperty("test.src", "."), "ReadZero.java");
        InputStream is = new FileInputStream(f) {
            public int read(byte [] b, int off, int len) {
                System.out.println("FileInputStream.read");
                return 0;
            }
        };
        try {
            is.read(new byte[1], 0, 1); // ok
            InputStreamReader isr = new InputStreamReader(is);

            try {
                int res = isr.read(new char[1], 0, 1);
            } catch (IOException x) {
                System.out.println("IOException caught");
                return;
            }
        } finally {
            is.close();
        }
        throw new RuntimeException("IOException not thrown");
    }
}
