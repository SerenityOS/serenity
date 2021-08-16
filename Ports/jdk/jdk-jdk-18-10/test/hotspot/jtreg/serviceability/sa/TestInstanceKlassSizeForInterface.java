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

import java.util.List;

import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.utilities.SystemDictionaryHelper;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.debugger.*;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

/**
 * @test
 * @library /test/lib
 * @requires vm.hasSA
 * @modules java.base/jdk.internal.misc
 *          jdk.hotspot.agent/sun.jvm.hotspot
 *          jdk.hotspot.agent/sun.jvm.hotspot.utilities
 *          jdk.hotspot.agent/sun.jvm.hotspot.oops
 *          jdk.hotspot.agent/sun.jvm.hotspot.debugger
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. TestInstanceKlassSizeForInterface
 */

import sun.hotspot.WhiteBox;

public class TestInstanceKlassSizeForInterface {

    public static WhiteBox wb = WhiteBox.getWhiteBox();

    private static LingeredAppWithInterface theApp = null;

    private static void SAInstanceKlassSize(int lingeredAppPid,
                                            String[] instanceKlassNames) {

        HotSpotAgent agent = new HotSpotAgent();
        try {
            agent.attach(lingeredAppPid);
        }
        catch (DebuggerException e) {
            System.out.println(e.getMessage());
            System.err.println("Unable to connect to process ID: " + lingeredAppPid);

            agent.detach();
            e.printStackTrace();
        }

        for (String instanceKlassName : instanceKlassNames) {
            InstanceKlass iKlass = SystemDictionaryHelper.findInstanceKlass(
                                       instanceKlassName);
            Asserts.assertNotNull(iKlass,
                String.format("Unable to find instance klass for %s", instanceKlassName));
            System.out.println("SA: The size of " + instanceKlassName +
                               " is " + iKlass.getSize());
        }
        agent.detach();
    }

    private static String getJcmdInstanceKlassSize(OutputAnalyzer output,
                                                   String instanceKlassName) {
        for (String s : output.asLines()) {
            if (s.contains(instanceKlassName)) {
                String tokens[];
                System.out.println(s);
                tokens = s.split("\\s+");
                return tokens[3];
            }
        }
        return null;
    }

    private static void createAnotherToAttach(
                            String[] instanceKlassNames,
                            int lingeredAppPid) throws Exception {
        // Start a new process to attach to the LingeredApp process to get SA info
        ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(
            "--add-modules=jdk.hotspot.agent",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+WhiteBoxAPI",
            "-Xbootclasspath/a:.",
            "TestInstanceKlassSizeForInterface",
            Integer.toString(lingeredAppPid));
        SATestUtils.addPrivilegesIfNeeded(processBuilder);
        OutputAnalyzer SAOutput = ProcessTools.executeProcess(processBuilder);
        SAOutput.shouldHaveExitValue(0);
        System.out.println(SAOutput.getOutput());

        // Match the sizes from both the output streams
        for (String instanceKlassName : instanceKlassNames) {
            Class<?> iklass = Class.forName(instanceKlassName);
            System.out.println ("Trying to match for " + instanceKlassName);
            String size = String.valueOf(wb.getKlassMetadataSize(iklass));
            boolean match = false;
            for (String s : SAOutput.asLines()) {
                if (s.contains(instanceKlassName)) {
                    Asserts.assertTrue(
                       s.contains(size), "The size computed by SA for" +
                       instanceKlassName + " does not match.");
                       match = true;
                    }
            }
            Asserts.assertTrue(match, "Found a match for " + instanceKlassName);
        }
    }

    public static void main (String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        String[] instanceKlassNames = new String[] {
                                          "Language",
                                          "ParselTongue",
                                          "LingeredAppWithInterface$1"
                                      };

        if (args == null || args.length == 0) {
            try {
                theApp = new LingeredAppWithInterface();
                LingeredApp.startApp(theApp);
                createAnotherToAttach(instanceKlassNames,
                                      (int)theApp.getPid());
            } finally {
                LingeredApp.stopApp(theApp);
            }
        } else {
            SAInstanceKlassSize(Integer.parseInt(args[0]), instanceKlassNames);
        }
    }
}
