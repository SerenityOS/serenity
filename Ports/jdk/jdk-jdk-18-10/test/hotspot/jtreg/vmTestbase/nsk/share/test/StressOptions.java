/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.test;

import jdk.test.lib.Utils;
import vm.share.options.Option;

import java.io.PrintStream;

/**
 * Options for stress tests.
 * <p>
 * The following options may be configured:
 * <p>
 * -stressTime [time] execution time in seconds
 * -stressIterationsFactor [factor] iterations factor.
 * The actual number of iterations is obtained by multiplying standard
 * number of iterations (which is defined by the test itself) and this factor.
 * -stressThreadsFactor [factor] number of threads factor
 * The actual number of threads is determined by multiplying standard
 * number of threads (which is determined by test itself and may also depend
 * on machine configuration) and this factor.
 */
public class StressOptions {
    /**
     * This enum contains names of stress options
     */
    public enum StressOptionsParam {
        stressTime,
        stressIterationsFactor,
        stressThreadsFactor,
        stressRunsFactor,
        stressDebug,
        stressDebugDetailed,
    }

    /* Execution time in seconds */
    @Option(name = "stressTime", default_value = "30", description = "Stress execution time in seconds")
    private long time;

    /* Iterations factor */
    @Option(name = "stressIterationsFactor", default_value = "1", description = "Stress iterations factor")
    private int iterationsFactor;

    /* Number of threads factor */
    @Option(name = "stressThreadsFactor", default_value = "1", description = "Stress threads factor")
    private int threadsFactor;

    @Option(name = "stressRunsFactor", default_value = "1", description = "Times to re-run the test (if supported by the test)")
    private int runsFactor;

    /* Debug stress execution */
    @Option(name = "stressDebugEnabled", default_value = "false", description = "Stress debug execution enabled")
    private boolean debugEnabled = false;

    /* Detailed stressExecution */
    @Option(name = "stressDebugDetailed", default_value = "false", description = "Stress debug detailed enabled")
    private boolean debugDetailed = false;

    /**
     * Create StressOptions with default settings.
     */
    public StressOptions() {
        time = 30;
        iterationsFactor = 1;
        threadsFactor = 1;
        runsFactor = 1;
    }

    /**
     * Create StressOptions configured from command line arguments.
     *
     * @param args arguments
     */
    public StressOptions(String[] args) {
        this();
        parseCommandLine(args);
    }

    public static boolean isValidStressOption(String option) {
        for (int i = 0; i < StressOptions.StressOptionsParam.values().length; i++) {
            if (option.equals(StressOptions.StressOptionsParam.values()[i].name()))
                return true;
        }

        return false;
    }

    /**
     * Parse command line options related to stress.
     * <p>
     * Other options are ignored.
     *
     * @param args arguments
     */
    public void parseCommandLine(String[] args) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            String value = null;

            int eqPos = arg.indexOf('=');
            if (eqPos != -1) {
                value = arg.substring(eqPos + 1);
                arg = arg.substring(0, eqPos);
            }

            if (arg.equals("-stressTime")) {
                try {
                    if (value == null) {
                        if (++i >= args.length)
                            error("Missing value of -stressTime parameter");
                        value = args[i];
                    }
                    time = Long.parseLong(value);
                    if (time < 0) {
                        error("Invalid value of -stressTime parameter: " + time);
                    }
                } catch (NumberFormatException e) {
                    error("Invalid value of -stressTime parameter: " + value);
                }
            } else if (arg.equals("-stressIterationsFactor")) {
                try {
                    if ( value == null ) {
                        if (++i >= args.length) {
                            error("Missing value of -stressIterationsFactor parameter");
                        }
                        value = args[i];
                    }
                    iterationsFactor = Integer.parseInt(value);
                    if (iterationsFactor <= 0) {
                        error("Invalid value of -stressIterationsFactor parameter: " + threadsFactor);
                    }
                } catch (NumberFormatException e) {
                    error("Invalid value of -stressIterationsFactor parameter: " + value);
                }
            } else if (arg.equals("-stressThreadsFactor")) {
                try {
                    if ( value == null ) {
                        if (++i >= args.length) {
                            error("Missing value of -stressThreadsFactor parameter");
                        }
                        value = args[i];
                    }
                    threadsFactor = Integer.parseInt(value);
                    if (threadsFactor <= 0) {
                        error("Invalid value of -stressThreadsFactor parameter: " + threadsFactor);
                    }
                } catch (NumberFormatException e) {
                    error("Invalid value of -stressThreadsFactor parameter: " + value);
                }
            } else if (arg.equals("-stressRunsFactor")) {
                try {
                    if (value == null) {
                        if (++i >= args.length) {
                            error("Missing value of -stressRunsFactor parameter");
                        }
                        value = args[i];
                    }
                    runsFactor = Integer.parseInt(value);
                    if (runsFactor <= 0) {
                        error("Invalid value of -stressRunsFactor parameter: " + threadsFactor);
                    }
                } catch (NumberFormatException e) {
                    error("Invalid value of -stressRunsFactor parameter: " + value);
                }
            } else if (arg.equals("-stressDebug")) {
                debugEnabled = true;
            } else if (arg.equals("-stressDebugDetailed")) {
                debugDetailed = true;
            }

            ++i;
        }
    }

    /**
     * Display information about stress options.
     *
     * @param out output stream
     */
    public void printInfo(PrintStream out) {
        out.println("Stress time: " + getTime() + " seconds");
        out.println("Stress iterations factor: " + getIterationsFactor());
        out.println("Stress threads factor: " + getThreadsFactor());
        out.println("Stress runs factor: " + getRunsFactor());
    }

    private void error(String msg) {
        throw new IllegalArgumentException(msg);
    }

    /**
     * Obtain execution time in seconds adjusted for TIMEOUT_FACTOR.
     *
     * @return time
     */
    public long getTime() {
        return Utils.adjustTimeout(time);
    }

    /**
     * Obtain iterations factor.
     *
     * @return iterations factor
     */
    public int getIterationsFactor() {
        return iterationsFactor;
    }

    /**
     * Obtain threads factor.
     *
     * @return threads factor
     */
    public int getThreadsFactor() {
        return threadsFactor;
    }

    /**
     * Obtain runs factor.
     *
     * @return runs factor
     */
    public int getRunsFactor() {
        return runsFactor;
    }

    /**
     * Determine if debugging of stress execution is set.
     *
     * @return true if debugging stress execution
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Determine if detailed debugging of stress execution is set.
     *
     * @return true if detailed debugging is enabled
     */
    public boolean isDebugDetailed() {
        return debugDetailed;
    }
}
