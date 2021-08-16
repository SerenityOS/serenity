/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import lib.jdb.Debuggee;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
 * @test
 * @bug 8184770
 * @summary Tests that JDWP agent honors jdk net properties
 * @library /test/lib
 *
 * @build HelloWorld JdwpNetProps
 * @run main/othervm JdwpNetProps
 */
public class JdwpNetProps {

    public static void main(String[] args) throws Exception {
        InetAddress addrs[] = InetAddress.getAllByName("localhost");
        InetAddress ipv4Address = null;
        InetAddress ipv6Address = null;
        for (int i =  0; i < addrs.length; i++) {
            if (addrs[i] instanceof Inet4Address) {
                ipv4Address = addrs[i];
            } else if (addrs[i] instanceof Inet6Address) {
                ipv6Address = addrs[i];
            }
        }

        if (ipv4Address != null) {
            new ListenTest("localhost", ipv4Address)
                    .preferIPv4Stack(true)
                    .run(TestResult.Success);
            new ListenTest("localhost", ipv4Address)
                    .preferIPv4Stack(false)
                    .run(TestResult.Success);
            if (ipv6Address != null) {
                // - only IPv4, so connection prom IPv6 should fail
                new ListenTest("localhost", ipv6Address)
                        .preferIPv4Stack(true)
                        .preferIPv6Addresses(true)
                        .run(TestResult.AttachFailed);
                // - listen on IPv4
                new ListenTest("localhost", ipv6Address)
                        .preferIPv6Addresses(false)
                        .run(TestResult.AttachFailed);
                // - listen on IPv6
                new ListenTest("localhost", ipv6Address)
                        .preferIPv6Addresses(true)
                        .run(TestResult.Success);
            }
        } else {
            // IPv6-only system - expected to fail on IPv4 address
            new ListenTest("localhost", ipv6Address)
                    .preferIPv4Stack(true)
                    .run(TestResult.ListenFailed);
        }
    }

    private enum TestResult {
        Success,
        ListenFailed,
        AttachFailed
    }

    private static class ListenTest {
        private final String listenAddress;
        private final InetAddress connectAddress;
        private Boolean preferIPv4Stack;
        private Boolean preferIPv6Addresses;
        public ListenTest(String listenAddress, InetAddress connectAddress) {
            this.listenAddress = listenAddress;
            this.connectAddress = connectAddress;
        }
        public ListenTest preferIPv4Stack(Boolean value) {
            preferIPv4Stack = value;
            return this;
        }
        public ListenTest preferIPv6Addresses(Boolean value) {
            preferIPv6Addresses = value;
            return this;
        }

        public void run(TestResult expectedResult) throws Exception {
            List<String> options = new LinkedList<>();
            if (preferIPv4Stack != null) {
                options.add("-Djava.net.preferIPv4Stack=" + preferIPv4Stack.toString());
            }
            if (preferIPv6Addresses != null) {
                options.add("-Djava.net.preferIPv6Addresses=" + preferIPv6Addresses.toString());
            }
            log("Starting listening debuggee at " + listenAddress
                    + (expectedResult == TestResult.ListenFailed ? ": expected to fail" : ""));
            Exception error = null;
            try (Debuggee debuggee = Debuggee.launcher("HelloWorld")
                    .setAddress(listenAddress + ":0")
                    .addOptions(options).launch()) {
                log("Debuggee is listening on " + listenAddress + ":" + debuggee.getAddress());
                log("Connecting from " + connectAddress.getHostAddress()
                        + ", expected: " + (expectedResult == TestResult.Success ? "Success" : "Failure"));
                try {
                    VirtualMachine vm = attach(connectAddress.getHostAddress(), debuggee.getAddress());
                    vm.dispose();
                    if (expectedResult == TestResult.Success) {
                        log("Attached successfully (as expected)");
                    } else {
                        error = new RuntimeException("ERROR: attached successfully");
                    }
                } catch (Exception ex) {
                    if (expectedResult == TestResult.AttachFailed) {
                        log("Attach failed (as expected)");
                    } else {
                        error = new RuntimeException("ERROR: failed to attach", ex);
                    }
                }
            } catch (Exception ex) {
                if (expectedResult == TestResult.ListenFailed) {
                    log("Listen failed (as expected)");
                } else {
                    error = new RuntimeException("ERROR: listen failed", ex);
                }
            }
            if (error != null) {
                throw error;
            }
        }
    }

    private static String ATTACH_CONNECTOR = "com.sun.jdi.SocketAttach";
    // cache socket attaching connector
    private static AttachingConnector attachingConnector;

    private static VirtualMachine attach(String address, String port) throws IOException {
        if (attachingConnector == null) {
            attachingConnector = (AttachingConnector)getConnector(ATTACH_CONNECTOR);
        }
        Map<String, Connector.Argument> args = attachingConnector.defaultArguments();
        setConnectorArg(args, "hostname", address);
        setConnectorArg(args, "port", port);
        try {
            return attachingConnector.attach(args);
        } catch (IllegalConnectorArgumentsException e) {
            // unexpected.. wrap in RuntimeException
            throw new RuntimeException(e);
        }
    }

    private static Connector getConnector(String name) {
        List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
        for (Iterator<Connector> iter = connectors.iterator(); iter.hasNext(); ) {
            Connector connector = iter.next();
            if (connector.name().equalsIgnoreCase(name)) {
                return connector;
            }
        }
        throw new IllegalArgumentException("Connector " + name + " not found");
    }

    private static void setConnectorArg(Map<String, Connector.Argument> args, String name, String value) {
        Connector.Argument arg = args.get(name);
        if (arg == null) {
            throw new IllegalArgumentException("Argument " + name + " is not defined");
        }
        arg.setValue(value);
    }

    private static void log(Object o) {
        System.out.println(String.valueOf(o));
    }

}
