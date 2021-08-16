/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @bug     8006997
 * @summary ContendedPaddingWidth should be range-checked
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver Options
 */
public class Options {

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer output;

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=-128", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("outside the allowed range");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=-8", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("outside the allowed range");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=-1", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("outside the allowed range");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=0", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=1", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("must be a multiple of 8");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8184", "-version"); // 8192-8 = 8184
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8191", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("must be a multiple of 8");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8192", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8193", "-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("outside the allowed range");
        output.shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-XX:ContendedPaddingWidth=8200", "-version"); // 8192+8 = 8200
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("ContendedPaddingWidth");
        output.shouldContain("outside the allowed range");
        output.shouldHaveExitValue(1);

   }

}
