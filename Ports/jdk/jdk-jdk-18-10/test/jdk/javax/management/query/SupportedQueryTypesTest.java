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
 * @bug 8058865
 * @summary Tests most of the existing query types.
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @compile TestQuery.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SupportedQueryTypesTest -mbeanClassName TestQuery
 */

import java.util.Map ;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.lang.reflect.Method;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName ;
import javax.management.QueryExp;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class SupportedQueryTypesTest {

    protected String mbeanClassName = null;

    private MBeanServerConnection mbsc = null;


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
        SupportedQueryTypesTest test = new SupportedQueryTypesTest();
        test.run(map);

    }

    public void run(Map<String, Object> args) {
        int errorCount = 0;

        ObjectName on = null;
        ObjectName serverDelegateObjectName = null;

        JMXConnectorServer cs = null;
        JMXConnector cc = null;

        System.out.println("SupportedQueryTypesTest::run: Start") ;
        try {
            // JMX MbeanServer used inside single VM as if remote.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
            cs.start();

            JMXServiceURL addr = cs.getAddress();
            cc = JMXConnectorFactory.connect(addr);
            mbsc = cc.getMBeanServerConnection();


            // Create and register the ServerDelegate MBean on the remote MBeanServer
            String serverDelegateClassName = ServerDelegate.class.getName();
            serverDelegateObjectName =
                    new ObjectName("defaultDomain:class=" + serverDelegateClassName);
            mbsc.createMBean(serverDelegateClassName, serverDelegateObjectName);

            // Retrieve the MBean class name
            mbeanClassName = (String) args.get("-mbeanClassName") ;
            on = new ObjectName("defaultDomain:class=" + mbeanClassName);

            // Create and register the MBean on the remote MBeanServer
            System.out.println("SupportedQueryTypesTest::run: CREATE " +
                    mbeanClassName + " on the remote MBeanServer with name "
                    + on);
            mbsc.createMBean(mbeanClassName, on);

            // Create a QueryFactory and setup which query we'll use.
            QueryFactory queries = new QueryFactory(mbeanClassName);
            queries.buildQueries();
            int maxIndex = queries.getSize();
            int minIndex = 1;

            // Create a reference Set<ObjectName> to check later on
            // the queryNames() results
            Set<ObjectName> referenceNameSet = new HashSet<ObjectName>();
            referenceNameSet.add(on);

            // Create a reference Set<ObjectInstance> to check later on
            // the queryMBeans() results
            ObjectInstance oi = new ObjectInstance(on, mbeanClassName);
            Set<ObjectInstance> referenceInstanceSet =
                    new HashSet<ObjectInstance>();
            referenceInstanceSet.add(oi);

            // Perform the queryNames and queryMBeans requests
            for (int i = minIndex; i <= maxIndex; i++ ) {
                QueryExp query = queries.getQuery(i);
                System.out.println("----");
                System.out.println("SupportedQueryTypesTest::run: Query # " + i);
                System.out.println("query " + query);
                errorCount +=
                    doQueryNames(query, referenceNameSet);
                errorCount +=
                    doQueryMBeans(query, referenceInstanceSet);
            }

        } catch(Exception e) {
            Utils.printThrowable(e, true);
            errorCount++;

        } finally {
            // Do unregister the MBean
            try {
                if (mbsc.isRegistered(on)) {
                    mbsc.unregisterMBean(on);
                }
                if (mbsc.isRegistered(serverDelegateObjectName)) {
                    mbsc.unregisterMBean(serverDelegateObjectName);
                }
            } catch (Exception e) {
                Utils.printThrowable(e, true) ;
                errorCount++;
            }

            try {
                // Close JMX Connector Client
                cc.close();
                // Stop connertor server
                cs.stop();

            } catch (Exception e) {
                Utils.printThrowable(e, true) ;
                errorCount++;
            }
        }

        System.out.println("");
        System.out.println("SupportedQueryTypesTest::run: Done") ;

        // Handle result
        if (errorCount == 0) {
            System.out.println("SupportedQueryTypesTest::run: (OK)");
        } else {
            String message = "SupportedQueryTypesTest::run: (ERROR) Got " +
                    + errorCount + " error(s)";
            System.out.println(message);
            throw new RuntimeException(message);
        }
    }


    private int doQueryNames(QueryExp query, Set<ObjectName> referenceSet) {
        int errorCount = 0;
        System.out.println(" <*> Perform queryNames call ");

        try {
            // Call queryNames on the remote MBeanServer
            Set<ObjectName> remoteSet =  mbsc.queryNames(null, query);

            // Compare the 2 Set<ObjectName>
            errorCount += checkSet(remoteSet, referenceSet);

            // Cleaning
            remoteSet.clear();

        } catch (Exception e) {
            Utils.printThrowable(e, true);
            errorCount++;
        }

        if ( errorCount == 0 ) {
            System.out.println("\t(OK)");
        } else {
            System.out.println("\t(ERROR) Query failed");
        }

        return errorCount;
    }


    private int doQueryMBeans(QueryExp query, Set<ObjectInstance> referenceSet) {
        int errorCount = 0;
        System.out.println(" <*> Perform queryMBeans call ");

        try {
            // Call queryMBeans on the remote MBeanServer
            Set<ObjectInstance> remoteSet =  mbsc.queryMBeans(null, query);

            // Compare the 2 Set<ObjectInstance>
            errorCount += checkSet(remoteSet, referenceSet);

            // Cleaning
            remoteSet.clear();

        } catch (Exception e) {
            Utils.printThrowable(e, true);
            errorCount++;
        }

        if ( errorCount == 0 ) {
            System.out.println("\t(OK)");
        } else {
            System.out.println("\t(ERROR) Query failed");
        }

        return errorCount;
    }

    /**
     * Pretty print of a Set content.
     * When the Set isn't empty, toString() is called on each element.
     * <br>The variable's name used to hold that Set is given via the setName
     * parameter and used in the output.
     */
    private static void printSet(Set<?> printableSet, String setName) {
        if ( printableSet.size() == 0 ) {
            System.out.println("The Set " + setName + " is empty");
        } else {
            System.out.println("The Set " + setName + " contains :");

            for (Iterator<?> it = printableSet.iterator(); it.hasNext();) {
                Object elem = it.next();
                System.out.println("\t" + elem.toString());
            }
        }
    }


    /**
     * This method check the Set remoteSet is equal to
     * the reference Set referenceSet,
     * which means same size and content (order doesn't matter).
     * <br>It returns 0 when the check is fine, otherwise 1.
     */
    private int checkSet(Set<?> remoteSet, Set<?> referenceSet) {
        if ( !  remoteSet.equals(referenceSet) ) {
            System.out.println("SupportedQueryTypesTest::checkSet:"
                    + " (ERROR) Set aren't as expected");
            printSet(remoteSet, "remoteSet");
            printSet(referenceSet, "referenceSet");
            return 1;
        } else {
            return 0;
        }
    }

    // Utility inner class coming from JMX Tonga test suite.
    private static class Utils {

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
