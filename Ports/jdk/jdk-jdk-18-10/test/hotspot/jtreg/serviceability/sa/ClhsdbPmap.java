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
import jdk.test.lib.util.CoreUtils;
import jdk.test.lib.Platform;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8190198
 * @summary Test clhsdb pmap command on a live process
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbPmap false
 */

/**
 * @test
 * @bug 8190198
 * @summary Test clhsdb pmap command on a core file
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbPmap true
 */

public class ClhsdbPmap {

    public static void main(String[] args) throws Exception {
        boolean withCore = Boolean.parseBoolean(args[0]);
        System.out.println("Starting ClhsdbPmap test: withCore==" + withCore);

        LingeredApp theApp = null;
        String coreFileName = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = new LingeredApp();
            theApp.setForceCrash(withCore);
            LingeredApp.startApp(theApp);
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            if (withCore) {
                String crashOutput = theApp.getOutput().getStdout();
                coreFileName = CoreUtils.getCoreFileLocation(crashOutput, theApp.getPid());
            }

            List<String> cmds = List.of("pmap");

            Map<String, List<String>> expStrMap = new HashMap<>();
            if (!withCore && Platform.isOSX()) {
                expStrMap.put("pmap", List.of("Not available for Mac OS X processes"));
            } else {
                expStrMap.put("pmap", List.of("jvm", "java", "jli", "jimage"));
            }

            if (withCore) {
                test.runOnCore(coreFileName, cmds, expStrMap, null);
            } else {
                test.run(theApp.getPid(), cmds, expStrMap, null);
            }
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            if (!withCore) {
                LingeredApp.stopApp(theApp);
            }
        }
        System.out.println("Test PASSED");
    }
}
