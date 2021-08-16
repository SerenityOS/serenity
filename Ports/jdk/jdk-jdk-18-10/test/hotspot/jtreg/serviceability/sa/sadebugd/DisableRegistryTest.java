/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 NTT DATA.
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

import java.io.IOException;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.SA.SATestUtils;

import jtreg.SkippedException;

/**
 * @test
 * @bug 8263636 8263635
 * @summary Test to use already started RMI registry
 * @requires vm.hasSA
 * @requires os.family != "windows"
 * @library /test/lib
 * @run driver DisableRegistryTest
 */

public class DisableRegistryTest {
    private static final int REGISTRY_PORT = 10000;
    private static final String PREFIX_1 = "app1";
    private static final String PREFIX_2 = "app2";

    private static DebugdUtils attachWithDebugd(int pid, boolean disableRegistry, String serverName) throws IOException {
        var debugd = new DebugdUtils();
        debugd.setRegistryPort(REGISTRY_PORT);
        debugd.setDisableRegistry(disableRegistry);
        debugd.setServerName(serverName);
        debugd.attach(pid);
        return debugd;
    }

    private static void test(String serverName) throws IOException, InterruptedException {
        assert serverName != null;

        JDKToolLauncher jhsdbLauncher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        jhsdbLauncher.addToolArg("jinfo");
        jhsdbLauncher.addToolArg("--connect");
        jhsdbLauncher.addToolArg("localhost:" + REGISTRY_PORT + "/" + serverName);

        Process jhsdb = (SATestUtils.createProcessBuilder(jhsdbLauncher)).start();
        OutputAnalyzer out = new OutputAnalyzer(jhsdb);
        jhsdb.waitFor();
        System.out.println(out.getStdout());
        System.err.println(out.getStderr());

        out.stderrShouldBeEmptyIgnoreDeprecatedWarnings();
        out.shouldContain("Attaching to remote server localhost:10000");
        out.shouldContain("java.vm.version");
        out.shouldHaveExitValue(0);

        // This will detect most SA failures, including during the attach.
        out.shouldNotMatch("^sun.jvm.hotspot.debugger.DebuggerException:.*$");
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        SATestUtils.validateSADebugDPrivileges();

        LingeredApp app1 = null;
        LingeredApp app2 = null;
        DebugdUtils debugd1 = null;
        DebugdUtils debugd2 = null;
        try {
            app1 = LingeredApp.startApp();
            app2 = LingeredApp.startApp();
            debugd1 = attachWithDebugd((int)app1.getPid(), false, PREFIX_1);
            debugd2 = attachWithDebugd((int)app2.getPid(), true, PREFIX_2);

            test(PREFIX_1);
            test(PREFIX_2);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            if (debugd2 != null) {
                debugd2.detach();
            }
            if (debugd1 != null) {
                debugd1.detach();
            }
            LingeredApp.stopApp(app1);
            LingeredApp.stopApp(app2);
        }
        System.out.println("Test PASSED");
    }
}
