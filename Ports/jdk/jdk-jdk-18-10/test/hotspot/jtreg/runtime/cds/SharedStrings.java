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
 */

/**
 * @test
 * @summary Check to make sure that shared strings in the bootstrap CDS archive
 *          are actually shared
 * @requires vm.cds.archived.java.heap
 * @library /test/lib
 * @build SharedStringsWb sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar whitebox.jar sun.hotspot.WhiteBox
 * @run driver SharedStrings
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class SharedStrings {
    public static void main(String[] args) throws Exception {
        // Note: This is a basic sanity test for Shared Strings feature.
        // This also serves as a reference on how to use this feature,
        // hence the command lines are spelled out instead of using the
        // test utils methods.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:SharedArchiveFile=./SharedStrings.jsa",
            "-Xlog:cds,cds+hashtables",
            // Needed for bootclasspath match, for CDS to work with WhiteBox API
            "-Xbootclasspath/a:" + ClassFileInstaller.getJarPath("whitebox.jar"),
            "-Xshare:dump");

        OutputAnalyzer out = CDSTestUtils.executeAndLog(pb, "dump");
        CDSTestUtils.checkDump(out, "Shared string table stats");


        pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:SharedArchiveFile=./SharedStrings.jsa",
                // needed for access to white box test API
                "-Xbootclasspath/a:" + ClassFileInstaller.getJarPath("whitebox.jar"),
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
                "-Xshare:on", "-showversion", "SharedStringsWb");

        out = CDSTestUtils.executeAndLog(pb, "exec");
        CDSTestUtils.checkExec(out);
    }
}
