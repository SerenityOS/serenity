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
 * @bug 8190198
 * @summary Test clhsdb printstatics command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbPrintStatics
 */

public class ClhsdbPrintStatics {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbPrintStatics test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of(
                    "printstatics", "printstatics SystemDictionary",
                    "printstatics Threads", "printstatics Universe",
                    "printstatics JvmtiExport");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("printstatics", List.of(
                    "All known static fields",
                    "Abstract_VM_Version::_vm_major_version",
                    "ClassLoaderDataGraph::_head",
                    "JNIHandles::_weak_global_handles", "PerfMemory::_top",
                    "java_lang_Class::_oop_size_offset"));
            expStrMap.put("printstatics Threads", List.of(
                    "Static fields of Threads",
                    "_number_of_threads", "_number_of_non_daemon_threads"));
            expStrMap.put("printstatics Universe", List.of(
                    "Static fields of Universe",
                    "Universe::_collectedHeap"));
            expStrMap.put("printstatics JvmtiExport", List.of(
                    "Static fields of JvmtiExport",
                    "bool JvmtiExport::_can_access_local_variables",
                    "bool JvmtiExport::_can_hotswap_or_post_breakpoint",
                    "bool JvmtiExport::_can_post_on_exceptions"));

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
