/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE and/or its affiliates. All rights reserved.
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
 * @bug 8149036 8150619
 * @summary os+thread output should contain logging calls for thread start stop attaches detaches
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver ThreadLoggingTest
 * @author Thomas Stuefe (SAP)
 */

import java.io.File;
import java.util.Map;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ThreadLoggingTest {

    static void analyzeOutputForInfoLevel(OutputAnalyzer output) throws Exception {
        output.shouldMatch("Thread .* started");
        output.shouldContain("Thread is alive");
        output.shouldContain("Thread finished");
        output.shouldHaveExitValue(0);
    }

    static void analyzeOutputForDebugLevel(OutputAnalyzer output) throws Exception {
        analyzeOutputForInfoLevel(output);
        output.shouldContain("stack dimensions");
        output.shouldContain("stack guard pages");
    }

    public static void main(String[] args) throws Exception {

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:os+thread", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        analyzeOutputForInfoLevel(output);

        pb = ProcessTools.createJavaProcessBuilder("-Xlog:os+thread=debug", "-version");
        output = new OutputAnalyzer(pb.start());
        analyzeOutputForDebugLevel(output);
        output.reportDiagnosticSummary();
    }

}
