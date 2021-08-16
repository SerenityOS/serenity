/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.apps.LingeredApp;

/*
 * @test
 * @summary This test verifies jps usage and checks that appropriate error message is shown
 *          when running jps with illegal arguments.
 * @library /test/lib
 * @modules jdk.jartool/sun.tools.jar
 *          java.management
 *          java.base/jdk.internal.misc
 * @build jdk.test.lib.apps.* JpsHelper
 * @run driver TestJpsSanity
 */
public class TestJpsSanity {

    public static void main(String[] args) throws Throwable {
        testJpsUsage();
        testJpsVersion();
        testJpsUnknownHost();
        testJpsShort();
        testJpsLong();
        testJpsShortPkg();
        testJpsLongPkg();
    }

    private static void testJpsShort() throws Exception {
        OutputAnalyzer output = JpsHelper.jps();
        output.shouldMatch("^[0-9]+ Jps$");
    }

    private static void testJpsLong() throws Exception {
        OutputAnalyzer output = JpsHelper.jps("-l");
        output.shouldMatch("^[0-9]+ jdk\\.jcmd/sun\\.tools\\.jps\\.Jps$");
    }

    private static void testJpsShortPkg() throws Exception {
        LingeredApp app = null;
        try {
            app = LingeredApp.startApp();
            OutputAnalyzer output = JpsHelper.jps();
            output.shouldMatch("^[0-9]+ LingeredApp$");
        } finally {
            LingeredApp.stopApp(app);
        }
    }

    private static void testJpsLongPkg() throws Exception {
        LingeredApp app = null;
        try {
            app = LingeredApp.startApp();
            OutputAnalyzer output = JpsHelper.jps("-l");
            output.shouldMatch("^[0-9]+ jdk\\.test\\.lib\\.apps\\.LingeredApp$");
        } finally {
            LingeredApp.stopApp(app);
        }
    }

    private static void testJpsUsage() throws Exception {
        OutputAnalyzer output = JpsHelper.jps("-?");
        JpsHelper.verifyOutputAgainstFile(output);

        output = JpsHelper.jps("-h");
        JpsHelper.verifyOutputAgainstFile(output);

        output = JpsHelper.jps("--help");
        JpsHelper.verifyOutputAgainstFile(output);
    }

    private static void testJpsVersion() throws Exception {
        OutputAnalyzer output = JpsHelper.jps("-version");
        Asserts.assertNotEquals(output.getExitValue(), 0, "Exit code shouldn't be 0");
        Asserts.assertFalse(output.getStderr().isEmpty(), "Error output should not be empty");
        output.shouldContain("illegal argument: -version");
    }

    private static void testJpsUnknownHost() throws Exception {
        String invalidHostName = "Oja781nh2ev7vcvbajdg-Sda1-C.invalid";
        OutputAnalyzer output = JpsHelper.jps(invalidHostName);
        Asserts.assertNotEquals(output.getExitValue(), 0, "Exit code shouldn't be 0");
        Asserts.assertFalse(output.getStderr().isEmpty(), "Error output should not be empty");
        output.shouldMatch(".*(RMI Registry not available at|Unknown host\\:) " + invalidHostName + ".*");
    }

}
