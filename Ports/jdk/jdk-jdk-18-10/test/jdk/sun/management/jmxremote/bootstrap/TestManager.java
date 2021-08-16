/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *
 * A test "management tool" used by unit tests -
 *   LocalManagementTest.java, CustomLauncherTest.java
 *
 * Usage:    java TestManager <pid> <port>
 *
 * where <pid> is the process-id of the test application, and <port> is
 * TCP port is used to shutdown the application.
 */
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnector;
import java.lang.management.RuntimeMXBean;
import static java.lang.management.ManagementFactory.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;

// Sun specific
import com.sun.tools.attach.VirtualMachine;

// Sun implementation specific
import jdk.internal.agent.ConnectorAddressLink;

public class TestManager {

    /*
     * Starts the management agent in the target VM
     */
    private static void startManagementAgent(String pid) throws IOException {
        try {
            VirtualMachine.attach(pid).startLocalManagementAgent();
        } catch (Exception x) {
            throw new IOException(x.getMessage(), x);
        }
    }

    private static void connect(String pid, String address) throws Exception {
        if (address == null) {
            throw new RuntimeException("Local connector address for " +
                                       pid + " is null");
        }

        System.out.println("Connect to process " + pid + " via: " + address);

        JMXServiceURL url = new JMXServiceURL(address);
        JMXConnector c = JMXConnectorFactory.connect(url);
        MBeanServerConnection server = c.getMBeanServerConnection();

        System.out.println("Connected.");

        RuntimeMXBean rt = newPlatformMXBeanProxy(server,
            RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        System.out.println(rt.getName());

        // close the connection
        c.close();
    }


    private final static String LOCAL_CONNECTOR_ADDRESS_PROP =
        "com.sun.management.jmxremote.localConnectorAddress";
    public static void main(String[] args) throws Exception {
        String pid = args[0]; // pid as a string
        System.out.println("Starting TestManager for PID = " + pid);
        System.out.flush();
        VirtualMachine vm = VirtualMachine.attach(pid);

        String agentPropLocalConnectorAddress = (String)
            vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS_PROP);

        int vmid = Integer.parseInt(pid);
        String jvmstatLocalConnectorAddress =
            ConnectorAddressLink.importFrom(vmid);

        if (agentPropLocalConnectorAddress == null &&
            jvmstatLocalConnectorAddress == null) {
            // No JMX Connector address so attach to VM, and start local agent
            startManagementAgent(pid);
            agentPropLocalConnectorAddress = (String)
                vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS_PROP);
            jvmstatLocalConnectorAddress =
                ConnectorAddressLink.importFrom(vmid);
        }


        // Test address obtained from agent properties
        System.out.println("Testing the connector address from agent properties");
        connect(pid, agentPropLocalConnectorAddress);

        // Test address obtained from jvmstat buffer
        System.out.println("Testing the connector address from jvmstat buffer");
        connect(pid, jvmstatLocalConnectorAddress);

        // Shutdown application
        int port = Integer.parseInt(args[1]);
        System.out.println("Shutdown process via TCP port: " + port);
        Socket s = new Socket();
        s.connect(new InetSocketAddress(port));
        s.close();
    }
}
