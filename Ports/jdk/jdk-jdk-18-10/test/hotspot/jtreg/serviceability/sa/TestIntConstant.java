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
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;
import java.util.Map;
import java.util.HashMap;
import jtreg.SkippedException;

/**
 * @test
 * @summary Test the 'intConstant' command of jhsdb clhsdb.
 * @bug 8190307
 * @requires vm.hasSA
 * @library /test/lib
 * @build jdk.test.lib.apps.*
 * @run main/othervm TestIntConstant
 */

public class TestIntConstant {

    public static void main (String... args) throws Exception {
        System.out.println("Starting TestIntConstant test");
        LingeredApp app = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            app = LingeredApp.startApp();

            System.out.println ("Started LingeredApp with pid " + app.getPid());

            List<String> cmds = List.of("intConstant",
                                        "intConstant _temp_constant 45",
                                        "intConstant _temp_constant");

            Map<String, List<String>> expStrMap = new HashMap<>();

            // Strings to check for in the output of 'intConstant'. The
            // 'intConstant' command prints out entries from the
            // 'gHotSpotVMIntConstants', which is a table of integer constants,
            // with names and the values derived from enums and #define preprocessor
            // macros in hotspot.
            expStrMap.put("intConstant", List.of(
                 "CollectedHeap::G1 3",
                 "RUNNABLE 2",
                 "Deoptimization::Reason_class_check 4",
                 "_thread_uninitialized 0"));
            expStrMap.put("intConstant _temp_constant", List.of(
                 "intConstant _temp_constant 45"));
            test.run(app.getPid(), cmds, expStrMap, null);
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
