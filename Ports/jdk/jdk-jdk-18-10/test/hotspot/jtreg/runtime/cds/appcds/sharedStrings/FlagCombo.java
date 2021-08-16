/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @summary Test relevant combinations of command line flags with shared strings
 * @requires vm.cds.archived.java.heap & vm.hasJFR
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build HelloString
 * @run driver FlagCombo
 */

/**
 * @test
 * @summary Test relevant combinations of command line flags with shared strings
 * @comment A special test excluding the case that requires JFR
 * @requires vm.cds.archived.java.heap & !vm.hasJFR
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build HelloString
 * @run driver FlagCombo noJfr
 */

import jdk.test.lib.BuildHelper;
import jdk.test.lib.Platform;

public class FlagCombo {
    public static void main(String[] args) throws Exception {
        SharedStringsUtils.buildJar("HelloString");

        SharedStringsUtils.dump(TestCommon.list("HelloString"),
            "SharedStringsBasic.txt", "-Xlog:cds,cds+hashtables");

        SharedStringsUtils.runWithArchive("HelloString", "-XX:+UseG1GC");

        if (args.length == 0) {
            SharedStringsUtils.runWithArchiveAuto("HelloString",
                "-XX:StartFlightRecording:dumponexit=true");
        }

        SharedStringsUtils.runWithArchive("HelloString", "-XX:+UnlockDiagnosticVMOptions",
           "-XX:NativeMemoryTracking=detail", "-XX:+PrintNMTStatistics");
    }
}
