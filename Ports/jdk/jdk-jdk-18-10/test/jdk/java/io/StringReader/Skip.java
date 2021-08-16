/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4175312
 * @summary Test StringReader.skip with negative param
 */

import java.io.*;

public class Skip {
    public static void main( String argv[] ) throws Exception {
        StringReader in = new StringReader("1234567");

        // Skip forward and read
        if (in.skip(3) != 3)
            throw new RuntimeException("skip(3) failed");
        if (in.read() != '4')
            throw new RuntimeException("post skip read failure");

        // Skip backward and read
        if (in.skip(-2) != -2)
            throw new RuntimeException("skip(-2) failed");
        if (in.read() != '3')
            throw new RuntimeException("read failed after negative skip");

        // Attempt to skip backward past the beginning and read
        if (in.skip(-6) != -3)
            throw new RuntimeException("skip(-6) failed");
        if (in.read() != '1')
            throw new RuntimeException("read after skip past beginning failed");

        // Skip beyond the end
        if (in.skip(30) != 6)
            throw new RuntimeException("skip(30) failed");
        if (in.read() != -1)
            throw new RuntimeException("read at EOF failed");

        // Test after reaching end of string
        if (in.skip(30) != 0)
            throw new RuntimeException("skip(30) failed");
        if (in.skip(-30) != 0)
            throw new RuntimeException("skip(30) failed");
    }
}
