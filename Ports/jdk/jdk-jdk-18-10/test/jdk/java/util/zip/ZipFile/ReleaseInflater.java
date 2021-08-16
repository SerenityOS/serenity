/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4214795
 * @summary Make sure the same inflater will only be recycled
 *          once.
 */

import java.io.*;
import java.util.zip.*;

public class ReleaseInflater {

    public static void main(String[] args) throws Exception {
        ZipFile zf = new ZipFile(new File(System.getProperty("test.src"),
                                          "input.jar"));
        ZipEntry e = zf.getEntry("ReleaseInflater.java");

        InputStream in1 = zf.getInputStream(e);
        // close the stream, the inflater will be released
        in1.close();
        // close the stream again, should be no-op
        in1.close();

        // create two new streams, allocating inflaters
        InputStream in2 = zf.getInputStream(e);
        InputStream in3 = zf.getInputStream(e);

        // check to see if they influence each other
        if (in2.read() != in3.read()) {
            throw new Exception("Stream is corrupted!");
        }
    }
}
