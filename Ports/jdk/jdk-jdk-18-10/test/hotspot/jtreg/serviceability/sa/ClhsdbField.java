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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8191538
 * @summary Test clhsdb 'field' command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbField
 */

public class ClhsdbField {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbField test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of("field");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("field", List.of(
                "field ConstantPool _pool_holder InstanceKlass*",
                "field InstanceKlass _methods Array<Method*>*",
                "field InstanceKlass _constants ConstantPool*",
                "field Klass _name Symbol*",
                "field JavaThread _osthread OSThread*",
                "field TenuredGeneration _the_space ContiguousSpace*",
                "field VirtualSpace _low_boundary char*",
                "field MethodCounters _backedge_counter InvocationCounter",
                "field nmethod _entry_bci int",
                "field Universe _collectedHeap CollectedHeap"));
            test.run(theApp.getPid(), cmds, expStrMap, null);
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
