/*
 * Copyright 2014 Google Inc.  All Rights Reserved.
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.Test;

/**
 * @test
 * @bug 8058520 8196748
 * @summary jar tf and jar xf should work on zip files with leading garbage
 * @library /test/lib
 * @run testng LeadingGarbage
 */
@Test
public class LeadingGarbage {

    final File[] files = { new File("a"), new File("b") };
    final File normalZip = new File("normal.zip");
    final File leadingGarbageZip = new File("leadingGarbage.zip");

    void createFile(File f) throws IOException {
        try (OutputStream fos = new FileOutputStream(f)) {
            fos.write(f.getName().getBytes("UTF-8"));
        }
    }

    void createFiles() throws IOException {
        for (File file : files)
            createFile(file);
    }

    void deleteFiles() throws IOException {
        for (File file : files)
            assertTrue(file.delete());
    }

    void assertFilesExist() throws IOException {
        for (File file : files)
            assertTrue(file.exists());
    }

    void createNormalZip() throws Throwable {
        createFiles();
        OutputAnalyzer a = jar("c0Mf", "normal.zip", "a", "b");
        a.shouldHaveExitValue(0);
        a.stdoutShouldMatch("\\A\\Z");
        a.stderrShouldMatchIgnoreVMWarnings("\\A\\Z");
        deleteFiles();
    }

    void createZipWithLeadingGarbage() throws Throwable {
        createNormalZip();
        createFile(leadingGarbageZip);
        try (OutputStream fos = new FileOutputStream(leadingGarbageZip, true)) {
            Files.copy(normalZip.toPath(), fos);
        }
        assertTrue(normalZip.length() < leadingGarbageZip.length());
        assertTrue(normalZip.delete());
    }

    public void test_canList() throws Throwable {
        createNormalZip();
        assertCanList("normal.zip");
    }

    public void test_canListWithLeadingGarbage() throws Throwable {
        createZipWithLeadingGarbage();
        assertCanList("leadingGarbage.zip");
    }

    void assertCanList(String zipFileName) throws Throwable {
        OutputAnalyzer a = jar("tf", zipFileName);
        a.shouldHaveExitValue(0);
        StringBuilder expected = new StringBuilder();
        for (File file : files)
            expected.append(file.getName()).append(System.lineSeparator());
        a.stdoutShouldMatch(expected.toString());
        a.stderrShouldMatchIgnoreVMWarnings("\\A\\Z");
    }

    public void test_canExtract() throws Throwable {
        createNormalZip();
        assertCanExtract("normal.zip");
    }

    public void test_canExtractWithLeadingGarbage() throws Throwable {
        createZipWithLeadingGarbage();
        assertCanExtract("leadingGarbage.zip");
    }

    void assertCanExtract(String zipFileName) throws Throwable {
        OutputAnalyzer a = jar("xf", zipFileName);
        a.shouldHaveExitValue(0);
        a.stdoutShouldMatch("\\A\\Z");
        a.stderrShouldMatchIgnoreVMWarnings("\\A\\Z");
        assertFilesExist();
    }

    OutputAnalyzer jar(String... args) throws Throwable {
        String jar = JDKToolFinder.getJDKTool("jar");
        List<String> cmds = new ArrayList<>();
        cmds.add(jar);
        cmds.addAll(Utils.getForwardVmOptions());
        Stream.of(args).forEach(x -> cmds.add(x));
        ProcessBuilder p = new ProcessBuilder(cmds);
        return ProcessTools.executeCommand(p);
    }
}
