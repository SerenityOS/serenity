/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035063 8054465
 * @summary Tests decoding of String[] into Options.
 *
 * @modules jdk.compiler/com.sun.tools.sjavac
 *          jdk.compiler/com.sun.tools.sjavac.client
 *          jdk.compiler/com.sun.tools.sjavac.comp
 *          jdk.compiler/com.sun.tools.sjavac.options
 * @build Wrapper
 * @run main Wrapper OptionDecoding
 */

import static util.OptionTestUtil.assertEquals;
import static util.OptionTestUtil.checkFilesFound;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.sjavac.CopyFile;
import com.sun.tools.sjavac.Module;
import com.sun.tools.sjavac.Source;
import com.sun.tools.sjavac.client.ClientMain;
import com.sun.tools.sjavac.comp.SjavacImpl;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.options.SourceLocation;

public class OptionDecoding {

    public static void main(String[] args) throws IOException {
        testPaths();
        testDupPaths();
        testSimpleOptions();
        testServerConf();
        testSearchPaths();
        testTranslationRules();
    }

    // Test decoding of output paths
    static void testPaths() throws IOException {
        final String H = "headers";
        final String G = "gensrc";
        final String D = "dest";
        final String stateDir = "stateDir";
        final String CMP = "srcRefList.txt";

        Options options = Options.parseArgs("-h", H, "-s", G, "-d", D, "--state-dir=" + stateDir,
                                            "--compare-found-sources", CMP);

        assertEquals(Paths.get(H).toAbsolutePath(), options.getHeaderDir());
        assertEquals(Paths.get(G).toAbsolutePath(), options.getGenSrcDir());
        assertEquals(Paths.get(D).toAbsolutePath(), options.getDestDir());
        assertEquals(Paths.get(stateDir).toAbsolutePath(), options.getStateDir());
        assertEquals(Paths.get(CMP), options.getSourceReferenceList());
    }

    // Providing duplicate header / dest / gensrc paths should produce an error.
    static void testDupPaths() throws IOException {
        try {
            Options.parseArgs("-h", "dir1", "-h", "dir2");
            throw new RuntimeException("Duplicate header directories should fail.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            Options.parseArgs("-s", "dir1", "-s", "dir2");
            throw new RuntimeException("Duplicate paths for generated sources should fail.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }

        try {
            Options.parseArgs("-d", "dir1", "-d", "dir2");
            throw new RuntimeException("Duplicate destination directories should fail.");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    // Test basic options
    static void testSimpleOptions() {
        Options options = Options.parseArgs("-j", "17", "--log=debug");
        assertEquals(17, options.getNumCores());
        assertEquals("debug", options.getLogLevel());
        assertEquals(false, options.isDefaultPackagePermitted());
        assertEquals(false, options.areUnidentifiedArtifactsPermitted());
        assertEquals(false, options.isUnidentifiedArtifactPermitted(Paths.get("bar.txt").toFile().getAbsolutePath()));

        options = Options.parseArgs("--permit-unidentified-artifacts",
                                    "--permit-artifact=bar.txt",
                                    "--permit-sources-without-package");
        assertEquals("info", options.getLogLevel());
        assertEquals(true, options.isDefaultPackagePermitted());
        assertEquals(true, options.areUnidentifiedArtifactsPermitted());
        assertEquals(true, options.isUnidentifiedArtifactPermitted(Paths.get("bar.txt").toFile().getAbsolutePath()));
    }

    // Test server configuration options
    static void testServerConf() {
        Options options = Options.parseArgs("--server:someServerConfiguration");
        assertEquals("someServerConfiguration", options.getServerConf());
        assertEquals(false, options.startServerFlag());

        options = Options.parseArgs("--startserver:someServerConfiguration");
        assertEquals("someServerConfiguration", options.getServerConf());
        assertEquals(true, options.startServerFlag());
    }

    // Test input paths
    static void testSearchPaths() {
        List<String> i, x, iF, xF;
        i = x = iF = xF = new ArrayList<>();

        SourceLocation dir1 = new SourceLocation(Paths.get("dir1"), i, x);
        SourceLocation dir2 = new SourceLocation(Paths.get("dir2"), i, x);
        String dir1_PS_dir2 = "dir1" + File.pathSeparator + "dir2";

        Options options = Options.parseArgs("--source-path", dir1_PS_dir2);
        assertEquals(options.getSourceSearchPaths(), Arrays.asList(dir1, dir2));

        options = Options.parseArgs("--module-path", dir1_PS_dir2);
        assertEquals(options.getModuleSearchPaths(), Arrays.asList(dir1, dir2));

        options = Options.parseArgs("--class-path", dir1_PS_dir2);
        assertEquals(options.getClassSearchPath(), Arrays.asList(dir1, dir2));
    }

    // Test -tr option
    static void testTranslationRules() {
        Class<?> cls = com.sun.tools.sjavac.CompileJavaPackages.class;

        Options options = Options.parseArgs(
                "-tr", ".exa=" + cls.getName(),
                "-tr", ".exb=" + cls.getName(),
                "-copy", ".html");

        assertEquals(cls, options.getTranslationRules().get(".exa").getClass());
        assertEquals(cls, options.getTranslationRules().get(".exb").getClass());
        assertEquals(CopyFile.class, options.getTranslationRules().get(".html").getClass());
    }
}
