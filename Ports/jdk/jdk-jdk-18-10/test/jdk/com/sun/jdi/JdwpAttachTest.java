/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * @test
 * @bug 8184770
 * @summary Tests for JDWP agent attach functionality (including IPv6 support)
 * @library /test/lib
 *
 * @build HelloWorld JdwpAttachTest
 * @run main/othervm JdwpAttachTest
 */
public class JdwpAttachTest {

    // Set to true to perform testing of attach from wrong address (expected to fail).
    // It's off by default as it caused significant test time increase
    // (tests <number_of_addresses> * <number_of_addresses> cases, each case fails by timeout).
    private static boolean testFailedAttach = false;

    // A flag to exclude testcases which can cause intermittent CI failures.
    private static boolean testPotentiallyUnstableCases = false;

    public static void main(String[] args) throws Exception {
        List<InetAddress> addresses = Utils.getAddressesWithSymbolicAndNumericScopes();

        boolean ipv4EnclosedTested = false;
        boolean ipv6EnclosedTested = false;
        for (InetAddress addr: addresses) {
            if (testFailedAttach) {
                for (InetAddress connectAddr : addresses) {
                    attachTest(addr.getHostAddress(), connectAddr.getHostAddress(), addr.equals(connectAddr));
                }
            } else {
                attachTest(addr.getHostAddress(), addr.getHostAddress(), true);
            }
            // listening on "*" should accept connections from all addresses
            attachTest("*", addr.getHostAddress(), true);

            // also test that addresses enclosed in square brackets are supported.
            if (addr instanceof Inet4Address && !ipv4EnclosedTested) {
                attachTest("[" + addr.getHostAddress() + "]", "[" + addr.getHostAddress() + "]", true);
                ipv4EnclosedTested = true;
            }
            if (addr instanceof Inet6Address && !ipv6EnclosedTested) {
                attachTest("[" + addr.getHostAddress() + "]", "[" + addr.getHostAddress() + "]", true);
                ipv6EnclosedTested = true;
            }
        }

        // By using "localhost" or empty hostname we should be able to attach
        // to both IPv4 and IPv6 addresses (127.0.0.1 & ::1).
        // By default test verifies only "preferred" family (IPv4 or IPv6).
        // Need to set testPotentiallyUnstableCases to true to perform full testing.
        InetAddress localAddresses[] = InetAddress.getAllByName("localhost");
        for (int i = 0; i < localAddresses.length; i++) {
            if (testPotentiallyUnstableCases || addressIsSafeToConnectToLocalhost(localAddresses[i])) {
                attachTest(localAddresses[i].getHostAddress(), "", true);
            }
        }
    }

    private static void attachTest(String listenAddress, String connectAddress, boolean expectedResult)
            throws Exception {
        log("\nTest: listen on '" + listenAddress + "', attach to '" + connectAddress + "'");
        log("  Starting listening at " + listenAddress);
        ListeningConnector connector = getListenConnector();
        Map<String, Connector.Argument> args = connector.defaultArguments();
        setConnectorArg(args, "localAddress", listenAddress);
        setConnectorArg(args, "port", "0");

        String actualAddress = connector.startListening(args);
        String actualPort = actualAddress.substring(actualAddress.lastIndexOf(':') + 1);
        String port = args.get("port").value();
        // port from connector.startListening must be the same as values from arguments
        if (!port.equals(actualPort)) {
            throw new RuntimeException("values from connector.startListening (" + actualPort
                    + " is not equal to values from arguments (" + port + ")");
        }
        log("  Listening port: " + port);

        log("  Attaching to " + connectAddress);
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit((Callable<Exception>)() -> {
                VirtualMachine vm = connector.accept(args);
                vm.dispose();
                return null;
            });
            executor.shutdown();

            try {
                LingeredApp debuggee = LingeredApp.startApp(
                        "-agentlib:jdwp=transport=dt_socket"
                                + ",address=" + connectAddress + ":" + port
                                + ",server=n,suspend=n"
                                // if failure is expected set small timeout (default is 20 sec)
                                + (!expectedResult ? ",timeout=1000" : ""));
                debuggee.stopApp();
                if (expectedResult) {
                    log("OK: attach succeeded as expected");
                } else {
                    throw new RuntimeException("ERROR: attach succeeded but was expected to fail");
                }
            } catch (Exception ex) {
                if (expectedResult) {
                    throw new RuntimeException("ERROR: attach failed but was expected to succeed");
                } else {
                    log("OK: attach failed as expected");
                }
            }
        } finally {
            connector.stopListening(args);
        }
    }

    private static String LISTEN_CONNECTOR = "com.sun.jdi.SocketListen";

    private static ListeningConnector getListenConnector() {
        return (ListeningConnector)getConnector(LISTEN_CONNECTOR);
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

    // Attach to localhost tries to connect to both IPv4 and IPv6 loopback addresses.
    // But sometimes it causes interference with other processes which can listen
    // on the same port but different loopback address.
    // The method checks if the address is safe to test with current network config.
    private static boolean addressIsSafeToConnectToLocalhost(InetAddress addr) {
        boolean ipv6 = Boolean.parseBoolean(System.getProperty("java.net.preferIPv6Addresses"));
        return ipv6 == (addr instanceof Inet6Address);
    }

    private static long startTime = System.currentTimeMillis();

    private static void log(Object o) {
        long time = System.currentTimeMillis() - startTime;
        System.out.println(String.format("[%7.3f] %s", (time / 1000f), String.valueOf(o)));
    }

}
