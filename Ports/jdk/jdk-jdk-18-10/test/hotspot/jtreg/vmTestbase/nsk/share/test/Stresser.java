/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.test;

import java.io.PrintStream;
import nsk.share.TestBug;

/**
 * Support class for implementing configurable stress execution.
 *
 * This class is intended to be used in one thread
 *
 * <code>
 * Stresser stresser = new Stresser(stressOptions);
 *
 * try {
 *      stresser.start(100);
 *      while (stresser.iteration()) {
 *              ...
 *      }
 * } finally {
 *      stresser.finish();
 * }
 * </code>
 *
 * Standard number of iterations (integer parameter to start() method) is
 * defined by particular test itself. It may be different for different tests
 * because average execution time of one iteration may be different.
 * This is value which is enough to do what test intends to do and it should
 * also give average execution time on most configurations less than
 * standard value of stressTime parameter (30 seconds).
 *
 * @see nsk.share.test.StressOptions for explanation of stress options.
 */
public class Stresser implements ExecutionController {

    private StressOptions options;
    private String name;
    private long maxIterations;
    private long iterations;
    private long startTime;
    private long finishTime;
    private long currentTime;
    private PrintStream defaultOutput = System.out;

    /*
     * Flag which indicates that execution is finished.
     * Volatile, because another thread might read this variable.
     */
    private volatile boolean finished;

    /*
     * Flag which indicates that execution should be forced to finish.
     * Volatile, because another thread might set this variable.
     */
    private volatile boolean forceFinish;

    /**
     * Creates stresser with default settings.
     */
    public Stresser() {
        this(new StressOptions());
    }

    /**
     * Creates stresser with given options.
     *
     * @param options stress options
     */
    public Stresser(StressOptions options) {
        setOptions(options);
    }

    /**
     * Create stresser configured from command line arguments.
     *
     * @param arg arguments
     */
    public Stresser(String[] args) {
        this(new StressOptions(args));
    }

    /**
     * Creates stresser configured from command line arguments and
     * sets its output stream to a given one
     *
     * @param arg arguments
     * @param out default output stream
     */
    public Stresser(String[] args, PrintStream out) {
        this(new StressOptions(args));
        setDefaultOutput(out);
    }

    /**
     * Creates stresser with default options and given name.
     *
     * @param name stresser name
     */
    public Stresser(String name) {
        this();
        setName(name);
    }

    /**
     * Creates stresser with given name and options.
     *
     * @param name stresser name
     * @param options stress options
     */
    public Stresser(String name, StressOptions options) {
        this(options);
        setName(name);
    }

    /**
     * Creates stresser with given name from command line arguments.
     *
     * @param name stresser name
     * @param args arguments
     */
    public Stresser(String name, String[] args) {
        this(args);
        setName(name);
    }

    /**
     * Sets default output stream for printing debug messages.
     * Initially it is set to System.out.
     *
     * @param out The stream to print to
     */
    public void setDefaultOutput(PrintStream out) {
        defaultOutput = out;
    }

    /**
     * Displays information about stress options.
     */
    public void printStressOptions(PrintStream out) {
        options.printInfo(out);
    }

    /**
     * Displays information about this stresser.
     *
     * @param out output stream
     */
    public void printStressInfo(PrintStream out) {
        println(out, "Stress time: " + options.getTime() + " seconds");
        println(out, "Iterations: " + maxIterations);
    }

    /**
     * Displays information about this particular execution
     * of this stresser.
     *
     * @param out output stream
     */
    public void printExecutionInfo(PrintStream out) {
        println(out, "Completed iterations: " + iterations);
        println(out, "Execution time: " + (currentTime - startTime) + " seconds");
        if (!finished) {
            println(out, "Execution is not finished yet");
        } else if (forceFinish) {
            println(out, "Execution was forced to finish");
        } else if (maxIterations != 0 && iterations >= maxIterations) {
            println(out, "Execution finished because number of iterations was exceeded: " + iterations + " >= " + maxIterations);
        } else if (finishTime != 0 && currentTime >= finishTime) {
            println(out, "Execution finished because time was exceeded: " + (currentTime - startTime) + " >= " + (finishTime - startTime));
        }
    }

    private void println(PrintStream out, String s) {
        if (name != null) {
            out.print(name);
            out.print(": ");
        }
        out.println(s);
        out.flush();
    }

    /**
     * Starts stress execution.
     *
     * @param stdIterations standard number of iterations.
     */
    public void start(long stdIterations) {
        maxIterations = stdIterations * options.getIterationsFactor();
        iterations = 0;
        long stressTime = options.getTime();
        startTime = System.currentTimeMillis();
        if (stressTime == 0) {
            finishTime = 0;
        } else {
            finishTime = startTime + stressTime * 1000;
        }
        finished = false;
        forceFinish = false;
        if (options.isDebugEnabled()) {
            println(defaultOutput, "Starting stress execution: " + stdIterations);
            printStressInfo(defaultOutput);
        }
    }

    /**
     * Finishes stress execution.
     *
     * This method should be called from the thread where
     * execution is performed after the loop. It is also
     * recommended that this method is called from
     * finally {} block.
     */
    public void finish() {
        currentTime = System.currentTimeMillis();
        finished = true;
        if (options.isDebugEnabled()) {
            printExecutionInfo(defaultOutput);
        }
    }

    /**
     * Forces execution to finish.
     *
     * This method may be called from other thread.
     */
    public void forceFinish() {
        forceFinish = true;
    }

    /**
     * Marks the beginning of new iteration.
     *
     * @return true if execution needs to continue
     */
    public boolean iteration() {
        ++iterations;
        if (options.isDebugDetailed()) {
            printExecutionInfo(defaultOutput);
        }
        return continueExecution();
    }

    /**
     * Checks if execution needs to continue. This does not mark new iteration.
     *
     * @return true if execution needs to continue
     */
    public boolean continueExecution() {
        currentTime = System.currentTimeMillis();
        if (startTime == 0) {
            throw new TestBug("Stresser is not started.");
        }
        return !forceFinish
                && !finished
                && (maxIterations == 0 || iterations < maxIterations)
                && (finishTime == 0 || currentTime < finishTime);
    }

    /**
     * Obtains current iteration number.
     *
     * @return current iteration
     */
    public long getIteration() {
        return iterations;
    }

    /**
     * Obtains maximum number of iterations.
     *
     * @return max number of iterations
     */
    public long getMaxIterations() {
        return maxIterations;
    }

    public long getIterationsLeft() {
        if (iterations >= maxIterations) {
            return 0;
        } else {
            return maxIterations - iterations;
        }
    }

    /**
     * Obtains time passed from start of stress execution in milliseconds.
     *
     * @return time
     */
    public long getExecutionTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Obtains time left till end of execution in milliseconds.
     *
     * @return time
     */
    public long getTimeLeft() {
        long current = System.currentTimeMillis();
        if (current >= finishTime) {
            return 0;
        } else {
            return finishTime - current;
        }
    }

    /**
     * Sets stress options for this stresser.
     *
     * @param options stress options
     */
    public void setOptions(StressOptions options) {
        this.options = options;
    }

    /**
     * Sets name of this stresser.
     *
     * @param name name of stresser
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtains name of this stresser.
     */
    public String getName() {
        return name;
    }
}
