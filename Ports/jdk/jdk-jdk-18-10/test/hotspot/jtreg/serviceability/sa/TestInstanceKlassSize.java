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

import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.utilities.SystemDictionaryHelper;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.debugger.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

import java.io.*;
import java.util.*;

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
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. TestInstanceKlassSize
 */

import sun.hotspot.WhiteBox;

public class TestInstanceKlassSize {

    public static WhiteBox wb = WhiteBox.getWhiteBox();
    private static String[] SAInstanceKlassNames = new String[] {
                                                "java.lang.Object",
                                                "java.util.ArrayList",
                                                "java.lang.String",
                                                "java.lang.Thread",
                                                "java.lang.Byte"
                                             };

    private static void startMeWithArgs() throws Exception {

        LingeredApp app = null;
        OutputAnalyzer output = null;
        try {
            app = LingeredApp.startApp("-XX:+UsePerfData");
            System.out.println ("Started LingeredApp with pid " + app.getPid());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        try {
            // Run this app with the LingeredApp PID to get SA output from the LingeredApp
            ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(
                "--add-modules=jdk.hotspot.agent",
                "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
                "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
                "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
                "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+WhiteBoxAPI",
                "-Xbootclasspath/a:.",
                "TestInstanceKlassSize",
                Long.toString(app.getPid()));
            SATestUtils.addPrivilegesIfNeeded(processBuilder);
            output = ProcessTools.executeProcess(processBuilder);
            System.out.println(output.getOutput());
            output.shouldHaveExitValue(0);

            // Check whether the size matches with value from VM.
            for (String instanceKlassName : SAInstanceKlassNames) {
                Class<?> iklass = Class.forName(instanceKlassName);
                System.out.println ("Trying to match for " + instanceKlassName);
                String size = String.valueOf(wb.getKlassMetadataSize(iklass));
                boolean match = false;
                for (String s : output.asLines()) {
                    if (s.contains(instanceKlassName)) {
                       Asserts.assertTrue(
                          s.contains(size), "The size computed by SA for " +
                          instanceKlassName + " does not match.");
                       match = true;
                    }
                }
                Asserts.assertTrue(match, "Found a match for " + instanceKlassName);
            }
        } finally {
            LingeredApp.stopApp(app);
        }
    }

    private static void SAInstanceKlassSize(int pid,
                                            String[] SAInstanceKlassNames) {
        HotSpotAgent agent = new HotSpotAgent();
        try {
            agent.attach(pid);
        }
        catch (DebuggerException e) {
            System.out.println(e.getMessage());
            System.err.println("Unable to connect to process ID: " + pid);

            agent.detach();
            e.printStackTrace();
        }

        for (String SAInstanceKlassName : SAInstanceKlassNames) {
            InstanceKlass ik = SystemDictionaryHelper.findInstanceKlass(
                               SAInstanceKlassName);
            Asserts.assertNotNull(ik,
                String.format("Unable to find instance klass for %s", SAInstanceKlassName));
            System.out.println("SA: The size of " + SAInstanceKlassName +
                               " is " + ik.getSize());
        }
        agent.detach();
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        if (args == null || args.length == 0) {
            System.out.println ("No args run. Starting with args now.");
            startMeWithArgs();
        } else {
            SAInstanceKlassSize(Integer.parseInt(args[0]), SAInstanceKlassNames);
        }
    }
}
