/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.apps;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.UUID;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputBuffer;
import jdk.test.lib.process.StreamPumper;
import jdk.test.lib.util.CoreUtils;

/**
 * This is a framework to launch an app that could be synchronized with caller
 * to make further attach actions reliable across supported platforms

 * Caller example:
 *
 *   LingeredApp a = LingeredApp.startApp(cmd);
 *     // do something.
 *     // a.getPid(). a.getProcess(), a.getProcessStdout() are available.
 *   LingeredApp.stopApp(a);
 *
 *   for use custom LingeredApp (class SmartTestApp extends LingeredApp):
 *
 *   SmartTestApp = new SmartTestApp();
 *   LingeredApp.startApp(a, cmd);
 *     // do something
 *   a.stopApp();   // LingeredApp.stopApp(a) can be used as well
 *
 *   or fine grained control
 *
 *   a = new SmartTestApp("MyLock.lck");
 *   a.createLock();
 *   a.runAppExactJvmOpts(Utils.getTestJavaOpts());
 *   a.waitAppReady();
 *     // do something
 *   a.deleteLock();
 *   a.waitAppTerminate();
 *
 *  After app termination (stopApp/waitAppTermination) its output is available
 *
 *   output = a.getAppOutput();
 *
 */
public class LingeredApp {

    private static final long spinDelay = 1000;

    private long lockCreationTime;
    private ByteArrayOutputStream stderrBuffer;
    private ByteArrayOutputStream stdoutBuffer;
    private Thread outPumperThread;
    private Thread errPumperThread;
    private boolean finishAppCalled = false;
    private boolean useDefaultClasspath = true;

    protected Process appProcess;
    protected OutputBuffer output;
    protected static final int appWaitTime = 100;
    protected static final int appCoreWaitTime = 480;
    protected final String lockFileName;
    protected String logFileName;

    protected boolean forceCrash = false; // set true to force a crash and core file

    /**
     * Create LingeredApp object on caller side. Lock file have be a valid filename
     * at writable location
     *
     * @param lockFileName - the name of lock file
     */
    public LingeredApp(String lockFileName) {
        this.lockFileName = lockFileName;
    }

    public LingeredApp() {
        final String lockName = UUID.randomUUID().toString() + ".lck";
        this.lockFileName = lockName;
    }

    public void setForceCrash(boolean forceCrash) {
        this.forceCrash = forceCrash;
    }

    native private static int crash();

    /**
     *
     * @return name of lock file
     */
    public String getLockFileName() {
        return this.lockFileName;
    }

    public void setLogFileName(String name) {
        logFileName = name;
    }

    /**
     *
     *  @return pid of java process running testapp
     */
    public long getPid() {
        if (appProcess == null) {
            throw new RuntimeException("Process is not alive");
        }
        return appProcess.pid();
    }

    /**
     *
     * @return process object
     */
    public Process getProcess() {
        return appProcess;
    }

    /**
     * @return the LingeredApp's output.
     * Can be called after the app is run.
     */
    public String getProcessStdout() {
        return stdoutBuffer.toString();
    }

    /**
     *
     * @return OutputBuffer object for the LingeredApp's output. Can only be called
     * after LingeredApp has exited.
     */
    public OutputBuffer getOutput() {
        if (appProcess.isAlive()) {
            throw new RuntimeException("Process is still alive. Can't get its output.");
        }
        if (output == null) {
            output = OutputBuffer.of(stdoutBuffer.toString(), stderrBuffer.toString(), appProcess.exitValue());
        }
        return output;
    }

    /*
     * Capture all stdout and stderr output from the LingeredApp so it can be returned
     * to the driver app later. This code is modeled after ProcessTools.getOutput().
     */
    private void startOutputPumpers() {
        stderrBuffer = new ByteArrayOutputStream();
        stdoutBuffer = new ByteArrayOutputStream();
        StreamPumper outPumper = new StreamPumper(appProcess.getInputStream(), stdoutBuffer);
        StreamPumper errPumper = new StreamPumper(appProcess.getErrorStream(), stderrBuffer);
        outPumperThread = new Thread(outPumper);
        errPumperThread = new Thread(errPumper);

        outPumperThread.setDaemon(true);
        errPumperThread.setDaemon(true);

        outPumperThread.start();
        errPumperThread.start();
    }

