/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;

import nsk.share.test.LazyFormatString;

/**
 * This class helps to print test-execution trace messages
 * and filter them when execution mode is not verbose.
 * <p>
 * Verbose mode if defined by providing <i>-verbose</i> command line
 * option, handled by <code>ArgumentParser</code>. Use <code>verbose()</code>
 * method to determine which mode is used.
 * <p>
 * <code>Log</code> provides with two main methods to print messages:
 * <ul>
 *   <li> <code>complain(String)</code> - to print error message
 *   <li> <code>display(String)</code> - to print additional log message
 * </ul>
 * No other way to print messages to the log stream should be used.
 * <p>
 * Error messages appeares in log stream in all modes. Additional log massages,
 * printed with <code>display()</code> method will be filtered out, if log mode
 * is not verbose. In verbose log made messages of both types are printed.
 * Additionally, in verbose mode a summary of all occured errors will be printed
 * at the program exit, by automatically invoking method
 * <code>printErrorsSummary()</code>.
 * <p>
 * To provide printing messages from different sources into one log
 * with distinct prefixes use internal <code>Log.Logger</code> class.
 *
 * @see #verbose()
 * @see #complain(String)
 * @see #display(String)
 * @see ArgumentParser
 * @see Log.Logger
 */
public class Log extends FinalizableObject {
    /**
     * Report step-by-step activity to this stream.
     *
     * @deprecated  Tests should not use this field directly.
     */
    @Deprecated
    protected PrintStream out = null;

    /**
     * Is log-mode verbose?
     * Default value is <code>false</code>.
     */
    private boolean verbose = false;

    /**
     * Should log messages prefixed with timestamps?
     * Default value is <code>false</code>.
     */
    private boolean timestamp = false;

    /**
     * Names for trace levels
     */
    public static final class TraceLevel {
        public static final int TRACE_NONE = 0;
        public static final int TRACE_IMPORTANT = 1;
        public static final int TRACE_NORMAL = 2;
        public static final int TRACE_VERBOSE = 3;
        public static final int TRACE_DEBUG = 4;

        public static final int DEFAULT = TRACE_IMPORTANT;

        public static final Map<String, Integer> NAME_TO_LEVEL_MAP = new HashMap<String, Integer>();
        static {
            NAME_TO_LEVEL_MAP.put("none", TRACE_NONE);
            NAME_TO_LEVEL_MAP.put("important", TRACE_IMPORTANT);
            NAME_TO_LEVEL_MAP.put("info", TRACE_NORMAL);
            NAME_TO_LEVEL_MAP.put("verbose", TRACE_VERBOSE);
            NAME_TO_LEVEL_MAP.put("debug", TRACE_DEBUG);
            NAME_TO_LEVEL_MAP.put("default", DEFAULT);
        }

        public static int nameToLevel(String value) throws IllegalArgumentException {
            Integer level = NAME_TO_LEVEL_MAP.get(value.toLowerCase());
            if ( level == null )
                throw new IllegalArgumentException("Wrong trace level: " + value);

            return level;
        }

        public static String getLevelsString() {
            StringBuffer result = new StringBuffer();
            for ( String s : NAME_TO_LEVEL_MAP.keySet() ) {
                result.append(s).append(", ");
            }
            return result.substring(0, result.length() - 3);
        }
    }

    /**
     * Threshold value for printing trace messages for debugging purpose.
     * Default value is <code>0</code> a.k.a. <code>TraceLevel.INFO</code>;
     */
    private int traceLevel = TraceLevel.DEFAULT;

    /**
     * Is printing errors summary enabled? Default value is <code>true</code>;
     */
    private boolean errorsSummaryEnabled = true;

    /**
     * Is printing saved verbose messages on error enabled? Default value is <code>true</code>;
     */
    private boolean verboseOnErrorEnabled = true;

    /**
     * This <code>errosBuffer</code> will keep all messages printed via
     * <code>complain()</code> method for final summary output.
     * Ensure that buffer has enough room for messages to keep,
     * to minimize probability or OutOfMemory error while keeping
     * an error message in stress tests.
     */
    private Vector<String> errorsBuffer = new Vector<String>(1000);

