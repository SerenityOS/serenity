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

/*
 * @test
 * @bug 4944680
 * @summary Tests that IllegalArgumentException is thrown when
 *          MBeanServerForwrder is null.
 * @author Daniel Fuchs
 * @modules java.management.rmi
 *          java.management/com.sun.jmx.remote.security
 * @run clean SetMBeanServerForwarder
 * @run build SetMBeanServerForwarder
 * @run main SetMBeanServerForwarder
 */
import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;
import java.util.Map;

import com.sun.jmx.remote.security.MBeanServerAccessController;

public class SetMBeanServerForwarder {

    static boolean isPresent(String cn) {
        try {
            Class.forName(cn);
            return true;
        } catch (ClassNotFoundException x) {
            return false;
        }
    }

    public static int test(String... urls) {
        int errorCount = 0;
        for (int i=0;i<urls.length;i++) {
            try {
                final MBeanServer mbs = MBeanServerFactory.newMBeanServer();
                final JMXConnectorServer cs1,cs2;
                final JMXServiceURL      url;

                System.out.println("*** -----------------------------------");
                System.out.println("*** JMXConnectorServer("+urls[i]+")");
                System.out.println("*** -----------------------------------");

                try {
                    url = new JMXServiceURL(urls[i]);
                    cs1 = JMXConnectorServerFactory
                        .newJMXConnectorServer(url,(Map)null,mbs);
                    cs2 = JMXConnectorServerFactory
                        .newJMXConnectorServer(url,(Map)null,null);
                } catch (Throwable thr) {
                    System.out.println("Failed to create ConnectorServer "+
                                       "from [" + urls[i] +"]: " + thr);
                    thr.printStackTrace();
                    errorCount++;
                    continue;
                }

                // Test using a JMXConnectorServer already connected to an
                // MBeanServer
                //

                // Set null MBeanServerForwarder - expect exception
                //
                try {
                    cs1.setMBeanServerForwarder(null);
                    errorCount++;
                    System.out.println("Expected IllegalArgumentException "+
                                       " not thrown (null forwarder) for " +
                                       url);
                    System.out.println("\t\t[connected to MBeanServer]");
                } catch (IllegalArgumentException iae) {
                    System.out.println("Received expected exception: " +
                                       iae);
                }

                // Now try with a real MBSF - should not throw exception
                //
                try {
                    final MBeanServerForwarder fwd = new
                        MBeanServerAccessController() {
                            protected void checkRead() {}
                            protected void checkWrite() {}
                        };
                    cs1.setMBeanServerForwarder(fwd);

                    // Verify that the MBSF was correctly set.
                    //
                    if (cs1.getMBeanServer() != fwd) {
                        System.out.println("MBeanServerForwarder not set "+
                                           "for " + url);
                        System.out.println("\t\t[connected to MBeanServer]");
                        throw new AssertionError("cs1.getMBeanServer()!=fwd");
                    }

                    // Verify that the MBS was correctly forwarded to the MBSF
                    //
                    if (fwd.getMBeanServer() != mbs) {
                        System.out.println("MBeanServer not set in Forwarder"+
                                           " for " + url);
                        System.out.println("\t\t[connected to MBeanServer]");
                        throw new AssertionError("fwd.getMBeanServer()!=mbs");
                    }
                    System.out.println("MBeanServerForwarder successfully "+
                                       "set for " + url);
                    System.out.println("\t\t[connected to MBeanServer]");
                } catch (Throwable x) {
                    errorCount++;
                    System.out.println("Failed to set forwarder for " +
                                       url);
                    System.out.println("\t\t[connected to MBeanServer]");
                    System.out.println("Unexpected exception: " +
                                       x);
                    x.printStackTrace();
                }

                // Test using a JMXConnectorServer not connected to any
                // MBeanServer
                //

                // Set null MBeanServerForwarder - expect exception
                //
                try {
                    cs2.setMBeanServerForwarder(null);
                    errorCount++;
                    System.out.println("Expected IllegalArgumentException "+
                                       " not thrown (null forwarder) for " +
                                       url);
                    System.out.println("\t\t[not connected to MBeanServer]");
                } catch (IllegalArgumentException iae) {
                    System.out.println("Received expected exception: " +
                                       iae);
                }

                // Now try with a real MBSF - should not throw exception
                //
                try {
                    final MBeanServerForwarder fwd = new
                        MBeanServerAccessController() {
                            protected void checkRead() {}
                            protected void checkWrite() {}
                        };
                    cs2.setMBeanServerForwarder(fwd);

                    // Verify that the MBSF was correctly set.
                    //
                    if (cs2.getMBeanServer() != fwd) {
                        System.out.println("MBeanServerForwarder not set "+
                                           "for " + url);
                        System.out.println("\t\t[not connected to MBeanServer]");
                        throw new AssertionError("cs2.getMBeanServer()!=fwd");
                    }

                    // Now register the connector
                    //
                    final ObjectName name =
                        new ObjectName(":type="+cs2.getClass().getName()+
                                       ",url="+ObjectName.quote(urls[i]));
                    mbs.registerMBean(cs2,name);
                    try {

                        // Verify that the MBSF was not disconnected.
                        //
                        if (cs2.getMBeanServer() != fwd) {
                            System.out.
                                println("MBeanServerForwarder changed "+
                                        "for " + url);
                            System.out.
                                println("\t\t[registerMBean]");
                            throw new
                                AssertionError("cs2.getMBeanServer()!=fwd");
                        }

                        // Verify that the MBS was not forwarded to the MBSF
                        //
                        if (fwd.getMBeanServer() != null) {
                            System.out.
                                println("MBeanServer changed in Forwarder"+
                                        " for " + url);
                            System.out.println("\t\t[registerMBean]");
                            throw new
                                AssertionError("fwd.getMBeanServer()!=null");
                        }

                    } finally {
                        mbs.unregisterMBean(name);
                    }

                    System.out.println("MBeanServerForwarder successfully "+
                                       "set for " + url);
                    System.out.println("\t\t[not connected to MBeanServer]");
                } catch (Throwable x) {
                    errorCount++;
                    System.out.println("Failed to set forwarder for " +
                                       url);
                    System.out.println("\t\t[not connected to MBeanServer]");
                    System.out.println("Unexpected exception: " +
                                       x);
                    x.printStackTrace();
                }

            } catch (Exception x) {
                System.err.println("Unexpected exception for " +
                                   urls[i] + ": " + x);
                x.printStackTrace();
                errorCount++;
            }
        }
        return errorCount;
    }

    public static void main(String args[]) {
        int errCount = 0;

        // mandatory
        errCount += test("service:jmx:rmi://");

        // optional
        if (isPresent("javax.management.remote.rmi._RMIConnectionImpl_Tie"))
            errCount += test("service:jmx:iiop://");
        if (isPresent("javax.management.remote.generic.GenericConnector"))
            errCount += test("service:jmx:jmxmp://");

        if (errCount > 0) {
            throw new RuntimeException("SetMBeanServerForwarder failed: " +
                                       errCount + " error(s) reported.");
        }
        System.out.println("SetMBeanServerForwarder passed.");
    }
}