    /* Make sure all part of the app use the same method to get dates,
     as different methods could produce different results
     */
    private static long epoch() {
        return new Date().getTime();
    }

    private static long lastModified(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.lastModifiedTime().toMillis();
    }

    private static void setLastModified(String fileName, long newTime) throws IOException {
        Path path = Paths.get(fileName);
        FileTime fileTime = FileTime.fromMillis(newTime);
        Files.setLastModifiedTime(path, fileTime);
    }

    /**
     * create lock
     *
     * @throws IOException
     */
    public void createLock() throws IOException {
        Path path = Paths.get(lockFileName);
        // Files.deleteIfExists(path);
        Files.createFile(path);
        lockCreationTime = lastModified(lockFileName);
    }

    /**
     * Delete lock
     *
     * @throws IOException
     */
    public void deleteLock() throws IOException {
        try {
            Path path = Paths.get(lockFileName);
            Files.delete(path);
        } catch (NoSuchFileException ex) {
            // Lock already deleted. Ignore error
        }
    }

    public void waitAppTerminate() {
        // This code is modeled after tail end of ProcessTools.getOutput().
        try {
            // If the app hangs, we don't want to wait for the to test timeout.
            if (!appProcess.waitFor(Utils.adjustTimeout(appWaitTime), TimeUnit.SECONDS)) {
                appProcess.destroy();
                appProcess.waitFor();
            }
            outPumperThread.join();
            errPumperThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // pass
        }
    }

    /**
     * The app touches the lock file when it's started
     * wait while it happens. Caller have to delete lock on wait error.
     *
     * @param timeout timeout in seconds
     * @throws java.io.IOException
     */
    public void waitAppReady(long timeout) throws IOException {
        // adjust timeout for timeout_factor and convert to ms
        timeout = Utils.adjustTimeout(timeout) * 1000;
        long here = epoch();
        while (true) {
            long epoch = epoch();
            if (epoch - here > timeout) {
                throw new IOException("App waiting timeout");
            }

            // Live process should touch lock file every second
            long lm = lastModified(lockFileName);
            if (lm > lockCreationTime) {
                break;
            }

            // Make sure process didn't already exit
            if (!appProcess.isAlive()) {
                if (forceCrash) {
                    return; // This is expected. Just return.
                } else {
                    throw new IOException("App exited unexpectedly with " + appProcess.exitValue());
                }
            }

            try {
                Thread.sleep(spinDelay);
            } catch (InterruptedException ex) {
                // pass
            }
        }
    }

    /**
     * Waits for the application to start with the default timeout.
     */
    public void waitAppReady() throws IOException {
        waitAppReady(forceCrash ? appCoreWaitTime : appWaitTime);
    }

    /**
     * Analyze an environment and prepare a command line to
     * run the app, app name should be added explicitly
     */
    private List<String> runAppPrepare(String[] vmArguments) {
        List<String> cmd = new ArrayList<>();
        cmd.add(JDKToolFinder.getTestJDKTool("java"));
        Collections.addAll(cmd, vmArguments);
        if (forceCrash) {
            cmd.add("-XX:+CreateCoredumpOnCrash");
            // We need to find libLingeredApp.so for the crash() native method
            cmd.add("-Djava.library.path=" + System.getProperty("java.library.path"));
        }

        if (useDefaultClasspath()) {
            // Make sure we set correct classpath to run the app
            cmd.add("-cp");
            String classpath = System.getProperty("test.class.path");
            cmd.add((classpath == null) ? "." : classpath);
        }

        return cmd;
    }

    /**
     * Adds application name to the command line.
     * By default adds name of this class.
     */
    protected void runAddAppName(List<String> cmd) {
        cmd.add(getClass().getName());
    }

