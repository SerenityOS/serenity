/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.Random;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class Server {
    public static String start() throws Exception {
        int serverPort = 12345;
        ObjectName name = new ObjectName("test", "foo", "bar");
        MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        SteMBean bean = new Ste();
        jmxServer.registerMBean(bean, name);
        boolean exported = false;
        Random rnd = new Random(System.currentTimeMillis());
        do {
            try {
                LocateRegistry.createRegistry(serverPort);
                exported = true;
            } catch (ExportException ee) {
                if (ee.getCause() instanceof BindException) {
                    serverPort = rnd.nextInt(10000) + 4096;
                } else {
                    throw ee;
                }
            }

        } while (!exported);
        JMXServiceURL serverUrl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + serverPort + "/test");
        JMXConnectorServer jmxConnector = JMXConnectorServerFactory.newJMXConnectorServer(serverUrl, null, jmxServer);
        jmxConnector.start();

        return serverUrl.toString();
    }
}
