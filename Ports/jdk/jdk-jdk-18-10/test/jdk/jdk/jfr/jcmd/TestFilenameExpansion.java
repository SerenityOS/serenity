/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.FileHelper;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary The test verifies JFR.start/dump/stop commands
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestFilenameExpansion
 */
public class TestFilenameExpansion {

    public static void main(String[] args) throws Exception {
        String pid = Long.toString(ProcessHandle.current().pid());
        String name = "output_%p_%t_%%.jfr";
        String pattern = "output_" + pid + "_" + "\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}" + "_%\\.jfr";

        JcmdHelper.jcmd("JFR.start name=test");
        String filename = JcmdHelper.readFilename(JcmdHelper.jcmd("JFR.dump name=test filename=" + name));
        File file = new File(filename);
        Asserts.assertTrue(file.exists(), file.getAbsolutePath() + " does not exist");
        Asserts.assertTrue(file.isFile(), file.getAbsolutePath() + " is not a file");
        Asserts.assertTrue(Pattern.compile(pattern).matcher(filename).find());
    }
}
