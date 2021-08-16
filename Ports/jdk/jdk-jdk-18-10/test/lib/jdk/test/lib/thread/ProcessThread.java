/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.thread;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.PrintWriter;

import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

/**
 * The helper class for starting and stopping {@link Process} in a separate thread.
 */
public class ProcessThread extends TestThread {

    /**
     * Creates a new {@code ProcessThread} object.
     *
     * @param threadName The name of thread
     * @param cmd The string array of program and its arguments to pass to {@link ProcessBuilder}
     */
    public ProcessThread(String threadName, String... cmd) {
        super(new ProcessRunnable(new ProcessBuilder(cmd)), threadName);
    }

    /**
     * Creates a new {@code ProcessThread} object.
     *
     * @param threadName The name of thread.
     * @param pb The ProcessBuilder to execute.
     */
    public ProcessThread(String threadName, ProcessBuilder pb) {
        super(new ProcessRunnable(pb), threadName);
    }


    /**
     * Creates a new {@code ProcessThread} object.
     *
     * @param threadName The name of thread
     * @param waitfor A predicate to determine whether the target process has been initialized
     * @param cmd The string array of program and its arguments to pass to {@link ProcessBuilder}
     */
    public ProcessThread(String threadName, Predicate<String> waitfor, String... cmd) {
        super(new ProcessRunnable(new ProcessBuilder(cmd), threadName, waitfor), threadName);
    }

    /**
     * Creates a new {@code ProcessThread} object.
     *
     * @param threadName The name of thread.
     * @param waitfor A predicate to determine whether the target process has been initialized
     * @param pb The ProcessBuilder to execute.
     */
    public ProcessThread(String threadName, Predicate<String> waitfor, ProcessBuilder pb) {
        super(new ProcessRunnable(pb, threadName, waitfor), threadName);
    }

    /**
     * Stops {@link Process} started by {@code ProcessRunnable}.
     *
     * @throws InterruptedException
     */
    public void stopProcess() throws InterruptedException {
        ((ProcessRunnable) getRunnable()).stopProcess();
    }

    /**
     * @return The process output, or null if the process has not yet completed.
     */
    public OutputAnalyzer getOutput() {
        return ((ProcessRunnable) getRunnable()).getOutput();
    }

    /**
    * Returns the PID associated with this process thread
    * @return The PID associated with this process thread
    */
    public long getPid() throws InterruptedException {
        return ((ProcessRunnable)getRunnable()).getPid();
    }

    public void sendMessage(String message) throws InterruptedException {
        ((ProcessRunnable)getRunnable()).sendMessage(message);
    }

    /**
     * {@link Runnable} interface for starting and stopping {@link Process}.
     */
    static class ProcessRunnable extends XRun {

        private final ProcessBuilder processBuilder;
        private final CountDownLatch latch;
        private volatile Process process;
        private volatile OutputAnalyzer output;
        private final Predicate<String> waitfor;
        private final String name;

        /**
         * Creates a new {@code ProcessRunnable} object.
         *
         * @param pb The {@link ProcessBuilder} to run.
         */
        public ProcessRunnable(ProcessBuilder pb) {
            this(pb, "", null);
        }

        /**
         * Creates a new {@code ProcessRunnable} object.
         *
         * @param pb The {@link ProcessBuilder} to run.
         * @param name An optional process name; may be null
         * @param waitfor A predicate to determine whether the target process has been initialized; may be null
         */
        public ProcessRunnable(ProcessBuilder pb, String name, Predicate<String> waitfor) {
            this.processBuilder = pb;
            this.latch = new CountDownLatch(1);
            this.name = name;
            this.waitfor = waitfor;
        }

        /**
         * Starts the process in {@code ProcessThread}.
         * All exceptions which occurs here will be caught and stored in {@code ProcessThread}.
         *
         * see {@link XRun}
         */
        @Override
        public void xrun() throws Throwable {
            this.process = ProcessTools.startProcess(name, processBuilder, waitfor);
            // Release when process is started
            latch.countDown();

            // Will block...
            try {
                this.process.waitFor();
                output = new OutputAnalyzer(this.process);
            } catch (Throwable t) {
                String name = Thread.currentThread().getName();
                System.out.println(String.format("ProcessThread[%s] failed: %s", name, t.toString()));
                throw t;
            } finally {
                this.process.destroyForcibly().waitFor();
                String logMsg = ProcessTools.getProcessLog(processBuilder, output);
                System.out.println(logMsg);
            }
        }

        /**
         * Stops the process.
         *
         * @throws InterruptedException
         */
        public void stopProcess() throws InterruptedException {
            // Wait until process is started
            latch.await();
            if (this.process != null) {
                System.out.println("ProcessThread.stopProcess() will kill process");
                this.process.destroy();
            }
        }

        /**
         * Returns the OutputAnalyzer with stdout/stderr from the process.
         * @return The process output, or null if process not completed.
         * @throws InterruptedException
         */
        public OutputAnalyzer getOutput() {
            return output;
        }

        /**
         * Returns the PID associated with this process runnable
         * @return The PID associated with this process runnable
         */
        public long getPid() throws InterruptedException {
            return getProcess().pid();
        }

        public void sendMessage(String message) throws InterruptedException {
            try (PrintWriter pw = new PrintWriter(this.getProcess().getOutputStream())) {
                pw.println(message);
                pw.flush();
            }
        }

        private Process getProcess() throws InterruptedException {
            latch.await();
            return process;
        }
    }

}
