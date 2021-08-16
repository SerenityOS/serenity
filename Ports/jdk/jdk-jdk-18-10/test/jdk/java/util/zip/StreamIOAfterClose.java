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
   @bug 4110528 4112112 4112103
   @summary Test if zip related in/output streams will
            prevent i/o after stream has been closed.
   */
import java.util.zip.*;
import java.util.jar.*;
import java.io.*;

public class StreamIOAfterClose {
    // compressed stub data
    private static byte[] compressed = {
        31,-117,8,0,0,0,0,0,0,0,-85,-107,-79,74,85,97,117,48,
        -9,117,-47,114,15,-87,-27,-9,-54,-48,49,-108,17,19,20,
        118,-87,-84,78,-15,-10,-87,-112,51,115,16,85,81,54,11,
        114,44,11,98,116,17,102,-10,72,-10,80,-79,101,14,47,-50,
        16,117,-9,-83,16,13,-55,83,83,103,-30,-117,-42,-82,-105,
        -119,46,-16,20,-111,-85,-16,-48,54,79,-53,-76,116,
        -80,-78,77,-88,-85,50,113,-54,15,-74,-28,-44,101,-43,47,
        85,54,-74,1,0,85,69,28,117,100,0,0,0
    };

    private static void testRead(InputStream in) throws Exception {
        in.close();
        try {
            in.read();
            throw new Exception("read allowed after stream is closed");
        } catch (IOException e) {
        }
    }

    private static void testWrite(ZipOutputStream out) throws Exception {
        out.close();
        try {
            out.putNextEntry(new ZipEntry(""));
            throw new Exception("write allowed after stream is closed");
        } catch (IOException e) {
        }
    }

    public static void main(String argv[]) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new ByteArrayOutputStream());
        zos.putNextEntry(new ZipEntry("1"));
        testWrite(zos);

        JarOutputStream jos = new JarOutputStream(new ByteArrayOutputStream());
        jos.putNextEntry(new ZipEntry("1"));
        testWrite(jos);

        InputStream bis = new ByteArrayInputStream(new byte[10]);
        InputStream bis1 = new ByteArrayInputStream(compressed);
        testRead(new ZipInputStream(bis));
        testRead(new GZIPInputStream(bis1));
    }
}
