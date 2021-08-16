/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdb;

import nsk.share.*;
import nsk.share.jpda.*;

import java.util.*;
import java.io.*;

import java.util.regex.*;

/**
 * Wrapper of <i>jdb</i>.s
 * This class provides abilities to launch it, to send command,
 * to read reply on the command, to set breakpoint on entry in debugggee's method.
 */
public class Jdb extends LocalProcess implements Finalizable {
    /** File to log <i>stdout</i> stream */
    static final String JDB_STDOUT_FILE = "jdb.stdout";
    /** File to log <i>stderr</i> stream */
    static final String JDB_STDERR_FILE = "jdb.stderr";
    /** File to log input jdb's commands */
    static final String JDB_COMMANDS_FILE = "jdb.commands";

    /** File to log emulated <i>jdb</i> session composed from commands and <i>jdb</i> replies on them  */
    static final String JDB_SESSION_FILE = "jdb.session";

    /** Pattern for message of listening at address. */
    public static final String LISTENING_AT_ADDRESS = "Listening at address:";

    /** Pattern for message of a breakpoint hit. */
    public static final String BREAKPOINT_HIT = "Breakpoint hit:";

    /** Pattern for message of an application exit. */
    public static final String APPLICATION_EXIT = "The application exited";

    /** Pattern for message of an application disconnect. */
    public static final String APPLICATION_DISCONNECTED = "The application has been disconnected";

    /** Pattern for message of connector name in the supported connectors list. */
    public static final String SUPPORTED_CONNECTOR_NAME = "Connector:";

    /** Pattern for message of transport name in the supported connectors list. */
    public static final String SUPPORTED_TRANSPORT_NAME = "Transport: ";

    /** This is jdb's prompt when debuggee is not started nor suspended after breakpoint */
    public static final String SIMPLE_PROMPT = "> ";

    public static final String lineSeparator = System.getProperty("line.separator");

    /** Internal streams handlers */
    private static PrintStream     jdbStdinWriter;
    private static JdbStdoutReader jdbStdoutReader;
    private static JdbStderrReader jdbStderrReader;

    private static PrintStream fout;
    private static PrintStream flog;
    private static PrintStream fin;

    /** Particular ident in a compound prompt, or null if any. */
    private String compoundPromptIdent = null;

    /** <i>Launcher</i> that creates this <i>Jdb</i> object. */
    private static Launcher launcher = null;

    /** Internal buffer to save all not-null string from <i>jdb</i> stdout */
    volatile private static StringBuffer stdoutBuffer = new StringBuffer();

    volatile private Object startNotify = new Object();

    /** Returns <i>Launcher</i> that created this <i>Jdb</i> object. */
    public static Launcher getLauncher() {
        return launcher;
    }

    public void finalizeAtExit() throws Throwable {
        finalize();
    }

    public void finalize() throws Throwable {
        if (fout != null) {
//            fout.flush();
            fout.close();
        }
        if (flog != null) {
            flog.close();
        }
        if (fin != null) {
            fin.close();
        }
        if (jdbStdoutReader != null) {
            jdbStdoutReader.close();
        }
        if (jdbStderrReader != null) {
            jdbStderrReader.close();
        }
        super.finalize();
    }

    /** Create <i>Jdb</i> object. */
    public Jdb (Launcher launcher) {
        super();
        this.launcher = launcher;
    }

    /** Set particular ident for compound prompt; or null for any ident. */
    void setCompoundPromptIdent(String ident) {
        compoundPromptIdent = ident;
    }

    /**
     * Launch <i>jdb</i> with options defined in <i>launchCmdArgs</i>.
     * Full path to <i>jdb</i> must be defined as first element in <i>launchCmdArgs</i>.
     */
    public void launch(String[] launchCmdArgs) throws IOException {
        super.launch(launchCmdArgs);
        redirectStreams();
    }

    /**
     * Launch <i>jdb</i> with options defined in <i>launchCmdLine</i>.
     * Full path to <i>jdb</i> must be defined as first token in <i>launchCmdLine</i>.
     */
    public void launch(String launchCmdLine) throws IOException {
        super.launch(launchCmdLine);
        redirectStreams();
    }

