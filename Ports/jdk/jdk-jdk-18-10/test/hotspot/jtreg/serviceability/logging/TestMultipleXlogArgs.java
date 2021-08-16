/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestMultipleXlogArgs
 * @summary Ensure multiple -Xlog arguments aggregate the logging options.
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestMultipleXlogArgs
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestMultipleXlogArgs {

    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:logging=debug",
                                                                  "-Xlog:logging=trace",
                                                                  "-Xlog:defaultmethods=trace",
                                                                  "-Xlog:defaultmethods=warning",
                                                                  "-Xlog:safepoint=info",
                                                                  "-Xlog:safepoint=info",
                                                                  "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        // -Xlog:logging=trace means that the log configuration will be printed.
        String stdoutConfigLine = "\\[logging *\\]  #0: stdout .*";
        // Ensure logging=trace has overwritten logging=debug
        output.shouldMatch(stdoutConfigLine + "logging=trace").shouldNotMatch(stdoutConfigLine + "logging=debug");
        // Make sure safepoint=info is printed exactly once even though we're setting it twice
        output.shouldMatch(stdoutConfigLine + "safepoint=info").shouldNotMatch(stdoutConfigLine + "safepoint=info.*safepoint=info");
        // Shouldn't see defaultmethods at all, because it should be covered by the initial 'all=warning' config
        output.shouldNotMatch(stdoutConfigLine + "defaultmethods");
        output.shouldHaveExitValue(0);
    }
}

