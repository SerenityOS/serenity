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

package nsk.jdi.IntegerArgument.intValue;

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
 * <code>com.sun.jdi.connect.Connector.IntegerArgument.intValue()</code> <BR>
 * complies with the following requirements in its specification:       <BR>
 *    "Returns the value of the argument as a int."                     <BR>
 * when the value of IntegerArgument is set     <BR>
 * with the method setValue(int value);         <BR>
 * The checking values are as follows:          <BR>
 *      min()   <BR>
 *      max()   <BR>
 *      min()+1 <BR>
 *      min()-1 <BR>
 *      max()+1 <BR>
 * <BR>
 * In case of a value set with setValue()                       <BR>
 * doesn't match a value get with the following intValue(),     <BR>
 * the test produces the return value 97 and    <BR>
 * a corresponding error message.               <BR>
 * Otherwise, the test is passed and produces   <BR>
 * the return value 95 and no message.          <BR>
 */

//
public class intvalue001 {

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
                        "jdi.Connector.IntegerArgument.intValue\n" ;
//
        String sErr2 =  "ERROR\n" +
                        "Method tested: " +
                        "jdi.Connector.IntegerArgument.intValue()\n" ;

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

        Integer intI = null;
        int i;

        i = intArgument.min();
        intArgument.setValue(i);
        if (intArgument.intValue() != i) {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: setValue(min()); intValue() == min()\n" +
                     "result: false\n");
        }

        i = intArgument.max();
        intArgument.setValue(i);
        if (intArgument.intValue() != i) {
            exitCode = exitCode2;
            out.println(sErr2 +
                     "check: setValue(max()); intValue() == max()\n" +
                     "result: false\n");
        }

        if (intArgument.min() < intArgument.max()) {
            i = intArgument.min() + 1;
            intArgument.setValue(i);
            if (intArgument.intValue() != i) {
                exitCode = exitCode2;
                out.println(sErr2 +
                        "check: setValue(min()+1); intValue() == min()+1\n" +
                        "result: false\n");
            }
        }

        if (intArgument.min() > intI.MIN_VALUE) {
            i = intArgument.min() - 1;
            intArgument.setValue(i);
            if (intArgument.intValue() != i) {
                exitCode = exitCode2;
                out.println(sErr2 +
                        "check: setValue(min()-1); intValue() == min()-1\n" +
                        "result: false\n");
            }
        }

        if (intArgument.max() < intI.MAX_VALUE) {
            i = intArgument.max() + 1;
            intArgument.setValue(i);
            if (intArgument.intValue() != i) {
                exitCode = exitCode2;
                out.println(sErr2 +
                        "check: setValue(max()+1); intValue() == max()+1\n" +
                        "result: false\n");
            }
        }

        if (exitCode != exitCode0) {
            out.println("TEST FAILED");
        }
        return exitCode;
    }
}
