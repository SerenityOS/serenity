/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 *
 * @author Adrian Colley
 * @author Laird Dornin
 * @author Peter Jones
 * @author Ann Wollrath
 *
 * The rmi library directory contains a set of simple utiltity classes
 * for use in rmi regression tests.
 *
 * NOTE: The JavaTest group has recommended that regression tests do
 * not make use of packages.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Properties;

import sun.rmi.registry.RegistryImpl;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.transport.Endpoint;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * Class of utility/library methods (i.e. procedures) that assist with
 * the writing and maintainance of rmi regression tests.
 */
public class TestLibrary {
    /**
     *                       IMPORTANT!
     *
     * RMI tests are run concurrently and port conflicts result when a single
     * port number is used by multiple tests.  When needing a port, use
     * getUnusedRandomPort() wherever possible.  If getUnusedRandomPort() cannot
     * be used, reserve and specify a port to use for your test here.   This
     * will ensure there are no port conflicts amongst the RMI tests.  The
     * port numbers specified here may also be specified in the respective
     * tests.  Do not change the reserved port numbers here without also
     * changing the port numbers in the respective tests.
     *
     * When needing an instance of the RMIRegistry, use
     * createRegistryOnUnusedPort wherever possible to prevent port conflicts.
     *
     * Reserved port range: FIXED_PORT_MIN to FIXED_PORT_MAX (inclusive) for
     * tests which cannot use a random port.  If new fixed ports are added below
     * FIXED_PORT_MIN or above FIXED_PORT_MAX, then adjust
     * FIXED_PORT_MIN/MAX appropriately.
     */
    public final static int FIXED_PORT_MIN = 60001;
    public final static int FIXED_PORT_MAX = 60010;
    public final static int INHERITEDCHANNELNOTSERVERSOCKET_ACTIVATION_PORT = 60003;
    public final static int INHERITEDCHANNELNOTSERVERSOCKET_REGISTRY_PORT = 60004;
    public final static int READTEST_REGISTRY_PORT = 60005;
    private final static int MAX_SERVER_SOCKET_TRIES = 2*(FIXED_PORT_MAX-FIXED_PORT_MIN+1);

    static void mesg(Object mesg) {
        System.err.println("TEST_LIBRARY: " + mesg.toString());
    }

    /**
     * Routines that enable rmi tests to fail in a uniformly
     * informative fashion.
     */
    public static void bomb(String message, Exception e) {
        String testFailed = "TEST FAILED: ";

        if ((message == null) && (e == null)) {
            testFailed += " No relevant information";
        } else if (e == null) {
            testFailed += message;
        }

        System.err.println(testFailed);
        if (e != null) {
            System.err.println("Test failed with: " +
                               e.getMessage());
            e.printStackTrace(System.err);
        }
        throw new TestFailedException(testFailed, e);
    }
    public static void bomb(String message) {
        bomb(message, null);
    }
    public static void bomb(Exception e) {
        bomb(null, e);
    }

    /**
     * Helper method to determine if registry has started
     *
     * @param port The port number to check
     * @param msTimeout The amount of milliseconds to spend checking
     */

    public static boolean checkIfRegistryRunning(int port, int msTimeout) {
        final long POLLTIME_MS = 100L;
        long stopTime = computeDeadline(System.currentTimeMillis(), msTimeout);
        do {
            try {
                Registry r = LocateRegistry.getRegistry(port);
                String[] s = r.list();
                // no exception. We're now happy that registry is running
                return true;
            } catch (RemoteException e) {
                // problem - not ready ? Try again
                try {
                    Thread.sleep(POLLTIME_MS);
                } catch (InterruptedException ie) {
                    // not expected
                }
            }
        } while (System.currentTimeMillis() < stopTime);
        return false;
    }

