/*
 * Copyright 2009 Google Inc.  All Rights Reserved.
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

import java.util.*;
import java.io.*;

/**
 * A manual test that demonstrates the ability to start a subprocess
 * on Linux without getting ENOMEM.  Run this test like:
 *
 * java -Xmx7000m BigFork
 *
 * providing a -Xmx flag suitable for your operating environment.
 * Here's the bad old behavior:
 *
 * ==> java -Xmx7000m -esa -ea BigFork
 * -------
 * CommitLimit:   6214700 kB
 * Committed_AS:  2484452 kB
 * -------
 * size=4.6GB
 * -------
 * CommitLimit:   6214700 kB
 * Committed_AS:  7219680 kB
 * -------
 * Exception in thread "main" java.io.IOException: Cannot run program "/bin/true": java.io.IOException: error=12, Cannot allocate memory
 *         at java.lang.ProcessBuilder.start(ProcessBuilder.java:1018)
 *         at BigFork.main(BigFork.java:79)
 * Caused by: java.io.IOException: java.io.IOException: error=12, Cannot allocate memory
 *         at java.lang.UNIXProcess.<init>(UNIXProcess.java:190)
 *         at java.lang.ProcessImpl.start(ProcessImpl.java:128)
 *         at java.lang.ProcessBuilder.start(ProcessBuilder.java:1010)
 *         ... 1 more
 */
public class BigFork {
    static final Random rnd = new Random();
    static void touchPages(byte[] chunk) {
        final int pageSize = 4096;
        for (int i = 0; i < chunk.length; i+= pageSize) {
            chunk[i] = (byte) rnd.nextInt();
        }
    }

    static void showCommittedMemory() throws IOException {
        BufferedReader r =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream("/proc/meminfo")));
        System.out.println("-------");
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("Commit")) {
                System.out.printf("%s%n", line);
            }
        }
        System.out.println("-------");
    }

    public static void main(String[] args) throws Throwable {
        showCommittedMemory();

        final int chunkSize = 1024 * 1024 * 100;
        List<byte[]> chunks = new ArrayList<byte[]>(100);
        try {
            for (;;) {
                byte[] chunk = new byte[chunkSize];
                touchPages(chunk);
                chunks.add(chunk);
            }
        } catch (OutOfMemoryError e) {
            chunks.set(0, null);        // Free up one chunk
            System.gc();
            int size = chunks.size();
            System.out.printf("size=%.2gGB%n", (double)size/10);

            showCommittedMemory();

            // Can we fork/exec in our current bloated state?
            Process p = new ProcessBuilder("/bin/true").start();
            p.waitFor();
        }
    }
}
