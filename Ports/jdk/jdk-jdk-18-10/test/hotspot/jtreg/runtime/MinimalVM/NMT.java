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

/*
 * @test
 * @requires vm.flavor == "minimal"
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver NMT
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class NMT {

    public static void main(String args[]) throws Exception {
        ProcessBuilder pb;

        pb = ProcessTools.createJavaProcessBuilder("-minimal", "-XX:NativeMemoryTracking=detail", "-version");
        new OutputAnalyzer(pb.start())
                .shouldContain("Native Memory Tracking is not supported in this VM")
                .shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-minimal", "-XX:NativeMemoryTracking=summary", "-version");
        new OutputAnalyzer(pb.start())
                .shouldContain("Native Memory Tracking is not supported in this VM")
                .shouldHaveExitValue(1);

        pb = ProcessTools.createJavaProcessBuilder("-minimal", "-XX:NativeMemoryTracking=off", "-version");
        new OutputAnalyzer(pb.start())
                .shouldContain("Native Memory Tracking is not supported in this VM")
                .shouldHaveExitValue(1);


    }
}
