/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6304463
 * @summary Test whether the zip file still can be read after thread is interrupted
 */

import java.io.*;
import java.util.zip.*;

public class InterruptibleZip {

    public static void main(String[] args) throws Exception {
        /* Interrupt the current thread. The is.read call below
           should continue reading input.jar.
        */
        Thread.currentThread().interrupt();
        ZipFile zf = new ZipFile(new File(System.getProperty("test.src", "."), "input.jar"));
        ZipEntry ze = zf.getEntry("Available.java");
        InputStream is = zf.getInputStream(ze);
        byte[] buf = new byte[512];
        int n = is.read(buf);
        boolean interrupted = Thread.interrupted();
        System.out.printf("interrupted=%s n=%d name=%s%n",
                          interrupted, n, ze.getName());
        if (! interrupted) {
            throw new Error("Wrong interrupt status");
        }
        if (n != buf.length) {
            throw new Error("Read error");
        }
    }
}
