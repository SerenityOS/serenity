/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary The test verifies JFR.dump command
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestJcmdDumpGeneratedFilename
 */
public class TestJcmdDumpGeneratedFilename {

    public static void main(String[] args) throws Exception {
        // Increase the id for a recording
        for (int i = 0; i < 300; i++) {
            new Recording();
        }
        try (Recording r = new Recording(Configuration.getConfiguration("default"))) {
            r.start();
            r.stop();
            testDumpFilename();
            testDumpFilename(r);
            testDumpDiectory();
            testDumpDiectory(r);
        }
    }

    private static void testDumpFilename() throws Exception {
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump");
        verifyFile(JcmdHelper.readFilename(output), null);
    }

    private static void testDumpFilename(Recording r) throws Exception {
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "name=" + r.getId());
        verifyFile(JcmdHelper.readFilename(output), r.getId());
    }

    private static void testDumpDiectory() throws Exception {
        Path directory = Paths.get(".").toAbsolutePath().normalize();
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + directory);
        String filename = JcmdHelper.readFilename(output);
        verifyFile(filename, null);
        verifyDirectory(filename, directory);
    }

    private static void testDumpDiectory(Recording r) throws Exception {
        Path directory = Paths.get(".").toAbsolutePath().normalize();
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "name=" + r.getId(), "filename=" + directory);
        String filename = JcmdHelper.readFilename(output);
        verifyFile(filename, r.getId());
        verifyDirectory(filename, directory);
    }

    private static void verifyDirectory(String filename, Path directory) throws Exception {
        if (!filename.contains(directory.toAbsolutePath().normalize().toString())) {
            throw new Exception("Expected dump to be at " + directory);
        }
    }

    private static void verifyFile(String filename, Long id) throws Exception {
        String idText = id == null ? "" : "-id-" + Long.toString(id);
        String expectedName = "hotspot-pid-" + ProcessHandle.current().pid() + idText;
        if (!filename.contains(expectedName)) {
            throw new Exception("Expected filename to contain " + expectedName);
        }
        FileHelper.verifyRecording(new File(filename));
    }
}
