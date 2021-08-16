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

package nsk.jdi.ListeningConnector.supportsMultipleConnections;

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
import com.sun.jdi.connect.ListeningConnector;


/**
 * The test for the implementation of an object of the type <BR>
 * ListeningConnector. <BR>
 * <BR>
 * The test checks up that the method                                   <BR>
 * <code>com.sun.jdi.connect.ListeningConnector.supportMultipleConnections()</code>     <BR>
 * complies with its specification.     <BR>
 * <BR>
 * In case of VMM doesn't have ListeningConnector <BR>
 * supporting multiple connections,             <BR>
 * the test prints warning message.             <BR>
 * The test is always passed and produces       <BR>
 * the return value 95.                         <BR>
 */

//
public class supportsmultipleconnections001 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + 95); // JCK-compatible exit status
    }

    public static int run(String argv[], PrintStream out) {

        int exitCode  = 1;
        int exitCode0 = 0;
//
        String sErr1 =  "WARNING\n" +
                        "Method tested: " +
                        "jdi.ListeningConnector.supportsMultipleConnections()\n" +
                        "no ListeningConnector supporting multiconnections\n" ;

        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List connectorsList = vmm.allConnectors();
        Iterator connectorsListIterator = connectorsList.iterator();

        for ( ; ; ) {
            try {
                ListeningConnector connector =
                (ListeningConnector) connectorsListIterator.next();
                if (connector.supportsMultipleConnections()) {
                    exitCode = exitCode0;
                }
            } catch ( ClassCastException e) {
            } catch ( NoSuchElementException e) {
                break ;
            }
        }

        if (exitCode != exitCode0) {
            out.println(sErr1);
        }
        return exitCode0;
    }
}
