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

package nsk.jdi.IntegerArgument.setValue;

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
 * <code>com.sun.jdi.connect.Connector.IntegerArgument.setValue()</code> <BR>
 * complies with the following requirements in its specification:       <BR>
 *    "Sets the value of the argument."                                 <BR>
 * <BR>
 * If after two following one by one methods, in all four possible cases, <BR>
 * setValue(int) and super.setValue(String)                             <BR>
 * with different values to set, the value returned by intValue()       <BR>
 * isn't equal to the second value set          <BR>
 * the test produces the return value 97 and    <BR>
 * a corresponding error message.               <BR>
 * Otherwise, the test is passed and produces   <BR>
 * the return value 95 and no message.          <BR>
 */

//
public class setvalue001 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + 95); // JCK-compatible exit status
    }

    static int exitCode  = 0;
    static int exitCode0 = 0;
    static int exitCode2 = 2;

//
    static Connector.IntegerArgument intArgument = null;
    static int i;

    private static void check(int i1, PrintStream out) {

//
        String sErr2 =  "ERROR\n" +
                        "Method tested: " +
                        "jdi.Connector.IntegerArgument.setValue()\n" ;


            intArgument.setValue(i);
            intArgument.setValue(i1);
            if (intArgument.intValue() != i1) {
                exitCode = exitCode2;
                out.println(sErr2 +
                         "check: setValue(int); setValue(int)\n" +
                         "result: no equality\n");
            }

            intArgument.setValue(i);
            intArgument.setValue(intArgument.stringValueOf(i1));
            if (intArgument.intValue() != i1) {
                exitCode = exitCode2;
                out.println(sErr2 +
                         "check: setValue(int); setValue(String)\n" +
                         "result: no equality\n");
            }

            intArgument.setValue(intArgument.stringValueOf(i));
            intArgument.setValue(i1);
            if (intArgument.intValue() != i1) {
                exitCode = exitCode2;
                out.println(sErr2 +
                         "check: setValue(String); setValue(int)\n" +
                         "result: no equality\n");
            }

            intArgument.setValue(intArgument.stringValueOf(i));
            intArgument.setValue(intArgument.stringValueOf(i1));
            if (intArgument.intValue() != i1) {
                exitCode = exitCode2;
                out.println(sErr2 +
                         "check: setValue(String); setValue(String)\n" +
                         "result: no equality\n");
            }
    }

    public static int run(String argv[], PrintStream out) {

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List connectorsList = vmm.allConnectors();
        Iterator connectorsListIterator = connectorsList.iterator();
//
        String sErr1 =  "WARNING\n" +
                        "Method tested: " +
                        "jdi.Connector.IntegerArgument.setValue\n" ;

        Integer intI = null;

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


        if (intArgument.min() >= 0) {
            i = -1;
        } else {
            i = 1;
        }

        check(intArgument.min(), out);
        check(intArgument.max(), out);
        if (intArgument.min() < intArgument.max()) {
            check(intArgument.min() + 1, out);
        }
        if (intArgument.min() > intI.MIN_VALUE) {
            check(intArgument.min() - 1, out);
        }
        if (intArgument.max() < intI.MAX_VALUE) {
            check(intArgument.max() + 1, out);
        }

        if (exitCode != exitCode0) {
            out.println("TEST FAILED");
        }
        return exitCode;
    }
}
