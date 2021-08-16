/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4697605 4741233
 * @summary Check decoder behaves correctly in ISO2022_JP
 * @modules jdk.charsets
 */

import java.io.*;

public class TestTrailingEscapesISO2022JP {

    public static void main(String[] args) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;

        InputStream in =
            new FileInputStream(new File(System.getProperty("test.src", "."),
                                        "ISO2022JP.trailEsc"));
        try {
            byte[] b = new byte[4096];
            while ( ( len = in.read( b, 0, b.length ) ) != -1 ) {
                out.write(b, 0, len);
            }
        } finally {
            in.close();
        }

        Reader inR = new InputStreamReader(new ByteArrayInputStream(
                                                        out.toByteArray()),
                                                       "iso-2022-jp");

        try {
            char[] c = new char[4096];
            while ( ( len = inR.read( c, 0, c.length ) ) != -1 ) {
                System.out.println(len);
                if (len == 0)
                    throw new Exception("Read returned zero!");
            }
        } finally {
            inR.close();
        }
    }
}
