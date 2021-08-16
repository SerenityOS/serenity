/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.lang.reflect.Method;
import javax.management.remote.JMXConnectorServerMBean;

// utility class for MXBean* tests coming from JMX Tonga test suite
class Utils {

    // DEBUG is printed depending on the DEBUG and DEBUG_LEVEL JAVA property
    private static final String DEBUG_HEADER = "[debug] ";

    // DEBUG levels
    private static int selectedDebugLevel = 0;
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
        Utils.debug(DEBUG_STANDARD, "TestRoot::parseParameters: Start");
        HashMap<String, Object> map = new HashMap<>();

        for ( int i = 0; i < args.length; i++ ) {
            if ( args[i].trim().startsWith("-") ) {
                if ((i+1) < args.length && !args[i+1].startsWith("-") ) {
                    Utils.debug(DEBUG_STANDARD,
                        "TestRoot::parseParameters: added in map = " +
                        args[i] +
                        " with value " +
                        args[i+1]) ;
                    map.put(args[i].trim(), args[i+1].trim()) ;
                } else if ((i+1) < args.length && args[i+1].startsWith("-") ||
                           (i+1) == args.length ) {
                    Utils.debug(DEBUG_STANDARD,
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

        Utils.debug(DEBUG_STANDARD, "TestRoot::parseParameters: Done") ;
        return map ;
    }

    /**
     * This method is to be used in all tests to print anything
     * that is temporary.
     * Printing is done only when debug is activated by the property DEBUG.
     * Printing depends also on the DEBUG_LEVEL property.
     * Here it encapsulates a System.out.println.
     */
    public static void debug(int level, String line) {
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
    public static void printThrowable(Throwable theThro, boolean withStack) {
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

    /**
     * Wait up to maxTimeInSeconds second(s) the given JMX connector server
     * comes up (which means isActive returns true).
     * If it fails to do so we throw a RunTime exception.
     */
    public static void waitReady(JMXConnectorServerMBean server,
                                 int maxTimeInSeconds) throws Exception {
        int elapsed = 0;

        while (!server.isActive() && elapsed < maxTimeInSeconds) {
            Thread.sleep(1000);
            elapsed++;
        }

        if (server.isActive()) {
            String message = "Utils::waitReady: JMX connector server came up";
            if ( elapsed == 0) {
                message += " immediately";
            } else {
                message += " after " + elapsed + " seconds";
            }
            message += " [" + server.getAddress() + "]";
            Utils.debug(DEBUG_STANDARD, message);
        } else {
            String message = "Utils::waitReady: (ERROR) JMX connector" +
                    " server didn't come up after " + elapsed + " seconds [" +
                    server.getAddress() + "]";
            System.out.println(message);
            throw new RuntimeException(message);
        }
    }
}
