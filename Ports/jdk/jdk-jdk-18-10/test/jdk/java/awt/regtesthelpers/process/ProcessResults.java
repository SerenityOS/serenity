/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

package test.java.awt.regtesthelpers.process;

import java.io.PrintStream;

/**
 * The calss incapsulates process related information and methods to
 * handle this information.
 *
 */

public class ProcessResults {
    private int exitValue;
    private StringBuilder stdout;
    private StringBuilder stderr;

    public ProcessResults() {
        exitValue = -1;
        stdout = new StringBuilder();
        stderr = new StringBuilder();
    }

    public synchronized int getExitValue () {
        return exitValue;
    }

    public synchronized String getStdOut() {
        return stdout.toString();
    }

   public synchronized String getStdErr() {
        return stderr.toString();
    }

    /** Prints child process standart output into the desired PrintStream.
     *
     * @param printTo PrintStraem where output have to be redirected
     */
    public synchronized void printProcessStandartOutput(PrintStream printTo) {
        if (stdout != null && stdout.length() > 0) {
            printTo.println("========= Child VM System.out ========");
            printTo.print(stdout);
            printTo.println("======================================");
        }
    }

    /** Prints child process error output into the desired PrintStream.
     *
     * @param printTo PrintStraem where output have to be redirected
     */
    public synchronized void printProcessErrorOutput(PrintStream printTo) {
        if (stderr != null && stderr.length() > 0) {
            printTo.println("========= Child VM System.err ========");
            printTo.print(stderr);
            printTo.println("======================================");
        }
    }

    /**  Prints child process error output into the desired {@code PrintStream},
     *   if child JVM error output contains any of the next words: "error",
     *   "exception". We cannot be sure that the test is failed when stderr is
     *   not empty, because in error stream could be written some debug information.
     *
     * @param err PrintStraem where output have to be redirected
     */
    public synchronized void verifyStdErr(PrintStream err) {
        if (stderr != null && ((stderr.toString().toLowerCase().indexOf("error") != -1)
                || (stderr.toString().toLowerCase().indexOf("exception") != -1)))
        {
            printProcessErrorOutput(err);
            throw new RuntimeException("WARNING: Child process  error stream " +
                    "is not empty!");
        }
    }

    /**  Throws new RuntimeException if the child JVM returns not 0 value.
     *
     * @param err PrintStraem where output have to be redirected
     */
    public synchronized void verifyProcessExitValue(PrintStream err) {
        if (exitValue != 0) {
            throw new RuntimeException("Child process returns not 0 value!" +
                    "Returned value is " + exitValue);
        }
    }

    public void verifyProcessExecutionResults(PrintStream err) {
        // the next functions are synchronized
        verifyStdErr(err);
        verifyProcessExitValue(err);
    }

    synchronized void appendToStdOut(char c) {
        stdout.append(c);
    }

    synchronized void appendToStdErr(char c) {
        stderr.append(c);
    }

    synchronized void setExitValue(int exitValue) {
        this.exitValue = exitValue;
    }
}
