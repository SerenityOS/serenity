/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8241071
 * @summary The same JDK build should always generate the same archive file (no randomness).
 * @requires vm.cds
 * @library /test/lib
 * @run driver DeterministicDump
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.Platform;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class DeterministicDump {
    public static void main(String[] args) throws Exception {
        doTest(false);

        if (Platform.is64bit()) {
            // There's no oop/klass compression on 32-bit.
            doTest(true);
        }
    }

    public static void doTest(boolean compressed) throws Exception {
        ArrayList<String> baseArgs = new ArrayList<>();

        // Use the same heap size as make/Images.gmk
        baseArgs.add("-Xmx128M");

        if (Platform.is64bit()) {
            // These options are available only on 64-bit.
            String sign = (compressed) ?  "+" : "-";
            baseArgs.add("-XX:" + sign + "UseCompressedOops");
            baseArgs.add("-XX:" + sign + "UseCompressedClassPointers");
        }

        String baseArchive = dump(baseArgs);

        // (1) Dump with the same args. Should produce the same archive.
        String baseArchive2 = dump(baseArgs);
        compare(baseArchive, baseArchive2);

        // (2) This will cause the archive to be relocated during dump time. We should
        //     still get the same bits. This simulates relocation that happens when
        //     Address Space Layout Randomization prevents the archive space to
        //     be mapped at the default location.
        String relocatedArchive = dump(baseArgs, "-XX:+UnlockDiagnosticVMOptions", "-XX:ArchiveRelocationMode=1");
        compare(baseArchive, relocatedArchive);
    }

    static int id = 0;
    static String dump(ArrayList<String> args, String... more) throws Exception {
        String logName = "SharedArchiveFile" + (id++);
        String archiveName = logName + ".jsa";
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-Xlog:cds=debug")
            .setArchiveName(archiveName)
            .addSuffix(more);
        CDSTestUtils.createArchiveAndCheck(opts);

        return archiveName;
    }

    static void compare(String file0, String file1) throws Exception {
        byte[] buff0 = new byte[4096];
        byte[] buff1 = new byte[4096];
        try (FileInputStream in0 = new FileInputStream(file0);
             FileInputStream in1 = new FileInputStream(file1)) {
            int total = 0;
            while (true) {
                int n0 = read(in0, buff0);
                int n1 = read(in1, buff1);
                if (n0 != n1) {
                    throw new RuntimeException("File contents (file sizes?) are different after " + total + " bytes; n0 = "
                                               + n0 + ", n1 = " + n1);
                }
                if (n0 == 0) {
                    System.out.println("File contents are the same: " + total + " bytes");
                    break;
                }
                for (int i = 0; i < n0; i++) {
                    byte b0 = buff0[i];
                    byte b1 = buff1[i];
                    if (b0 != b1) {
                        throw new RuntimeException("File content different at byte #" + (total + i) + ", b0 = " + b0 + ", b1 = " + b1);
                    }
                }
                total += n0;
            }
        }
    }

    static int read(FileInputStream in, byte[] buff) throws IOException {
        int total = 0;
        while (total < buff.length) {
            int n = in.read(buff, total, buff.length - total);
            if (n <= 0) {
                return total;
            }
            total += n;
        }

        return total;
    }
}
