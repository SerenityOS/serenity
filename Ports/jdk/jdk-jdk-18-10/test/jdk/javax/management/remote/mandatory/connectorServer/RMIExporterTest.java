/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 5016705
 * @summary Tests the use of the RMIExporter class.
 * @author Luis-Miguel Alventosa
 * @modules java.management.rmi/com.sun.jmx.remote.internal.rmi
 * @run clean RMIExporterTest
 * @run build RMIExporterTest
 * @run main RMIExporterTest
 */

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import com.sun.jmx.remote.internal.rmi.RMIExporter;
import java.io.ObjectInputFilter;

public class RMIExporterTest {

    public static class CustomRMIExporter implements RMIExporter {

        public boolean rmiServerExported = false;
        public boolean rmiServerUnexported = false;
        public boolean rmiConnectionExported = false;
        public boolean rmiConnectionUnexported = false;

        public Remote exportObject(Remote obj,
                                   int port,
                                   RMIClientSocketFactory csf,
                                   RMIServerSocketFactory ssf,
                                   ObjectInputFilter unused)
            throws RemoteException {
            System.out.println("CustomRMIExporter::exportObject():: " +
                               "Remote = " + obj);
            if (obj.toString().startsWith(
                    "javax.management.remote.rmi.RMIJRMPServerImpl"))
                rmiServerExported = true;
            if (obj.toString().startsWith(
                    "javax.management.remote.rmi.RMIConnectionImpl"))
                rmiConnectionExported = true;
            return UnicastRemoteObject.exportObject(obj, port, csf, ssf);
        }

        public boolean unexportObject(Remote obj, boolean force)
            throws NoSuchObjectException {
            System.out.println("CustomRMIExporter::unexportObject():: " +
                               "Remote = " + obj);
            if (obj.toString().startsWith(
                    "javax.management.remote.rmi.RMIJRMPServerImpl"))
                rmiServerUnexported = true;
            if (obj.toString().startsWith(
                    "javax.management.remote.rmi.RMIConnectionImpl"))
                rmiConnectionUnexported = true;
            return UnicastRemoteObject.unexportObject(obj, force);
        }
    }

    public static void main(String[] args) {

        try {
            // Instantiate the MBean server
            //
            System.out.println("Create the MBean server");
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();

            // Initialize environment map to be passed to the connector server
            //
            System.out.println("Initialize environment map");
            HashMap env = new HashMap();
            CustomRMIExporter exporter = new CustomRMIExporter();
            env.put(RMIExporter.EXPORTER_ATTRIBUTE, exporter);

            // Create an RMI connector server
            //
            System.out.println("Create an RMI connector server");
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
            cs.start();

            // Create an RMI connector client
            //
            System.out.println("Create an RMI connector client");
            JMXConnector cc =
                JMXConnectorFactory.connect(cs.getAddress(), null);

            // Close RMI connector client
            //
            System.out.println("Close the RMI connector client");
            cc.close();

            // Stop RMI connector server
            //
            System.out.println("Stop the RMI connector server");
            cs.stop();

            // Check if remote objects were exported/unexported successfully
            //
            int errorCount = 0;

            if (exporter.rmiServerExported) {
                System.out.println("RMIServer exported OK!");
            } else {
                System.out.println("RMIServer exported KO!");
                errorCount++;
            }

            if (exporter.rmiServerUnexported) {
                System.out.println("RMIServer unexported OK!");
            } else {
                System.out.println("RMIServer unexported KO!");
                errorCount++;
            }

            if (exporter.rmiConnectionExported) {
                System.out.println("RMIConnection exported OK!");
            } else {
                System.out.println("RMIConnection exported KO!");
                errorCount++;
            }

            if (exporter.rmiConnectionUnexported) {
                System.out.println("RMIConnection unexported OK!");
            } else {
                System.out.println("RMIConnection unexported KO!");
                errorCount++;
            }

            System.out.println("Bye! Bye!");

            if (errorCount > 0) {
                System.out.println("RMIExporterTest FAILED!");
                System.exit(1);
            } else {
                System.out.println("RMIExporterTest PASSED!");
            }
        } catch (Exception e) {
            System.out.println("Unexpected exception caught = " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
