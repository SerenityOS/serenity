/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;

/**
 * RMI regression test utility class that uses Runtime.exec to spawn a
 * java process that will run a named java class.
 */
public class JavaVM {

    static class CachedOutputStream extends OutputStream {
        ByteArrayOutputStream ba;
        OutputStream os;

        public CachedOutputStream(OutputStream os) {
            this.os = os;
            this.ba = new ByteArrayOutputStream();
        }

        public void write(int b) throws IOException {
            ba.write(b);
            os.write(b);
        }

        public void reset() throws IOException {
            os.flush();
            ba.reset();
        }
    }

    public static final long POLLTIME_MS = 100L;

    protected Process vm = null;

    private String classname = "";
    protected String args = "";
    protected String options = "";
    protected CachedOutputStream outputStream = new CachedOutputStream(System.out);
    protected CachedOutputStream errorStream = new CachedOutputStream(System.err);
    private String policyFileName = null;
    private StreamPipe outPipe;
    private StreamPipe errPipe;

    private static void mesg(Object mesg) {
        System.err.println("JAVAVM: " + mesg.toString());
    }

    /** string name of the program execd by JavaVM */
    private static String javaProgram = "java";

    static {
        try {
            javaProgram = TestLibrary.getProperty("java.home", "") +
                File.separator + "bin" + File.separator + javaProgram;
        } catch (SecurityException se) {
        }
    }

    public JavaVM(String classname,
                  String options, String args) {
        this.classname = classname;
        this.options = options;
        this.args = args;
    }

    public JavaVM(String classname,
                  String options, String args,
                  OutputStream out, OutputStream err) {
        this(classname, options, args);
        this.outputStream = new CachedOutputStream(out);
        this.errorStream = new CachedOutputStream(err);
    }

    // Prepends passed opts array to current options
    public void addOptions(String... opts) {
        String newOpts = "";
        for (int i = 0 ; i < opts.length ; i ++) {
            newOpts += " " + opts[i];
        }
        newOpts += " ";
        options = newOpts + options;
    }

    // Prepends passed arguments array to current args
    public void addArguments(String... arguments) {
        String newArgs = "";
        for (int i = 0 ; i < arguments.length ; i ++) {
            newArgs += " " + arguments[i];
        }
        newArgs += " ";
        args = newArgs + args;
    }

    public void setPolicyFile(String policyFileName) {
        this.policyFileName = policyFileName;
    }

    /**
     * This method is used for setting VM options on spawned VMs.
     * It returns the extra command line options required
     * to turn on jcov code coverage analysis.
     */
    protected static String getCodeCoverageOptions() {
        return TestLibrary.getExtraProperty("jcov.options","");
    }

    /**
     * Exec the VM as specified in this object's constructor.
     */
    private void start0() throws IOException {
        outputStream.reset();
        errorStream.reset();

        if (vm != null)
            throw new IllegalStateException("JavaVM already started");

        /*
         * If specified, add option for policy file
         */
        if (policyFileName != null) {
            String option = "-Djava.security.policy=" + policyFileName;
            addOptions(new String[] { option });
        }

        addOptions(new String[] {
            getCodeCoverageOptions(),
            TestParams.testJavaOpts,
            TestParams.testVmOpts
        });

        StringTokenizer optionsTokenizer = new StringTokenizer(options);
        StringTokenizer argsTokenizer = new StringTokenizer(args);
        int optionsCount = optionsTokenizer.countTokens();
        int argsCount = argsTokenizer.countTokens();

        String javaCommand[] = new String[optionsCount + argsCount + 2];
        int count = 0;

        javaCommand[count++] = JavaVM.javaProgram;
        while (optionsTokenizer.hasMoreTokens()) {
            javaCommand[count++] = optionsTokenizer.nextToken();
        }
        javaCommand[count++] = classname;
        while (argsTokenizer.hasMoreTokens()) {
            javaCommand[count++] = argsTokenizer.nextToken();
        }

        mesg("command = " + Arrays.asList(javaCommand).toString());

        vm = Runtime.getRuntime().exec(javaCommand);
    }

    public void start() throws IOException {
        start0();

        /* output from the exec'ed process may optionally be captured. */
        outPipe = StreamPipe.plugTogether(vm.getInputStream(), this.outputStream);
        errPipe = StreamPipe.plugTogether(vm.getErrorStream(), this.errorStream);
    }

    public void destroy() {
        if (vm != null) {
            vm.destroyForcibly();
        }
        vm = null;
    }

    /**
     * Return exit value for vm process.
     * @return exit value for vm process
     * @throws IllegalThreadStateException if the vm process has not yet terminated
     */
    public int exitValue() {
        return vm.exitValue();
    }

    /**
     * Destroy the vm process, and do necessary cleanup.
     */
    public void cleanup() {
        destroy();
    }

    /**
     * Destroys the VM, waits for it to terminate, and returns
     * its exit status.
     *
     * @throws IllegalStateException if the VM has already been destroyed
     * @throws InterruptedException if the caller is interrupted while waiting
     */
    public int terminate() throws InterruptedException {
        if (vm == null) {
            throw new IllegalStateException("JavaVM already destroyed");
        }

        vm.destroy();
        int status = waitFor();
        vm = null;
        return status;
    }


    /**
     * Waits for the subprocess to exit, joins the pipe threads to ensure that
     * all output is collected, and returns its exit status.
     */
    public int waitFor() throws InterruptedException {
        if (vm == null)
            throw new IllegalStateException("can't wait for JavaVM that isn't running");

        int status = vm.waitFor();
        outPipe.join();
        errPipe.join();
        return status;
    }

    /**
     * Causes the current thread to wait the vm process to exit, if necessary,
     * wait until the vm process has terminated, or the specified waiting time
     * elapses. Release allocated input/output after vm process has terminated.
     * @param timeout the maximum milliseconds to wait.
     * @return exit value for vm process.
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting.
     * @throws TimeoutException if subprocess does not end after timeout
     *         milliseconds passed
     */
    public int waitFor(long timeout)
            throws InterruptedException, TimeoutException {
        if (vm == null)
            throw new IllegalStateException("can't wait for JavaVM that isn't running");
        long deadline = TestLibrary.computeDeadline(System.currentTimeMillis(), timeout);

        while (true) {
            try {
                int status = vm.exitValue();
                outPipe.join();
                errPipe.join();
                return status;
            } catch (IllegalThreadStateException ignore) { }

            if (System.currentTimeMillis() > deadline)
                throw new TimeoutException();

            Thread.sleep(POLLTIME_MS);
        }
    }

    /**
     * Starts the subprocess, waits for it to exit, and returns its exit status.
     */
    public int execute() throws IOException, InterruptedException {
        start();
        return waitFor();
    }
}
