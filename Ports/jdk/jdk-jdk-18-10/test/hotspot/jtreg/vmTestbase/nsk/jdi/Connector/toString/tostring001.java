/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Connector.toString;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 */
public class tostring001 {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.dummy.dummy";
    private final static String className = "tostring001";
    private final static String debuggerName = prefix + className;

    //------------------------------------------------------- test specific fields

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }


    //------------------------------------------------------ mutable common method

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List acl = vmm.allConnectors();
        if (acl.size() > 0) {
            display("Number of all known JDI connectors: " + acl.size());
        } else {
            complain("No JDI connectors found!");
            return Consts.TEST_FAILED;
        }

        Iterator aci = acl.iterator();
        for (int i = 1; aci.hasNext(); i++) {
            Connector c = (Connector) aci.next();
            String cnm = c.toString();
            if (cnm == null) {
                complain("toString() method returns null for Connector #" + i + " : ");
                complain("         Description: " + c.description());
                complain("         Transport: " + c.transport().name());
                exitStatus = Consts.TEST_FAILED;
            } else if (cnm.length() == 0) {
                complain("toString() method returns empty string for Connector #" + i + " : ");
                complain("         Description: " + c.description());
                complain("         Transport: " + c.transport().name());
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("toString() method returns for Connector #" + i + " : " + cnm);
            }
        };

        return exitStatus;
    }

    //--------------------------------------------------------- test specific methods

}
//--------------------------------------------------------- test specific classes
