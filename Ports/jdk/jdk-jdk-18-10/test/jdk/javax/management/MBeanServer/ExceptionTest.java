/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058865
 * @summary Checks that exceptions are correctly wired (compared to reference).
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD ExceptionTest
 */

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.lang.reflect.Method;

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;


public class ExceptionTest {

    /*
     * First Debug properties and arguments are collect in expected
     * map  (argName, value) format, then calls original test's run method.
     */
    public static void main(String args[]) throws Exception {

        System.out.println("=================================================");

        // Parses parameters
        Utils.parseDebugProperties();
        Map<String, Object> map = Utils.parseParameters(args) ;

        // Run test
        ExceptionTest test = new ExceptionTest();
        test.run(map);

    }

    public void run(Map<String, Object> args) {

        System.out.println("ExceptionTest::run: Start");
        int errorCount = 0;

        JMXConnectorServer cs = null;
        JMXConnector cc = null;

        try {
            // JMX MbeanServer used inside single VM as if remote.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();

            JMXServiceURL addr = cs.getAddress();
            cc = JMXConnectorFactory.connect(addr);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();

            // ----
            ObjectName objName =
                new ObjectName(ExceptionThrower.EXCEPTION_THROWER_NAME);
            System.out.println("ExceptionTest::run: Create and register MBean " + objName);
            mbsc.createMBean("ExceptionThrower", objName);
            System.out.println("---- OK\n");

            // ----
            System.out.println("ExceptionTest::run: Ask for exception(s)");
            Object[] throwExceptionParam = new Object[1];
            String[] throwExceptionSig = new String[]{"int"};

            for (int i = 0; i < ExceptionFactory.exceptions.size(); i++) {
                throwExceptionParam[0] = new Integer(i);

                Exception ex =
                    (Exception)mbsc.invoke(objName,
                        "throwException", throwExceptionParam, throwExceptionSig);

                if ( ! matches(ex, ExceptionFactory.exceptions.get(i)) ) {
                    errorCount++;
                    System.out.println("ExceptionTest::run: (ERROR) Received \n["
                            + ex + "]\nin place of\n["
                            + ExceptionFactory.exceptions.get(i) + "]");
                } else {
                    System.out.println("OK [" + ex + "]");
                }
            }

            System.out.println("---- DONE\n");

        } catch (Exception e) {
            Utils.printThrowable(e, true);
            throw new RuntimeException();
        } finally {
            try {
                // Close JMX Connector Client
                cc.close();
                // Stop connertor server
                cs.stop();

            } catch (Exception e) {
                Utils.printThrowable(e, true);
                throw new RuntimeException(
                    "Unable to either close connector client or stop connector server");
            }
        }

        if (errorCount == 0) {
            System.out.println("ExceptionTest::run: Done without any error");
        } else {
            System.out.println("ExceptionTest::run: Done with " + errorCount
                    + " error(s)");
            throw new RuntimeException("errorCount = " + errorCount);
        }

        System.out.println("ExceptionTest::run: Done");
    }

    // Check both Exception are identical.
    // That means:
    // - none is null.
    // - they are of the same Class.
    // - if their respective messages aren't null they're equal.
    // - if the message of one is null the message of the other is null too.
    private boolean matches(Exception ex, Exception refex) {
        if ( ex == null || refex == null ) {
            System.out.println("(ERROR) Called with one or more null parameter; check "
                    + ex + " against " + refex);
            return false;
        }

        String exClass = ex.getClass().getName();
        String refexClass = refex.getClass().getName();

        if ( ! exClass.equals(refexClass) ) {
            System.out.println("(ERROR) Class names don't match; check ["
                        + exClass + "] against [" + refexClass + "]");
            return false;
        }

        String exMes = ex.getMessage();
        String refexMes = refex.getMessage();

        if ( exMes != null && refexMes != null ) {
            if ( ! exMes.equals(refexMes) ) {
                System.out.println("(ERROR) Non null messages don't match; check ["
                        + exMes + "] against [" + refexMes + "]");
                return false;
            }
        } else if ( (exMes == null && refexMes != null)
                || (exMes != null && refexMes == null) ) {
                System.out.println("(ERROR) Messages don't match; check [" + exMes
                        + "] against [" + refexMes + "]");
        }

        return true;
    }

    // Utility inner class coming from JMX Tonga test suite.
    // Also used by ExceptionFactory.
    static class Utils {

        // DEBUG is printed depending on the DEBUG and DEBUG_LEVEL JAVA property
        static final String DEBUG_HEADER = "[debug] ";

        // DEBUG levels
        static int selectedDebugLevel = 0;
        static final int DEBUG_STANDARD = 1;
        static final int DEBUG_VERBOSE = 2;  // Mainly used for stress tests
        static final int DEBUG_ALL = DEBUG_STANDARD | DEBUG_VERBOSE;

