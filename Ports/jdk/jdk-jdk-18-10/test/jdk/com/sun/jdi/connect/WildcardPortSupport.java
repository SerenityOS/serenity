/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163083
 * @summary verifies that multiple listeners could be started using wildcard port number
 */


import com.sun.jdi.*;
import com.sun.jdi.connect.*;

import java.io.IOException;
import java.io.PrintStream;

import java.util.Map;

public class WildcardPortSupport {

    private static final String PORT_ARG = "port";

    public static void main(String argv[]) throws Exception {
        WildcardPortSupport test = new WildcardPortSupport();
        test.runAllTests();
    }

    public void runAllTests() throws Exception {
        ListeningConnector connector =
                Bootstrap.virtualMachineManager().listeningConnectors().stream().
                        filter(c -> c.name().equals("com.sun.jdi.SocketListen")).findFirst().get();

        if (connector == null) {
            throw new RuntimeException("FAILURE: no com.sun.jdi.SocketListen connectors found!");
        }

        testWithDefaultArgs1(connector);
        testWithDefaultArgs2(connector);
        testWithWildcardPort1(connector);
        testWithWildcardPort2(connector);
        testWithDefaultArgsNegative(connector);
    }


    // Start listeners with unspecified port number and use their bound port numbers to stop them
    private void testWithDefaultArgs1(ListeningConnector connector) throws IOException,
            IllegalConnectorArgumentsException {
        int port1 = startListening(connector, connector.defaultArguments());
        int port2 = startListening(connector, connector.defaultArguments());
        connector.stopListening(getArgumentsMap(connector, port1));
        connector.stopListening(getArgumentsMap(connector, port2));
    }

    // Start listeners with unspecified port number and use the original argument map instances to stop them
    private void testWithDefaultArgs2(ListeningConnector connector) throws IOException,
            IllegalConnectorArgumentsException {
        Map<String, Connector.Argument> args1 = connector.defaultArguments();
        startListening(connector, args1);
        Map<String, Connector.Argument> args2 = connector.defaultArguments();
        startListening(connector, args2);
        connector.stopListening(args1);
        connector.stopListening(args2);
    }

    // Start listeners with wildcard  port number ("0") and use their bound port numbers to stop them
    private void testWithWildcardPort1(ListeningConnector connector) throws IOException,
            IllegalConnectorArgumentsException {
        int port1 = startListening(connector, getArgumentsMap(connector, 0));
        int port2 = startListening(connector, getArgumentsMap(connector, 0));
        connector.stopListening(getArgumentsMap(connector, port1));
        connector.stopListening(getArgumentsMap(connector, port2));
    }

    // Start listeners with wildcard port number ("0") and use the original argument map instances to stop them
    private void testWithWildcardPort2(ListeningConnector connector) throws IOException,
            IllegalConnectorArgumentsException {
        Map<String, Connector.Argument> args1 = getArgumentsMap(connector, 0);
        startListening(connector, args1);
        Map<String, Connector.Argument> args2 = getArgumentsMap(connector, 0);
        startListening(connector, args2);
        connector.stopListening(args1);
        connector.stopListening(args2);
    }

    // Tries to start two listeners using the same instance of default argument map
    private void testWithDefaultArgsNegative(ListeningConnector connector) throws IOException,
            IllegalConnectorArgumentsException {
        Map<String, Connector.Argument> args = connector.defaultArguments();
        connector.startListening(args);
        String port = args.get(PORT_ARG).value();
        if (port.isEmpty() || "0".equals(port)) {
            throw new RuntimeException("Test testWithDefaultArgsNegative failed." +
                    " The argument map was not updated with the bound port number.");
        }
        try {
            // This call should fail since the previous the argument map is
            // already updated with the port number of the started listener
            connector.startListening(args);
        } catch (IllegalConnectorArgumentsException ex) {
            System.out.println("Expected exception caught" + ex.getMessage());
            return;
        } finally {
            connector.stopListening(args);
        }
        throw new RuntimeException("Test testWithDefaultArgsNegative failed. No expected " +
                "com.sun.jdi.IllegalConnectorArgumentsException exception was thrown.");
    }

    private int startListening(ListeningConnector connector, Map<String, Connector.Argument> args)
            throws IOException, IllegalConnectorArgumentsException {
        String address = connector.startListening(args);
        return Integer.valueOf(address.split(":")[1]);
    }


    private Map<String, Connector.Argument> getArgumentsMap(ListeningConnector connector, int port) {
        Map<String, Connector.Argument> args = connector.defaultArguments();
        Connector.Argument arg = args.get(PORT_ARG);
        arg.setValue(String.valueOf(port));
        return args;
    }
}
