/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Unit test for LingeredApp
 * @library /test/lib
 * @build jdk.test.lib.apps.LingeredAppTest jdk.test.lib.apps.LingeredApp
 * @run main jdk.test.lib.apps.LingeredAppTest
 */

package jdk.test.lib.apps;

import java.io.IOException;
import java.util.ArrayList;

public class LingeredAppTest {

    public static void main(String[] args) {
        try {
            System.out.println("Starting LingeredApp with default parameters");

            ArrayList<String> cmd = new ArrayList<String>();

            // Propagate test.vm.options to LingeredApp, filter out possible empty options
            String testVmOpts[] = System.getProperty("test.vm.opts","").split("\\s+");
            for (String s : testVmOpts) {
                if (!s.equals("")) {
                    cmd.add(s);
                }
            }

            cmd.add("-XX:+PrintFlagsFinal");

            LingeredApp a = LingeredApp.startApp(cmd.toArray(new String[cmd.size()]));
            System.out.printf("App pid: %d\n", a.getPid());
            a.stopApp();

            System.out.println("App output:");
            int count = 0;
            for (String line : a.getOutput().getStdoutAsList()) {
                count += 1;
            }
            System.out.println("Found " + count + " lines in VM output");
            System.out.println("Test PASSED");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Test ERROR");
            System.exit(3);
        }
    }
}