        static void parseDebugProperties() {
            int level = 0;
            Properties p = System.getProperties();

            // get selected levels
            if (p.getProperty("DEBUG_STANDARD") != null) {
                level |= DEBUG_STANDARD;
            }

            if (p.getProperty("DEBUG_VERBOSE") != null) {
                level |= DEBUG_VERBOSE;
            }

            if (p.getProperty("DEBUG_ALL") != null) {
                level |= DEBUG_ALL;
            }

            selectedDebugLevel = level;
        }

        /**
         * Reproduces the original parsing and collection of test parameters
         * from the DTonga JMX test suite.
         *
         * Collects passed args and returns them in a map(argname, value) structure,
         * which will be then propagated as necessary to various called methods.
         */
        static Map<String, Object> parseParameters(String args[])
        throws Exception {
            debug(DEBUG_STANDARD, "TestRoot::parseParameters: Start");
            HashMap<String, Object> map = new HashMap<>();

            for ( int i = 0; i < args.length; i++ ) {
                if ( args[i].trim().startsWith("-") ) {
                    if ((i+1) < args.length && !args[i+1].startsWith("-") ) {
                        debug(DEBUG_STANDARD,
                            "TestRoot::parseParameters: added in map = " +
                            args[i] +
                            " with value " +
                            args[i+1]) ;
                        map.put(args[i].trim(), args[i+1].trim()) ;
                    } else if ((i+1) < args.length && args[i+1].startsWith("-") ||
                               (i+1) == args.length ) {
                        debug(DEBUG_STANDARD,
                                "TestRoot::parseParameters: added in map = " +
                                args[i] +
                                " with null value") ;
                        map.put(args[i].trim(), null) ;
                    } else {
                        System.out.println(
                            "TestRoot::parseParameters: (WARNING) not added in map = " +
                            args[i]) ;
                    }
                }
            }

            debug(DEBUG_STANDARD, "TestRoot::parseParameters: Done") ;
            return map ;
        }

        /**
         * This method is to be used in all tests to print anything
         * that is temporary.
         * Printing is done only when debug is activated by the property DEBUG.
         * Printing depends also on the DEBUG_LEVEL property.
         * Here it encapsulates a System.out.println.
         */
        static void debug(int level, String line) {
            if ((selectedDebugLevel & level) != 0) {
                System.out.println(DEBUG_HEADER + line);
            }
        }

        /**
         * Do print stack trace when withStack is true.
         * Does try to call getTargetException() and getTargetError() then
         * print embedded stacks in the case of an Exception wrapping
         * another Exception or an Error. Recurse until no more wrapping
         * is found.
         */
        static void printThrowable(Throwable theThro, boolean withStack) {
            try {
                if (withStack) {
                    theThro.printStackTrace(System.out);
                }
                if (theThro instanceof Exception) {
                    Exception t = (Exception) theThro;
                    Method target = null;
                    String blank = " ";
                    try {
                        target = t.getClass().getMethod("getTargetException",
                                (java.lang.Class<?>[]) null);
                    } catch (Exception ee) {
                    // OK: getTargetException method could be there or not
                    }
                    System.out.println(blank + t.getClass() + "==>" + t.getMessage());
                    while (target != null) {
                        try {
                            t = (Exception) target.invoke(t,
                                    (java.lang.Object[]) null);
                        } catch (Exception ee) {
                            t = null;
                        }
                        try {
                            if (t != null) {
                                blank = blank + "  ";
                                System.out.println(blank + t.getClass() + "==>" +
                                        t.getMessage());
                                try {
                                    target =
                                            t.getClass().getMethod("getTargetException",
                                            (java.lang.Class<?>[]) null);
                                } catch (Exception ee) {
                                // OK: getTargetException method could be there or not                            }
                                }
                            } else {
                                target = null;
                            }
                        } catch (Exception ee) {
                            target = null;
                        }
                    }

                    // We may have exceptions wrapping an Error then it is
                    // getTargetError that is likely to be called
                    try {
                        target = ((Exception) theThro).getClass().getMethod("getTargetError",
                                (java.lang.Class<?>[]) null);
                    } catch (Exception ee) {
                    // OK: getTargetError method could be there or not
                    }
                    Throwable err = theThro;
                    while (target != null) {
                        try {
                            err = (Error) target.invoke(err,
                                    (java.lang.Object[]) null);
                        } catch (Exception ee) {
                            err = null;
                        }
                        try {
                            if (err != null) {
                                blank = blank + "  ";
                                System.out.println(blank + err.getClass() + "==>" +
                                        err.getMessage());
                                if (withStack) {
                                    err.printStackTrace(System.out);
                                }
                                try {
                                    target = err.getClass().getMethod("getTargetError",
                                            (java.lang.Class<?>[]) null);
                                } catch (Exception ee) {
                                // OK: getTargetError method could be there or not
                                }
                            } else {
                                target = null;
                            }
                        } catch (Exception ee) {
                            target = null;
                        }
                    }
                } else {
                    System.out.println("Throwable is : " + theThro);
                }
            } catch (Throwable x) {
                System.out.println("Exception : raised in printException : " + x);
            }
        }
    }

}
