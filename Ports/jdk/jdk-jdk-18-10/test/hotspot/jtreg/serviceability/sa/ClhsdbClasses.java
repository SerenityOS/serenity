/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242142
 * @summary Test clhsdb class and classes commands
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbClasses
 */

public class ClhsdbClasses {
    static final String APP_DOT_CLASSNAME = LingeredApp.class.getName();
    static final String APP_SLASH_CLASSNAME = APP_DOT_CLASSNAME.replace('.', '/');

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbClasses test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            String classCmdOutput;

            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Run "class jdk/test/lib/apps/LingeredApp"
            {
                String cmd = "class " + APP_SLASH_CLASSNAME;
                List<String> cmds = List.of(cmd);
                Map<String, List<String>> expStrMap = new HashMap<>();
                expStrMap.put(cmd, List.of(APP_SLASH_CLASSNAME + " @0x"));
                classCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, null);
            }

            // Run "print <addr>" on the address printed above for LingeredApp. Also
            // run "classes" command to verify LingeredApp and java.lang.Class are
            // in the list of classes. Note we can't do the above "class LingeredApp"
            // command as part of this command because then we won't have the address
            // for the LingeredApp class, which we need for the print command and also
            // to verify it matches the address in the classes commands.
            {
                String classAddress = classCmdOutput.substring(classCmdOutput.indexOf("@0x")+1);
                String[] lines = classAddress.split("\\R");
                classAddress = lines[0];
                String printCmd = "print " + classAddress;
                String classesCmd = "classes";
                List<String> cmds = List.of(printCmd, classesCmd);
                Map<String, List<String>> expStrMap = new HashMap<>();
                expStrMap.put(printCmd,
                              List.of("public class " + APP_DOT_CLASSNAME + " @" + classAddress));
                expStrMap.put(classesCmd, List.of(
                        APP_SLASH_CLASSNAME + " @" + classAddress, // check for app class at right address
                        "java/lang/Class @0x",                // check for java/lang/Class
                        "java/lang/Object @0x",               // check for java/lang/Object
                        "java/lang/Cloneable @0x",            // check for an interface type
                        "\\[Ljava/lang/String; @0x",          // check for array type
                        "\\[J @0x", "\\[I @0x", "\\[Z @0x")); // check for array of a few pimitive types
                test.run(theApp.getPid(), cmds, expStrMap, null);
            }
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
