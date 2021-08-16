/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import nsk.share.test.StressOptions;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Parser for JDI test's command-line arguments.
 * <p>
 * Test's command line may contain two kind of arguments, namely:
 * <ul>
 * <li> options for ArgumentParser
 * <li> other arguments for the test itself
 * </ul>
 * <p>
 * We call <i>raw arguments</i> the <code>args[]</code> array
 * passed to the test's method <code>main(String&nbsp;args[])</code>.
 * ArgumentParser instance initialized with raw arguments serves to parse
 * these two kinds of arguments. Use <code>ArgumentParser(args[])</code>
 * constructor, or <code>setRawArguments(args[])</code> method
 * to initialize a ArgumentParser instance with particular raw arguments.
 * <p>
 * Arguments, started with ``<code>-</code>'' symbol are called <i>options</i>.
 * They are recognized by ArgumentParser and are used by support classes
 * (such as Log, Binder, etc.).
 * These options should be specified in the following general form:
 * <ul>
 * <li> <code>-option=<i>value</i></code>
 * </ul>
 * or
 * <ul>
 * <li> <code>-option <i>value</i></code>
 * </ul>
 * List of the recognized options with their values may be obtained by
 * invoking method <code>getOptions()</code> that returns
 * a <code>Properties</code> object with options values.
 * It is not recommended to get options value directly. An appropriate methods
 * such as <code>verbose()</code>, <code>getArch()</code>, etc. should be used
 * instead.
 * Options may appear in the test command line in any order.
 * <p>
 * All the other arguments of command line are called <i>test arguments</i>
 * (or simply <i>arguments</i>). These arguments should be handled by test itself.
 * Full list of the test arguments in the same order as they appears in the command line
 * may be obtained by invoking method <code>getArguments()</code>.
 * <p>
 * Following is the list of basic options accepted by ArgumentParser:
 * <ul>
 * <li> <code>-arch=</code>&lt;<i>${ARCH}</i>&gt; -
 *   architecture name
 * <li> <code>-waittime=</code>&lt;<i>minutes</i>&gt; -
 *   timeout in minutes for waiting events or so
 * <li> <code>-verbose</code> -
 *   verbose Log mode (default is quiet)
 * <li> <code>-trace.time</code> -
 *   prefix log messages with timestamps (default is no)
 * </ul>
 * Also ArgumentParser supports following stress options (see nsk.share.test.StressOptions for details):
 * <ul>
 * <li> <code>-stressTime</code>
 * <li> <code>-stressIterationsFactor</code>
 * <li> <code>-stressThreadsFactor</code>
 * <li> <code>-stressDebug</code>
 * </ul>
 * <p>
 * Note that the tests from the particular suites have its own argument handlers
 * which accepts additional options.
 *
 * @see #setRawArguments(String[])
 * @see #getRawArguments()
 * @see #getArguments()
 * @see #getOptions()
 * @see nsk.share.jpda.DebugeeArgumentHandler
 * @see nsk.share.jdwp.ArgumentHandler
 * @see nsk.share.jdi.ArgumentHandler
 * @see nsk.share.jvmti.ArgumentHandler
 * @see nsk.monitoring.share.ArgumentHandler
 */
public class ArgumentParser {
    /**
     * Raw array of command-line arguments.
     *
     * @see #setRawArguments(String[])
     * @see #getRawArguments()
     */
    protected String[] rawArguments = null;

    /**
     * Refined arguments -- raw arguments but options.
     *
     * @see #options
     * @see #getArguments()
     */
    protected String[] arguments = null;

    /**
     * Recognized options for ArgumentParser class.
     *
     * @see #arguments
     * @see #getOptions()
     */
    protected Properties options = new Properties();