    /**
     * Assemble command line to a printable string
     */
    public void printCommandLine(List<String> cmd) {
        // A bit of verbosity
        System.out.println(cmd.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(" ", "Command line: [", "]")));
    }

    public boolean useDefaultClasspath() { return useDefaultClasspath; }
    public void setUseDefaultClasspath(boolean value) { useDefaultClasspath = value; }

    /**
     * Run the app.
     * User should provide exact options to run app. Might use #Utils.getTestJavaOpts() to set default test options.
     * @param vmOpts
     * @throws IOException
     */
    public void runAppExactJvmOpts(String[] vmOpts)
            throws IOException {

        List<String> cmd = runAppPrepare(vmOpts);

        runAddAppName(cmd);
        cmd.add(lockFileName);
        if (forceCrash) {
            cmd.add("forceCrash"); // Let the subprocess know to force a crash
        }

        printCommandLine(cmd);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (forceCrash) {
            // If we are going to force a core dump, apply "ulimit -c unlimited" if we can.
            pb = CoreUtils.addCoreUlimitCommand(pb);
        }
        // ProcessBuilder.start can throw IOException
        appProcess = pb.start();

        startOutputPumpers();
    }

    private void finishApp() {
        if (appProcess != null) {
            if (finishAppCalled) {
                return;
            } else {
                finishAppCalled = true;
            }
            OutputBuffer output = getOutput();
            String msg =
                    " LingeredApp stdout: [" + output.getStdout() + "];\n" +
                    " LingeredApp stderr: [" + output.getStderr() + "]\n" +
                    " LingeredApp exitValue = " + appProcess.exitValue();

            if (logFileName != null) {
                System.out.println(" LingeredApp exitValue = " + appProcess.exitValue());
                System.out.println(" LingeredApp output: " + logFileName + " (" + msg.length() + " chars)");
                try (FileOutputStream fos = new FileOutputStream(logFileName);
                     PrintStream ps = new PrintStream(fos);) {
                    ps.print(msg);
                 } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                 }
            } else {
                System.out.println(msg);
            }
        }
    }

    /**
     * Delete lock file that signals app to terminate, then
     * wait until app is actually terminated.
     * @throws IOException
     */
    public void stopApp() throws IOException {
        deleteLock();
        // The startApp() of the derived app can throw
        // an exception before the LA actually starts
        if (appProcess != null) {
            waitAppTerminate();

            finishApp();

            int exitcode = appProcess.exitValue();
            if (exitcode != 0) {
                throw new IOException("LingeredApp terminated with non-zero exit code " + exitcode);
            }
        }
    }

    /**
     *  High level interface for test writers
     */

    /**
     * Factory method that starts pre-created LingeredApp
     * lock name is autogenerated
     * User should provide exact options to run app. Might use #Utils.getTestJavaOpts() to set default test options.
     * @param jvmOpts - the exact vm options used to start LingeredApp
     * @param theApp - app to start
     * @throws IOException
     */
    public static void startAppExactJvmOpts(LingeredApp theApp, String... jvmOpts) throws IOException {
        theApp.createLock();
        try {
            theApp.runAppExactJvmOpts(jvmOpts);
            theApp.waitAppReady();
        } catch (Exception ex) {
            System.out.println("LingeredApp failed to start: " + ex);
            theApp.finishApp();
            theApp.deleteLock();
            throw ex;
        }
    }

    /**
     * Factory method that starts pre-created LingeredApp
     * lock name is autogenerated, additionalJvmOpts are appended to default test options
     * @param additionalJvmOpts - additional Jvm options, appended to #Utils.getTestJavaOpts();
     * @param theApp - app to start
     * @throws IOException
     */
    public static void startApp(LingeredApp theApp, String... additionalJvmOpts) throws IOException {
        startAppExactJvmOpts(theApp, Utils.prependTestJavaOpts(additionalJvmOpts));
    }

    /**
     * Factory method that creates LingeredApp object with ready to use application
     * lock name is autogenerated, additionalJvmOpts are appended to default test options
     * @param additionalJvmOpts - additional Jvm options, appended to #Utils.getTestJavaOpts();
     * @return LingeredApp object
     * @throws IOException
     */
    public static LingeredApp startApp(String... additionalJvmOpts) throws IOException {
        LingeredApp a = new LingeredApp();
        startApp(a, additionalJvmOpts);
        return a;
    }

    public static void stopApp(LingeredApp app) throws IOException {
        if (app != null) {
            // LingeredApp can throw an exception during the intialization,
            // make sure we don't have cascade NPE
            app.stopApp();
        }
    }

    /**
     * LastModified time might not work correctly in some cases it might
     * cause later failures
     */

    public static boolean isLastModifiedWorking() {
        boolean sane = true;
        try {
            long lm = lastModified(".");
            if (lm == 0) {
                System.err.println("SANITY Warning! The lastModifiedTime() doesn't work on this system, it returns 0");
                sane = false;
            }

            long now = epoch();
            if (lm > now) {
                System.err.println("SANITY Warning! The Clock is wrong on this system lastModifiedTime() > getTime()");
                sane = false;
            }

            setLastModified(".", epoch());
            long lm1 = lastModified(".");
            if (lm1 <= lm) {
                System.err.println("SANITY Warning! The setLastModified doesn't work on this system");
                sane = false;
            }
        }
        catch(IOException e) {
            System.err.println("SANITY Warning! IOException during sanity check " + e);
            sane = false;
        }

        return sane;
    }

    /**
     * Support for creating a thread whose stack trace is always readable. There are
     * occassional isues trying to get the stack trace for LingeredApp.main() since
     * it sometimes wakes up from sleep and may breifly have an unreadable thread
     * stack trace. The code below is used to create "SteadyStateThread" whose
     * stack trace is always readable.
     */

    private static volatile boolean steadyStateReached = false;

    private static void steadyState(Object steadyStateObj) {
        steadyStateReached = true;
        synchronized(steadyStateObj) {
        }
    }

    private static void startSteadyStateThread(Object steadyStateObj) {
        Thread steadyStateThread = new Thread() {
            public void run() {
                steadyState(steadyStateObj);
            }
        };
        steadyStateThread.setName("SteadyStateThread");
        steadyStateThread.start();

        // Wait until the thread has started running.
        while (!steadyStateReached) {
            Thread.onSpinWait();
        }

        // Now wait until we get into the synchronized block.
        while (steadyStateThread.getState() != Thread.State.BLOCKED) {
            Thread.onSpinWait();
        }
    }


    /**
     * This part is the application itself. First arg is optional "forceCrash".
     * Following arg is the lock file name.
     */
    public static void main(String args[]) {
        boolean forceCrash = false;

        if (args.length == 0) {
            System.err.println("Lock file name is not specified");
            System.exit(7);
        } else if (args.length > 2) {
            System.err.println("Too many arguments specified: "  + args.length);
            System.exit(7);
        }

        if (args.length == 2) {
            if (args[1].equals("forceCrash")) {
                forceCrash = true;
            } else {
                System.err.println("Invalid 1st argment: " + args[1]);
                System.exit(7);
            }
        }

        String theLockFileName = args[0];
        Path path = Paths.get(theLockFileName);

        try {
            Object steadyStateObj = new Object();
            synchronized(steadyStateObj) {
                startSteadyStateThread(steadyStateObj);
                if (forceCrash) {
                    System.loadLibrary("LingeredApp"); // location of native crash() method
                    crash();
                }
                while (Files.exists(path)) {
                    // Touch the lock to indicate our readiness
                    setLastModified(theLockFileName, epoch());
                    Thread.sleep(spinDelay);
                }
            }
        } catch (IOException ex) {
            // Lock deleted while we are setting last modified time.
            // Ignore the error and let the app exit.
            if (Files.exists(path)) {
                // If the lock file was not removed, return an error.
                System.err.println("LingeredApp IOException: lock file still exists");
                System.exit(4);
            }
        } catch (Exception ex) {
            System.err.println("LingeredApp ERROR: " + ex);
            // Leave exit_code = 1 to Java launcher
            System.exit(3);
        }

        System.exit(0);
    }
}
