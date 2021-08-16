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

package nsk.share.gc;

import nsk.share.*;

/**
 * Parser for GC tests' arguments.
 * <p>
 * <code>ArgumentHandler</code> handles specific command line arguments
 * related to way of execution of a test in addition to general arguments
 * recognized by {@link ArgumentParser <code>ArgumentParser</code>}.
 * <p>
 * Following is the list of specific options for <code>ArgumentHandler</code>:
 * <ul>
 * <li><code>-iterations="<i>value</i>"</code>, where <i>value</i> must either
 *     be "infinity", or an integer number, greater than 0. This parameter
 *     specifies the number of iterations to run the testcase. If the value is
 *     "infinity", then the test will be run for at least <code>gcTimeout</code>
 *     minutes. Otherwise, the testcase will be repeated for
 *     <code>iterations</code> times.
 * <li><code>-gcTimeout="<i>value</i>"</code>, where <i>value</i> must be an
 *     integer number, greater than 0. If <i>infinity</i> is set to
 *     <code>iterations</code>, then the test consider <code>gcTimeout</code>
 *     argument to run the test for at least specified number of minutes.
 * <li><code>-threads="<i>value</i>"</code>, where <i>value</i> must be an
 *     integer number, greater than 0. A user may specify the number of threads
 *     to start in the test with that paramenter. However, a test may ignore
 *     this value, if it does know the number of threads to start. It
 *     depends on a test: read its README file.
 * <li><code>-memoryEater="<i>value</i>"</code>, where <i>value</i> must be
 *     either "single", or "multi" string. This argument specifies if a single
 *     thread should be used to eat the whole heap or not. If "multi" string is
 *     assigned to <code>-memoryEater</code>, then a number of threads will be
 *     started to eat the heap. The number is equal to number of available
 *     processors plus 1.
 * <li><code>-largeClassesPath="<i>value</i>"</code>, where <i>value</i> is a
 *     directory to load large classes from.
 * <li><code>-fieldsLimitation="<i>value</i>"</code>, where <i>value</i> must
 *     be either "over", or "under" string. This argument specifies what classes
 *     should be loaded from <code>largeClassesPath</code> directory. If
 *     <i>over</i> is set, then the classes that have number of fileds over
 *     JVM limitation should be loaded, otherwise -- classes that have number
 *     of fileds under limitation.
 * </ul>
 * @see ArgumentParser
 */
public class ArgumentHandler extends ArgumentParser {

    // Define all possible arguments
    private final static String ITERATIONS = "iterations";
    private final static String AGGREGATION_DEPTH = "aggregationDepth";
    private final static String GC_TIMEOUT = "gcTimeout";
    private final static String THREADS = "threads";
    private final static String MEM_EATER = "memoryEater";
    private final static String LARGE_CLASSES_PATH = "largeClassesPath";
    private final static String FIELDS_LIMITATION = "fieldsLimitation";

    // An acceptible value for ITERATIONS
    private final static String INFINITY = "infinity";

    // Acceptible values for MEM_EATER
    private final static String ME_SINGLE = "single";
    private final static String ME_MULTI = "multi";

    // Acceptible values for FIELDS_LIMITATION
    private final static String FL_OVER = "over";
    private final static String FL_UNDER = "under";

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param args Array of the raw command-line arguments.
     *
     * @throws BadOption  If unknown option or illegal option value found
     *
     * @see ArgumentParser
     */
    public ArgumentHandler(String args[]) {
        super(args);
    }

