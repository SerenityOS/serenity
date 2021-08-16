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
import nsk.share.jdi.ArgumentHandler;

import java.io.*;

/**
 * Interface defining methods to control mirror of debuggee (i.e. debugged VM).
 */
public interface Debuggee {

    /** Default prefix for log messages. */
    public static final String LOG_PREFIX = "debuggee> ";
    public static final String DEBUGEE_STDOUT_LOG_PREFIX = "debuggee.stdout> ";
    public static final String DEBUGEE_STDERR_LOG_PREFIX = "debuggee.stderr> ";

    /**
     * Launch debuggee.
     *
     * @throws IOException
     */
    public void launch (String[] args) throws IOException;

    /** Return exit status. */
    public int getStatus ();

    /** Check whether the process has been terminated. */
    public boolean terminated();

    /** Kill the debuggee VM. */
    public void killDebuggee ();

    /** Wait until the debuggee VM shutdown or crash. */
    public int waitForDebuggee () throws InterruptedException;

    /** Get a pipe to write to the debuggee's stdin stream. */
    public OutputStream getInPipe ();

    /** Get a pipe to read the debuggee's stdout stream. */
    public InputStream getOutPipe ();

    /** Get a pipe to read the debuggee's stderr stream. */
    public InputStream getErrPipe ();

    /** Redirect stdout stream to <code>Log</code> */
    public void redirectStdout(Log log, String prefix);

    /** Redirect stderr stream to <code>Log</code> */
    public void redirectStderr(Log log, String prefix);
}

/**
 * Mirror of locally launched debuggee.
 */
final class LocalLaunchedDebuggee extends LocalProcess implements Debuggee {

    private IORedirector stdoutRedirector = null;
    private IORedirector stderrRedirector = null;
    private IORedirector stdinRedirector = null;

    /** Messages prefix. */
    protected String prefix = LOG_PREFIX;

    /** Launcher that creates this debuggee. */
    private Launcher launcher = null;

    /** Enwrap the existing <code>VM</code> mirror. */
    LocalLaunchedDebuggee (Launcher launcher) {
        super();
        this.launcher = launcher;
    }

    /**
     * Launch debuggee on local host.
     */
    public void launch(String[] args) throws IOException {
        launcher.display("Starting local debuggee.");

        super.launch(args);
        redirectStdout(launcher.getLog(), DEBUGEE_STDOUT_LOG_PREFIX );
        redirectStderr(launcher.getLog(), DEBUGEE_STDERR_LOG_PREFIX);
    }

    /** Kill the debuggee VM. */
    public void killDebuggee () {
        super.kill();
        if (stdinRedirector != null) {
           stdinRedirector.cancel();
        }
        if (stdoutRedirector != null) {
           stdoutRedirector.cancel();
        }
        if (stderrRedirector != null) {
           stderrRedirector.cancel();
        }
    }


    /**
     * Wait until the debuggee VM shutdown or crash,
     * and let finish its stdout, stderr, and stdin
     * redirectors (if any).
     *
     * @return  Debuggee process exit code.
     */
    public int waitForDebuggee () throws InterruptedException {
        int timeout = launcher.getJdbArgumentHandler().getWaitTime() * 60 * 1000;
        int exitCode;
        try {
            exitCode = super.waitFor();
            if (stdinRedirector != null) {
                if (stdinRedirector.isAlive()) {
                    stdinRedirector.join(timeout);
                    if (stdinRedirector.isAlive()) {
                        launcher.complain("Timeout for waiting STDIN redirector exceeded");
                        stdinRedirector.interrupt();
                    }
                }
                stdinRedirector = null;
            };
            if (stdoutRedirector != null) {
                if (stdoutRedirector.isAlive()) {
                    stdoutRedirector.join(timeout);
                    if (stdoutRedirector.isAlive()) {
                        launcher.complain("Timeout for waiting STDOUT redirector exceeded");
                        stdoutRedirector.interrupt();
                    }
                }
                stdoutRedirector = null;
            };
            if (stderrRedirector != null) {
                if (stderrRedirector.isAlive()) {
                    stderrRedirector.join(timeout);
                    if (stderrRedirector.isAlive()) {
                        launcher.complain("Timeout for waiting STDERR redirector exceeded");
                        stderrRedirector.interrupt();
                    }
                }
                stderrRedirector = null;
            };
        } catch (InterruptedException ie) {
            ie.printStackTrace(launcher.getLog().getOutStream());
            throw new Failure("Caught exception while waiting for LocalProcess termination: \n\t" + ie);
        }
        return exitCode;
    }

