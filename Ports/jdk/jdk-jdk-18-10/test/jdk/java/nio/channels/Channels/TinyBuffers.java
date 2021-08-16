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
 * @bug 4654722
 * @summary Ensure that ridiculously tiny buffers work with
 *          Channels.newReader
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;


public class TinyBuffers {

    private static Charset cs = Charset.forName("UTF-16");

    private static void test(int sz) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(new byte[100]);
        ReadableByteChannel ch = Channels.newChannel(bis);
        Reader r = Channels.newReader(ch, cs.newDecoder(), sz);
        char [] arr = new char[100];
        System.out.println(r.read(arr, 0, arr.length));
    }

    public static void main(String[] args) throws Exception {
        for (int i = -2; i < 10; i++)
            test(i);
    }

}
