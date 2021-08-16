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

/**
 * @test
 * @bug 8192985
 * @summary Test the clhsdb 'inspect' command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm/timeout=480 ClhsdbInspect
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import jdk.test.lib.apps.LingeredApp;
import jtreg.SkippedException;

public class ClhsdbInspect {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting the ClhsdbInspect test");

        LingeredAppWithLock theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();

            theApp = new LingeredAppWithLock();
            LingeredApp.startApp(theApp);
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            // Run the 'jstack -v' command to get the address of a Method*,
            // the oop address of a java.lang.ref.ReferenceQueue$Lock
            // and the oop address of a java.lang.Class object
            List<String> cmds = List.of("jstack -v");

            String jstackOutput = test.run(theApp.getPid(), cmds, null, null);

            Map<String, String> tokensMap = new HashMap<>();
            tokensMap.put("(a java.lang.Class for LingeredAppWithLock)",
                          "instance of Oop for java/lang/Class");
            tokensMap.put("Method*=", "Type is Method");
            tokensMap.put("(a java.lang.ref.ReferenceQueue$Lock)",
                          "instance of Oop for java/lang/ref/ReferenceQueue\\$Lock");

            String[] lines = jstackOutput.split("\\R");

            for (String key: tokensMap.keySet()) {
                cmds = new ArrayList<String>();
                Map<String, List<String>> expStrMap = new HashMap<>();

                String addressString = null;
                for (String line : lines) {
                    if (line.contains(key)) {
                        // Escape the token "Method*=" because the split method uses
                        // a regex, not just a straight String.
                        String escapedKey = key.replace("*","\\*");
                        String[] words = line.split(escapedKey+"|[ ]");
                        for (String word : words) {
                            word = word.replace("<","").replace(">","");
                            if (word.startsWith("0x")) {
                                addressString = word;
                                break;
                            }
                        }
                        if (addressString != null)
                            break;
                      }
                  }

                String cmd = "inspect " + addressString;
                cmds.add(cmd);
                expStrMap.put(cmd, List.of(tokensMap.get(key)));
                test.run(theApp.getPid(), cmds, expStrMap, null);
            }

            // This part is testing JDK-8261269. When inspecting a java object, we want to make
            // sure the address is not printed twice and that "Oop for ..." is not printed twice.
            //
            // The goal of this test is to dump the Class instance for java.lang.System. It contains
            // some Oop statics, and that's where the redundant "Oop for..." was noticed. The script
            // looks something like this:
            //
            // hsdb> class java.lang.System
            // java/lang/System @0x000000080000f388
            //
            // hsdb> inspect 0x000000080000f388
            // Type is InstanceKlass (size of 480)
            // ...
            // OopHandle Klass::_java_mirror: OopHandle @ 0x000000080000f400
            // ...
            //
            // hsdb> examine 0x000000080000f400
            // 0x000000080000f400: 0x00007fd8b812e5e8
            //
            // hsdb> examine 0x00007fd8b812e5e8
            // 0x00007fd8b812e5e8: 0x00000007fef00770
            //
            // hsdb> inspect 0x00000007fef00770
            // instance of Oop for java/lang/Class @ 0x00000007fef00770 @ 0x00000007fef00770 (size = 160)
            // in: Oop for java/io/BufferedInputStream @ 0x0000000082005b08 Oop for java/io/BufferedInputStream @ 0x0000000082005b08
            // out: Oop for java/io/PrintStream @ 0x0000000082007b60 Oop for java/io/PrintStream @ 0x0000000082007b60
            // err: Oop for java/io/PrintStream @ 0x000000008200e0c8 Oop for java/io/PrintStream @ 0x000000008200e0c8

            String cmd;
            Map<String, List<String>> expStrMap;
            Map<String, List<String>> unexpStrMap;

            // Start with the "class java.lang.System"
            cmd = "class java.lang.System";
            cmds = List.of(cmd);
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of("java.lang.System @0x"));
            String classCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, null);

            // "inspect" the address produced by the "class java.lang.System". This is the InstanceKlass.
            String classAddress = classCmdOutput.substring(classCmdOutput.indexOf("@0x")+1);
            lines = classAddress.split("\\R");
            classAddress = lines[0];
            cmd = "inspect " + classAddress;
            cmds = List.of(cmd);
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of("Type is InstanceKlass", "Klass::_java_mirror: OopHandle @"));
            String inspectCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, null);

            // Get the Klass::_java_mirror value from the InstanceKlass
            String mirrorPattern = "Klass::_java_mirror: OopHandle @ ";
            String mirrorAddress = inspectCmdOutput.substring(
                     inspectCmdOutput.indexOf(mirrorPattern) + mirrorPattern.length());
            lines = mirrorAddress.split("\\R");
            mirrorAddress = lines[0];

            // Use "examine" to do an indirection of the _java_mirror.
            cmd = "examine " + mirrorAddress;
            cmds = List.of(cmd);
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of(mirrorAddress + ": 0x"));
            String examineCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, null);
            String examineResult = examineCmdOutput.substring(examineCmdOutput.indexOf(": 0x")+2);
            lines = examineResult.split("\\R");
            examineResult = lines[0].trim(); // examine leaves a trailing space

            // Do another indirection using "examine" to get to the address of the Class instance.
            cmd = "examine " + examineResult;
            cmds = List.of(cmd);
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of(examineResult + ": 0x"));
            examineCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, null);
            examineResult = examineCmdOutput.substring(examineCmdOutput.indexOf(": 0x")+2);
            lines = examineResult.split("\\R");
            examineResult = lines[0].trim(); // examine leaves a trailing space

            // inspect the Class instance
            String instanceOfString = "instance of Oop for java/lang/Class @ ";
            String staticFieldString = "Oop for java/io/BufferedInputStream @";
            cmd = "inspect " + examineResult;
            cmds = List.of(cmd);
            expStrMap = new HashMap<>();
            expStrMap.put(cmd, List.of(instanceOfString + examineResult,
                                       "in: " + staticFieldString));
            unexpStrMap = new HashMap<>();
            // Make sure we don't see the address of the class intance twice, and make sure
            // we don't see "Oop for ..." twice for the "in" static field.
            unexpStrMap.put(cmd, List.of(
                    instanceOfString  + examineResult + " @ " + examineResult,
                    "in: " + staticFieldString + " .* " + staticFieldString));
            inspectCmdOutput = test.run(theApp.getPid(), cmds, expStrMap, unexpStrMap);
        } catch (SkippedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
