/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share;

import nsk.share.log.Log;
import nsk.share.TestBug;
import nsk.share.ArgumentParser;
import java.lang.management.*;

/**
 * Parser for JSR-174 test's arguments.
 * <p>
 * <code>ArgumentHandler</code> handles specific command line arguments
 * related to way of execution of a test in addition to general arguments
 * recognized by {@link ArgumentParser <code>ArgumentParser</code>}.
 * <p>
 * Following is the list of specific options for <code>ArgumentHandler</code>:
 * <ul>
 * <li><code>-testMode="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>directly</code> -- to call methods in
 *      the MBean directly within the same JVM, <code>server</code> -- to call
 *      methods through MBeanServer, <code>proxy</code> -- to call methods
 *      through MBean proxy (not yet implemented).
 * <li><code>-MBeanServer="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>default</code> -- to execute test for
 *      default JMX implementation of MBeanServer or <code>custom</code> -- for
 *      implementation provided by NSK J2SE SQE Team.
 * <li><code>-loadableClassCount=<i>value</i></code>, where <i>value</i> defines
 *      amount of loadable classes. Default values is <code>100</code>.
 * <li><code>-loadersCount=<i>value</i></code>, where <i>value</i> defines
 *      amount of class loaders. Default values is <code>100</code>.
 * <li><code>-singleClassloaderClass</code> specifies whether class loaders are
 *      instances of the same class.
 * <li><code>-memory="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>heap</code> -- to test heap memory,
 *      <code>nonheap</code> to test nonheap memory, <code>mixed</code> -- to
 *      test both heap and nonheap memory.
 * <li><code>-invocationType="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>java</code> -- to start java threads,
 *      <code>native</code> -- to start native threads, <code>mixed</code> -- to
 *      both java and native threads.
 * <li><code>-monitoring="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>polling</code> -- to start polling
 *      mechanism of monitoring, <code>notification</code> -- to start
 *      notification mechanism of monitoring.
 * <li><code>-threshold="<i>value</i>"</code>, where <i>value</i> may take
 *      one of the following values: <code>usage</code> -- to test usage
 *      thresholds, <code>collection</code> -- to test collection usage
 *      thresholds.
 * <li><code>-depth=<i>value</i></code>, where <i>value</i> defines
 *      depth of recursion. Default values is <code>1</code>.
 * <li><code>-threadCount=<i>value</i></code>, where <i>value</i> defines
 *      number of threads to start. Default values is <code>1</code>.
 * <li><code>-timeout=<i>value</i></code>, where <i>value</i> defines
 *      number of minutes to run the test.
 * </ul>
 * <p>
 * See also list of basic options recognized by <code>ArgumentParser</code>.
 * <p>
 * See also comments to <code>ArgumentParser</code> how to work with
 * command line arguments and options.
 *
 * @see ArgumentParser
 */
public class ArgumentHandler extends ArgumentParser {
    static final String TEST_MODE = "testMode";
    static final String DIRECTLY_MODE = "directly";
    static final String SERVER_MODE = "server";
    static final String PROXY_MODE = "proxy";

    static final String SERVER_TYPE = "MBeanServer";
    static final String DEFAULT_TYPE = "default";
    static final String CUSTOM_TYPE = "custom";

    static final String LOADABLE_CLASSES_COUNT = "loadableClassCount";
    static final String LOADERS_COUNT = "loadersCount";
    static final String SINGLE_CLASSLOADER_CLASS = "singleClassloaderClass";

    static final String MEMORY_TYPE = "memory";
    static final String MT_HEAP = "heap";
    static final String MT_NONHEAP = "nonheap";
    static final String MT_MIXED = "mixed";

    static final String INVOCATION_TYPE = "invocationType";
    static final String JAVA_TYPE = "java";
    static final String NATIVE_TYPE = "native";
    static final String MIXED_TYPE = "mixed";

    static final String MONITORING = "monitoring";
    static final String MON_POLLING = "polling";
    static final String MON_NOTIF = "notification";

    static final String THRESHOLD = "threshold";
    static final String TH_USAGE = "usage";
    static final String TH_COLLECTION = "collection";

    static final String THREAD_DEPTH = "depth";
    static final String THREAD_COUNT = "threadCount";
    static final String TIMEOUT = "timeout";

    static final String SCENARIO_TYPE = "scenarioType";

    static final String ITERATIONS = "iterations";

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param  args  Array of the raw command-line arguments.
     *
     * @throws  BadOption  If unknown option or illegal
     *                     option value found
     *
     * @see ArgumentParser
     */
    public ArgumentHandler(String args[]) {
        super(args);
    }

