/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import java.io.*;
import java.util.*;
import nsk.share.*;

public class ProcessExecutor {

    private String[] cmdLine;

    private long timeout;

    private boolean printProcessOutput;

    private String processOutputPrefix;

    private InputStreamReaderThread outReader;

    private InputStreamReaderThread errReader;

    private Process startedProcess;

    private ProcessWaiterThread processWaiter;

    private long expectedFinishTime;

    private volatile boolean executionCompleted;

    private int exitCode;

    private class InputStreamReaderThread extends Thread {
        private BufferedReader in;

        private String outputPrefix;

        private List<String> output = new ArrayList<String>();

        private volatile boolean streamWasAbruptlyClosed;

        private Throwable unexpectedException;

        public InputStreamReaderThread(InputStream in, String prefix) {
            this.in = new BufferedReader(new InputStreamReader(in));
            this.outputPrefix = prefix;
            setDaemon(true);
        }

        public void streamWasAbruptlyClosed(boolean newValue) {
            streamWasAbruptlyClosed = newValue;
        }

        public void run() {
            try {
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        return;

                    output.add(line);

                    if (printProcessOutput)
                        System.out.println(outputPrefix + line);
                }
            } catch (IOException e) {
                if (!streamWasAbruptlyClosed) {
                    unexpectedException = e;
                    e.printStackTrace( );
                }
            } catch (Throwable t) {
                unexpectedException = t;
                t.printStackTrace( );
            }
        }

        void checkStatus() {
            if (unexpectedException != null)
                throw new Failure("Exception was thrown during InputStreamReaderThread work: " + unexpectedException,
                        unexpectedException);
        }
    }

    private class ProcessWaiterThread extends Thread {

        private Throwable unexpectedException;

        private Process process;

        private InputStreamReaderThread outReader;

        private InputStreamReaderThread errReader;

        ProcessWaiterThread(Process process, InputStreamReaderThread outReader, InputStreamReaderThread errReader) {
            this.process = process;
            this.outReader = outReader;
            this.errReader = errReader;

            setDaemon(true);
        }

        public void run() {
            try {
                exitCode = process.waitFor();
                outReader.join();
                errReader.join();

                synchronized (ProcessWaiterThread.this) {
                    executionCompleted = true;
                    ProcessWaiterThread.this.notify();
                }
            } catch (InterruptedException e) {
                /*
                 * ProcessWaiterThread is interrupted if started process
                 * didn't finish in expected time
                 */
            } catch (Throwable t) {
                unexpectedException = t;
                t.printStackTrace();
            }
        }

        void checkStatus() {
            if (unexpectedException != null)
                throw new Failure("Exception was thrown during ProcessWaiterThread work: "
                        + unexpectedException, unexpectedException);
        }
    }

    public ProcessExecutor(String cmdLine, long timeout) {
        this.cmdLine = new String[]{cmdLine};
        this.timeout = timeout;
    }

    public ProcessExecutor(String cmdLine, long timeout, String outputPrefix) {
        this(cmdLine, timeout);
        this.printProcessOutput = true;
        this.processOutputPrefix = outputPrefix;
    }

    public void startProcess() throws IOException {
        if (cmdLine.length == 1)
            startedProcess = Runtime.getRuntime().exec(cmdLine[0]);
        else
            startedProcess = Runtime.getRuntime().exec(cmdLine);

        expectedFinishTime = System.currentTimeMillis() + timeout;

        outReader = new InputStreamReaderThread(startedProcess.getInputStream(),
                processOutputPrefix == null ? "" : processOutputPrefix + " (stdout): ");
        errReader = new InputStreamReaderThread(startedProcess.getErrorStream(),
                processOutputPrefix == null ? "" : processOutputPrefix + " (stderr): ");

        outReader.start();
        errReader.start();

        processWaiter = new ProcessWaiterThread(startedProcess, outReader, errReader);
        processWaiter.start();
    }


    public void waitForProcess() throws InterruptedException {
        synchronized (processWaiter) {
            while ((System.currentTimeMillis() < expectedFinishTime) && !executionCompleted) {
                processWaiter.wait(expectedFinishTime - System.currentTimeMillis());
            }
        }

        if (!executionCompleted) {
            destroyProcessAndWaitThreads();

            executionCompleted = true;

            throw new Failure("Execution timed out (timeout: " + timeout + "ms)");
        }
    }

    private void destroyProcessAndWaitThreads() {
        outReader.streamWasAbruptlyClosed(true);
        errReader.streamWasAbruptlyClosed(true);

        processWaiter.interrupt();
        startedProcess.destroy();

        try {
            outReader.join();
            errReader.join();
            processWaiter.join();

            outReader.checkStatus();
            errReader.checkStatus();
            processWaiter.checkStatus();
        } catch (InterruptedException e) {
            throw new Failure("Unexpected InterruptedException", e);
        }
    }

    private void checkProcessState() {
        if (!executionCompleted)
            throw new IllegalStateException("Process didn't finish execution");
    }

    public void destroyProcess() {
        if (executionCompleted)
            return;

        destroyProcessAndWaitThreads();
    }

    public long pid() {
        return startedProcess.pid();
    }

    public int getExitCode() {
        checkProcessState();

        return exitCode;
    }

    public List<String> getProcessOut() {
        checkProcessState();

        return outReader.output;
    }

    public List<String> getProcessErr() {
        checkProcessState();

        return errReader.output;
    }
}
