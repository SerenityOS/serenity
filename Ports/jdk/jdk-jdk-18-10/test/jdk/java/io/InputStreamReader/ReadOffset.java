/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4399447
 * @summary Ensure that read offsets work properly
 */

import java.io.*;

public class ReadOffset {

    public static void main(String[] args) throws Exception {
        InputStream is
            = new ByteArrayInputStream("foo bar".getBytes("US-ASCII"));
        InputStreamReader isr = new InputStreamReader(is, "US-ASCII");
        char[] cbuf = new char[100];
        int n;
        System.out.println(n = isr.read(cbuf, 0, 3));
        System.out.println(isr.read(cbuf, n, cbuf.length - n));
    }

}
