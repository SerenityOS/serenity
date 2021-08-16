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
 * @bug 4910428
 * @summary Tests target MBean class loader used before JSR 160 loader
 * @author Eamonn McManus
 *
 * @run clean TargetMBeanTest
 * @run build TargetMBeanTest
 * @run main TargetMBeanTest
 */

/*
  The JSR 160 spec says that, when invoking a method (or setting an
  attribute or creating) on a target MBean, that MBean's class loader
  is used to deserialize parameters.  The problem is that the RMI
  connector protocol wraps these parameters as MarshalledObjects.
  When you call get() on a MarshalledObject, the context class loader
  is used to deserialize, so if we set this to the target MBean's
  class loader everything should work.  EXCEPT that MarshalledObject
  first tries to load classes using the first class loader it finds in
  the caller's stack.  If our JSR 160 implementation is part of J2SE,
  it will not find any such class loader (only the system class
  loader).  But if it's standalone, then it will find the class loader
  of the JSR 160 implementation.  If the class name of a parameter is
  known to both the 160 loader and the target MBean loader, then we
  will use the wrong loader for deserialization and the attempt to
  invoke the target MBean with the deserialized object will fail.

  We test this as follows.  We fabricate an MLet that has the same set
  of URLs as the 160 class loader, which we assume is the system class
  loader (or at least, it is a URLClassLoader).  This MLet is
  therefore a "shadow class loader" -- for every class name known to
  the 160 class loader, it can load the same name, but the result is
  not the same class, since it has not been loaded by the same loader.
  Then, we use the MLet to create an RMIConnectorServer MBean.  This
  MBean is an instance of "shadow RMIConnectorServer", and its
  constructor has a parameter of type "shadow JMXServiceURL".  If the
  constructor is invoked with "real JMXServiceURL" it will fail.

  While we are at it, we also test that the behaviour is correct for
  the JMXMP protocol, if that optional protocol is present.
 */
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import javax.management.*;
import javax.management.loading.*;
import javax.management.remote.*;
import javax.management.remote.rmi.RMIConnectorServer;

public class TargetMBeanTest {
    private static final ObjectName mletName;
    static {
        try {
            mletName = new ObjectName("x:type=mlet");
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Test that target MBean class loader is used " +
                           "before JMX Remote API class loader");

        ClassLoader jmxRemoteClassLoader =
            JMXServiceURL.class.getClassLoader();
        if (jmxRemoteClassLoader == null) {
            System.out.println("JMX Remote API loaded by bootstrap " +
                               "class loader, this test is irrelevant");
            return;
        }
        if (!(jmxRemoteClassLoader instanceof URLClassLoader)) {
            System.out.println("TEST INVALID: JMX Remote API not loaded by " +
                               "URLClassLoader");
            System.exit(1);
        }

        URLClassLoader jrcl = (URLClassLoader) jmxRemoteClassLoader;
        URL[] urls = jrcl.getURLs();
        PrivateMLet mlet = new PrivateMLet(urls, null, false);
        Class shadowClass = mlet.loadClass(JMXServiceURL.class.getName());
        if (shadowClass == JMXServiceURL.class) {
            System.out.println("TEST INVALID: MLet got original " +
                               "JMXServiceURL not shadow");
            System.exit(1);
        }

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.registerMBean(mlet, mletName);

        final String[] protos = {"rmi", "iiop", "jmxmp"};
        boolean ok = true;
        for (int i = 0; i < protos.length; i++) {
            try {
                ok &= test(protos[i], mbs);
            } catch (Exception e) {
                System.out.println("TEST FAILED WITH EXCEPTION:");
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto, MBeanServer mbs)
            throws Exception {
        System.out.println("Testing for proto " + proto);

        JMXConnectorServer cs;
        JMXServiceURL url = new JMXServiceURL(proto, null, 0);
        try {
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null,
                                                                 mbs);
        } catch (MalformedURLException e) {
            System.out.println("System does not recognize URL: " + url +
                               "; ignoring");
            return true;
        }
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXServiceURL rmiurl = new JMXServiceURL("rmi", null, 0);
        JMXConnector client = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = client.getMBeanServerConnection();
        ObjectName on = new ObjectName("x:proto=" + proto + ",ok=yes");
        mbsc.createMBean(RMIConnectorServer.class.getName(),
                         on,
                         mletName,
                         new Object[] {rmiurl, null},
                         new String[] {JMXServiceURL.class.getName(),
                                       Map.class.getName()});
        System.out.println("Successfully deserialized with " + proto);
        mbsc.unregisterMBean(on);

        client.close();
        cs.stop();
        return true;
    }
}