    /**
     * Gets <i>stdin, stdout, stderr</i> streams of the <i>jdb's</i> process and
     * redirects them to special streams handlers.
     */
    private void redirectStreams() {
        OutputStream jdbStdin = this.getStdin();
        if (jdbStdin == null) {
            throw new Failure("jdb stdin after launching is null");
        }
        jdbStdinWriter = new PrintStream(jdbStdin, true);

        String fileStdout = getLauncher().getJdbArgumentHandler().getWorkDir() + File.separator + JDB_STDOUT_FILE;
        InputStream jdbStdout = this.getStdout();
        if (jdbStdout == null) {
            throw new Failure("jdb stdout after launching is null");
        }

        launcher.getLog().display("Creating file for jdb stdout stream: " + fileStdout);
        try {
            fout = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileStdout)));
        } catch (Exception e) {
            e.printStackTrace(getLauncher().getLog().getOutStream());
            throw new Failure("Caught unexpected exception while creating file for jdb stdout stream: " + e);
        }

        String fileCommands = getLauncher().getJdbArgumentHandler().getWorkDir() + File.separator + JDB_COMMANDS_FILE;
        try {
            fin = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileCommands)));
        } catch (Exception e) {
            e.printStackTrace(getLauncher().getLog().getOutStream());
            throw new Failure("Caught unexpected exception while creating file for jdb input commands: " + e);
        }

        String fileSession = getLauncher().getJdbArgumentHandler().getWorkDir() + File.separator + JDB_SESSION_FILE;
        launcher.getLog().display("Creating file for jdb session: " + fileSession);
        try {
            flog = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileSession)));
        } catch (Exception e) {
            e.printStackTrace(getLauncher().getLog().getOutStream());
            throw new Failure("Caught unexpected exception while creating file for jdb session: " + e);
        }

        String fileStderr = getLauncher().getJdbArgumentHandler().getWorkDir() + File.separator + JDB_STDERR_FILE;
        InputStream jdbStderr = this.getStderr();
        if (jdbStderr == null) {
            throw new Failure("jdb stderr after launching is null");
        }

        jdbStdoutReader = new JdbStdoutReader(this);
        startReader(jdbStdoutReader);

        jdbStderrReader = new JdbStderrReader(this, fileStderr);
        startReader(jdbStderrReader);
    }

    /** Starts reading threads for jdb streams. */
    private void startReader (Thread reader) {
        long max = getLauncher().getJdbArgumentHandler().getWaitTime() * 60 * 1000;  // maximum time to wait.
        boolean notified = false;
        synchronized (startNotify) {
            reader.start();
            try {
                startNotify.wait(max);
                notified = true;
            } catch (InterruptedException ie) {
                ie.printStackTrace(getLauncher().getLog().getOutStream());
                throw new Failure("Caught InterruptedException while waiting for start of : " + reader, ie);
            }
        }
        if (!notified) {
            throw new Failure("Main thread was not notified during " + max + " milliseconds" +
                "\n\t waiting for start of : " + reader);
        }
    }

    /**
     * Waits for given stream reader of the <i>jdb's</i> process to finish
     * or interrupts after given timeout.
     */
    private void waitForReader(Thread reader, long timeMillisec) {
        if (reader != null) {
            try {
                reader.join(timeMillisec);
            } catch (InterruptedException ie) {
                ie.printStackTrace(getLauncher().getLog().getOutStream());
                throw new Failure("Caught interrupted exception while waiting for reader finished:\n\t" + ie);
            }
            if (reader.isAlive()) {
                getLauncher().getLog().display("Interrupting reader not finished for timeout: " + timeMillisec + " millisec");
                reader.interrupt();
            }
        }
    }

    /**
     * Waits for all readers of redirected streams of the <i>jdb's</i> process
     * to finish.
     */
    private void waitForAllReaders(long timeMillisec) {
        waitForReader(jdbStdoutReader, timeMillisec);
        waitForReader(jdbStderrReader, timeMillisec);
    }

    /**
      * Wait until the jdb process shutdown or crash
      * and all redirected stream readers finished.
      */
    public int waitFor() throws InterruptedException {
        int exitCode = super.waitFor();
        waitForAllReaders(0);
        return exitCode;
    }

    /**
     * Wait until the process shutdown or crash for given timeout in milliseconds,
     * and all redirected stream readers finished.
     * Returns <code>LocalProcess.PROCESS_IS_ALIVE</code> if process is not terminated
     * after timeout.
     */
    public int waitFor(long timeMillisec) throws InterruptedException {
        int exitCode = super.waitFor(timeMillisec);
        if (exitCode != LocalProcess.PROCESS_IS_ALIVE) {
            waitForAllReaders(timeMillisec);
        }
        return exitCode;
    }

    /**
     * Writes <i>jdbCommand</i> to <i>jdb's</i> input stream.
     */
    public synchronized void sendCommand(String jdbCommand) {

        if (terminated()) {
            throw new Failure("Attempt to send command :" + jdbCommand + "\t to terminated jdb.");
        }

        if (jdbCommand != null) {
            String logCmd;
            if (!jdbCommand.endsWith(lineSeparator)) {
                logCmd = jdbCommand;
                jdbCommand += lineSeparator;
            } else {
                // we don't want to log the line separator
                logCmd = jdbCommand.substring(0, jdbCommand.length() - lineSeparator.length());
            }
            launcher.getLog().display("Sending command: " + logCmd);

            jdbStdinWriter.print(jdbCommand);
            jdbStdinWriter.flush();

            synchronized(flog) {
                flog.print(/*LOG_COMMAND_PREFIX +*/ jdbCommand);
                flog.flush();
            }

            fin.print(jdbCommand);
            fin.flush();

            if (jdbStdinWriter.checkError()) {
                throw new Failure("Unexpected IO error while writing command <" + jdbCommand + "> to jdb stdin stream");
            }
        }
    }

    /**
     * Sends command to <i>jdb's</i> input stream, waits for compound promt received,
     * and then returns reply from <i>jdb's</i> output stream.
     *
     * @param command string representing full command with all arguments if any.
     */
    public String[] receiveReplyFor(String command) {
        return receiveReplyFor(command, true);
    }

    /**
     * Sends command to <i>jdb's</i> input stream, waits for promt received,
     * and then returns reply from <i>jdb's</i> output stream.
     *
     * @param command string representing full command with all arguments if any.
     * @param compoundPromptOnly read <i>output</i> until compound prompt is found.
     */
    public String[] receiveReplyFor(String command, boolean compoundPromptOnly) {
        return receiveReplyFor(command, compoundPromptOnly, 1);
    }

    /**
     * Sends command to <i>jdb's</i> input stream, waits for given number of promts received,
     * and then returns reply from <i>jdb's</i> output stream.
     *
     * @param command string representing full command with all arguments if any.
     * @param compoundPromptOnly read <i>output</i> until compound prompt is found.
     * @param count number of prompt instances to found.
     */
    public String[] receiveReplyFor(String command, boolean compoundPromptOnly, int count) {
        if (command == null) {
           return null;
        }

        int startPos = stdoutBuffer.length();
        sendCommand(command);
        return receiveReply(startPos, compoundPromptOnly, count);
    }

    /**
     * Sends command to <i>jdb's</i> input stream, waits for specified message to be received,
     * and then returns reply from <i>jdb's</i> output stream.
     *
     * @param command string representing full command with all arguments if any.
     * @param waitMsg string representing the message that must be sent back before returing.
     */
    public String[] receiveReplyForWithMessageWait(String command, String waitMsg) {
        if (command == null) {
           return null;
        }

        int startPos = stdoutBuffer.length();
        sendCommand(command);
        waitForMessage(startPos, waitMsg);
        return receiveReply(startPos, true, 1);
    }

    /**
     * Waits for compound prompt and returns reply from <i>jdb</i> stdout
     * beginning from <i>startPos</i> in the <i>stdoutBuffer</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     */
    public String[] receiveReply(int startPos) {
        return receiveReply(startPos, true);
    }

    /**
     * Waits for particular prompt and returns reply from <i>jdb</i> stdout
     * beginning from <i>startPos</i> in the <i>stdoutBuffer</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @param compoundPromptOnly waits for compound prompt only.
     */
    public String[] receiveReply(int startPos, boolean compoundPromptOnly) {
        return receiveReply(startPos, compoundPromptOnly, 1);
    }

    /**
     * Waits for <i>count</i> number of prompts and returns reply from <i>jdb</i> stdout
     * beginning from <i>startPos</i> in the <i>stdoutBuffer</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @param compoundPromptOnly waits for compound prompt only.
     * @param count number of prompt instances to wait for.
     */
    public String[] receiveReply(int startPos, boolean compoundPromptOnly, int count) {
        nsk.share.Failure e = null;
        try {
            waitForPrompt(startPos, compoundPromptOnly, count);
        } catch (nsk.share.Failure nsf) {
            e = nsf;
            launcher.getLog().display("receiveReply FAILED due to \"" + e + "\".");
            launcher.getLog().display("Pending reply output follows:");
        }

        String reply = stdoutBuffer.substring(startPos, stdoutBuffer.length());
        String[] replyArr = toStringArray(reply);

        // Send reply to the logfile. This complements sendCommand(), which does the same.
        for (int i = 0; i < replyArr.length; i++) {
            launcher.getLog().display("reply[" + i + "]: " + replyArr[i]);
        }

        if (e != null) throw e;
        return replyArr;
    }

    /**
     * Reads <i>JDB_STDOUT_FILE</i> file until prompt is found in the <i>stdoutBuffer</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @param compoundPromptOnly search for compound prompt only.
     * @throws Failure if prompt is not encountered during <i>WaitTime</i>.
     * @return number of prompt instances really found.
     */
    public int waitForPrompt(int startPos, boolean compoundPromptOnly) {
        return waitForPrompt(startPos, compoundPromptOnly, 1);
    }

    /**
     * Reads <i>JDB_STDOUT_FILE</i> file until prompt is found in the <i>stdoutBuffer</i>
     * <i>count</i> times.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @param compoundPromptOnly search for compound prompt only.
     * @throws Failure if prompt is not encountered <i>count</i> times during <i>WaitTime</i>.
     * @return number of prompt instances actually found
     *
     * @see #setCompoundPromptIdent(String)
     */
    public int waitForPrompt(int startPos, boolean compoundPromptOnly, int count) {

        long delta = 200; // time in milliseconds to wait at every iteration.
        long total = 0;    // total time has waited.
        long max = getLauncher().getJdbArgumentHandler().getWaitTime() * 60 * 1000;  // maximum time to wait.

        if (count <= 0) {
            throw new TestBug("Wrong number of prompts count in Jdb.waitForPrompt(): " + count);
        }

        Object dummy = new Object();
        while ((total += delta) <= max) {
            int found = 0;

            // check if compound prompt is found
            {
                found = findPrompt(stdoutBuffer, true, startPos);
                if (found >= count) {
                    return found;
                }
            }

            // check also if simple prompt is found
            if (!compoundPromptOnly) {
                found += findPrompt(stdoutBuffer, false, startPos);
                if (found >= count) {
                    return found;
                }
            }

            // exit loop when a debugged application exited
            if (stdoutBuffer.indexOf(APPLICATION_EXIT) >= 0 || stdoutBuffer.indexOf(APPLICATION_DISCONNECTED) >= 0) {
                return found;
            } else if (startPos > 0 && !jdbStdoutReader.isAlive()) {
                return found;
            }

            // sleep for awhile
            synchronized(dummy) {
                try {
                    dummy.wait(delta);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(getLauncher().getLog().getOutStream());
                    throw new Failure("Caught interrupted exception while waiting for jdb prompt:\n\t" + ie);
                }
            }
        }

        Pattern debuggeeExceptionPattern = Pattern.compile("Exception occurred: (?<DebuggeeException>\\S+) \\(uncaught\\)");
        String buf = stdoutBuffer.toString();
        Matcher m = debuggeeExceptionPattern.matcher(buf);

        if (m.find(startPos)) {
            throw new DebuggeeUncaughtException(m.group("DebuggeeException"));
        }

        String times = (count > 1 ? count + " times " : "");
        throw new Failure("Prompt is not received " + times + "during " + total + " milliseconds.");
    }

    /**
     * Reads <i>JDB_STDOUT_FILE</i> file until expected message is found in the <i>stdoutBuffer</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @throws Failure if expected message is not encountered during <i>WaitTime</i>.
     * @return number of messages actually found
     */
    public int waitForMessage(int startPos, String message) {

        long delta = 200; // time in milliseconds to wait at every iteration.
        long total = 0;    // total time has waited.
        long max = getLauncher().getJdbArgumentHandler().getWaitTime() * 60 * 1000;  // maximum time to wait.

        Object dummy = new Object();
        while ((total += delta) <= max) {
            int found = 0;

            // search for message
            {
                found = findMessage(startPos, message);
                if (found > 0) {
                    return found;
                }
            }

            // exit loop when a debugged application exited.
            if (stdoutBuffer.indexOf(APPLICATION_EXIT) >= 0 || stdoutBuffer.indexOf(APPLICATION_DISCONNECTED) >= 0) {
                return found;
            } else if (startPos > 0 && !jdbStdoutReader.isAlive()) {
                return found;
            }

            // spleep for awhile
            synchronized(dummy) {
                try {
                    dummy.wait(delta);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(getLauncher().getLog().getOutStream());
                    throw new Failure("Caught interrupted exception while waiting for jdb reply:\n\t" + ie);
                }
            }

        }

        // If we never recieved the expected reply, display a warning, and also
        // display what we did recieve. This is accomplished by calling receiveReply().
        Log log = getLauncher().getLog();
        log.display("WARNING: message not recieved: " + message);
        log.display("Remaining debugger output follows:");
        receiveReply(startPos);
        throw new Failure("Expected message not received during " + total + " milliseconds:"
                            + "\n\t" + message);
    }

    /**
     * Find message in <i>JDB_STDOUT_FILE</i> file starting from <i>startPos</i>.
     *
     * @param startPos start position for search in <i>stdoutBuffer</i>.
     * @return number of messages actually found
     */
    public int findMessage(int startPos, String message) {
        int bufLength = stdoutBuffer.length();
        int msgLength = message.length();
        int found = 0;

        for (int pos = startPos; pos < bufLength; ) {
            pos = stdoutBuffer.indexOf(message, pos);
            if (pos < 0) break;
            found++;
            pos += msgLength;
        }
        return found;
    }

    /**
     * Searches input lines for <i>jdb</i> prompt of particular kind.
     * starting from <code>startPos</code>.
     * The possible prompt kinds are simple prompt "> " and compound prompt,
     * that looks like '.*\[[0-9]*\] ' regexp on a single line.
     * For example, 'main[1] ' (see setCompoundPromptIdent(String)).
     * <p>
     * In order to make compatible with jdk prior to 1.4.0 avoid using
     * java.util.regex classes.
     *
     * @return number of prompt instances found
     *
     * @see #setCompoundPromptIdent(String)
     */
    int findPrompt(StringBuffer lines, boolean compoundPromptOnly, int startPos) {

        final String nameDelimiters = "-_";

        int noPrompt = -1; // prompt is not found;
        int simplePrompt = 1;
        int complexPrompt = 2;

        int length = lines.length();
        int found = 0;

        // search for simple prompt
        if (!compoundPromptOnly) {
            int promptLength = SIMPLE_PROMPT.length();
            for (int pos = startPos; pos < length; ) {
                pos = lines.indexOf(SIMPLE_PROMPT, pos);
                if (pos < 0) break;
                found++;
                pos += promptLength;
            }
            return found;
        }

        // search for compound prompt
        StringBuffer prompt = new StringBuffer(100);
        searching:
        for (int pos = startPos; pos < length; ) {

            // skip each simbol not suitable for prompt begin
            if (!Character.isLetterOrDigit(lines.charAt(pos))) {
                pos++;
                continue searching;
            }

            // check for compound prompt
            prompt.setLength(0);

            // read name (letters or digits or delimiters)
            while (nameDelimiters.indexOf(lines.charAt(pos)) > 0
                        || Character.isLetterOrDigit(lines.charAt(pos))
                        || lines.charAt(pos) == '-'
                        || lines.charAt(pos) == '_') {
                prompt.append(lines.charAt(pos++));
                if (pos >= length) {
                    break searching;
                }
            }

            // read opening '['
            if (lines.charAt(pos) != '[') {
                continue searching;
            }
            prompt.append(lines.charAt(pos++));
            if (pos >= length) {
                break searching;
            }

            // read number (digits)
            if (!Character.isDigit(lines.charAt(pos))){
                continue searching;
            }
            while (Character.isDigit(lines.charAt(pos))) {
                prompt.append(lines.charAt(pos++));
                if (pos >= length) {
                    break searching;
                }
            }

            // read closing ']'
            if (lines.charAt(pos) != ']') {
                continue searching;
            }
            prompt.append(lines.charAt(pos++));
            if (pos >= length) {
                break searching;
            }

            // read last ' '
            if (lines.charAt(pos) != ' ') {
                continue searching;
            }
            prompt.append(lines.charAt(pos++));

            // check if not particular ident found
            if (compoundPromptIdent != null
                    && !prompt.toString().startsWith(compoundPromptIdent + "[")) {
                continue searching;
            }

            // compound prompt found
            found++;
        }

        return found;
    }

    /**
     * Splits string which may include line separators to string array.
     *
     */
    public static String[] toStringArray (String string) {
        Vector<String> v = new Vector<String>();
        int ind;
        for (ind = 0; ind < string.length(); ) {
            int i = string.indexOf(lineSeparator, ind);
            if (i >= 0) {
                v.add(string.substring(ind, i));
                ind = i + lineSeparator.length();
            } else {
                v.add(string.substring(ind));
                break;
            }
        }
        String[] result = new String [v.size()];
        v.toArray(result);
        return result;
    }

    /**
     * Set breakpoint for debuggee on method invocation.
     */
    public void setBreakpointInMethod(String methodName) {
        String nextCommand = JdbCommand.stop_in + methodName;
        String[] reply = receiveReplyFor(nextCommand);

        Paragrep grep = new Paragrep(reply);
        if (grep.find("Unable to set") > 0) {
            throw new Failure("jdb failed to set breakpoint in method: " + methodName);
        }
        if (grep.find("Set breakpoint") <= 0 && grep.find("Deferring breakpoint") <= 0) {
            throw new Failure("jdb did not set breakpoint in method: " + methodName);
        }
    }

    /**
     * Set deferred breakpoint for debuggee on method invocation.
     * This method must be used before <run> command.
     */
    public void setDeferredBreakpointInMethod(String methodName) {
        String nextCommand = JdbCommand.stop_in + methodName;
        String[] reply = receiveReplyFor(nextCommand, false);

        Paragrep grep = new Paragrep(reply);
        if (grep.find("Unable to set") > 0) {
            throw new Failure("jdb failed to set deffered breakpoint in method: " + methodName);
        }
        if (grep.find("Set breakpoint") <= 0 && grep.find("Deferring breakpoint") <= 0) {
            throw new Failure("jdb did not set deffered breakpoint in method: " + methodName);
        }
    }

    /**
     * Returns true if reply contains breakpoint message.
     */
    public boolean isAtBreakpoint(String[] reply) {
        return isAtBreakpoint(reply, "", "");
    }

    /**
     * Returns true if reply contains breakpoint message in certain method.
     */
    public boolean isAtBreakpoint(String[] reply, String method) {
        return isAtBreakpoint(reply, method, "");
    }

    /**
     * Returns true if reply contains breakpoint message in certain method
     * and in certain thread id.
     */
    public boolean isAtBreakpoint(String[] reply, String method, String thread) {
        boolean result = false;
        Vector<String> v = new Vector<String>();
        Paragrep grep = new Paragrep(reply);

        v.add(BREAKPOINT_HIT);
        if (method.length() > 0) {
            v.add(method);
        }
        if (thread.length() > 0) {
            v.add(thread);
        }
        if (grep.find(v) > 0) {
            result = true;
        }
        return result;
    }

    /**
     * Load and start execution of given debuggee's class with arguments.
     */
    public void startDebuggeeClass(String classWithArgs) {
        String[] reply = receiveReplyFor(JdbCommand.run + " " + classWithArgs);

        // give one more chance to reach breakpoint
        if (!isAtBreakpoint(getTotalReply(), "main")) {
            waitForMessage(0, BREAKPOINT_HIT);
        }
    }

    /**
     * Start execution of pre-loaded debuggee's class.
     */
    public void startDebuggeeClass() {
        String[] reply = receiveReplyFor(JdbCommand.run);

        // give one more chance to reach breakpoint
        if (!isAtBreakpoint(getTotalReply(), "main")) {
            waitForMessage(0, BREAKPOINT_HIT);
        }
    }

    /**
     * Returns as string array all id's for a given <i>threadName</i>.
     */
    public String[] getThreadIds(String threadName) {

        if (!threadName.startsWith("(")) {
            threadName = "(" + threadName;
        }
        if (!threadName.endsWith(")")) {
            threadName = threadName + ")";
        }

        Vector<String> v = new Vector<String>();
        String[] reply = receiveReplyFor(JdbCommand.threads);
        Paragrep grep = new Paragrep(reply);

        String[] found = grep.findStrings(threadName);
        for (int i = 0; i < found.length; i++) {
            String string = found[i];
            int j = string.indexOf(threadName);
            if (j >= 0) {
               j += threadName.length();
               String threadId = string.substring(j, string.indexOf(" ", j));
               v.add(threadId);
            }
        }

        String[] result = new String [v.size()];
        v.toArray(result);
        return result;
    }

    /**
     * Quit <i>jdb</i> using "quit" command.
     */
    public void quit() {
        if (!terminated()) {
            sendCommand(JdbCommand.quit);
        }
    }

    /**
     * Sends "cont" command up to maxTimes until debuggee exit.
     */
    public void contToExit (int maxTimes) {
        boolean exited = false;
        for (int i = 0; i < maxTimes; i++) {
            if (!terminated()) {
                String [] reply = receiveReplyFor(JdbCommand.cont);
                Paragrep grep = new Paragrep(reply);
                if (grep.find(APPLICATION_EXIT) > 0) {
                    exited = true;
                    break;
                }
            } else {
                exited = true;
                break;
            }
        }
        if (!exited) {
            if (terminated()) {
                exited = true;
            } else {
                quit();
                throw new Failure("Debuggee did not exit after " + maxTimes + " <cont> commands");
            }
        }
    }

    /**
     * Returns string array containing all strings from <i>jdb</i> stdout.
     */
    public String[] getTotalReply() {
        return toStringArray(stdoutBuffer.toString());
    }

    /**
     * Prints given message to log files and adds to <i>stdoutBuffer</i>.
     */
    public void logToFile(String s) {
        synchronized(fout) {
            fout.print(s);
            fout.flush();
        }
        synchronized(stdoutBuffer) {
            stdoutBuffer.append(s);
        }
        synchronized(flog) {
            flog.print(s);
            flog.flush();
        }
    }


    /**
     *  Starts jdb with attaching connector. Makes several tries during <i>waitTime</i>
     *  until success. Unsuccessful launches are caused that the debuggee is not yet
     *  ready to accept debugger.
     */
    public static Jdb startAttachingJdb (Launcher launcher, String[] jdbCmdArgs, String message)
            throws IOException  {
        Jdb jdb = null;

        long delta = Launcher.DEBUGGEE_START_DELAY; // time in milliseconds to wait at every iteration.
        long max = getLauncher().getJdbArgumentHandler().getWaitTime() * 60 * 1000;  // maximum time to wait.

        int result = -1;
        boolean found = false;

        long start = System.currentTimeMillis();

        while (!found && (System.currentTimeMillis() - start)<= max) {

            jdb = new Jdb(launcher);
            jdb.launch(jdbCmdArgs);

            while (!found && (System.currentTimeMillis() - start)<= max) {

                try {
                    Thread.currentThread().sleep(delta);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(getLauncher().getLog().getOutStream());
                    throw new Failure("Caught unexpected InterruptedException while sleep in waiting for debuggee's start:\n\t"
                       + ie);
                }

                if (jdb.terminated() ||
                    !jdbStdoutReader.isAlive() ||
                    stdoutBuffer.indexOf(APPLICATION_EXIT) >= 0 ||
                    stdoutBuffer.indexOf(APPLICATION_DISCONNECTED) >= 0) {

                    System.out.println("Unsuccessful launch of attaching jdb. Next try...");
                    try {
                        jdb.finalize();
                    } catch (Throwable t) {
                        t.printStackTrace(getLauncher().getLog().getOutStream());
                        throw new Failure("Caught unexpected error while finalizing jdb: " + t);
                    }
                    break;

                } else if (stdoutBuffer.length() > 0) {
                    result = stdoutBuffer.indexOf(message);
                    if (result >= 0) {
                        found = true; // exit loop
                    }
                }
            }

        }

        if (result < 0) {
            throw new Failure("Launched jdb could not attach to debuggee during " + max + " milliseconds.");
        }

        return jdb;
    }

    /**
     *  Waits for jdb to print message about listening at address for connection,
     *  and returns this address string.
     */
    public String waitForListeningJdb() {

        waitForMessage(0, LISTENING_AT_ADDRESS);
        int msgStart = stdoutBuffer.indexOf(LISTENING_AT_ADDRESS);
        int msgEnd = stdoutBuffer.indexOf("\n", msgStart);
        int promptLen = LISTENING_AT_ADDRESS.length();

        /*
         * The LISTENING_AT_ADDRESS string and the terminating "\n"
         * may or may not be included in the same read so we allow
         * this message to be terminated by "\n" or NULL.
         */
        if (msgEnd < 0) {
            msgEnd = stdoutBuffer.length();
        }

        if (msgEnd <= 0 || msgEnd - msgStart <= promptLen) {
            throw new Failure("Unknown format of message: " + LISTENING_AT_ADDRESS);
        }

        int addrStart = msgStart + promptLen;
        String address = stdoutBuffer.substring(addrStart, msgEnd).trim();

        if (address.length() <= 0) {
            throw new Failure("Empty address in message: " + LISTENING_AT_ADDRESS);
        }

        return address;
    }

    // ---------------------------------------------- //

    class JdbStdoutReader extends Thread {
        private Jdb jdb = null;
        private InputStream in = null;

        volatile boolean stop = false;

        public JdbStdoutReader (Jdb jdb) {
            super("jdb stdout reader");
            this.jdb = jdb;
            this.in = jdb.getStdout();
            if (in == null) {
                throw new Failure("Can not get jdb stdout stream");
            }
            this.setDaemon(true);
        }

        public String toString() {
            return getClass().getName() + '@' + Integer.toHexString(hashCode());
        }

        public void run() {
            synchronized(jdb.startNotify) {
                jdb.startNotify.notifyAll();
            }

            long delta = 10; // time in milliseconds to wait at every iteration.
            boolean jdbWasTerminated = false;
            while (!stop) {
                if(jdb.terminated())
                        jdbWasTerminated = true;
                try {
                    int size = in.available();
                    if (size > 0) {
                        byte[] buffer = new byte [size];
                        int result = in.read(buffer, 0, size);
                        if (result < 0) {
                            throw new Failure("No bytes read from jdb's output stream ");
                        } else if (result < size) {
                            throw new Failure("Number bytes read from jdb's output stream are less than available " +
                                 "\n\t available : " + size + ", read : " + result);
                        }
                        logToFile(new String(buffer, 0, result));
                    }
                } catch (Exception e) {
                    e.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                    throw new Failure("Caught unexpected exception while reading jdb's stdout stream: " + e);
                }
                if(jdbWasTerminated)
                        break;
                try {
                    sleep(delta);
                } catch (InterruptedException ie) {
                    ie.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                    throw new Failure("Caught interrupted exception while waiting for jdb reply:\n\t" + ie);
                }
            }
        }

        public void close() {
            stop = true;
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                throw new Failure("Caught unexpected IOException while closing jdb stdout stream: " + ioe);
            }
        }
    }

    /** Handler for <i>jdb</i> stderr stream. */
    class JdbStderrReader extends Thread {

        private Jdb jdb = null;
        private volatile boolean cancelled = false;
        private boolean empty = true;

        private BufferedReader bin;
        private PrintStream fout;
        private String fileName;

        JdbStderrReader (Jdb jdb, String jdbStderrFile) {
            super("jdb stderr reader");
            this.jdb = jdb;
            InputStream in = jdb.getStderr();
            if (in == null) {
                throw new Failure("Can not get jdb stderr stream");
            }
            this.bin = new BufferedReader(new InputStreamReader(in));
            this.setDaemon(true);

            this.fileName = jdbStderrFile;

            launcher.getLog().display("Creating file for jdb stderr stream: " + fileName);
            try {
                this.fout = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            } catch (Exception e) {
                e.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                throw new Failure("Caught unexpected exception while creating file for jdb stderr stream: " + e);
            }
        }

        public void run () {
            synchronized(jdb.startNotify) {
                jdb.startNotify.notifyAll();
            }

            long delta = 10; // time in milliseconds to wait at every iteration.

            while (!cancelled) {
                String line = null;
                try {
                    line = bin.readLine();
                    if (line == null)
                        break; //EOF
                } catch (IOException ioe) {
                    ioe.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                    throw new Failure("Caught unexpected IOException while reading from jdb stderr: " + ioe);
                }

                if (line != null) {
                   empty = false;
                   logToFile(line);
                }

                try {
                    sleep(delta);
                } catch (InterruptedException ie) {
                    throw new Failure("Caught interrupted exception while waiting for jdb reply:\n\t" + ie);
                }
            }
            close();
        }

        /**
         * Signal to <i>run()</i> method that it should terminate,
         * and wait until it is finished.
         */
        public void cancel () {
            cancelled = true;
            while (this.isAlive()) {
                try {
                    this.join();
                } catch (InterruptedException ie) {
                    close();
                    throw new Failure("Caught InterruptedException while waiting for JdbStderrReader termination " + ie);
                }
            }
            close();
        }

        public void close() {
            if (fout != null) {
                synchronized (fout) {
                    fout.close();
                }
            }

            try {
                if (bin != null) {
                    bin.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace(jdb.getLauncher().getLog().getOutStream());
                throw new Failure("Caught unexpected IOException while closing jdb stderr stream: " + ioe);
            }
            if (!empty) {
                 // Should not throw exception here because of non-empty stderr in case of unsuccessful launch of attaching jdb.
                jdb.getLauncher().getLog().display("JdbStderrReader: jdb's stderr is not empty. Check jdb.stderr file");
            }
        }

        public String getFileName () {
            return this.fileName;
        }

        public void logToFile(String line) {
            synchronized (fout) {
                fout.println(line);
                fout.flush();
            }
        }
    } // end of JdbStderrReader
}  // end of Jdb