    public static String getProperty(final String property,
                                     final String defaultVal) {
        try {
            return java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(property, defaultVal);
                    }
                });
        } catch (Exception ex) {
            bomb("Exception getting property " + property, ex);
            throw new AssertionError("this should be unreachable");
        }
    }

    public static double getTimeoutFactor() {
        String prop = getProperty("test.timeout.factor", "1.0");
        double timeoutFactor = 1.0;

        try {
            timeoutFactor = Double.parseDouble(prop);
        } catch (NumberFormatException ignore) { }

        return timeoutFactor;
    }

    /**
     * Computes a deadline from a timestamp and a timeout value.
     * Maximum timeout (before multipliers are applied) is one hour.
     */
    public static long computeDeadline(long timestamp, long timeout) {
        if (timeout < 0L) {
            throw new IllegalArgumentException("timeout " + timeout + "ms out of range");
        }

        return timestamp + (long)(timeout * getTimeoutFactor());
    }

    /**
     * Property mutators
     */
    public static void setBoolean(String property, boolean value) {
        setProperty(property, (new Boolean(value)).toString());
    }
    public static void setInteger(String property, int value) {
        setProperty(property, Integer.toString(value));
    }
    public static void setProperty(String property, String value) {
        final String prop = property;
        final String val = value;
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Void>() {
                public Void run() {
                    System.setProperty(prop, val);
                    return null;
                }
        });
    }

    /**
     * Routines to print out a test's properties environment.
     */
    public static void printEnvironment() {
        printEnvironment(System.err);
    }
    public static void printEnvironment(PrintStream out) {
        out.println("-------------------Test environment----------" +
                    "---------");

        for(Enumeration<?> keys = System.getProperties().keys();
            keys.hasMoreElements();) {

            String property = (String) keys.nextElement();
            out.println(property + " = " + getProperty(property, null));
        }
        out.println("---------------------------------------------" +
                    "---------");
    }

    /**
     * Routine that "works-around" a limitation in jtreg.
     * Currently it is not possible for a test to specify that the
     * test harness should build a given source file and install the
     * resulting class in a location that is not accessible from the
     * test's classpath.  This method enables a test to move a
     * compiled test class file from the test's class directory into a
     * given "codebase" directory.  As a result the test can only
     * access the class file for <code>className</code>if the test loads
     * it from a classloader (e.g. RMIClassLoader).
     *
     * Tests that use this routine must have the following permissions
     * granted to them:
     *
     *   getProperty user.dir
     *   getProperty etc.
     */
    public static URL installClassInCodebase(String className,
                                             String codebase)
        throws MalformedURLException
    {
        return installClassInCodebase(className, codebase, true);
    }

    public static URL installClassInCodebase(String className,
                                             String codebase,
                                             boolean delete)
        throws MalformedURLException
    {
        /*
         * NOTES/LIMITATIONS: The class must not be in a named package,
         * and the codebase must be a relative path (it's created relative
         * to the working directory).
         */
        String classFileName = className + ".class";

        /*
         * Specify the file to contain the class definition.  Make sure
         * that the codebase directory exists (underneath the working
         * directory).
         */
        File dstDir = (new File(getProperty("user.dir", "."), codebase));

        if (!dstDir.exists()) {
            if (!dstDir.mkdir()) {
                throw new RuntimeException(
                    "could not create codebase directory");
            }
        }
        File dstFile = new File(dstDir, classFileName);

        /*
         * Obtain the URL for the codebase.
         */
        URL codebaseURL = dstDir.toURI().toURL();

        /*
         * Specify where we will copy the class definition from, if
         * necessary.  After the test is built, the class file can be
         * found in the "test.classes" directory.
         */
        File srcDir = new File(getProperty("test.classes", "."));
        File srcFile = new File(srcDir, classFileName);

        mesg(srcFile);
        mesg(dstFile);

        /*
         * If the class definition is not already located at the codebase,
         * copy it there from the test build area.
         */
        if (!dstFile.exists()) {
            if (!srcFile.exists()) {
                throw new RuntimeException(
                    "could not find class file to install in codebase " +
                    "(try rebuilding the test): " + srcFile);
            }

            try {
                copyFile(srcFile, dstFile);
            } catch (IOException e) {
                throw new RuntimeException(
                    "could not install class file in codebase");
            }

            mesg("Installed class \"" + className +
                "\" in codebase " + codebaseURL);
        }

        /*
         * After the class definition is successfully installed at the
         * codebase, delete it from the test's CLASSPATH, so that it will
         * not be found there first before the codebase is searched.
         */
        if (srcFile.exists()) {
            if (delete && !srcFile.delete()) {
                throw new RuntimeException(
                    "could not delete duplicate class file in CLASSPATH");
            }
        }

        return codebaseURL;
    }

    public static void copyFile(File srcFile, File dstFile)
        throws IOException
    {
        FileInputStream src = new FileInputStream(srcFile);
        FileOutputStream dst = new FileOutputStream(dstFile);

        byte[] buf = new byte[32768];
        while (true) {
            int count = src.read(buf);
            if (count < 0) {
                break;
            }
            dst.write(buf, 0, count);
        }

        dst.close();
        src.close();
    }

    /** routine to unexport an object */
    public static void unexport(Remote obj) {
        if (obj != null) {
            try {
                mesg("unexporting object...");
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (NoSuchObjectException munch) {
            } catch (Exception e) {
                e.getMessage();
                e.printStackTrace();
            }
        }
    }

    /**
     * Allow test framework to control the security manager set in
     * each test.
     *
     * @param managerClassName The class name of the security manager
     *                         to be instantiated and set if no security
     *                         manager has already been set.
     */
    public static void suggestSecurityManager(String managerClassName) {
        SecurityManager manager = null;

        if (System.getSecurityManager() == null) {
            try {
                if (managerClassName == null) {
                    managerClassName = TestParams.defaultSecurityManager;
                }
                manager = ((SecurityManager) Class.
                           forName(managerClassName).newInstance());
            } catch (ClassNotFoundException cnfe) {
                bomb("Security manager could not be found: " +
                     managerClassName, cnfe);
            } catch (Exception e) {
                bomb("Error creating security manager. ", e);
            }

            System.setSecurityManager(manager);
        }
    }

    /**
     * Creates an RMI {@link Registry} on a random, un-reserved port.
     *
     * @returns an RMI Registry, using a random port.
     * @throws RemoteException if there was a problem creating a Registry.
     */
    public static Registry createRegistryOnUnusedPort() throws RemoteException {
        return LocateRegistry.createRegistry(getUnusedRandomPort());
    }

    /**
     * Creates an RMI {@link Registry} on an ephemeral port.
     *
     * @returns an RMI Registry
     * @throws RemoteException if there was a problem creating a Registry.
     */
    public static Registry createRegistryOnEphemeralPort() throws RemoteException {
        return LocateRegistry.createRegistry(0);
    }

    /**
     * Returns the port number the RMI {@link Registry} is running on.
     *
     * @param registry the registry to find the port of.
     * @return the port number the registry is using.
     * @throws RuntimeException if there was a problem getting the port number.
     */
    public static int getRegistryPort(Registry registry) {
        int port = -1;

        try {
            RemoteRef remoteRef = ((RegistryImpl)registry).getRef();
            LiveRef liveRef = ((UnicastServerRef)remoteRef).getLiveRef();
            Endpoint endpoint = liveRef.getChannel().getEndpoint();
            TCPEndpoint tcpEndpoint = (TCPEndpoint) endpoint;
            port = tcpEndpoint.getPort();
        } catch (Exception ex) {
            throw new RuntimeException("Error getting registry port.", ex);
        }

        return port;
    }

    /**
     * Returns an unused random port number which is not a reserved port.  Will
     * try up to 10 times to get a random port before giving up and throwing a
     * RuntimeException.
     *
     * @return an unused random port number.
     * @throws RuntimeException if there was a problem getting a port.
     */
    public static int getUnusedRandomPort() {
        int numTries = 0;
        IOException ex = null;

        while (numTries++ < MAX_SERVER_SOCKET_TRIES) {
            int unusedRandomPort = -1;
            ex = null; //reset

            try (ServerSocket ss = new ServerSocket(0)) {
                unusedRandomPort = ss.getLocalPort();
            } catch (IOException e) {
                ex = e;
                // temporarily print stack trace here until we find out why
                // tests are failing.
                System.err.println("TestLibrary.getUnusedRandomPort() caught "
                        + "exception on iteration " + numTries
                        + (numTries==MAX_SERVER_SOCKET_TRIES ? " (the final try)."
                        : "."));
                ex.printStackTrace();
            }

            if (unusedRandomPort >= 0) {
                if (isReservedPort(unusedRandomPort)) {
                    System.out.println("INFO: On try # " + numTries
                        + (numTries==MAX_SERVER_SOCKET_TRIES ? ", the final try, ": ",")
                        + " ServerSocket(0) returned the reserved port "
                        + unusedRandomPort
                        + " in TestLibrary.getUnusedRandomPort() ");
                } else {
                    return unusedRandomPort;
                }
            }
        }

        // If we're here, then either an exception was thrown or the port is
        // a reserved port.
        if (ex==null) {
            throw new RuntimeException("Error getting unused random port. The"
                    +" last port returned by ServerSocket(0) was a reserved port");
        } else {
            throw new RuntimeException("Error getting unused random port.", ex);
        }
    }

    /**
     * Determines if a port is one of the reserved port numbers.
     *
     * @param port the port to test.
     * @return {@code true} if the port is a reserved port, otherwise
     *         {@code false}.
     */
    public static boolean isReservedPort(int port) {
        return ((port >= FIXED_PORT_MIN) && (port <= FIXED_PORT_MAX) ||
                (port == 1099));
    }

    /**
     * Method to capture the stack trace of an exception and return it
     * as a string.
     */
    public String stackTraceToString(Exception e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);

        e.printStackTrace(ps);
        return bos.toString();
    }

    /** extra properties */
    private static Properties props;

    /**
     * Returns extra test properties. Looks for the file "../../test.props"
     * and reads it in as a Properties file. Assuming the working directory
     * is "<path>/JTwork/scratch", this will find "<path>/test.props".
     */
    private static synchronized Properties getExtraProperties() {
        if (props != null) {
            return props;
        }
        props = new Properties();
        File f = new File(".." + File.separator + ".." + File.separator +
                          "test.props");
        if (!f.exists()) {
            return props;
        }
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                props.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("extra property setup failed", e);
        }
        return props;
    }

    /**
     * Returns an extra test property. Looks for the file "../../test.props"
     * and reads it in as a Properties file. Assuming the working directory
     * is "<path>/JTwork/scratch", this will find "<path>/test.props".
     * If the property isn't found, defaultVal is returned.
     */
    public static String getExtraProperty(String property, String defaultVal) {
        return getExtraProperties().getProperty(property, defaultVal);
    }
}
