/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdwp;

import nsk.share.*;
import nsk.share.jpda.*;

import java.io.*;

/**
 * This class provides debugger with connection to debugee VM
 * using JDWP protocol.
 * <p>
 * This class provides abilities to launch and bind to debugee VM
 * as described for base <code>DebugeeBinder</code> class,
 * using raw JDWP protocol.
 * <p>
 * When <code>Binder</code> is asked to bind to debugee by invoking
 * <code>bindToBebugee()</code> method it launches process
 * with debugee VM and makes connection to it using JDWP transport
 * corresponding to value of command line options <code>-connector</code>
 * and <code>-transport</code>.
 * After debugee is launched and connection is established
 * <code>Binder</code> constructs <code>Debugee</code> object,
 * that provides abilities to interact with debugee VM.
 *
 * @see Debugee
 * @see DebugeeBinder
 */
final public class Binder extends DebugeeBinder {

    /**
     * Default message prefix for <code>Binder</code> object.
     */
    public static final String LOG_PREFIX = "binder> ";

    /**
     * Get version string.
     */
    public static String getVersion () {
        return "@(#)Binder.java %I% %E%";
    }

    // -------------------------------------------------- //

    /**
     * Handler of command line arguments.
     */
    private ArgumentHandler argumentHandler = null;

    /**
     * Return <code>argumentHandler</code> of this binder.
     */
    public ArgumentHandler getArgumentHandler() {
        return argumentHandler;
    }

    // -------------------------------------------------- //

    /**
     * Make new <code>Binder</code> object with specified
     * <code>argumentHandler</code> and <code>log</code>.
     */
    public Binder (ArgumentHandler argumentHandler, Log log) {
        super(argumentHandler, log);
        this.argumentHandler = argumentHandler;
    }

    // -------------------------------------------------- //

    /**
     * Start debugee VM and establish JDWP connection to it.
     */
    public Debugee bindToDebugee (String classToExecute) {

        Debugee debugee = null;

        prepareForPipeConnection(argumentHandler);

        if (argumentHandler.isLaunchedRemotely()) {
            connectToBindServer(classToExecute);
            debugee = launchDebugee(classToExecute);
        } else {
            debugee = launchDebugee(classToExecute);
            debugee.redirectOutput(log);
        }

        Finalizer finalizer = new Finalizer(debugee);
        finalizer.activate();

        Transport transport = debugee.connect();

        return debugee;
    }

    /**
     * Launch debugee VM for specified class.
     */
    public Debugee launchDebugee (String classToExecute) {

        try {

            if (argumentHandler.isLaunchedLocally()) {
                LocalLaunchedDebugee debugee = new LocalLaunchedDebugee(this);
                String address = debugee.prepareTransport(argumentHandler);
                if (address == null)
                    address = makeTransportAddress();
                String[] argsArray = makeCommandLineArgs(classToExecute, address);
                debugee.launch(argsArray);
                return debugee;
            }

            if (argumentHandler.isLaunchedRemotely()) {
                RemoteLaunchedDebugee debugee = new RemoteLaunchedDebugee(this);
                String address = debugee.prepareTransport(argumentHandler);
                if (address == null)
                    address = makeTransportAddress();
                String[] argsArray = makeCommandLineArgs(classToExecute, address);
                debugee.launch(argsArray);
                return debugee;
            }

            if (argumentHandler.isLaunchedManually()) {
                ManualLaunchedDebugee debugee = new ManualLaunchedDebugee(this);
                String address = debugee.prepareTransport(argumentHandler);
                if (address == null)
                    address = makeTransportAddress();
                String cmdLine = makeCommandLineString(classToExecute, address, "\"");
                debugee.launch(cmdLine);
                return debugee;
            }

            throw new TestBug("Unexpected launching mode: "
                            + argumentHandler.getLaunchMode());
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught exception while launching debugee:\n\t" + e);
        }
    }

}

/**
 * Mirror of locally launched debugee.
 */
final class LocalLaunchedDebugee extends Debugee {

    /** Enwrap the existing <code>VM</code> mirror. */
    public LocalLaunchedDebugee (Binder binder) {
        super(binder);
        checkTermination = true;
    }

    // ---------------------------------------------- //

    public void launch(String[] args) throws IOException {
        String cmdLine = ArgumentHandler.joinArguments(args, "\"");
        display("Starting java process:\n" + cmdLine);
        process = binder.launchProcess(args);
    }

    /** Return exit status of the debugee VM. */
    public int getStatus () {
        return process.exitValue();
    }

