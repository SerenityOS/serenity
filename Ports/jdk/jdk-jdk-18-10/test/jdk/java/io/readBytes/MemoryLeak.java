/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6791458
 * @summary Reading from closed input files leaks native memory
 */

import java.io.*;

public class MemoryLeak {
    public static void main(String[] args) throws Throwable {
        byte[] bytes = new byte[1 << 20];
        String dir = System.getProperty("test.src", ".");
        File testFile = new File(dir, "input.txt");
        FileInputStream s = new FileInputStream(testFile);
        s.close();
        for (int i = 0; i < 10000; i++) {
            try {
                s.read(bytes);
                throw new Error("expected IOException");
            } catch (IOException expected) {
                /* OK */
            } catch (OutOfMemoryError oome) {
                System.out.printf("Got OutOfMemoryError, i=%d%n", i);
                throw oome;
            }
        }
    }
}
