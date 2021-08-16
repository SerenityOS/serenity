/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for RandomAccessFile open-sync modes
 */

import java.io.*;


public class OpenSync {

    static PrintStream log = System.err;

    public static void main(String[] args) throws Exception {

        File blah = File.createTempFile("OpenSync", null);
        blah.deleteOnExit();

        String[] badModes = { "d", "s", "rd", "rs", "rwx", "foo" };
        for (int i = 0; i < badModes.length; i++) {
            String mode = badModes[i];
            try {
                new RandomAccessFile(blah, mode);
            } catch (IllegalArgumentException x) {
                log.println("Mode \"" + mode +"\": Thrown as expected: "
                            + x.getClass().getName());
                log.println("  " + x.getMessage());
                continue;
            }
            throw new Exception("Exception not thrown for illegal mode "
                                + mode);
        }

        new RandomAccessFile(blah, "rw").close();
        new RandomAccessFile(blah, "r").close();

        String hi = "Hello, world!";
        RandomAccessFile raf = new RandomAccessFile(blah, "rws");
        raf.writeUTF(hi);
        raf.close();

        raf = new RandomAccessFile(blah, "rwd");
        if (!raf.readUTF().equals(hi))
            throw new Exception("File content mismatch");
        raf.close();

    }

}