    /**
     * Most tests in nsk do not log exceptions, they only log an error message.
     * This makes failure analysis harder.
     * To solve this we will automatically generate Exceptions for error logs.
     * To not log too many Exceptions, we try to log each unique error only once.
     * <code>loggedExceptions</code> contains all messages that have already been logged.
     */
    private Set<String> loggedExceptions = new HashSet<String>();

    /**
     * This <code>logBuffer</code> will keep all messages printed via
     * <code>display()</code> method in non-verbose mode until
     * swithching verbose mode on or invoking <code>complain()</code>.
     * Ensure that buffer has enough room for messages to keep,
     * to minimize probability or OutOfMemory error while keeping
     * an error message in stress tests.
     */
    private Vector<String> logBuffer = new Vector<String>(1000);

    /**
     * Did I already warned if output stream is not assigned?
     */
    private boolean noOutWarned = false;

    /////////////////////////////////////////////////////////////////

    /**
     * Create new Log's only with <code>Log(out)</code> or with
     * <code>Log(out,argsHandler)</code> constructors.
     *
     * @deprecated  Extending test class with Log is obsolete.
     */
    @Deprecated
    protected Log() {
        // install finalizer to print errors summary at exit
        Finalizer finalizer = new Finalizer(this);
        finalizer.activate();

        // Don't log exceptions from this method. It would just add unnecessary logs.
        loggedExceptions.add("nsk.share.jdi.SerialExecutionDebugger.executeTests");
    }

    /**
     * Incarnate new Log for the given <code>stream</code> and
     * for non-verbose mode.
     */
    public Log(PrintStream stream) {
        this();
        out = stream;
    }

    /**
     * Incarnate new Log for the given <code>stream</code>; and
     * either for verbose or for non-verbose mode accordingly to
     * the given <code>verbose</code> key.
     */
    public Log(PrintStream stream, boolean verbose) {
        this(stream);
        this.verbose = verbose;
    }

    /**
     * Incarnate new Log for the given <code>stream</code>; and
     * either for verbose or for non-verbose mode accordingly to
     * the given <code>argsHandler</code>.
     */
    public Log(PrintStream stream, ArgumentParser argsParser) {
        this(stream, argsParser.verbose());
        traceLevel = argsParser.getTraceLevel();
        timestamp = argsParser.isTimestamp();
    }

    /////////////////////////////////////////////////////////////////

    /**
     * Return <i>true</i> if log mode is verbose.
     */
    public boolean verbose() {
        return verbose;
    }

    /**
     * Return <i>true</i> if printing errors summary at exit is enabled.
     */
    public boolean isErrorsSummaryEnabled() {
        return errorsSummaryEnabled;
    }

    /**
     * Enable or disable printing errors summary at exit.
     */
    public void enableErrorsSummary(boolean enable) {
        errorsSummaryEnabled = enable;
    }

    /**
     * Return <i>true</i> if printing saved verbose messages on error is enabled.
     */
    public boolean isVerboseOnErrorEnabled() {
        return errorsSummaryEnabled;
    }

    /**
     * Enable or disable printing saved verbose messages on error.
     */
    public void enableVerboseOnError(boolean enable) {
        verboseOnErrorEnabled = enable;
    }

    /**
     * Enable or disable verbose mode for printing messages.
     */
    public void enableVerbose(boolean enable) {
        if (!verbose) {
            flushLogBuffer();
        }
        verbose = enable;
    }

    public int getTraceLevel() {
        return traceLevel;
    }

    /**
     * Set threshold for printing trace messages.
     * Warning: trace level changes may NOT be observed by other threads immediately.
     */
    public void setTraceLevel(int level) {
        traceLevel = level;
    }

    /**
     * Return output stream of this <code>Log</code> object.
     */
    public PrintStream getOutStream() {
        return out;
    }