    /** Check whether the debugee VM has been terminated. */
    public boolean terminated () {
        if (process == null)
            return true;

        try {
            int value = process.exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    // ---------------------------------------------- //

    /** Kill the debugee VM. */
    protected void killDebugee () {
        super.killDebugee();
        if (!terminated()) {
            log.display("Killing debugee VM process");
            process.destroy();
        }
    }

    /** Wait until the debugee VM shutdown or crash. */
    protected int waitForDebugee () throws InterruptedException {
        return process.waitFor();
    }

    /** Get a pipe to write to the debugee's stdin stream. */
    protected OutputStream getInPipe () {
        return process.getOutputStream();
    }

    /** Get a pipe to read the debugee's stdout stream. */
    protected InputStream getOutPipe () {
        return process.getInputStream();
    }

    /** Get a pipe to read the debugee's stderr stream. */
    protected InputStream getErrPipe () {
        return process.getErrorStream();
    }
}


/**
 * Mirror of remotely launched debugee.
 */
final class RemoteLaunchedDebugee extends Debugee {

    /** Enwrap the existing <code>VM</code> mirror. */
    public RemoteLaunchedDebugee (Binder binder) {
        super(binder);
    }

    // ---------------------------------------------- //

    public void launch(String[] args) throws IOException {
        String cmdLine = ArgumentHandler.joinArguments(args, "\"");
        display("Starting remote java process:\n" + cmdLine);
        binder.launchRemoteProcess(args);
    }

    /** Return exit status of the debugee VM. */
    public int getStatus () {
        return binder.getRemoteProcessStatus();
    }

    /** Check whether the debugee VM has been terminated. */
    public boolean terminated () {
        return binder.isRemoteProcessTerminated();
    }

    // ---------------------------------------------- //

    /** Kill the debugee VM. */
    protected void killDebugee () {
        super.killDebugee();
        if (!terminated()) {
            log.display("Killing debugee VM process");
            binder.killRemoteProcess();
        }
    }

    /** Wait until the debugee VM shutdown or crash. */
    protected int waitForDebugee () {
        return binder.waitForRemoteProcess();
    }

    /** Get a pipe to write to the debugee's stdin stream. */
    protected OutputStream getInPipe () {
        return null;
    }

    /** Get a pipe to read the debugee's stdout stream. */
    protected InputStream getOutPipe () {
        return null;
    }

    /** Get a pipe to read the debugee's stderr stream. */
    protected InputStream getErrPipe () {
        return null;
    }

    public void redirectStdout(OutputStream out) {
    }

    public void redirectStdout(Log log, String prefix) {
    }

    public void redirectStderr(OutputStream out) {
    }

    public void redirectStderr(Log log, String prefix) {
    }
}


/**
 * Mirror of manually launched debugee.
 */
final class ManualLaunchedDebugee extends Debugee {

    private int exitCode = 0;
    private boolean finished = false;
    private static BufferedReader bin = new BufferedReader(new InputStreamReader(System.in));

    /** Enwrap the existing <code>VM</code> mirror. */
    public ManualLaunchedDebugee (Binder binder) {
        super(binder);
    }

    // ---------------------------------------------- //

    public void launch(String commandLine) throws IOException {
        putMessage("Launch target VM using such command line:\n"
                    + commandLine);
        String answer = askQuestion("Has the VM successfully started? (yes/no)", "yes");
        for ( ; ; ) {
            if (answer.equals("yes"))
                break;
            if (answer.equals("no"))
                throw new Failure ("Unable to manually launch debugee VM");
            answer = askQuestion("Wrong answer. Please type yes or no", "yes");
        }
    }

    private void putMessage(String msg) {
        System.out.println("\n>>> " + msg);
    }

    private String askQuestion(String question, String defaultAnswer) {
        try {
            System.out.print("\n>>> " + question);
            System.out.print(" [" + defaultAnswer + "] ");
            System.out.flush();
            String answer = bin.readLine();
            if (answer.equals(""))
                return defaultAnswer;
            return answer;
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Caught exception while reading answer:\n\t" + e);
        }
    }

    /** Return exit status of the debugee VM. */
    public int getStatus () {
        if (! terminated()) {
            throw new Failure("Unable to get status of debugee VM: process still alive");
        }
        return exitCode;
    }

    /** Check whether the debugee VM has been terminated. */
    public boolean terminated () {
        if(! finished) {
            String answer = askQuestion("Has the VM exited?", "no");
            for ( ; ; ) {
                if (answer.equals("no"))
                    return false;
                if (answer.equals("yes")) {
                    finished = true;
                    waitForDebugee();
                    break;
                }
                answer = askQuestion("Wrong answer. Please type yes or no", "yes");
            }
        }
        return finished;
    }

    // ---------------------------------------------- //

    /** Kill the debugee VM. */
    protected void killDebugee () {
        super.killDebugee();
        if (!terminated()) {
            putMessage("Kill launched VM");
            String answer = askQuestion("Has the VM successfully terminated? (yes/no)", "yes");
            for ( ; ; ) {
                if (answer.equals("yes")) {
                    finished = true;
                    break;
                }
                if (answer.equals("no"))
                    throw new Failure ("Unable to manually kill debugee VM");
                answer = askQuestion("Wrong answer. Please type yes or no", "yes");
            }
        }
    }

    /** Wait until the debugee VM shutdown or crash. */
    protected int waitForDebugee () {
        putMessage("Wait for launched VM to exit.");
        String answer = askQuestion("What is VM exit code?", "95");
        for ( ; ; ) {
            try {
                exitCode = Integer.parseInt(answer);
                break;
            } catch (NumberFormatException e) {
                answer = askQuestion("Wrong answer. Please type integer value", "95");
            }
        }
        finished = true;
        return exitCode;
    }

    /** Get a pipe to write to the debugee's stdin stream. */
    protected OutputStream getInPipe () {
        return null;
    }

    /** Get a pipe to read the debugee's stdout stream. */
    protected InputStream getOutPipe () {
        return null;
    }

    /** Get a pipe to read the debugee's stderr stream. */
    protected InputStream getErrPipe () {
        return null;
    }

    public void redirectStdout(OutputStream out) {
    }

    public void redirectStdout(Log log, String prefix) {
    }

    public void redirectStderr(OutputStream out) {
    }

    public void redirectStderr(Log log, String prefix) {
    }

    public void close() {
        try {
            bin.close();
        } catch (IOException e) {
            log.display("WARNING: Caught IOException while closing InputStream");
        }
        bin = null;
        super.close();
    }
}
