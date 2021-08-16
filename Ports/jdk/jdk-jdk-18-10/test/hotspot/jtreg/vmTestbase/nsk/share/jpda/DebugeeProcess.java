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

package nsk.share.jpda;

import nsk.share.*;
import java.io.*;
import java.net.*;

/**
 * This class is used to control debugee VM process.
 * <p>
 * Object of this class is constructed by <code>DebugeeBinder</code>
 * and is used as a mirror of debugee VM process.
 * It provides abilities to launch such process,
 * redirect standard output streams, wait for process terminates
 * or kill the process, and so on.
 * <p>
 * This is an abstract class that declares abstract methods to control
 * debugee VM process.
 * Derived classes should implement these methods corresponding to the mode
 * that the process should be started in (locally, remotely or manually).
 * <p>
 * Particular derived classes <code>nsk.share.jdi.Debugee</code> and
 * <code>nsk.share.jdwp.Debugee</code> provides additional abilities
 * to control debugee VM using JDI or JDWP specific features.
 *
 * @see DebugeeBinder
 *
 * @see nsk.share.jdi.Debugee
 * @see nsk.share.jdwp.Debugee
 */
abstract public class DebugeeProcess extends FinalizableObject {

    /** Default prefix for log messages. */
    public static final String LOG_PREFIX = "binder> ";
    public static final String DEBUGEE_STDOUT_LOG_PREFIX = "debugee.stdout> ";
    public static final String DEBUGEE_STDERR_LOG_PREFIX = "debugee.stderr> ";

    /** Messages prefix. */
    protected String prefix = LOG_PREFIX;

    /** Binder that creates this debugee process. */
    protected DebugeeBinder binder = null;

    /** Messages log from binder. */
    protected Log log = null;

    /** Communicational channel between debuger and debugee. */
    protected IOPipe pipe = null;

    /** Argument handler from binder. */
    protected DebugeeArgumentHandler argumentHandler = null;

    /** Need or not to check debuggee process termination at exit. */
    protected boolean checkTermination = false;

    /** Debugee VM process or <i>null</i> if not available. */
    protected Process process = null;

    /** Make new <code>Debugee</code> object for the given binder. */
    protected DebugeeProcess (DebugeeBinder binder) {
        this.binder = binder;
        this.log = binder.getLog();
    }

    /**
     * Return already bound ServerSocket for IOPipe connection or null.
     */
    protected ServerSocket getPipeServerSocket() {
        return binder.getPipeServerSocket();
    }

    /** Return <code>DebugeeArgumentHandler</code> of the debugee object. */
    public DebugeeArgumentHandler getArgumentHandler() {
        return binder.getArgumentHandler();
    }

    /** Return <code>Log</code> of the debugee object. */
    public Log getLog() {
        return log;
    }

    /** Return <code>Process</code> object associated with debugee VM or <i>null</i>. */
    public Process getProcess() {
        return process;
    }

    // --------------------------------------------------- //

    /** Created and return new IOPipe channel to the debugee VM. */
    public IOPipe createIOPipe() {
        if (pipe != null) {
            throw new TestBug("IOPipe channel is already created");
        }
        pipe = new IOPipe(this);
        return pipe;
    }

    /** Return IOPipe channel or null if not yet ctreated. */
    public IOPipe getIOPipe() {
        return pipe;
    }

    /** Receive and return signal from the debugee VM via IOPipe channel.
     *
     *  @throws TestBug if IOPipe channel is not created
     */
    public String receiveSignal() {
        if ( pipe == null )
            throw new TestBug("IOPipe channel is not initialized");
        return pipe.readln();
    }

    /** Receive an expected <code>signal</code> from the debugee VM via IOPipe channel.
     *
     *  @throws Failure if received signal is not equal to the expected one
     *  @throws TestBug if IOPipe channel is not created
     */
    public void receiveExpectedSignal(String signal) {
        String line = receiveSignal();
        if (line == null || !line.equals(signal))
            throw new Failure("Received unexpected signal from debugee: " + line);
        display("Received expected signal from debugee: " + signal);
    }

    /** Send <code>signal</code> to the debugee VM via IOPipe channel.
     *
     *  @throws TestBug if IOPipe channel is not defined created.
     */
    public void sendSignal(String signal) {
        if (pipe == null)
            throw new TestBug("IOPipe channel is not initialized");
        pipe.println(signal);
    }