    /**
     * Returns a string that contains prefix concatenated
     * with Throwable.printStackTrace() output.
     */
    public static String printExceptionToString(Object prefix, Throwable exception) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bos);
        pw.println(prefix);
        exception.printStackTrace(pw);
        pw.close();
        return bos.toString();
    }

    /**
     * Print <code>message</code> to the assigned output stream.
     *
     * @deprecated  Test ought to be quiet if log mode is non-verbose
     *              and there is no errors found by the test. Methods
     *              <code>display()</code> and <code>complain()</code>
     *              are enough for testing purposes.
     */
    @Deprecated
    public synchronized void println(String message) {
        doPrint(message);
        if (!verbose() && isVerboseOnErrorEnabled()) {
            keepLog(composeLine(message));
        }
    }

    /**
     * Print <code>message</code> to the assigned output stream,
     * if log mode is <i>non</i>-verbose.
     *
     * @deprecated  Test ought to be quiet if log mode is non-verbose
     *              and there is no errors found by the test. Methods
     *              <code>display()</code> and <code>complain()</code>
     *              are enough for testing purposes.
     */
    @Deprecated
    public synchronized void comment(String message) {
        if (!verbose()) {
            doPrint(message);
        }
    }

    /**
     * Print trace <code>message</code> to the assigned output stream,
     * only if specified <code>level</code> is less or equal for the
     * trace level specified in command line by <code>-trace.level</code>
     * option.
     */
    public void trace(int level, Object message) {
        if (level <= traceLevel) {
            synchronized ( this ) {
                doPrint("### TRACE " + level + ": " + message);
            }
        }
    }
    /**
     * Print trace <code>message</code> and <code>exception</code> to
     * the assigned output stream,
     * only if specified <code>level</code> is less or equal for the
     * trace level specified in command line by <code>-trace.level</code>
     * option.
     */
    public void trace(int level, Object message, Throwable exception) {
        if (level <= traceLevel) {
            trace(level, printExceptionToString(message, exception));
        }
    }

    /**
     * Print <code>message</code> to the assigned output stream,
     * if log mode is verbose. The <code>message</code> will be lost,
     * if execution mode is non-verbose, and there is no error messages
     * printed.
     */
    public synchronized void display(Object message) {
        if (verbose()) {
            doPrint(message.toString());
        } else if (isVerboseOnErrorEnabled()) {
            keepLog(composeLine(message.toString()));
        } else {
            // ignore
        }
    }

    /**
     * Print error <code>message</code> to the assigned output stream
     * (or to stderr if output is not specified) and keep the message
     * into <code>errorsBuffer</code>.
     */
    public synchronized void complain(Object message) {
        if (!verbose() && isVerboseOnErrorEnabled()) {
            PrintStream stream = findOutStream();
            stream.println("#>  ");
            stream.println("#>  WARNING: switching log to verbose mode,");
            stream.println("#>      because error is complained");
            stream.println("#>  ");
            stream.flush();
            enableVerbose(true);
        }
        String msgStr = message.toString();
        printError(msgStr);
        if (isErrorsSummaryEnabled()) {
            keepError(msgStr);
        }

        logExceptionForFailureAnalysis(msgStr);
    }

    /**
     * Print error <code>message</code> and <code>exception</code>
     * to the assigned output stream
     * (or to stderr if output is not specified) and keep the message
     * into <code>errorsBuffer</code>.
     */
    public void complain(Object message, Throwable exception) {
        if ( exception != null )
            complain(printExceptionToString(message, exception));
        else
            complain(message);
    }

    /**
     * Create an Exception and print the stack trace for an error msg.
     * This makes it possible to detect a failure reason for this error.
     */
    private void logExceptionForFailureAnalysis(String msg) {
        // Some error messages are formatted in multiple lines and with tabs.
        // We clean the messages to help parse the stack traces for failure analysis.
        // We keep at most 2 lines, otherwise the error message may be too long.
        String[] lines = msg.split("[\r\n]+");
        msg = lines.length >= 2 ? lines[0] + " " + lines[1] : lines[0];
        msg = msg.replaceAll("\t", " ");

        // Create a dummy exception just so we can print the stack trace.
        TestFailure e = new TestFailure(msg);
        StackTraceElement[] elements = e.getStackTrace();

        final int callerIndex = 2; // 0=this function, 1=complain(), 2=caller
        if (elements.length <= callerIndex) {
            return;
        }

        // Only log the first complain message from each function.
        // The reason is that some functions splits an error message
        // into multiple lines and call complain() many times.
        // We do not want a RULE for each of those calls.
        // This means that we may miss some rules, but that
        // is better than to create far too many rules.
        String callerClass = elements[callerIndex].getClassName();
        String callerMethod = elements[callerIndex].getMethodName();
        String callerKey = callerClass + "." + callerMethod;
        boolean isAlreadyLogged = loggedExceptions.contains(msg) || loggedExceptions.contains(callerKey);

        if (!isAlreadyLogged) {
            PrintStream stream = findOutStream();
            stream.println("The following stacktrace is for failure analysis.");
            e.printStackTrace(stream);
        }

        loggedExceptions.add(callerKey);
        loggedExceptions.add(msg);
    }

    /////////////////////////////////////////////////////////////////

    /**
     * Redirect log to the given <code>stream</code>, and switch
     * log mode to verbose.
     * Prints errors summary to current stream, cancel current stream
     * and switches to new stream. Turns on verbose mode for new stream.
     *
     * @deprecated  This method is obsolete.
     */
    @Deprecated
    protected synchronized void logTo(PrintStream stream) {
        finalize(); // flush older log stream
        out = stream;
        verbose = true;
    }

    /////////////////////////////////////////////////////////////////

    /**
     * Print all messages from log buffer which were hidden because
     * of non-verbose mode,
     */
    private synchronized void flushLogBuffer() {
        if (!logBuffer.isEmpty()) {
            PrintStream stream = findOutStream();
            for (int i = 0; i < logBuffer.size(); i++) {
                stream.println(logBuffer.elementAt(i));
            }
            stream.flush();
        }
    }

    /**
     * Return <code>out</code> stream if defined or <code>Sytem.err<code> otherwise;
     * print a warning message when <code>System.err</code> is used first time.
     */
    private synchronized PrintStream findOutStream() {
        PrintStream stream = out;
        if (stream == null) {
            stream = System.err;
            if (!noOutWarned) {
                noOutWarned = true;
                stream.println("#>  ");
                stream.println("#>  WARNING: switching log stream to stderr,");
                stream.println("#>      because no output stream is assigned");
                stream.println("#>  ");
            };
        };
        stream.flush();
        return stream;
    }

    /**
     * Compose line to print possible prefixing it with timestamp.
     */
    private String composeLine(String message) {
        if (timestamp) {
            long time = System.currentTimeMillis();
            long ms = time % 1000;
            time /= 1000;
            long secs = time % 60;
            time /= 60;
            long mins = time % 60;
            time /= 60;
            long hours = time % 24;
            return "[" + hours + ":" + mins + ":" + secs + "." + ms + "] " + message;
        }
        return message;
    }

    /**
     * Print the given <code>message</code> either to <code>out</code>
     * stream, or to <code>System.err</code> if <code>out</code>
     * stream is not specified.
     */
    private synchronized void doPrint(String message) {
        PrintStream stream = findOutStream();
        stream.println(composeLine(message));
        stream.flush();
    }

    /**
     * Print the given error <code>message</code> either to <code>out</code>
     * stream, or to <code>System.err</code> if <code>out</code>
     * stream is not specified.
     */
    private synchronized void printError(String message) {
        // Print each line with the ERROR prefix:
        BufferedReader br = new BufferedReader(new StringReader(message));
        for (String line; ; ) {
            try {
                line = br.readLine();
                if (line == null)
                    break;
                doPrint("# ERROR: " + line);
            } catch (IOException e) {
                throw new TestBug("Exception in Log.printError(): " + e);
            };
        }
    }

    /**
     * Keep the given log <code>message</code> into <code>logBuffer</code>.
     */
    private synchronized void keepLog(String message) {
        logBuffer.addElement(message);
    }

    /**
     * Keep the given error <code>message</code> into <code>errorsBuffer</code>.
     */
    private synchronized void keepError(String message) {
        errorsBuffer.addElement(message);
    }

    /**
     * Print errors messages summary from errors buffer if any;
     * print a warning message first.
     */
    private synchronized void printErrorsSummary() {
        if (errorsBuffer.size() <= 0)
            return;

        PrintStream stream = findOutStream();
        stream.println();
        stream.println();
        stream.println("#>  ");
        stream.println("#>  SUMMARY: Following errors occured");
        stream.println("#>      during test execution:");
        stream.println("#>  ");
        stream.flush();

        for (Enumeration e = errorsBuffer.elements(); e.hasMoreElements(); ) {
            printError((String) e.nextElement());
        }
    }

    /**
     * Print errors summary if mode is verbose, flush and cancel output stream.
     */
    protected void finalize() {
        if (verbose() && isErrorsSummaryEnabled()) {
            printErrorsSummary();
        }
        if (out != null)
            out.flush();
        out = null;
    }

    /**
     * Perform finalization at the exit.
     */
    public void finalizeAtExit() {
        finalize();
    }

    /**
     * This class can be used as a base for each class that use <code>Log</code>
     * for print messages and errors.
     * <code>Logger</code> provides with ability to print such messages with
     * specified prefix to make it possible to distinct messages printed from
     * different sources.
     *
     * @see Log
     */
    public static class Logger {

        /**
         * Default prefix for messages.
         */
        public static final String LOG_PREFIX = "";

        /**
         * Log to print messages to.
         */
        protected Log log = null;

        /**
         * Prefix for messages.
         */
        protected String logPrefix = LOG_PREFIX;

        /**
         * Make <code>Logger</code> object with empty <code>Log</code> and
         * default prefix.
         * This method may be used only in derived class, that should specify
         * the used <code>Log</code> object further and assign it to <code>log</code>.
         *
         * @see #log
         * @see #setLogPrefix
         */
        protected Logger() {
        }

        /**
         * Make <code>Logger</code> object for specified <code>Log</code>
         * with default prefix.
         *
         * @see #setLogPrefix
         */
        public Logger(Log log) {
            this.log = log;
        }

        /**
         * Make <code>Logger</code> object for specified <code>Log</code> with
         * given messages prefix.
         */
        public Logger(Log log, String prefix) {
            this.log = log;
            this.logPrefix = prefix;
        }

        /**
         * Return <code>Log</code> object.
         */
        public Log getLog() {
            return log;
        }

        /**
         * Return output stream of this <code>Log</code> object.
         */
        public PrintStream getOutStream() {
            return log.getOutStream();
        }

        /**
         * Set prefix for printed messages.
         */
        public void setLogPrefix(String prefix) {
            logPrefix = prefix;
        }

        /**
         * Make printable message by adding <code>logPrefix<code> to it.
         */
        public String makeLogMessage(String message) {
            return logPrefix + message;
        }

        /**
         * Print trace message by invoking <code>Log.trace()</code>.
         *
         * @see Log#trace
         */
        public void trace(int level, String message) {
            log.trace(level, makeLogMessage(message));
        }

        /**
         * Print message by invoking <code>Log.println()</code>.
         *
         * @see Log#println
         */
        public void println(String message) {
            log.println(makeLogMessage(message));
        }

        /**
         * Print message by invoking <code>Log.display()</code>.
         *
         * @see Log#display
         */
        public void display(String message) {
            log.display(makeLogMessage(message));
        }

        /**
         * Complain about an error by invoking <code>Log.complain()</code> method.
         *
         * @see Log#complain
         */
        public void complain(String message) {
            log.complain(makeLogMessage(message));
        }

    }

}