    /**
     * Returns the test mode.
     * <p>
     * To access the metrics directly, <code>testMode</code> option should
     * be defined in command line <code>-testMode="directly"</code>.
     * To access the metrics via MBeanServer, <code>"server"</code> should be
     * assigned to <code>-testMode="directly"</code>.
     * <p>
     * If <code>testMode</code> is not defined by command line, a test is
     * executed in <code>directly</code> mode.
     *
     * @return name of test mode.
     *
     */
    public String getTestMode() {
        return options.getProperty(TEST_MODE, DIRECTLY_MODE);
    }

    /**
     * Returns a type of MBean server if any.
     * Two kinds of MBean servers are allowed: default and custom servers.
     * Default server is an implementation of {@link
     * javax.management.MBeanServer <tt>javax.management.MBeanServer</tt>}
     * interface provided by JMX. Custom server is an implementation provided
     * by NSK J2SE SQE Team. Server type is defined by <tt>MBeanServer</tt>
     * key in command line <code>-MBeanServer="default"</code> or
     * <code>-MBeanServer="custom"</code>
     *
     * @return <i>MBeanServer</i> server type.
     *
     */
    public String getServerType() {
        return options.getProperty(SERVER_TYPE, DEFAULT_TYPE);
    }

    /**
     * Returns <i>true</i> if default implementation is used.
     *
     * @return <i>true</i> if default implementation is used.
     *
     * @see #getServerType()
     */
    public boolean isDefaultServer() {
        return getServerType().equals(DEFAULT_TYPE);
    }