    // --------------------------------------------------- //

    /** Wait until the debugee VM shutdown or crash. */
    abstract protected int waitForDebugee () throws InterruptedException;

    /** Kill the debugee VM. */
    abstract protected void killDebugee ();

    /** Check whether the debugee VM has been terminated. */
    abstract public boolean terminated ();

    /** Return the debugee VM exit status. */
    abstract public int getStatus ();

    /** Get a pipe to write to the debugee's stdin stream. */
    abstract protected OutputStream getInPipe ();

    /** Get a pipe to read the debugee's stdout stream. */
    abstract protected InputStream getOutPipe ();

    /** Get a pipe to read the debugee's stderr stream. */
    abstract protected InputStream getErrPipe ();

    // --------------------------------------------------- //

    /**
     * Wait until the debugee VM shutdown or crash,
     * and let finish its stdout, stderr, and stdin
     * redirectors (if any).
     *
     * @return  Debugee process exit code.
     * @see #waitForRedirectors(long)
     */
    public int waitFor () {
        long timeout = binder.getArgumentHandler().getWaitTime() * 60 * 1000;
        int exitCode = 0;
        try {
            exitCode = waitForDebugee();
        } catch (InterruptedException ie) {
            ie.printStackTrace(log.getOutStream());
            throw new Failure("Caught exception while waiting for debuggee process: \n\t" + ie);
        }
        waitForRedirectors(timeout);
        if (process != null) {
            process.destroy();
        }
        return exitCode;
    }

    /**
     * Wait until the debugee VM redirectors to complete for specified <code>timeout</code>.
     *
     * @see #waitFor()
     */
    public void waitForRedirectors (long timeout) {
        try {
            if (stdinRedirector != null) {
                if (stdinRedirector.isAlive()) {
                    stdinRedirector.join(timeout);
                    if (stdinRedirector.isAlive()) {
                        log.complain("Timeout for waiting STDIN redirector exceeded");
                        stdinRedirector.interrupt();
                    }
                }
                stdinRedirector = null;
            };
            if (stdoutRedirector != null) {
                if (stdoutRedirector.isAlive()) {
                    stdoutRedirector.join(timeout);
                    if (stdoutRedirector.isAlive()) {
                        log.complain("Timeout for waiting STDOUT redirector exceeded");
                        stdoutRedirector.interrupt();
                    }
                }
                stdoutRedirector = null;
            };
            if (stderrRedirector != null) {
                if (stderrRedirector.isAlive()) {
                    stderrRedirector.join(timeout);
                    if (stderrRedirector.isAlive()) {
                        log.complain("Timeout for waiting STDERR redirector exceeded");
                        stderrRedirector.interrupt();
                    }
                }
                stderrRedirector = null;
            };
        } catch (InterruptedException ie) {
            ie.printStackTrace(log.getOutStream());
            throw new Failure("Caught exception while waiting for debuggee output redirectors: \n\t"
                                + ie);
        }
    }

    // --------------------------------------------------- //

    /**
     * Get a pipe to write to the debugee's stdin stream,
     * or throw TestBug exception is redirected.
     */
    final public OutputStream getStdin () {
        if (stdinRedirector != null)
            throw new TestBug("Debugee's stdin is redirected");
        return getInPipe();
    }

    /**
     * Get a pipe to read the debugee's stdout stream,
     * or throw TestBug exception is redirected.
     */
    final public InputStream getStdout () {
        if (stdoutRedirector != null)
            throw new TestBug("Debugee's stdout is redirected");
        return getOutPipe();
    }

    /**
     * Get a pipe to read the debugee's stderr stream,
     * or throw TestBug exception is redirected.
     */
    final public InputStream getStderr () {
        if (stderrRedirector != null)
            throw new TestBug("Debugee's stderr is redirected");
        return getErrPipe();
    }

    // --------------------------------------------------- //

    private IORedirector stdoutRedirector = null;
    private IORedirector stderrRedirector = null;
    private IORedirector stdinRedirector = null;

//    /**
//     * Start a thread redirecting the given <code>in</code> stream
//     * to the debugee's stdin. If the debugee's stdin was already
//     * redirected, the TestBug exception is thrown.
//     */
//    final public void redirectStdin(InputStream in) {
//        if (stdinRedirector != null)
//            throw new TestBug("the debugee's stdin is already redirected");
//        stdinRedirector = new IORedirector(in,getInPipe());
//        stdinRedirector.setName("IORedirector for stdin");
//        stdinRedirector.setDaemon(true);
//        stdinRedirector.start();
//    }

