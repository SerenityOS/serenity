/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4091757 6652379 8177809
   @requires os.maxMemory >= 16G
   @summary Basic test for setLastModified method
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class SetLastModified {

    private static void ck(File f, long nt, long rt) throws Exception {
        if (rt == nt) return;
        if ((rt / 10 == nt / 10)
            || (rt / 100 == nt / 100)
            || (rt / 1000 == nt / 1000)
            || (rt / 10000 == (nt / 10000))) {
            System.err.println(f + ": Time set to " + nt
                               + ", rounded down by filesystem to " + rt);
            return;
        }
        if ((rt / 10 == (nt + 5) / 10)
            || (rt / 100 == (nt + 50) / 100)
            || (rt / 1000 == (nt + 500) / 1000)
            || (rt / 10000 == ((nt + 5000) / 10000))) {
            System.err.println(f + ": Time set to " + nt
                               + ", rounded up by filesystem to " + rt);
            return;
        }
        throw new Exception(f + ": Time set to " + nt
                            + ", then read as " + rt);
    }

    public static void main(String[] args) throws Exception {
        File d = new File(System.getProperty("test.dir", "."));
        File d2 = new File(d, "x.SetLastModified.dir");
        File f = new File(d2, "x.SetLastModified");
        long ot, t;

        /* New time: One week ago */
        long nt = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;

        if (f.exists()) f.delete();
        if (d2.exists()) d2.delete();
        if (!d2.mkdir()) {
            throw new Exception("Can't create test directory " + d2);
        }

        boolean threw = false;
        try {
            d2.setLastModified(-nt);
        } catch (IllegalArgumentException x) {
            threw = true;
        }
        if (!threw)
            throw new Exception("setLastModified succeeded with a negative time");

        ot = d2.lastModified();
        if (ot != 0) {
            if (d2.setLastModified(nt)) {
                ck(d2, nt, d2.lastModified());
                d2.setLastModified(ot);
            } else {
                System.err.println("Warning: setLastModified on directories "
                                   + "not supported");
            }
        }

        if (f.exists()) {
            if (!f.delete())
                throw new Exception("Can't delete test file " + f);
        }
        if (f.setLastModified(nt))
            throw new Exception("Succeeded on non-existent file: " + f);

        // set/check last modified on files of size 1, 1GB+1, 2GB+1, ..
        // On Windows we only test with a tiny file as that platform doesn't
        // support sparse files by default and so the test takes too long.
        final long G = 1024L * 1024L * 1024L;
        final long MAX_POSITION =
            System.getProperty("os.name").startsWith("Windows") ? 0L : 3L*G;
        long pos = 0L;
        while (pos <= MAX_POSITION) {
            try (FileChannel fc = new FileOutputStream(f).getChannel()) {
                fc.position(pos).write(ByteBuffer.wrap("x".getBytes()));
            }
            ot = f.lastModified();
            System.out.format("check with file size: %d\n", f.length());
            if (!f.setLastModified(nt))
                throw new Exception("setLastModified failed on file: " + f);
            ck(f, nt, f.lastModified());
            pos += G;
        }

        if (!f.delete()) throw new Exception("Can't delete test file " + f);
        if (!d2.delete()) throw new Exception("Can't delete test directory " + d2);
    }

}
