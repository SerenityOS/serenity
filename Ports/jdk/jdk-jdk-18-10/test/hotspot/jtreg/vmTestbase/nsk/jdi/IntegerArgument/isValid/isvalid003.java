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

package nsk.jdi.IntegerArgument.isValid;

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
import com.sun.jdi.connect.Connector.IntegerArgument;


/**
 * The test for the implementation of an object of the type <BR>
 * Connector_IntegerArgument. <BR>
 * <BR>
 * The test checks up that results of the method                    <BR>
 * <code>Connector.IntegerArgument.isValid(String value)</code>     <BR>
 * complies with the following requirements:                        <BR>
 *      "true if value represents an int that is                    <BR>
 *               min() <= value <= max()"                           <BR>
 * when the value is one of the following strings:                  <BR>
 *      "a"             <BR>
 *      ""              <BR>
 *      (String) null   <BR>
 *      a null-string   <BR>
 * to which expected results are false. <BR>
 * <BR>
 * In case of a wrong result returned,          <BR>
 * the test produces the return value 97 and    <BR>
 * a corresponding error message.               <BR>
 * Otherwise, the test is passed and produces   <BR>
 * the return value 95 and no message.          <BR>
 */

//
public class isvalid003 {

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
                        "jdi.Connector.IntegerArgument.isValid\n" ;
//
        String sErr2 =  "ERROR\n" +
                        "Method tested: " +
                        "jdi.Connector.IntegerArgument.isValid\n" ;

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List connectorsList = vmm.allConnectors();
        Iterator connectorsListIterator = connectorsList.iterator();
//
        Connector.IntegerArgument intArgument = null;

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
                            intArgument = (Connector.IntegerArgument)
                                       defaultArguments.get(argName);
                            break ;
                        } catch ( ClassCastException e) {
                        }
                    } catch ( NoSuchElementException e) {
                        break ;
                    }
                }
                if (intArgument != null) {
                    break ;
                }
            } catch ( NoSuchElementException e) {
                out.println(sErr1 +
//
                    "no Connector with IntegerArgument found\n");
                return exitCode0;
            }
        }

        if (!intArgument.isValid("")) {
        } else {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: isValid('')\n" +
                     "result: true\n");
        }

        if (!intArgument.isValid("a")) {
        } else {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: isValid('a')\n" +
                     "result: true\n");
        }

        if (!intArgument.isValid((String) null)) {
        } else {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: isValid((String) null))\n" +
                     "result: true\n");
        }

        String s = null;
        if (!intArgument.isValid(s)) {
        } else {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: isValid(String s = null)\n" +
                     "result: true\n");
        }

        if (exitCode != exitCode0) {
            out.println("TEST FAILED");
        }
        return exitCode;
    }
}