    /**
     * Start thread redirecting the debugee's stdout to the
     * given <code>out</code> stream. If the debugee's stdout
     * was already redirected, the TestBug exception is thrown.
     *
     * @deprecated Use redirectStdout(Log, String) instead.
     */
    @Deprecated
    public void redirectStdout(OutputStream out) {
        if (stdoutRedirector != null) {
            return;
        }
//            throw new TestBug("Debugee's stdout is already redirected");
        stdoutRedirector = new IORedirector(getOutPipe(),out);
        stdoutRedirector.setPrefix(DEBUGEE_STDOUT_LOG_PREFIX);
        stdoutRedirector.setName("IORedirector for stdout");
        stdoutRedirector.setDaemon(true);
        stdoutRedirector.start();
    }

    /**
     * Start thread redirecting the debugee's stdout to the
     * given <code>Log</code>. If the debugee's stdout
     * was already redirected, the TestBug exception is thrown.
     */
    public void redirectStdout(Log log, String prefix) {
        if (stdoutRedirector != null) {
//            stdoutRedirector.setPrefix(prefix);
            return;
//            throw new TestBug("the debugee's stdout is already redirected");
        }
        stdoutRedirector = new IORedirector(new BufferedReader(new InputStreamReader(getOutPipe())), log, prefix);
        stdoutRedirector.setName("IORedirector for stdout");
        stdoutRedirector.setDaemon(true);
        stdoutRedirector.start();
    }

    /**
     * Start thread redirecting the debugee's stderr to the
     * given <code>err</code> stream. If the debugee's stderr
     * was already redirected, the TestBug exception is thrown.
     *
     * @deprecated Use redirectStderr(Log, String) instead.
     */
    @Deprecated
    public void redirectStderr(OutputStream err) {
        if (stderrRedirector != null) {
            return;
        }
//            throw new TestBug("Debugee's stderr is already redirected");
        stderrRedirector = new IORedirector(getErrPipe(),err);
        stderrRedirector.setPrefix(DEBUGEE_STDERR_LOG_PREFIX);
        stdoutRedirector.setName("IORedirector for stderr");
        stderrRedirector.setDaemon(true);
        stderrRedirector.start();
    }

    /**
     * Start thread redirecting the debugee's stderr to the
     * given <code>Log</code>. If the debugee's stderr
     * was already redirected, the TestBug exception is thrown.
     */
    public void redirectStderr(Log log, String prefix) {
        if (stderrRedirector != null) {
//            stderrRedirector.setPrefix(prefix);
            return;
//            throw new TestBug("Debugee's stderr is already redirected");
        }
        stderrRedirector = new IORedirector(new BufferedReader(new InputStreamReader(getErrPipe())), log, prefix);
        stdoutRedirector.setName("IORedirector for stderr");
        stderrRedirector.setDaemon(true);
        stderrRedirector.start();
    }

    /**
     * Start thread redirecting the debugee's stdout/stderr to the
     * given <code>Log</code> using standard prefixes.
     * If the debugee's stdout/stderr were already redirected,
     * the TestBug exception is thrown.
     */
    public void redirectOutput(Log log) {
        redirectStdout(log, "debugee.stdout> ");
        redirectStderr(log, "debugee.stderr> ");
    }
    // --------------------------------------------------- //

    /**
     * Kill the debugee VM if it is not terminated yet.
     *
     * @throws Throwable if any throwable exception is thrown during finalization
     */
    public void close() {
        if (checkTermination) {
            if (!terminated()) {
                complain("Debugee VM has not exited correctly: trying to kill it");
                killDebugee();
            }
            checkTermination = false;
        }
    }

    // --------------------------------------------------- //

    /**
     * Display log message with prefix.
     */
    protected void display(String message) {
        log.display(prefix + message);
    }

    /**
     * Complain about error with specified message.
     */
    protected void complain(String message) {
        log.complain(prefix + message);
    }

    /**
     * Finalize debuggee VM wrapper by invoking <code>close()</code>.
     *
     * @throws Throwable if any throwable exception is thrown during finalization
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
