/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4659408 4659410
 * @summary Verify transfers with closed channels throws ClosedChannelException
 */

import java.nio.*;
import java.nio.channels.*;
import java.io.*;

public class ClosedChannelTransfer {
    public static void main (String args []) throws Exception {
        File file = File.createTempFile("test1", null);
        file.deleteOnExit();
        FileChannel channel = (new RandomAccessFile("aaa","rw")).getChannel();
        test1(channel);
        test2(channel);
        channel.close();
        file.delete();
    }

    static void test1(FileChannel channel) throws Exception {
        ByteArrayInputStream istr = new ByteArrayInputStream(
            new byte [] {1, 2, 3, 4}
        );
        ReadableByteChannel rbc = Channels.newChannel(istr);
        rbc.close();
        try {
            channel.transferFrom(rbc, 0, 2);
            throw new Exception("Test1: No ClosedChannelException was thrown");
        } catch (ClosedChannelException cce) {
            // Correct result
        }
    }

    static void test2(FileChannel channel) throws Exception {
        ByteArrayOutputStream istr = new ByteArrayOutputStream(4);
        WritableByteChannel wbc = Channels.newChannel(istr);
        wbc.close();
        try {
            channel.transferTo(0, 2, wbc);
            throw new Exception("Test2: No ClosedChannelException was thrown");
        } catch (ClosedChannelException cce) {
            // Correct result
        }
    }
}