    /**
     * Returns number of iterations.
     * <p>
     * If <code>-iterations="<i>infinity</i>"</code>, the method returns -1.
     * If the argument is not set, the method returns 1. Otherwise, the
     * specified number is returned.
     *
     * @return number of iterations.
     *
     */
    public int getIterations() {
        String value = options.getProperty(ITERATIONS, "1");

        if (INFINITY.equals(value))
            return -1;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not an integer value of \"" + ITERATIONS
                            + "\" argument: " + value);
        }
    }


    /**
     * Returns the depth of object aggregation.
     * <p>
     * If the argument is not set, the method returns 0. Otherwise, the
     * specified number is returned.
     *
     * @return number of aggregation depth.
     *
     */
    public int getAggregationDepth() {
        String value = options.getProperty(AGGREGATION_DEPTH, "0");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not an integer value of \"" + AGGREGATION_DEPTH
                            + "\" argument: " + value);
        }
    }


    /**
     * Returns number of minutes to run the test.
     * <p>
     * @return number of minutes to run the test.
     *
     */
    public int getGCTimeout() {
        String value = options.getProperty(GC_TIMEOUT);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("\"" + GC_TIMEOUT + "\" argument is not defined "
                            + "or is not integer: " + value);
        }
    }

    /**
     * Returns a directory to load large classes from.
     * <p>
     * @return a directory to load large classes from.
     *
     */
    public String getLargeClassesPath() {
        return options.getProperty(LARGE_CLASSES_PATH);
    }

    /**
     * Returns number of threads to start in a test. If <code>threads</code>
     * is not set, the method returns specified number of threads.
     * <p>
     * @param defaultValue default value, if <code>threads</code> is not set.
     * @return number of threads to start in a test.
     *
     */
    public int getThreads(int defaultValue) {
        String value = options.getProperty(THREADS);

        if (value == null)
            return defaultValue;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestBug("Not an integer value of \"" + THREADS
                            + "\" argument: " + value);
        }
    }

    /**
     * Returns true if single thread should be used to eat the whole heap,
     * false otherwise.
     *
     * @return true if single thread should be used to eat the whole heap,
     * false otherwise.
     *
     */
    public boolean isSingleMemoryEater() {
        String value = options.getProperty(MEM_EATER);

        if (value == null)
            return true;
        else if (value.equals(ME_SINGLE))
            return true;
        else if (value.equals(ME_MULTI))
            return false;
        else
            throw new TestBug("Value for \"" + MEM_EATER + "\" must be either "
                            + ME_SINGLE + ", or " + ME_MULTI);
    }

    /**
     * Returns true if classes with number of fileds over limitation should be
     * loaded, false otherwise.
     *
     * @return true if classes with number of fileds over limitation should be
     * loaded, false otherwise.
     *
     */
    public boolean isOverFieldsLimitation() {
        String value = options.getProperty(FIELDS_LIMITATION);

        if (value == null)
            return false;
        else if (value.equals(FL_OVER))
            return true;
        else if (value.equals(FL_UNDER))
            return false;
        else
            throw new TestBug("Value for \"" + FIELDS_LIMITATION + "\" must be "
                            + "either " + FL_OVER + ", or " + FL_UNDER);
    }

    /**
     * Checks if an option is allowed and has proper value.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @param option option name
     * @param value string representation of value
     *                      (could be an empty string too)
     *              null if this option has no value
     * @return <i>true</i> if option is allowed and has proper value,
     *         <i>false</i> if option is not admissible
     *
     * @throws <i>BadOption</i> if option has an illegal value
     *
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {

        // Define iterations
        if (option.equals(ITERATIONS)) {
            if (INFINITY.equals(value))
                return true;

            try {
                int number = Integer.parseInt(value);

                if (number < 1)
                    throw new BadOption(option + ": value must be greater than "
                                               + "zero.");
            } catch (NumberFormatException e) {
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be integer or \"" + INFINITY + "\": "
                                  + value);
            }
            return true;
        }

        // Define timeout
        if (option.equals(GC_TIMEOUT)) {
            try {
                int number = Integer.parseInt(value);

                if (number < 0)
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
            } catch (NumberFormatException e) {
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be integer: " + value);
            }
            return true;
        }

        // Define threads
        if (option.equals(THREADS)) {
            try {
                int number = Integer.parseInt(value);

                if (number < 0)
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
            } catch (NumberFormatException e) {
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be integer: " + value);
            }
            return true;
        }

        // Define path to large classes
        if (option.equals(LARGE_CLASSES_PATH))
            return true;

        // Define memory eater
        if (option.equals(MEM_EATER)) {
            if ( (ME_SINGLE.equals(value)) || (ME_MULTI.equals(value)) )
                return true;
            else
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be either " + ME_SINGLE + ", or "
                                  + ME_MULTI + ": " + value);
        }

        // Define fields limitation
        if (option.equals(FIELDS_LIMITATION)) {
            if ( (FL_OVER.equals(value)) || (FL_UNDER.equals(value)) )
                return true;
            else
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be either " + FL_OVER + ", or "
                                  + FL_UNDER + ": " + value);
        }

        // Define aggregationDepth
        if (option.equals(AGGREGATION_DEPTH)) {
            try {
                int number = Integer.parseInt(value);

                if (number < 0)
                    throw new BadOption(option + ": value must be a positive "
                                      + "integer");
            } catch (NumberFormatException e) {
                throw new BadOption("Value for option \"" + option + "\" must "
                                  + "be integer: " + value);
            }
            return true;
        }

        return super.checkOption(option, value);
    }

    /**
     * Checks if the values of all options are consistent.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @throws <i>BadOption</i> if options have inconsistent values
     *
     * @see ArgumentParser#parseArguments()
     */
    protected void checkOptions() {
        super.checkOptions();
    }
} // ArgumentHandler
