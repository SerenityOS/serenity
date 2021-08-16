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

import java.util.ArrayList;
import java.util.List;

import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.utilities.SystemDictionaryHelper;
import sun.jvm.hotspot.oops.InstanceKlass;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.Method;
import sun.jvm.hotspot.utilities.MethodArray;
import sun.jvm.hotspot.ui.classbrowser.HTMLGenerator;

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
 *          jdk.hotspot.agent/sun.jvm.hotspot.ui.classbrowser
 * @run driver TestCpoolForInvokeDynamic
 */

public class TestCpoolForInvokeDynamic {

    private static LingeredAppWithInvokeDynamic theApp = null;

    private static void printBytecodes(String pid,
                                       String[] instanceKlassNames) {
        HotSpotAgent agent = new HotSpotAgent();
        try {
            agent.attach(Integer.parseInt(pid));
        }
        catch (DebuggerException e) {
            System.out.println(e.getMessage());
            System.err.println("Unable to connect to process ID: " + pid);

            agent.detach();
            e.printStackTrace();
        }

        for (String instanceKlassName : instanceKlassNames) {
            InstanceKlass iKlass = SystemDictionaryHelper.findInstanceKlass(instanceKlassName);
            MethodArray methods = iKlass.getMethods();
            for (int i = 0; i < methods.length(); i++) {
                Method m = methods.at(i);
                System.out.println("Method: " + m.getName().asString() +
                                   " in instance klass: " + instanceKlassName);
                HTMLGenerator gen = new HTMLGenerator(false);
                System.out.println(gen.genHTML(m));
            }
        }
        agent.detach();
    }

    private static void createAnotherToAttach(
                            String[] instanceKlassNames,
                            long lingeredAppPid) throws Exception {
        // Start a new process to attach to the lingered app
        ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(
            "--add-modules=jdk.hotspot.agent",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.utilities=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.oops=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.debugger=ALL-UNNAMED",
            "--add-exports=jdk.hotspot.agent/sun.jvm.hotspot.ui.classbrowser=ALL-UNNAMED",
            "TestCpoolForInvokeDynamic",
            Long.toString(lingeredAppPid));
        SATestUtils.addPrivilegesIfNeeded(processBuilder);
        OutputAnalyzer SAOutput = ProcessTools.executeProcess(processBuilder);
        SAOutput.shouldHaveExitValue(0);
        System.out.println(SAOutput.getOutput());

        SAOutput.shouldContain("invokedynamic");
        SAOutput.shouldContain("Name and Type");
        SAOutput.shouldContain("run:()Ljava.lang.Runnable");
        SAOutput.shouldContain("compare:()LTestComparator");
        SAOutput.shouldNotContain("Corrupted constant pool");
    }

    public static void main (String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.

        String[] instanceKlassNames = new String[] {
                                          "LingeredAppWithInvokeDynamic"
                                      };

        if (args == null || args.length == 0) {
            try {
                theApp = new LingeredAppWithInvokeDynamic();
                LingeredApp.startApp(theApp, "-XX:+UsePerfData");
                createAnotherToAttach(instanceKlassNames,
                                      theApp.getPid());
            } finally {
                LingeredApp.stopApp(theApp);
            }
        } else {
            printBytecodes(args[0], instanceKlassNames);
        }
    }
}
