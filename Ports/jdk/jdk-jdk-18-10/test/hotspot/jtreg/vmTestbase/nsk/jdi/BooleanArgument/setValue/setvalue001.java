/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.BooleanArgument.setValue;

import java.io.PrintStream;
import java.io.Serializable;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.Connector.BooleanArgument;


/**
 * The test for the implementation of a BooleanArgument object. <BR>
 *
 * The test checks up that results of the method <BR>
 * <code>com.sun.jdi.connect.Connector.BooleanArgument.setValue()</code> <BR>
 * complies with specification. <BR>
 * The test works as follows:
 * <BR>
 * - Virtual Machine Manager is invoked.
 * - First BooleanArgument is searched among Connectors.<BR>
 * If no a BooleanArgument is found out the test exits with <BR>
 * the return value = 95 and a warning message. <BR>
 * - Under the assumption that the method <BR>
 * BooleanArgument.booleanValue() works correctly, <BR>
 * to the value of the BooleanArgument founded, <BR>
 * which may not have been set or may have an invalid value, <BR>
 * the sequence of 5 following checks is applied: <BR>
 * <BR>
 *    1) setValue(true);   booleanValue() must return true;  <BR>
 *    2) setValue(false);  booleanValue() must return false; <BR>
 *    3) setValue(false);  booleanValue() must return false; <BR>
 *    4) setValue(true);   booleanValue() must return true;  <BR>
 *    5) setValue(true);   booleanValue() must return true;  <BR>
 * <BR>
 *    In case of any check results in a wrong value, <BR>
 *    the test produces the return value 97 and <BR>
 *    a corresponding error message. <BR>
 *    Otherwise, the test is passed and produces <BR>
 *    the return value 95 and no message. <BR>
 */


public class setvalue001 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + 95); // JCK-compatible exit status
    }

    public static int run(String argv[], PrintStream out) {

        int exitCode  = 0;
        int exitCode0 = 0;
        int exitCode2 = 2;
//
        String sErr1 =  "WARNING\n" +
                        "Method tested: " +
                        "jdi.Connector.BooleanArgument.setValue\n" ;
//
        String sErr2 =  "ERROR\n" +
                        "Method tested: " +
                        "jdi.Connector.BooleanArgument.setValue\n" ;

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List connectorsList = vmm.allConnectors();
        Iterator connectorsListIterator = connectorsList.iterator();
//
        Connector.BooleanArgument argument = null;

        for ( ; ; ) {
            try {
                Connector connector =
                (Connector) connectorsListIterator.next();

                Map defaultArguments = connector.defaultArguments();
                Set keyset     = defaultArguments.keySet();
                int keysetSize = defaultArguments.size();
                Iterator  keysetIterator = keyset.iterator();

                for ( ; ; ) {
                    try {
                        String argName = (String) keysetIterator.next();

                        try {
//
                            argument = (Connector.BooleanArgument)
                                       defaultArguments.get(argName);
                            break ;
                        } catch ( ClassCastException e) {
                        }
                    } catch ( NoSuchElementException e) {
                        break ;
                    }
                }
                if (argument != null) {
                    break ;
                }
            } catch ( NoSuchElementException e) {
                out.println(sErr1 +
                    "no Connecter with BooleanArgument found\n");
                return exitCode0;
            }
        }

        // 1) initial -> true
        argument.setValue(true);
        if (argument.booleanValue() != true) {
            exitCode = 2;
            out.println(sErr2 +
                      "case: unknown -> true\n" +
                      "error: a returned value != true");
        }

        // 2) true -> false
        argument.setValue(false);
        if (argument.booleanValue() != false) {
            exitCode = 2;
            out.println(sErr2 +
                      "case: true -> false\n" +
                      "error: a returned value != false\n");
        }

        // 3) false -> false
        argument.setValue(false);
        if (argument.booleanValue() != false) {
            exitCode = 2;
            out.println(sErr2 +
                      "case: false -> false\n" +
                      "error: a returned value != false");
        }

        // 4) false -> true
        argument.setValue(true);
        if (argument.booleanValue() != true) {
            exitCode = 2;
            out.println(sErr2 +
                      "case: false -> true\n" +
                      "error: a returned value != true\n");
        }

        // 5) true -> true
        argument.setValue(true);
        if (argument.booleanValue() != true) {
            exitCode = 2;
            out.println(sErr2 +
                      "case: true -> true\n" +
                      "error: a returned value != true\n");
        }

        if (exitCode != exitCode0)
            out.println("TEST FAILED");

        return exitCode;
    }
}
