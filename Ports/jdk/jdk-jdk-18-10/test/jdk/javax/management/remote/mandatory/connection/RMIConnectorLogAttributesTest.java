/*
 * Copyright (c) 2016, Red Hat Inc.
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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * @test
 * @bug 8147857
 * @summary Tests whether RMIConnector logs attribute names correctly.
 * @author Severin Gehwolf
 *
 * @modules java.logging
 *          java.management.rmi
 */
public class RMIConnectorLogAttributesTest {

    private static final String ILLEGAL = ", FirstName[LastName]";
    private static final Logger logger = Logger.getLogger("javax.management.remote.rmi");
    private static final String ANY_NAME = "foo";
    private static final TestLogHandler handler;
    static {
        handler = new TestLogHandler(ILLEGAL);
        handler.setLevel(Level.FINEST);
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    private JMXConnectorServer startServer(int rmiPort) throws Exception {
        System.out.println("DEBUG: Create RMI registry on port " + rmiPort);
        LocateRegistry.createRegistry(rmiPort);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        HashMap<String,Object> env = new HashMap<String,Object>();

        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + rmiPort + "/jmxrmi");
        JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        cs.start();
        System.out.println("DEBUG: Started the RMI connector server");
        return cs;
    }

    private int findPort() {
        for (int i = 13333; i < 13333 + 100; i++) {
            try {
                ServerSocket socket = new ServerSocket(i);
                socket.close();
                return i;
            } catch (IOException e) {
                continue;
            }
        }
        return -1;
    }

    private void runTest() {
        int rmiPort = findPort();
        if (rmiPort == -1) {
            throw new RuntimeException("Test failed. No available port");
        }
        JMXConnectorServer server = null;
        try {
            server = startServer(rmiPort);
            JMXConnector connector = connectToServer(server);
            doTest(connector);
        } catch (Exception e) {
            throw new RuntimeException("Test failed unexpectedly", e);
        } finally {
            if (server != null) {
                try {
                    server.stop();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private JMXConnector connectToServer(JMXConnectorServer server) throws IOException, MalformedObjectNameException, NullPointerException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException, ReflectionException, MBeanException {
        JMXServiceURL url = server.getAddress();
        Map<String, Object> env = new HashMap<String, Object>();
        JMXConnector connector = JMXConnectorFactory.connect(url, env);

        System.out.println("DEBUG: Client connected to RMI at: " + url);

        return connector;
    }

    private void doTest(JMXConnector connector) throws IOException,
    MalformedObjectNameException, ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException,
    MBeanException, NotCompliantMBeanException, InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException {
        MBeanServerConnection  mbsc = connector.getMBeanServerConnection();


        ObjectName objName = new ObjectName("com.redhat.test.jmx:type=NameMBean");
        System.out.println("DEBUG: Calling createMBean");
        mbsc.createMBean(Name.class.getName(), objName);

        System.out.println("DEBUG: Calling setAttributes");
        AttributeList attList = new AttributeList();
        attList.add(new Attribute("FirstName", ANY_NAME));
        attList.add(new Attribute("LastName", ANY_NAME));
        mbsc.setAttributes(objName, attList);
    }

    public static void main(String[] args) throws Exception {
        RMIConnectorLogAttributesTest test = new RMIConnectorLogAttributesTest();
        test.runTest();
        if (handler.testFailed()) {
            throw new RuntimeException("Test failed. Logged incorrect: '" + ILLEGAL + "'");
        }
        System.out.println("Test passed!");
    }

}
