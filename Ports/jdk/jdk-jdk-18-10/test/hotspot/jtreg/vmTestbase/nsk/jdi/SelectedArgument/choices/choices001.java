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

package nsk.jdi.SelectedArgument.choices;

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
import com.sun.jdi.connect.Connector.SelectedArgument;


/**
 * The test for the implementation of an object of the type <BR>
 * Connector.SelectedArgument. <BR>
 * <BR>
 * The test checks up that results of the method                    <BR>
 * <code>com.sun.jdi.connect.Connector.SelectedArgument.choices()</code> <BR>
 * complies with its specification, in particular,      <BR>
 * a returned value is a non-empty "List of String".    <BR>
 * Values of Strings are not checked up.                <BR>
 * <BR>
 * In case of error the test produces the return value 97 and <BR>
 * the test produces the return value 97 and    <BR>
 * a corresponding error message.               <BR>
 * Otherwise, the test is passed and produces   <BR>
 * the return value 95 and no message.          <BR>
 */


public class choices001 {

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
                        "jdi.Connector.SelectedArgument.choices\n" ;
//
        String sErr2 =  "ERROR\n" +
                        "Method tested: " +
                        "jdi.Connector.SelectedArgument.choices\n" ;


        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List connectorsList = vmm.allConnectors();
        Iterator connectorsListIterator = connectorsList.iterator();
//
        Connector.SelectedArgument argument = null;

        for ( ; ; ) {
            try {
//out.println("connector");
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
//out.println("          argument");
                            argument = (Connector.SelectedArgument)
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
                    "no Connector with SelectedArgument found\n");
                return exitCode0;
            }
        }

        List listofChoices = argument.choices();

        if (listofChoices.isEmpty()) {
            exitCode = exitCode2;
            out.println(sErr2 +
                      "check: isEmpty\n" +
                      "error: returned List of String is empty\n");
        } else {
            Iterator listIterator = listofChoices.iterator();

            for ( ; ; ) {
                try {
                    String choice = (String) listIterator.next();

                } catch ( ClassCastException e1 ) {
                    exitCode = exitCode2;
                    out.println(sErr2 +
                              "check: String\n" +
                              "error: List contains non-String\n");
                            break ;
                 } catch ( NoSuchElementException e2) {
                            break ;
                 }
            }
        }

        if (exitCode != exitCode0)
            out.println("TEST FAILED");

        return exitCode;
    }
}
