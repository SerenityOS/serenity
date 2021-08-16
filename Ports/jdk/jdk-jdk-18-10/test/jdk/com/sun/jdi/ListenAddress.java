/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4932074
 * @summary Test that startListening(Map) method of the com.sun.jdi.SocketListen
 *          Connector returns an address that is usable for the address option on
 *          remove debuggees.
 */
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.io.IOException;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.connect.*;

public class ListenAddress {

    /*
     * Find Connector by name
     */
    private static Connector findConnector(String name) {
        List connectors = Bootstrap.virtualMachineManager().allConnectors();
        Iterator iter = connectors.iterator();
        while (iter.hasNext()) {
            Connector connector = (Connector)iter.next();
            if (connector.name().equals(name)) {
                return connector;
            }
        }
        return null;
    }

    /*
     * Failure count
     */
    static int failures = 0;

    /*
     * Start listening with the specified Connector and check that the
     * returned address includes a host component. If 'addr' is not null
     * then set the localAddress argument to be the address.
     */
    private static void check(ListeningConnector connector, InetAddress addr)
        throws IOException, IllegalConnectorArgumentsException
    {
        Map args = connector.defaultArguments();
        if (addr != null) {
            Connector.StringArgument addr_arg =
              (Connector.StringArgument)args.get("localAddress");
            addr_arg.setValue(addr.getHostAddress());
        }

        String address = connector.startListening(args);
        if (address.indexOf(':') < 0) {
            System.out.println(address + " => Failed - no host component!");
            failures++;
        } else {
            System.out.println(address);
        }
        connector.stopListening(args);
    }

    public static void main(String args[]) throws Exception {
        ListeningConnector connector = (ListeningConnector)findConnector("com.sun.jdi.SocketListen");

        // check wildcard address
        check(connector, (InetAddress)null);

        // iterate over all IPv4 addresses and check that binding to
        // that address results in the correct result from startListening(Map)
        Enumeration nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface ni = (NetworkInterface)nifs.nextElement();
            Enumeration addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = (InetAddress)addrs.nextElement();

                // JPDA implementation only currently supports IPv4
                if (!(addr instanceof Inet4Address)) {
                    continue;
                }

                check(connector, addr);
            }
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " test(s) failed - see output for details.");
        }
    }
}
