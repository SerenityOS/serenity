/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.process;

import nsk.share.TestBug;
import nsk.share.TestFailure;
import nsk.share.log.Log;
import vm.share.ProcessUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;
import java.util.*;
import java.lang.reflect.Field;

public class ProcessExecutor {
    private static long CLEANUP_TIMEOUT = 60000;
    private Process process;
    private StreamReader stdoutReader = new StreamReader("stdout");
    private StreamReader stderrReader = new StreamReader("stderr");
    private Waiter waiter = new Waiter();
    private OutputStream stdin;
    private volatile boolean running;
    private volatile int result = -1;
    private List<String> args = new ArrayList<String>();

    public int getResult() {
        return result;
    }

    public void clearArgs() {
        this.args.clear();
    }

    public void addArg(String arg) {
        this.args.add(arg);
    }

    public void addArgs(String[] args) {
        for (String arg : args) {
            this.args.add(arg);
        }
    }

    public void addArgs(Collection<String> args) {
        this.args.addAll(args);
    }

    private void printCommandLine() {
        for (String arg : args) {
            System.out.println(arg);
        }
    }

    /*
     * Start process.
     */
    public void start() {
        if (process != null) {
            throw new TestBug("Process is already started");
        }
        printCommandLine();
        try {
            process = createProcess();
            stdoutReader.setDescription("stdout: " + toString());
            stdoutReader.setStream(process.getInputStream());
            stderrReader.setDescription("stderr: " + toString());
            stderrReader.setStream(process.getErrorStream());
            stdin = process.getOutputStream();
            stdoutReader.start();
            stderrReader.start();
            waiter.start();
        } catch (IOException e) {
            throw new TestFailure("Error running process: " + toString(), e);
        }
    }

    protected Process createProcess() throws IOException {
        String[] commandLine = args.toArray(new String[args.size()]);
        return Runtime.getRuntime().exec(commandLine);
    }

    /**
     * Run and wait for completion.
     */
    public int execute(long timeout) {
        if (timeout <= 0) {
            throw new TestBug("Positive timeout is required");
        }
        start();
        return waitFor(timeout);
    }

    public int waitFor(long timeout) {
        if (process == null) {
            throw new TestBug("Process is not yet started");
        }
        if (timeout <= 0) {
            throw new TestBug("Positive timeout is required");
        }
        long timeleft = timeout;
        long startTime = 0;
        while (timeleft > 0) {
            try {
                startTime = System.currentTimeMillis();
                waiter.join(timeout);
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeleft -= (System.currentTimeMillis() - startTime);
        }
        return -1;
    }

    public int getPid() {
        return ProcessUtils.getPid(process);
    }

    public OutputStream getStdIn() {
        if (process == null) {
            throw new TestBug(
                    "Process is not running; prepare writers after it is started");
        }
        return stdin;
    }

    public PrintStream getStdInPrint() {
        return new PrintStream(getStdIn(),
                true); // Autoflush is important here.
    }

/*
    public InputStream getStdOut() {
                if (process != null)
                        throw new TestBug("Process is already running; prepare readers before it is started or some output may be missed");
                return stdoutReader.getInputStream();
        }

        public BufferedReader getStdOutReader() {
                return new BufferedReader(new InputStreamReader(getStdOut()));
        }

        public InputStream getStdErr() {
                if (process != null)
                        throw new TestBug("Process is already running; prepare readers before it is started or some output may be missed");
                return stderrReader.getInputStream();
        }

        public BufferedReader getStdErrReader() {
                return new BufferedReader(new InputStreamReader(getStdOut()));
        }

        public String getStdoutOutput() {
                if (stdoutReader == null)
                        throw new TestBug("Process is not running");
                return stdoutReader.getOutput();
        }

        public String getStderrOutput() {
                if (stderrReader == null)
                        throw new TestBug("Process is not running");
                return stderrReader.getOutput();
        }
        */

    public void addStdOutListener(StreamListener l) {
        stdoutReader.addListener(l);
    }

    public void addStdErrListener(StreamListener l) {
        stderrReader.addListener(l);
    }

    public void logStdOut(String prefix, Log log) {
        stdoutReader.addListener(new StreamLogger(prefix, log));
    }

    public void logStdErr(String prefix, Log log) {
        stderrReader.addListener(new StreamLogger(prefix, log));
    }

    public void logStdOutErr(String prefix, Log log) {
        logStdOut(prefix, log);
        logStdErr("(stderr)" + prefix, log);
    }

    public boolean isStarted() {
        return (process != null);
    }

    public void kill() {
        if (process == null) {
            throw new TestBug("Process is not running");
        }
        process.destroy();
        if (stdoutReader != null) {
            stdoutReader.kill();
        }
        if (stderrReader != null) {
            stderrReader.kill();
        }
        if (waiter != null && waiter.isAlive()) {
            waiter.kill();
        }
        process = null;
    }

    public void finish() {
        if (stdoutReader != null) {
            try {
                stdoutReader.join(CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stdoutReader = null;
        }
        if (stderrReader != null) {
            try {
                stderrReader.join(CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stderrReader = null;
        }
        process = null;
    }

    public String toString() {
        String ts = "";
        if (args != null) {
            for (String s : args) {
                ts += s;
                ts += " ";
            }
        }
        return ts;
    }

    private class Waiter extends Thread {
        public Waiter() {
            super("Process Waiter: (not setup)");
            setDaemon(true);
        }

        public void run() {
            setName("Process Waiter: " + ProcessExecutor.this);
            try {
                result = process.waitFor();
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        public void kill() {
            this.interrupt();
            long timeleft = CLEANUP_TIMEOUT;
            long startTime = 0;
            while (timeleft > 0) {
                try {
                    startTime = System.currentTimeMillis();
                    this.join(timeleft);
                    return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeleft -= (System.currentTimeMillis() - startTime);
            }
        }
    }
}
