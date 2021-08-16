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

package nsk.jdi.BooleanArgument.booleanValue;

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
import com.sun.jdi.connect.LaunchingConnector;


/**
 * The test for the implementation of an object of the type <BR>
 * Connector.BooleanArgument. <BR>
 * <BR>
 * The test checks up that results of the method                             <BR>
 * <code>com.sun.jdi.connect.Connector.BooleanArgument.BooleanValue()</code> <BR>
 * complies with its specification in the following cases:                   <BR>
     <BR>
     <BR>
 * <BR>
 * In case of the method booleanValue() returns a wrong value,   <BR>
 * the test produces the return value 97 and   <BR>
 * a corresponding error message.              <BR>
 * Otherwise, the test is passed and produces  <BR>
 * the return value 95 and no message.         <BR>
 */


public class booleanvalue002 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + 95); // JCK-compatible exit status
    }

    public static int run(String argv[], PrintStream out) {

        int exitCode  = 0;
        int exitCode0 = 0;
        int exitCode2 = 2;
//
        String sErr1 =  "WARNING:\n" +
                        "Method tested: " +
                        "jdi.Connector.BooleanArgument.booleanValue\n" ;
//
        String sErr2 =  "INFO:\n" +
                        "Method tested: " +
                        "jdi.Connector.BooleanArgument.booleanValue\n" ;


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
                    "no Connecter with needed BooleanArgument found\n");
                return exitCode0;
            }
        }

        boolean b;

        argument.setValue(true);
        argument.setValue("");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: '' -> true = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue(true);
        argument.setValue("tru");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: 'tru' -> true = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue(false);
        argument.setValue("");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: '' -> false = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue(false);
        argument.setValue("fals");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: '' -> false = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue("true");
        argument.setValue("");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: '' -> 'true' = ? \n" +
                     "result: booleanValue() == " + b + "\n");
        }

        argument.setValue("true");
        argument.setValue("tru");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: 'tru' -> 'true' = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue("false");
        argument.setValue("");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: '' -> 'false' = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        argument.setValue("false");
        argument.setValue("fals");
        b = argument.booleanValue();
        if (b) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: 'fals' -> 'false' = ? \n" +
                      "result: booleanValue() == " + b + "\n");
        }

        if (exitCode != exitCode0)
            out.println("TEST FAILED");

        return exitCode;
    }
}
