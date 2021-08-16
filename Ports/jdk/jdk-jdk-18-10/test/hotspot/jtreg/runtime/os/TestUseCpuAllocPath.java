/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary If #processors > 1024 os_linux.cpp uses special coding.
 *   Excercise this by forcing usage of this coding. If this fails, this
 *   VM was either compiled on a platform which does not define CPU_ALLOC,
 *   or it is executed on a platform that does not support it.
 * @requires os.family == "linux"
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestUseCpuAllocPath
 */
public class TestUseCpuAllocPath {

    static final String SUCCESS_STRING = "active_processor_count: using dynamic path (forced)";

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb =
            ProcessTools.createJavaProcessBuilder("-Xlog:os=trace",
                                                  "-XX:+UnlockDiagnosticVMOptions",
                                                  "-XX:+UseCpuAllocPath",
                                                  "-version");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain(SUCCESS_STRING);
    }
}
