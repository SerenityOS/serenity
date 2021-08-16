/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires os.family == "Linux"
 * @bug 4863423
 * @summary Test Util caching policy
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class BigReadWrite {

    static int testSize = 15;

    public static void main(String[] args) throws Exception {
        FileOutputStream fos = new FileOutputStream("/dev/zero");
        FileChannel fc = fos.getChannel();

        // Three small writes to fill up the Util cache
        ByteBuffer buf = ByteBuffer.allocate(900);
        fc.write(buf);
        buf = ByteBuffer.allocate(950);
        fc.write(buf);
        buf = ByteBuffer.allocate(975);
        fc.write(buf);
        buf = ByteBuffer.allocate(4419000);

        // Now initiate large write to create larger direct buffers
        long iterations = 0;
        while (iterations < 50) {
            fc.write(buf);
            buf.rewind();
            iterations++;
        }
        // Clean up
        fc.close();
    }
}