    /**
     * Get a pipe to write to the debuggee's stdin stream,
     * or throw TestBug exception is redirected.
     */
    public OutputStream getInPipe () {
        if (stdinRedirector != null)
            throw new TestBug("debuggee's stdin is redirected");
        return getStdin();
    }

    /**
     * Get a pipe to read the debuggee's stdout stream,
     * or throw TestBug exception is redirected.
     */
    public InputStream getOutPipe () {
        if (stdoutRedirector != null)
            throw new TestBug("debuggee's stdout is redirected");
        return getStdout();
    }

    /**
     * Get a pipe to read the debuggee's stderr stream,
     * or throw TestBug exception is redirected.
     */
    public InputStream getErrPipe () {
        if (stderrRedirector != null)
            throw new TestBug("debuggee's stderr is redirected");
        return getStderr();
    }

    // --------------------------------------------------- //

    /**
     * Start thread redirecting the debuggee's stdout to the
     * given <code>Log</code>. If the debuggee's stdout
     * was already redirected, the TestBug exception is thrown.
     *
     * @throws nsk.share.TestBug
     */
    public void redirectStdout(Log log, String prefix) {
        if (stdoutRedirector != null) {
            throw new TestBug("Debuggee's stdout already redirected.");
        }
        stdoutRedirector = new IORedirector(new BufferedReader(new InputStreamReader(getStdout())), log, prefix);
        stdoutRedirector.start();
    }


    /**
     * Start thread redirecting the debuggee's stderr to the
     * given <code>Log</code>. If the debuggee's stderr
     * was already redirected, the TestBug exception is thrown.
     *
     * @throws nsk.share.TestBug
     */
    public void redirectStderr(Log log, String prefix) {
        if (stderrRedirector != null) {
            throw new TestBug("Debuggee's stdout already redirected.");
        }
        stderrRedirector = new IORedirector(new BufferedReader(new InputStreamReader(getStderr())), log, prefix);
        stderrRedirector.start();
    }
}


/**
 * Mirror of remotely launched debuggee.
 */
final class RemoteLaunchedDebuggee implements Debuggee {

    /** Launcher that creates this debuggee. */
    private Launcher launcher = null;

    /** Enwrap the existing <code>VM</code> mirror. */
    RemoteLaunchedDebuggee (Launcher launcher) {
        super();
        this.launcher = launcher;
    }

    /**
     * Launch debugee on remote host via <code>Launcher</code> object.
     */
    public void launch(String[] args) throws IOException {
        String cmdLine = ArgumentHandler.joinArguments(args, "\"");
        launcher.display("Starting remote java process:\n" + cmdLine);
        launcher.launchRemoteProcess(args);
    }

    /** Return exit status of the debuggee VM. */
    public int getStatus () {
        return launcher.getRemoteProcessStatus();
    }

    /** Check whether the debuggee VM has been terminated. */
    public boolean terminated () {
        return launcher.isRemoteProcessTerminated();
    }

    // ---------------------------------------------- //

    /** Kill the debuggee VM. */
    public void killDebuggee () {
        launcher.killRemoteProcess();
    }

    /** Wait until the debuggee VM shutdown or crash. */
    public int waitForDebuggee () {
        return launcher.waitForRemoteProcess();
    }

    /** Get a pipe to write to the debuggee's stdin stream. */
    public OutputStream getInPipe () {
        return null;
    }

    /** Get a pipe to read the debuggee's stdout stream. */
    public InputStream getOutPipe () {
        return null;
    }

    /** Get a pipe to read the debuggee's stderr stream. */
    public InputStream getErrPipe () {
        return null;
    }

    public void redirectStdout(Log log, String prefix) {
    }

    public void redirectStderr(Log log, String prefix) {
    }

}
