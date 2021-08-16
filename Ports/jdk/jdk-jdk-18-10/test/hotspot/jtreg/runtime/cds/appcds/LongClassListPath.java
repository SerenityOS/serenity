/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Test the handling of long path to the classlist file.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run driver LongClassListPath
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class LongClassListPath {
    private static final int MAX_PATH = 260;
    public static void main(String[] args) throws Exception {
        String[] classes = {"hello"};
        String classList =
            CDSTestUtils.makeClassList(classes).getPath();
        String archiveName = "LongClassListPath.jsa";

        // Create a directory with long path and copy the classlist file to
        // the directory.
        Path classDir = Paths.get(CDSTestUtils.getOutputDir());
        Path destDir = classDir;
        int subDirLen = MAX_PATH - classDir.toString().length() - 2;
        if (subDirLen > 0) {
            char[] chars = new char[subDirLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            destDir = Paths.get(CDSTestUtils.getOutputDir(), subPath);
        }
        File longDir = destDir.toFile();
        longDir.mkdir();
        String destClassList = longDir.getPath() + File.separator + "LongClassListPath.classlist";
        Files.copy(Paths.get(classList), Paths.get(destClassList), StandardCopyOption.REPLACE_EXISTING);

        CDSOptions opts = (new CDSOptions())
            .addPrefix("-XX:ExtraSharedClassListFile=" + destClassList, "-cp", JarBuilder.getOrCreateHelloJar())
            .setArchiveName(archiveName);
        CDSTestUtils.createArchiveAndCheck(opts);
    }
}
