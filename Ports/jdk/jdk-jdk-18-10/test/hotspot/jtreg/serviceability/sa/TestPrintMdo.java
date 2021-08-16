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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;
import jtreg.SkippedException;

/**
 * @test
 * @library /test/lib
 * @requires vm.hasSA
 * @requires vm.flavor == "server" & !vm.emulatedClient & !(vm.opt.TieredStopAtLevel == 1)
 * @build jdk.test.lib.apps.*
 * @run main/othervm TestPrintMdo
 */

public class TestPrintMdo {

    public static void main (String... args) throws Exception {
        System.out.println("Starting TestPrintMdo test");
        LingeredApp app = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            app = LingeredApp.startApp("-XX:+ProfileInterpreter", "-XX:CompileThreshold=100");
            System.out.println ("Started LingeredApp with pid " + app.getPid());
            List<String> cmds = List.of("printmdo -a");

            Map<String, List<String>> expStrMap = new HashMap<>();
            Map<String, List<String>> unExpStrMap = new HashMap<>();
            expStrMap.put("printmdo -a", List.of(
                "VirtualCallData",
                "CounterData",
                "bci",
                "MethodData",
                "java/lang/Object"));
            unExpStrMap.put("printmdo -a", List.of(
                            "missing reason for "));
            test.run(app.getPid(), cmds, expStrMap, unExpStrMap);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(app);
        }

        System.out.println("Test PASSED");
    }
}
