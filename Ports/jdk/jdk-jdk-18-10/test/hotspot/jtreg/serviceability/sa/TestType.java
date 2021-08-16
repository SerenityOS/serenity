/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the 'type' command of jhsdb clhsdb.
 * @bug 8190307
 * @requires vm.hasSA
 * @library /test/lib
 * @build jdk.test.lib.apps.*
 * @run main/othervm TestType
 */

public class TestType {

    public static void main (String... args) throws Exception {

        System.out.println("Starting TestType test");
        LingeredApp app = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            app = LingeredApp.startApp();
            System.out.println ("Started LingeredApp with pid " + app.getPid());
            List<String> cmds = List.of("type", "type InstanceKlass");

            Map<String, List<String>> expStrMap = new HashMap<>();
            // Strings to check for in the output of 'type'. The 'type'
            // command prints out entries from 'gHotSpotVMTypes', which
            // is a table containing the hotspot types, their supertypes,
            // sizes, etc, which are of interest to the SA.
            expStrMap.put("type", List.of(
                "type G1CollectedHeap CollectedHeap",
                "type ConstantPoolCache MetaspaceObj",
                "type ConstantPool Metadata",
                "type CompilerThread JavaThread",
                "type CardGeneration Generation",
                "type ArrayKlass Klass",
                "type InstanceKlass Klass"));
            // String to check for in the output of "type InstanceKlass"
            expStrMap.put("type InstanceKlass", List.of("type InstanceKlass Klass"));
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
