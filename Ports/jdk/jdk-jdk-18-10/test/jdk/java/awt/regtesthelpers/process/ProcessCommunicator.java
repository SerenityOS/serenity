/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

/**
 *  This class is created to solve interprocess communication problems.
 *  When you need to write a regression test which should verify inter jvm
 *  behavior such as DnD data transfer, Clipboard data transfer, focus
 *  transfer etc., you could use the next scenario:
 *
 *  1. Write an implementation for the parent JVM, using applet test.
 *  2. Write an implementation for the child JVM or native application, using
 *     main() function.
 *  3. Execute child process using  ProcessCommunicator.executeChildProcess()
 *     method.
 *  4. You can decide whether the test is passed on the basis of
 *     ProcessResults class data.
 *
 *  Note: The class is not thread safe. You should access its methods only from
 *        the same thread.
 */

public class ProcessCommunicator {

    private static final String javaHome = System.getProperty("java.home", "");
    private static final String javaPath = javaHome + File.separator + "bin" +
            File.separator + "java ";
    private static String command = "";
    private static volatile Process process;

    private ProcessCommunicator() {}

    /**
     * The same as {#link #executeChildProcess(Class,String)} except
     * the {@code classPathArgument} parameter. The class path
     * parameter is for the debug purposes
     *
     * @param classToExecute is passed to the child JVM
     * @param classPathArguments class path for the child JVM
     * @param args arguments that will be passed to the executed class
     * @return results of the executed {@code Process}
     */
    public static ProcessResults executeChildProcess(final Class classToExecute,
                           final String classPathArguments, final String [] args)
    {
        try {
            String command = buildCommand(classToExecute, classPathArguments, args);
            process = Runtime.getRuntime().exec(command);
            return doWaitFor(process);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes child {code Process}
     *
     * @param classToExecute class to be executed as a child java process
     * @param args args to be passed in to the child process
     * @return results of the executed {@code Process}
     */
    public static ProcessResults executeChildProcess(final Class classToExecute,
                           final String [] args)
    {
        return executeChildProcess(classToExecute, System.getProperty("java.class.path"), args);
    }

    /**
     * Waits for a process and return its results.
     * This is a workaround for {@code Process.waitFor()} never returning.
     *
     * @return results of the executed {@code Process}
     */
    public static ProcessResults doWaitFor(final Process p) {
        ProcessResults pres = new ProcessResults();

        final InputStream in;
        final InputStream err;

        try {
            in = p.getInputStream();
            err = p.getErrorStream();

            boolean finished = false;

            while (!finished) {
                try {
                    while (in.available() > 0) {
                        pres.appendToStdOut((char)in.read());
                    }
                    while (err.available() > 0) {
                        pres.appendToStdErr((char)err.read());
                    }
                    // Ask the process for its exitValue. If the process
                    // is not finished, an IllegalThreadStateException
                    // is thrown. If it is finished, we fall through and
                    // the variable finished is set to true.
                    pres.setExitValue(p.exitValue());
                    finished  = true;
                }
                catch (IllegalThreadStateException e) {
                    // Process is not finished yet;
                    // Sleep a little to save on CPU cycles
                    Thread.currentThread().sleep(500);
                }
            }
            if (in != null) in.close();
            if (err != null) err.close();
        }
        catch (Throwable e) {
            System.err.println("doWaitFor(): unexpected exception");
            e.printStackTrace();
        }
        return pres;
    }

    /**
     * Builds command on the basis of the passed class name,
     * class path and arguments.
     *
     * @param classToExecute with class will be executed in the new JVM
     * @param classPathArguments java class path (only for test purposes)
     * @param args arguments for the new application. This could be used
     *             to pass some information from the parent to child JVM.
     * @return command to execute the {@code Process}
     */
    private static String buildCommand(final Class classToExecute,
                         final String classPathArguments, final String [] args)
    {
        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append(javaPath).append(" ");
        commandBuilder.append("-Djava.security.manager=allow ");
        commandBuilder.append("-cp ").append(System.getProperty("test.classes", ".")).append(File.pathSeparatorChar);

        if (classPathArguments.trim().length() > 0) {
            commandBuilder.append(classPathArguments).append(" ");
        }

        commandBuilder.append(" ");
        commandBuilder.append(classToExecute.getName());
        for (String argument:args) {
            commandBuilder.append(" ").append(argument);
        }
        command = commandBuilder.toString();
        return command;
    }

    /**
     * Could be used for the debug purposes.
     *
     * @return command that was build to execute the child process
     */
    public static String getExecutionCommand () {
        return command;
    }

    /**
     * Terminates the process created by {@code executeChildProcess} methods.
     */
    public static void destroyProcess() {
        if (process != null) {
            if (process.isAlive()) {
                process.destroy();
            }
            process = null;
        }
    }
}