    /**
     * Returns amount of class loaders.
     *
     * @return <i>loadersCount</i> as an integer value
     */
    public int getLoadersCount() {
        String val = options.getProperty(LOADERS_COUNT, "100");
        int number;
        try {
            number = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"" + LOADERS_COUNT
                                    + "\" argument: " + val);
        }
        return number;
    }

    /**
     * Returns <i>true</i> if class loaders, which perform class loading, are
     * instances of the same class. If <code>-singleClassloaderClass</code> key
     * is not set in command line options, then <i>false</i> is returned.
     *
     * @return if class loaders are instances of the same class.
     *
     */
    public boolean singleClassloaderClass() {
        return options.getProperty(SINGLE_CLASSLOADER_CLASS) != null;
    }

    /**
     * Returns amount of loadable classes. If <code>-loadableClassesCount</code>
     * key is not set with command line, <code>100</code> is returned.
     *
     * @return <i>loadableClassesCount</i> as an integer value
     *
     * @throws TestBug <i>loadableClassesCount</i> is non-numeric value.
     *
     */
    public int getLoadableClassesCount() {
        String val = options.getProperty(LOADABLE_CLASSES_COUNT, "1");
        int number;
        try {
            number = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"" + LOADABLE_CLASSES_COUNT
                                    + "\" argument: " + val);
        }
        return number;
    }

    /**
     * Returns invocation type.
     *
     * @return <i>invocationType</i> value
     *
     */
    public String getInvocationType() {
        return options.getProperty(INVOCATION_TYPE, JAVA_TYPE);
    }

    /**
     * Returns tested memory type.
     *
     * @return <i>memory</i> value
     *
     */
    public String getTestedMemory() {
        return options.getProperty(MEMORY_TYPE, MT_HEAP);
    }

    /**
     * Returns timeout.
     *
     * @return <i>timeout</i> value
     *
     */
    public int getTimeout() {
        String value = options.getProperty(TIMEOUT, "30");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"" + TIMEOUT
                            + "\" argument: " + value);
        }
    }

    /**
     * Returns recursion depth.
     *
     * @return <i>depth</i> value
     *
     */
    public int getThreadDepth() {
        String value = options.getProperty(THREAD_DEPTH, "1");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"" + THREAD_DEPTH
                            + "\" argument: " + value);
        }
    }

    /**
     * Returns number of threads.
     *
     * @return <i>threadCount</i> value
     *
     */
    public int getThreadCount() {
        String value = options.getProperty(THREAD_COUNT, "1");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"" + THREAD_COUNT
                            + "\" argument: " + value);
        }
    }

    /**
     * Returns type of monitoring.
     *
     * @return <i>monitoring</i> value
     *
     */
    public String getMonitoring() {
        return options.getProperty(MONITORING, MON_NOTIF);
    }

    /**
     * Returns type of threshold.
     *
     * @return <i>threshold</i> value
     *
     */
    public String getThreshold() {
        return options.getProperty(THRESHOLD, TH_USAGE);
    }

    /**
     * Returns thread type to create.
     */
    public String getScenarioType() {
            return options.getProperty(SCENARIO_TYPE, "running");
    }

    public int getIterations() {
            return Integer.parseInt(options.getProperty(ITERATIONS, "3"));
    }

    /**
     * Checks if an option is allowed and has proper value.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @param option option name
     * @param value string representation of value
     *                      (could be an empty string too)
     *              null if this option has no value
     * @return <i>true</i> if option is allowed and has proper value
     *         <i>false</i> if otion is not admissible
     *
     * @throws <i>BadOption</i> if option has an illegal value
     *
     * @see nsk.share.ArgumentParser#parseArguments
     */
    protected boolean checkOption(String option, String value) {

        // defines directly, server or proxytest mode
        if (option.equals(TEST_MODE)) {
            if ( (!value.equals(DIRECTLY_MODE)) &&
                 (!value.equals(SERVER_MODE)) &&
                 (!value.equals(PROXY_MODE))
               ) {
                throw new BadOption(option + ": must be one of: "
                                  + "\"" + DIRECTLY_MODE + "\", "
                                  + "\"" + SERVER_MODE   + "\", "
                                  + "\"" + PROXY_MODE    + "\"");
            }
            return true;
        }

        // defines invocation type for stack filling
        if (option.equals(INVOCATION_TYPE)) {
            if ( (!value.equals(JAVA_TYPE)) &&
                 (!value.equals(NATIVE_TYPE)) &&
                 (!value.equals(MIXED_TYPE))
               ) {
                throw new BadOption(option + ": must be one of: "
                                  + "\"" + JAVA_TYPE + "\", "
                                  + "\"" + NATIVE_TYPE + "\", "
                                  + "\"" + MIXED_TYPE + "\"");
            }
            return true;
        }

        // defines default or custom MBean server
        if (option.equals(SERVER_TYPE)) {
            if ((!value.equals(DEFAULT_TYPE))
                && (!value.equals(CUSTOM_TYPE))) {
                throw new BadOption(option + ": must be one of: \""
                                           + DEFAULT_TYPE + "\", \""
                                           + CUSTOM_TYPE + "\"");
            }

            return true;
        }

        // defines loadable classes and loaders counts
        if (option.equals(LOADABLE_CLASSES_COUNT) ||
                option.equals(LOADERS_COUNT) ||
                option.equals(THREAD_DEPTH) || option.equals(THREAD_COUNT)) {
            try {
                int number = Integer.parseInt(value);
                if (number < 0) {
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
                }
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": value must be an integer");
            }
            return true;
        }

        // defines timeout
        if (option.equals(TIMEOUT)) {
            try {
                int number = Integer.parseInt(value);

                if (number < 0)
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": value must be an integer");
            }
            return true;
        }

        // defines if classloader class is single
        if (option.equals(SINGLE_CLASSLOADER_CLASS)) {
            if (!(value == null || value.length() <= 0)) {
                throw new BadOption(option + ": no value must be specified");
            }
            return true;
        }

        // defines memory types
        if (option.equals(MEMORY_TYPE)) {
            if ( (!value.equals(MT_HEAP)) &&
                 (!value.equals(MT_NONHEAP)) &&
                 (!value.equals(MT_MIXED))
               )
                throw new BadOption(option + ": must be one of: "
                                  + "\"" + MT_HEAP + "\", "
                                  + "\"" + MT_NONHEAP + "\", "
                                  + "\"" + MT_MIXED + "\"");
            return true;
        }

        // defines type of monitoring
        if (option.equals(MONITORING)) {
            if ( (!value.equals(MON_POLLING)) &&
                 (!value.equals(MON_NOTIF))
               )
                throw new BadOption(option + ": must be one of: "
                                  + "\"" + MON_POLLING + "\", "
                                  + "\"" + MON_NOTIF + "\"");
            return true;
        }

        // defines threshold
        if (option.equals(THRESHOLD)) {
            if ( (!value.equals(TH_USAGE)) &&
                 (!value.equals(TH_COLLECTION))
               )
                throw new BadOption(option + ": must be one of: "
                                  + "\"" + TH_USAGE + "\", "
                                  + "\"" + TH_COLLECTION + "\"");
            return true;
        }

        if (option.equals(SCENARIO_TYPE)) {
                return true;
        }
        if (option.equals(ITERATIONS)) {
            try {
                int number = Integer.parseInt(value);

                if (number < 0)
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
                return true;
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": value must be an integer");
            }

        }
        return super.checkOption(option, value);
    }

    /**
     * Check if the values of all options are consistent.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @throws <i>BadOption</i> if options have inconsistent values
     *
     * @see nsk.share.ArgumentParser#parseArguments
     */
    protected void checkOptions() {
        super.checkOptions();
    }

    public void dump(Log log) {
            log.info("Test mode: " + getTestMode());
            log.info("Server type: " + getServerType());
            log.info("loadableClassesCount: " + getLoadableClassesCount());
            log.info("loadersCount: " + getLoadersCount());
            log.info("singleClassloaderClass: " + singleClassloaderClass());
    }
} // ArgumentHandler
