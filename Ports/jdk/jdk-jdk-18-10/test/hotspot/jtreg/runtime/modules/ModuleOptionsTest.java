/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136930
 * @summary Test that the VM only recognizes the last specified --list-modules
 *          options but accumulates --add-module values.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver ModuleOptionsTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

// Test that the VM behaves correctly when processing module related options.
public class ModuleOptionsTest {

    public static void main(String[] args) throws Exception {

        // Test that multiple --add-modules options are cumulative, not last one wins.
        // An exception should be thrown because module i_dont_exist doesn't exist.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "--add-modules=i_dont_exist", "--add-modules=java.base", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("FindException");
        output.shouldContain("i_dont_exist");
        output.shouldHaveExitValue(1);

        // Test that the last --limit-modules is the only one recognized.  No exception
        // should be thrown.
        pb = ProcessTools.createJavaProcessBuilder(
            "--limit-modules=i_dont_exist", "--limit-modules=java.base", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }
}
