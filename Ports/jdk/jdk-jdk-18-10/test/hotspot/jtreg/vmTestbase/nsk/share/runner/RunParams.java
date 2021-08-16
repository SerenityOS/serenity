/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.runner;

import nsk.share.log.Log;
import nsk.share.test.StressOptions;
import java.io.PrintStream;
import jdk.test.lib.Utils;

public class RunParams {
        private StressOptions stressOptions;
        private long sleepTime = 500;
        private long iterations = 0;
        private int numberOfThreads;
        private boolean runGCThread = false;
        private boolean runFinThread = false;
        private boolean runMemDiagThread = false;
        private boolean runFinDiagThread = false;
        private boolean runAllDiagThread = false;
        private boolean runForever = false;
        private long threadBlockSize = 64 * 1024 * 1024;
        private boolean interruptThreads = false;

        public RunParams() {
                this(new StressOptions());
        }

        public RunParams(StressOptions stressOptions) {
                this.stressOptions = stressOptions;
                numberOfThreads = getMediumLoadThreadsCount();
        }

        public RunParams(String[] args) {
                this();
                parseCommandLine(args);
        }

        /**
         *  Get an approximate memory which test should fill.
         *
         *  This can be used to adjust the parameters of allocated objects
         *  to test run environment. Currently it is 3/5 of
         *  Runtime.getRuntime().maxMemory().
         */
        public long getTestMemory() {
                return 3 * Runtime.getRuntime().maxMemory() / 5;
        }

        /**
         * Return memory to use for allocation of threads.
         *
         * This is currently 3/4 of getTestMemory();
         */
        public long getThreadsMemory() {
                return 3 * getTestMemory() / 4;
        }

        public final long getSleepTime() {
                return sleepTime;
        }

        public final void setSleepTime(long sleepTime) {
                this.sleepTime = sleepTime;
        }

        public final long getIterations() {
                return iterations;
        }

        public final void setIterations(long iterations) {
                if (this.iterations != iterations) {
                        this.iterations = iterations;
                        System.out.println("Iterations: " + iterations);
                }
        }

        public int getBasicLoadThreadsCount() {
                int cnt = (int) Math.min(
                        Integer.MAX_VALUE,
                        Math.min(
                                Runtime.getRuntime().availableProcessors(),
                                Math.round((double) Runtime.getRuntime().maxMemory() / threadBlockSize)
                        )
                );

                // cnt could be equal to 0 in case maxMemory is less than threadBlockSize then
                // so, need to check this
                return (cnt > 0 ? cnt : 1);
        }

        public int getMediumLoadThreadsCount() {
                return 2 * getBasicLoadThreadsCount();
        }

        public int getHighLoadThreadsCount() {
                return 100 * getBasicLoadThreadsCount();
        }

        public final int getNumberOfThreads() {
                return numberOfThreads * stressOptions.getThreadsFactor();
        }

        public final void setNumberOfThreads(int numberOfThreads) {
                this.numberOfThreads = numberOfThreads;
        }

        public final long getSeed() {
                // ensure that seed got printed out
                Utils.getRandomInstance();
                return Utils.SEED;
        }

        public final boolean isRunGCThread() {
                return runGCThread;
        }

        public final void setRunGCThread(boolean runGCThread) {
                this.runGCThread = runGCThread;
        }

        public final boolean isRunFinThread() {
                return runFinThread;
        }

        public final void setRunFinThread(boolean runFinThread) {
                this.runFinThread = runFinThread;
        }

        public final boolean isRunMemDiagThread() {
                return runMemDiagThread;
        }

        public final void setRunMemDiagThread(boolean runMemDiagThread) {
                this.runMemDiagThread = runMemDiagThread;
        }

        public final boolean isRunFinDiagThread() {
                return runFinDiagThread;
        }

        public final void setRunFinDiagThread(boolean runFinDiagThread) {
                this.runFinDiagThread = runFinDiagThread;
        }

        public final boolean isRunAllDiagThread() {
                return runAllDiagThread;
        }

        public final void setRunAllDiagThread(boolean runAllDiagThread) {
                this.runAllDiagThread = runAllDiagThread;
        }

        public final boolean isRunForever() {
                return runForever;
        }

        public final void setRunForever(boolean runForever) {
                this.runForever = runForever;
        }

        public final boolean isInterruptThreads() {
                return interruptThreads;
        }

        public final void setInterruptThreads(boolean interruptThreads) {
                this.interruptThreads = interruptThreads;
        }

        public final StressOptions getStressOptions() {
                return stressOptions;
        }

        public void parseCommandLine(String[] args) {
                if (args == null)
                        return;
                stressOptions.parseCommandLine(args);
                for (int i = 0; i < args.length; ++i) {
                        if (args[i].equals("-f"))
                                runForever = true;
                        else if (args[i].equals("-tg"))
                                runGCThread = true;
                        else if (args[i].equals("-tf"))
                                runFinThread = true;
                        else if (args[i].equals("-Dm"))
                                runMemDiagThread = true;
                        else if (args[i].equals("-Dm-"))
                                runMemDiagThread = false;
                        else if (args[i].equals("-Df1"))
                                runFinDiagThread = true;
                        else if (args[i].equals("-Df"))
                                runFinDiagThread = true;
                        else if (args[i].equals("-t"))
                                numberOfThreads = Integer.parseInt(args[++i]);
                        else if (args[i].equals("-it"))
                                interruptThreads = true;
                        else if (args[i].equals("-iterations"))
                                iterations = Integer.parseInt(args[++i]);
                }
                printConfig(System.out);
        }

        public void prinUsage() {
        }

        public void printConfig(PrintStream out) {
                stressOptions.printInfo(out);
                out.println("Max memory: " + Runtime.getRuntime().maxMemory());
                out.println("Sleep time: " + sleepTime);
                out.println("Iterations: " + iterations);
                out.println("Number of threads: " + numberOfThreads);
                out.println("Run GC thread: " + runGCThread);
                out.println("Run mem diag thread: " + runMemDiagThread);
                out.println("Run forever: " + runForever);
        }

        public void logConfig(Log log) {
                log.debug("Max memory: " + Runtime.getRuntime().maxMemory());
                log.debug("Sleep time: " + sleepTime);
                log.debug("Iterations: " + iterations);
                log.debug("Number of threads: " + numberOfThreads);
                log.debug("Run GC thread: " + runGCThread);
                log.debug("Run mem diag thread: " + runMemDiagThread);
                log.debug("Run forever: " + runForever);
        }

        private static RunParams instance;

        public static RunParams getInstance() {
                synchronized (RunParams.class) {
                        if (instance == null)
                                instance = new RunParams();
                        return instance;
                }
        }

        public static void setInstance(RunParams runParams) {
                synchronized (RunParams.class) {
                        instance = runParams;
                }
        }
}
