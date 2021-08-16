/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test read method of FileChannel
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;


/**
 * Testing a read into buffers that have no room.
 */
public class ReadFull {

    public static void main(String[] args) throws Exception {

        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        ByteBuffer[] dstBuffers = new ByteBuffer[10];
        for(int i=0; i<10; i++) {
            dstBuffers[i] = ByteBuffer.allocateDirect(10);
            dstBuffers[i].position(10);
        }
        FileInputStream fis = new FileInputStream(blah);
        FileChannel fc = fis.getChannel();

       // No space left in buffers, this should return 0
        long bytesRead = fc.read(dstBuffers) ;
        if (bytesRead != 0)
            throw new RuntimeException("Nonzero return from read");

        fc.close();
        fis.close();
        blah.delete();
    }
}