    /**
     * Make new ArgumentParser object with default values of options.
     * This constructor is used only to obtain default values of options.
     *
     * @see #setRawArguments(String[])
     */
    protected ArgumentParser() {
        this(new String[0]);
    }

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param args Array of the raw command-line arguments.
     * @throws BadOption If option values are invalid.
     * @see #setRawArguments(String[])
     * @see BadOption
     */
    public ArgumentParser(String[] args) {
        ArrayList<String> list = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; ++i) {
            StringBuilder arg = new StringBuilder(args[i]);
            // jtreg splits the command string into arguments by space symbol
            // and doesn't keep arguments within double quotes as one argument,
            // so we need to join them back
            long doubleQuotes = numberOfDoubleQuotes(args[i]);
            while (i < args.length - 1 && (doubleQuotes % 2) != 0) {
                arg.append(" ").append(args[++i]);
                doubleQuotes += numberOfDoubleQuotes(args[i]);
            }
            if (doubleQuotes % 2 != 0) {
                throw new TestBug("command-line has odd number of double quotes:" + String.join(" ", args));
            }

            list.add(arg.toString());
        }
        setRawArguments(list.toArray(String[]::new));
    }

    private static long numberOfDoubleQuotes(String s) {
        return s.chars().filter(c -> c == '"').count();
    }

    /**
     * Return a copy of the raw command-line arguments kept by
     * this ArgumentParser instance.
     *
     * @throws NullPointerException If raw arguments were not
     *                              set for this instance.
     * @see #setRawArguments(String[])
     */
    public String[] getRawArguments() {
        return rawArguments.clone();
    }

    /**
     * Return given raw command-line argument.
     *
     * @param index index of argument
     * @return value of raw argument
     */
    public String getRawArgument(int index) {
        return rawArguments[index];
    }

    /**
     * Return refined array of test arguments (only those of the raw
     * arguments which are not recognized as options for ArgumentParser).
     *
     * <p>Note, that syntax of test arguments was not checked;
     * while syntax of arguments describing ArgumentParser's options
     * was checked while raw arguments were set to this ArgumentParser
     * instance.
     *
     * @throws NullPointerException If raw arguments were not
     *                              set for this instance.
     * @see #setRawArguments(String[])
     * @see #getOptions()
     */
    public String[] getArguments() {
        return arguments.clone();
    }

    /**
     * Return list of recognized options with their values in the form of
     * <code>Properties</code> object.
     * If no options has been recognized, this list will be empty.
     *
     * @see #setRawArguments(String[])
     * @see #getArguments()
     */
    public Properties getOptions() {
        return (Properties) options.clone();
    }

    /**
     * Join specified arguments into one line using given quoting
     * and separator symbols.
     *
     * @param args      Array of the command-line arguments
     * @param quote     Symbol used to quote each argument
     * @param separator Symbol used as separator between arguments
     * @return Single line with arguments
     */
    static public String joinArguments(String[] args, String quote, String separator) {
        if (args.length <= 0) {
            return "";
        }
        StringBuilder line = new StringBuilder(quote).append(args[0]).append(quote);
        for (int i = 1; i < args.length; i++) {
            line.append(separator).append(quote).append(args[i]).append(quote);
        }
        return line.toString();
    }

    /**
     * Join specified arguments into one line using given quoting symbol
     * and space as a separator symbol.
     *
     * @param args  Array of the command-line arguments
     * @param quote Symbol used to quote each argument
     * @return Single line with arguments
     */
    static public String joinArguments(String[] args, String quote) {
        return joinArguments(args, quote, " ");
    }

    /**
     * Keep a copy of command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param args Array of the raw command-line arguments.
     * @throws BadOption If an option has invalid value.
     * @see #getRawArguments()
     * @see #getArguments()
     */
    public void setRawArguments(String[] args) {
        this.rawArguments = args.clone();
        parseArguments();
    }

    /**
     * Add or replace given option value in options list and in raw arguments list.
     * Use specified <code>rawPrefix</code> while adding to raw arguments list.
     *
     * @see #getRawArguments()
     * @see #getOptions()
     */
    public void setOption(String rawPrefix, String name, String value) {
        String prefix = rawPrefix + name + "=";
        String arg = prefix + value;

        options.setProperty(name, value);

        int length = rawArguments.length;
        boolean found = false;
        for (int i = 0; i < length; i++) {
            if (rawArguments[i].startsWith(prefix)) {
                found = true;
                rawArguments[i] = arg;
                break;
            }
        }

        if (!found) {
            String[] newRawArguments = new String[length + 1];
            System.arraycopy(rawArguments, 0, newRawArguments, 0, length);
            newRawArguments[length] = arg;
            rawArguments = newRawArguments;
        }
    }

    /**
     * Return current architecture name from ArgumentParser's
     * options.
     *
     * <p>Note that null string is returning if test argument
     * <code>-arch</code> has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public String getArch() {
        return options.getProperty("arch");
    }

    /**
     * Timeout (in minutes) for test's critical section like:
     * (a) awaiting for an event, or conversely (b) making sure
     * that there is no unexpected event.
     *
     * <p>By default, <i>2</i> minutes is returned if option
     * <code>-waittime</code> is not set with command line.
     *
     * @see TimeoutHandler
     */
    public int getWaitTime() {
        String val = options.getProperty("waittime", "2");
        int minutes;
        try {
            minutes = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new TestBug("Not integer value of \"waittime\" argument: " + val);
        }
        return minutes;
    }

    /**
     * Return boolean value of current Log mode:
     * <ul>
     * <li><i>true</i> if Log mode is verbose.
     * <li><i>false</i> otherwise.
     *
     * <p>Note that default Log mode is quiet if test argument
     * <code>-verbose</code> has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public boolean verbose() {
        return options.getProperty("verbose") != null;
    }

    /**
     * Return boolean value of setting of timestamp for log messages:
     * <ul>
     * <li><i>true</i> if Log messages are timestamp'ed.
     * <li><i>false</i> otherwise.
     *
     * <p>Note that by default Log messages won't be timestamp'ed until
     * <code>-trace.time</code> has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public boolean isTimestamp() {
        return options.getProperty("trace.time") != null;
    }

    /**
     * Return level of printing tracing messages for debugging purpose.
     * Level <i>0</i> means no tracing messages at all.
     *
     * <p>Note that by default no tracing messages will be printed out
     * until <code>-trace.level</code> has not been set.
     *
     * @see #setRawArguments(String[])
     */
    public int getTraceLevel() {
        String value = options.getProperty("trace.level", Integer.toString(Log.TraceLevel.DEFAULT));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new Failure("Not integer value of -trace.level option: " + value);
        }
    }

    /**
     * Parse arguments from rawArguments, extract recognized options,
     * check legality of options values options and store non-option
     * arguments.
     *
     * @throws NullPointerException If raw arguments were not set
     *                              for this ArgumentParser instance.
     * @throws BadOption            If Option name is not accepted or
     *                              option has illegal value.
     * @see #setRawArguments(String[])
     * @see #checkOption(String, String)
     * @see #checkOptions()
     */
    protected void parseArguments() {
        String[] selected = new String[rawArguments.length];
        Properties properties = new Properties();
        int count = 0;
        for (int i = 0; i < rawArguments.length; i++) {
            String argument = rawArguments[i];
            if (argument.startsWith("-")) {
                int pos = argument.indexOf("=", 1);
                String option, value;
                if (pos < 0) {
                    option = argument.substring(1);
                    if (i + 1 < rawArguments.length && !rawArguments[i + 1].startsWith("-")) {
                        value = rawArguments[i + 1];
                        ++i;
                    } else {
                        value = "";
                    }
                } else {
                    option = argument.substring(1, pos);
                    value = argument.substring(pos + 1);
                }

                if (!checkOption(option, value)) {
                    throw new BadOption("Unrecognized command line option: " + argument);
                }
                properties.setProperty(option, value);
            } else {
                selected[count++] = rawArguments[i];
            }
        }
        // Strip away the dummy tail of the selected[] array:
        arguments = new String[count];
        System.arraycopy(selected, 0, arguments, 0, count);
        options = properties;
        checkOptions();
    }

    public StressOptions getStressOptions() {
        return new StressOptions(rawArguments);
    }

    /**
     * Check if the specified option is allowed and has legal value.
     * <p>
     * Derived classes for handling test arguments in particular sub-suites
     * override this method to allow to accept sub-suite specific options.
     * However, they should invoke this method of the base class to ensure
     * that the basic options will be accepted too.
     *
     * @return <i>true</i> if option is allowed and has legal value
     * <i>false</i> if option is unknown
     * @throws BadOption If value of the allowed option is illegal.
     * @see #setRawArguments(String[])
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {
        // accept arguments of nsk.share.test.StressOptions
        if (StressOptions.isValidStressOption(option))
            return true;

        // options with any string value
        if (option.equals("arch")) {
            return true;
        }

        // options with positive integer value
        if (option.equals("waittime")
                || option.equals("trace.level")) {
            try {
                int number = Integer.parseInt(value);
                if (number < 0) {
                    throw new BadOption(option + ": value must be a positive integer");
                }
            } catch (NumberFormatException e) {
                throw new BadOption(option + ": value must be an integer");
            }
            return true;
        }

        // options without any value
        if (option.equals("verbose")
                || option.equals("vbs")
                || option.equals("trace.time")) {
            if (!(value == null || value.length() <= 0)) {
                throw new BadOption(option + ": no value must be specified");
            }
            return true;
        }

        return false;
    }

    /**
     * Check that the value of all options are not inconsistent.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @throws BadOption If value of the options are inconsistent
     * @see #parseArguments()
     */
    protected void checkOptions() {
        // do nothing
    }

    /**
     * Thrown if invalid option or option value is found.
     */
    public static class BadOption extends IllegalArgumentException {
        /**
         * Explain the reason.
         *
         * @param message Printing message.
         */
        public BadOption(String message) {
            super(message);
        }
    }
}
