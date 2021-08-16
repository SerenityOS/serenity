/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4287596
 * @summary Unit test for "Pluggable Connectors and Transports" feature.
 *
 * This tests launches a debuggee using a custom LaunchingConnector.
 *
 * @modules jdk.jdi/com.sun.tools.jdi
 * @build DebugUsingCustomConnector SimpleLaunchingConnector Foo NullTransportService
 * @run main/othervm DebugUsingCustomConnector
 */
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.util.*;

public class DebugUsingCustomConnector {

    static Connector find(List l, String name) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Connector c = (Connector)i.next();
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static void main(String main_args[]) throws Exception {
        /*
         * In development builds the JDI classes are on the boot class
         * path so defining class loader for the JDI classes will
         * not find classes on the system class path.
         */
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        if (vmm.getClass().getClassLoader() == null) {
            System.out.println("JDI on bootclasspath - test skipped");
            return;
        }

        List launchers = vmm.launchingConnectors();

        LaunchingConnector connector =
            (LaunchingConnector)find(launchers, "SimpleLaunchingConnector");

        Map args = connector.defaultArguments();

        Connector.StringArgument arg =
            (Connector.StringArgument)args.get("class");
        arg.setValue("Foo");

        VirtualMachine vm = connector.launch(args);
        vm.resume();
    }

}
