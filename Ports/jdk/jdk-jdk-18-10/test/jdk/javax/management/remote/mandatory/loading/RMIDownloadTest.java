/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5021246
 * @summary Check that class downloading is supported by RMI connector
 * @author Eamonn McManus
 *
 * @run main/othervm -Djava.security.manager=allow RMIDownloadTest receive without
 * @run main/othervm -Djava.security.manager=allow RMIDownloadTest send without
 * @run main/othervm -Djava.security.manager=allow RMIDownloadTest receive with
 * @run main/othervm -Djava.security.manager=allow RMIDownloadTest send with
 */

/*
 * This test checks that class downloading is supported by the RMI connector.
 * We copy a precompiled class file into the temporary directory (which we
 * assume is not in the classpath).  We also create an instance of that
 * class using a hardcoded ClassLoader.  Then we try to get a remote attribute
 * that returns that instance, and we try to set the remote attribute to the
 * instance.  In both cases, this will only work if the class can be downloaded
 * based on the codebase that we have set to the temporary directory.  We also
 * test that it does *not* work when the codebase is not set, in case the test
 * is succeeding for some other reason.
 *
 * We run the test four times, for each combination of (send, receive) x
 * (with-codebase, without-codebase).  Doing all four tests within the same
 * run doesn't work, probably because RMI remembers the codebase property
 * setting at some point.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.Permission;
import java.util.Arrays;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class RMIDownloadTest {
    /* Following byte array was produced from this class:
     *
     * public class Zooby implements java.io.Serializable {}
     *
     * by this program:
     *
     * public class MakeZooby {
     *     public static void main(String[] args) throws Exception {
     *         int b;
     *         for (int offset = 0; (b = System.in.read()) >= 0; offset++) {
     *             System.out.print((byte) b + "," +
     *                              ((offset % 16) == 15 ? '\n' : ' '));
     *         }
     *         System.out.println();
     *     }
     * }
     */
    private static final byte[] zoobyClassBytes = {
        -54, -2, -70, -66, 0, 0, 0, 49, 0, 12, 10, 0, 3, 0, 8, 7,
        0, 9, 7, 0, 10, 7, 0, 11, 1, 0, 6, 60, 105, 110, 105, 116,
        62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 12, 0,
        5, 0, 6, 1, 0, 5, 90, 111, 111, 98, 121, 1, 0, 16, 106, 97,
        118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 1, 0,
        20, 106, 97, 118, 97, 47, 105, 111, 47, 83, 101, 114, 105, 97, 108, 105,
        122, 97, 98, 108, 101, 0, 33, 0, 2, 0, 3, 0, 1, 0, 4, 0,
        0, 0, 1, 0, 1, 0, 5, 0, 6, 0, 1, 0, 7, 0, 0, 0,
        17, 0, 1, 0, 1, 0, 0, 0, 5, 42, -73, 0, 1, -79, 0, 0,
        0, 0, 0, 0,
    };

    private static class ZoobyClassLoader extends ClassLoader {
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("Zooby")) {
                return super.defineClass(name, zoobyClassBytes,
                        0, zoobyClassBytes.length);
            } else
                throw new ClassNotFoundException(name);
        }
    }


    private static MBeanServer pmbs;
    private static ObjectName getSetName;
    private static GetSet getSetInstance;

    public static void main(String[] args) throws Exception {
        int sendIndex = -1;
        int withIndex = -1;
        if (args.length == 2) {
            sendIndex =
                    Arrays.asList("send", "receive").indexOf(args[0]);
            withIndex =
                    Arrays.asList("with", "without").indexOf(args[1]);
        }
        if (sendIndex < 0 || withIndex < 0)
            throw new Exception("Usage: RMIDownloadTest (send|receive) (with|without)");
        final boolean send = (sendIndex == 0);
        final boolean with = (withIndex == 0);

        pmbs = ManagementFactory.getPlatformMBeanServer();
        getSetName = new ObjectName(":type=GetSet");
        getSetInstance = new GetSet();
        pmbs.registerMBean(getSetInstance, getSetName);

        System.setSecurityManager(new LaidBackSecurityManager());

//        System.setProperty("sun.rmi.loader.logLevel", "VERBOSE");

        String tmpdir = System.getProperty("java.io.tmpdir");
        String classfile = tmpdir + File.separator + "Zooby.class";
        File zoobyFile = new File(classfile);
        zoobyFile.deleteOnExit();
        OutputStream os = new FileOutputStream(zoobyFile);
        for (byte b : zoobyClassBytes)
            os.write(b);
        os.close();

        // Check that we can't load the Zooby class from the classpath
        try {
            Class.forName("Zooby");
            throw new Exception("Class \"Zooby\" is in the classpath!");
        } catch (ClassNotFoundException e) {
            // OK: expected
        }

        if (send)
            System.out.println("Testing we can send an object from client to server");
        else
            System.out.println("Testing we can receive an object from server to client");

        if (with) {
            // Test with the codebase property.  Downloading should work.
            URL zoobyURL = zoobyFile.getParentFile().toURI().toURL();
            System.setProperty("java.rmi.server.codebase", zoobyURL.toString());
            System.out.println("Testing with codebase, should work");
            System.out.println("Codebase is " +
                    System.getProperty("java.rmi.server.codebase"));
            test(send, true);
        } else {
            // Test without setting the codebase property.
            // This should not work; if it does it probably means java.io.tmpdir
            // is in the classpath.
            System.out.println("Testing without codebase, should fail");
            test(send, false);
        }

    }

    private static void test(boolean send, boolean shouldWork) throws Exception {
        try {
            testWithException(send);
        } catch (Exception e) {
            if (shouldWork)
                throw e;
            System.out.println("Got exception as expected: " + e);
            return;
        }
        if (!shouldWork)
            throw new Exception("Test passed without codebase but should not");
    }

    private static void testWithException(boolean send)
    throws Exception {
        ClassLoader zoobyCL = new ZoobyClassLoader();
        Class<?> zoobyClass = Class.forName("Zooby", false, zoobyCL);
        Object zooby = zoobyClass.newInstance();

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///");
        JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, pmbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();

        Object rzooby;
        if (send) {
            System.out.println("Sending object...");
            mbsc.setAttribute(getSetName, new Attribute("It", zooby));
            rzooby = getSetInstance.getIt();
        } else {
            System.out.println("Receiving object...");
            getSetInstance.setIt(zooby);
            rzooby = mbsc.getAttribute(getSetName, "It");
        }

        if (!rzooby.getClass().getName().equals("Zooby")) {
            throw new Exception("FAILED: remote object is not a Zooby");
        }
        if (rzooby.getClass().getClassLoader() ==
                zooby.getClass().getClassLoader()) {
            throw new Exception("FAILED: same class loader: " +
                    zooby.getClass().getClassLoader());
        }

        cc.close();
        cs.stop();
    }

    public static interface GetSetMBean {
        public Object getIt();
        public void setIt(Object x);
    }

    public static class GetSet implements GetSetMBean {
        public GetSet() {
        }

        public Object getIt() {
            return what;
        }

        public void setIt(Object x) {
            this.what = x;
        }

        private Object what;
    }

    public static class LaidBackSecurityManager extends SecurityManager {
        public void checkPermission(Permission perm) {
            // OK, dude
        }
    }
}
