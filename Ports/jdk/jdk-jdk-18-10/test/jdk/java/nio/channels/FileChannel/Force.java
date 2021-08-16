/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4434115 4802789
 * @summary Check for regressions in FileChannel.force
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;


public class Force {
    public static void main(String[] args) throws Exception {
        writeAfterForce();
        forceReadableOnly();
    }

    // 4434115: FileChannel.write() fails when preceded by force() operation
    private static void writeAfterForce() throws Exception {
        byte[] srcData = new byte[20];
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        FileOutputStream fis = new FileOutputStream(blah);
        FileChannel fc = fis.getChannel();
        fc.write(ByteBuffer.wrap(srcData));
        fc.force(false);
        fc.write(ByteBuffer.wrap(srcData));
        fc.close();
    }

    // 4802789: FileChannel.force(true) throws IOException (windows)
    private static void forceReadableOnly() throws Exception {
        File f = File.createTempFile("blah", null);
        f.deleteOnExit();
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        fc.force(true);
        fc.close();
        fis.close();
    }
}
