/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test InterpreterMethodEntries
 * @bug 8169711
 * @summary Test interpreter method entries for intrinsics with CDS (class data sharing)
 *          and the intrinsic flag enabled during dump and disabled during use of the archive.
 * @requires vm.cds
 * @library /test/lib
 * @run driver TestInterpreterMethodEntries true false
 */

/**
 * @test InterpreterMethodEntries
 * @bug 8169711
 * @summary Test interpreter method entries for intrinsics with CDS (class data sharing)
 *          and the intrinsic flag disabled during dump and enabled during use of the archive.
 * @requires vm.cds
 * @library /test/lib
 * @run driver TestInterpreterMethodEntries false true
 */

import java.lang.Math;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class TestInterpreterMethodEntries {

    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
          boolean dump = Boolean.parseBoolean(args[0]);
          boolean use  = Boolean.parseBoolean(args[1]);

          // Dump and use shared archive with different flag combinations
          dumpAndUseSharedArchive(dump ? "+" : "-", use ? "+" : "-");
        } else {
          // Call intrinsified java.lang.Math::fma()
          Math.fma(1.0, 2.0, 3.0);

          byte[] buffer = new byte[256];
          // Call intrinsified java.util.zip.CRC32::update()
          CRC32 crc32 = new CRC32();
          crc32.update(buffer, 0, 256);

          // Call intrinsified java.util.zip.CRC32C::updateBytes(..)
          CRC32C crc32c = new CRC32C();
          crc32c.update(buffer, 0, 256);
        }
    }

    private static void dumpAndUseSharedArchive(String dump, String use) throws Exception {
        String unlock     = "-XX:+UnlockDiagnosticVMOptions";

        String dumpFMA    = "-XX:" + dump + "UseFMA";
        String dumpCRC32  = "-XX:" + dump + "UseCRC32Intrinsics";
        String dumpCRC32C = "-XX:" + dump + "UseCRC32CIntrinsics";
        String useFMA     = "-XX:" + use  + "UseFMA";
        String useCRC32   = "-XX:" + use  + "UseCRC32Intrinsics";
        String useCRC32C  = "-XX:" + use  + "UseCRC32CIntrinsics";

        CDSTestUtils.createArchiveAndCheck(unlock, dumpFMA, dumpCRC32, dumpCRC32C);

        CDSOptions opts = (new CDSOptions())
            .addPrefix(unlock, useFMA, useCRC32, useCRC32C, "-showversion")
            .addSuffix("TestInterpreterMethodEntries", "run")
            .setUseVersion(false);
        CDSTestUtils.runWithArchiveAndCheck(opts);
    }
}

