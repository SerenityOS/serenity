/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.connect.*;
import com.sun.jdi.*;
import java.util.Map;
import java.util.List;
import jdk.test.lib.Asserts;

/*
 * @test
 * @summary Verifies that PathSearchingVirtualMachine.bootClassPath()
 *          returns an empty list in case no bootclass path specified
 *          regardless of sun.boot.class.path option, which is now obsolete
 * @library /test/lib
 * @compile TestClass.java
 * @compile SunBootClassPathEmptyTest.java
 * @run main/othervm SunBootClassPathEmptyTest
 */
public class SunBootClassPathEmptyTest {

    /**
     * Helper class to facilitate the debuggee VM launching
     */
    private static class VmConnector {

        LaunchingConnector lc;
        VirtualMachine vm;

        VmConnector() {
            for (LaunchingConnector c : Bootstrap.virtualMachineManager().launchingConnectors()) {
                System.out.println("name: " + c.name());
                if (c.name().equals("com.sun.jdi.CommandLineLaunch")) {
                    lc = c;
                    break;
                }
            }
            if (lc == null) {
                throw new RuntimeException("Connector not found");
            }
        }

        PathSearchingVirtualMachine launchVm(String cmdLine, String options) throws Exception {
            Map<String, Connector.Argument> vmArgs = lc.defaultArguments();
            vmArgs.get("main").setValue(cmdLine);
            if (options != null) {
                vmArgs.get("options").setValue(options);
            }
            System.out.println("Debugger is launching vm ...");
            vm = lc.launch(vmArgs);
            if (!(vm instanceof PathSearchingVirtualMachine)) {
                throw new RuntimeException("VM is not a PathSearchingVirtualMachine");
            }
            return (PathSearchingVirtualMachine) vm;
        }

    }

    private static VmConnector connector = new VmConnector();

    public static void main(String[] args) throws Exception {
        testWithObsoleteClassPathOption(null);
        testWithObsoleteClassPathOption("someclasspath");
    }

    private static void testWithObsoleteClassPathOption(String obsoleteClassPath) throws Exception {
        PathSearchingVirtualMachine vm = connector.launchVm("TestClass", makeClassPathOptions(obsoleteClassPath));
        List<String> bootClassPath = vm.bootClassPath();
        Asserts.assertNotNull(bootClassPath, "Expected bootClassPath to be empty but was null");
        Asserts.assertEquals(0, bootClassPath.size(), "Expected bootClassPath.size() 0 but was: " + bootClassPath.size());
    }

    private static String makeClassPathOptions(String obsoleteClassPath) {
        return obsoleteClassPath == null ? null : "-Dsun.boot.class.path=" + obsoleteClassPath;
    }

}
