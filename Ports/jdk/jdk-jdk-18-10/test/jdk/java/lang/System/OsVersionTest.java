/*
 * Copyright (c) 2015, 2021 SAP SE. All rights reserved.
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

import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;

/*
 * @test
 * @bug 8132374
 * @summary Check that the value of the os.version property is equal
 *          to the value of the corresponding OS provided tools.
 * @library /test/lib
 * @run main OsVersionTest
 * @author Volker Simonis
 */
public class OsVersionTest {

    public static void main(String args[]) throws Throwable {
        final String osVersion = System.getProperty("os.version");
        if (osVersion == null) {
            throw new Error("Cant query 'os.version' property!");
        }
        if (Platform.isLinux()) {
            OutputAnalyzer output = ProcessTools.executeProcess("uname", "-r");
            if (!osVersion.equals(output.getOutput().trim())) {
                throw new Error(osVersion + " != " + output.getOutput().trim());
            }
        }
        else if (Platform.isOSX()) {
            OutputAnalyzer output = ProcessTools.executeProcess("sw_vers", "-productVersion");
            String swVersOutput = output.getOutput().trim();
            if (!osVersion.equals(swVersOutput)) {
                // This section can be removed if minimum build SDK is xcode 12+
                if (swVersOutput.startsWith(osVersion)) {
                    throw new SkippedException("MacOS version only matches in parts, this is expected when " +
                                               "JDK was built with Xcode < 12 and MacOS version patch is > 0");
                }
                throw new Error(osVersion + " != " + swVersOutput);
            }
        }
        else if (Platform.isAix()) {
            OutputAnalyzer output1 = ProcessTools.executeProcess("uname", "-v");
            OutputAnalyzer output2 = ProcessTools.executeProcess("uname", "-r");
            String version = output1.getOutput().trim() + "." + output2.getOutput().trim();
            if (!osVersion.equals(version)) {
                throw new Error(osVersion + " != " + version);
            }
        }
        else if (Platform.isWindows()) {
            OutputAnalyzer output = ProcessTools.executeProcess("cmd", "/c", "ver");
            String version = output.firstMatch(".+\\[Version ([0-9.]+)\\]", 1);
            if (version == null || !version.startsWith(osVersion)) {
                throw new Error(osVersion + " != " + version);
            }
        }
        else {
            System.out.println("This test is currently not supported on " +
                               Platform.getOsName());
        }
    }
}
