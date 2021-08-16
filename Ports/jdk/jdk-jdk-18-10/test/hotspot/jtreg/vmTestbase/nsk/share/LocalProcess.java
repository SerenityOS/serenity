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

package nsk.share;

import nsk.share.*;

import java.io.*;

/**
 * Wrapper for local process.
 * <p>
 * This class provides abilities to launch such process,
 * redirect standard output streams, wait for process terminates
 * or kill the process, and so on.
 * <p>
 * This object is finalized with <code>nsk.share.Finalizer</code>.
 *
 * @see nsk.share.FinalizableObject
 * @see nsk.share.Finalizer
 */

public class LocalProcess extends FinalizableObject {

    public final static int PROCESS_IS_ALIVE = 222;

    private Process process;

    protected Process getProcess() {
        return process;
    }

    public void launch (String[] args) throws IOException {
        System.out.println("Launching process by array of args: ");
        for (int mm=0; mm < args.length; mm++) {
            System.out.println("    args[" + Integer.toString(mm) + "]: >" +
                               args[mm] + "<");

        }

        process = Runtime.getRuntime().exec(args);

        Finalizer finalizer = new Finalizer(this);
        finalizer.activate();
    }

    public void launch (String cmdLine) throws IOException {
        System.out.println("Launching process by command line: " + cmdLine);

        process = Runtime.getRuntime().exec(cmdLine);

        Finalizer finalizer = new Finalizer(this);
        finalizer.activate();
    }

    /** Return exit status. */
    public int getStatus () {
        return process.exitValue();
    }

    /** Check whether the process has been terminated. */
    public boolean terminated() {
        try {
            int value = process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    /** Wait until the process shutdown or crash. */
    public int waitFor () throws InterruptedException {
        return process.waitFor();
    }

    /**
     * Wait until the process shutdown or crash for given timeout in milliseconds.
     * Returns <code>LocalProcess.PROCESS_IS_ALIVE</code> if process is not terminated
     * after timeout.
     */

    public int waitFor (long timeMillisec) throws InterruptedException {
        final Object waitObject = new Object();

        class Watcher extends Thread {
            int exitCode = LocalProcess.PROCESS_IS_ALIVE;
            Process process;

            Watcher (Process process) {
               this.process = process;
            }

            public void run () {
                try {
                    synchronized (this) {
                       exitCode = process.waitFor();
                    }
                } catch (InterruptedException ie) {
                }
                synchronized (waitObject) {
                    waitObject.notifyAll();
                }
            }

            synchronized public int getExitCode() {
                return exitCode;
            }
        }

        Watcher watcher;
        // yield control to watcher for timeMillisec time.
        synchronized (waitObject) {
            watcher = new Watcher(process);
            watcher.start();

            waitObject.wait(timeMillisec);
        }

        if (watcher.isAlive()) {
            watcher.interrupt();
        }

        return watcher.getExitCode();
    }

    // --------------------------------------------------- //

    /** Get a pipe to write to the process' stdin stream. */
    public OutputStream getStdin () {
        return process.getOutputStream();
    }

    /** Get a pipe to read the process' stdout stream. */
    public InputStream getStdout () {
        return process.getInputStream();
    }

    /** Get a pipe to read the process stderr stream. */
    public InputStream getStderr () {
        return process.getErrorStream();
    }

    /** Kill the process. */
    protected void kill() {
        process.destroy();
    }

    /**
     * Finalize mirror by invoking <code>close()</code>.
     *
     * @throws Throwable if any throwable exception is thrown during finalization
     */
    protected void finalize() throws Throwable {
        kill();
        super.finalize();
    }
}
