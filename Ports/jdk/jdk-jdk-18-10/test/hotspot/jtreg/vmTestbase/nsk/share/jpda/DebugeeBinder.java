/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jpda;

import nsk.share.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class provides debugger with ability to launch
 * debuggee VM and to make connection to it using JDI connector or
 * JDWP transport.
 * <p>
 * The present version of <code>Binder</code> allows
 * to launch debuggee VM either on local machine (<i>local</i> launch mode),
 * or on remote host using <code>BindServer</code> utility
 * (<i>remote</i> launch mode). Also there is an ability to launch
 * debuggee VM manually as a separate process on local or remote machine
 * (<i>manual</i> launch mode), which is usefull for debugging.
 * All these launching modes are specified by command line option
 * <code>-debugee.launch</code> recognized by <code>DebugeeArgumentHandler</code>.
 * <p>
 * <code>Binder</code> also makes it possible to establish TCP/IP
 * connection between debugger and debuggee throw <code>IOPipe</code>
 * object. This connection allows debugger to communicate with debuggee
 * by exchanging with synchronization messages and data.
 * <p>
 * To launch debuggee VM and bind to it use <code>bindToDebugee()</code>
 * method. This method construct mirror of debugee VM, represented by
 * object of <code>DebugeeProcess</code> class or derived. This mirror object
 * allows to control debuggee VM.
 * <p>
 * See also <code>nsk.share.jdi.Binder</code> and <code>nsk.share.jdwp.Binder</code>
 * classes which provide launching and binding to debuggee VM using specific
 * JDI and JDWP features.
 *
 * @see DebugeeArgumentHandler
 * @see DebugeeProcess
 * @see IOPipe
 * @see BindServer
 *
 * @see nsk.share.jdi.Binder
 * @see nsk.share.jdwp.Binder
 */
public class DebugeeBinder extends Log.Logger implements Finalizable {

    private static final boolean IS_WINDOWS = System.getProperty("os.name")
                                                    .toLowerCase()
                                                    .startsWith("win");

    public static int TRY_DELAY = 1000;                     // milliseconds

    public static int CONNECT_TIMEOUT = 1 * 60 * 1000;      // milliseconds
    public static int CONNECT_TRY_DELAY = 2 * 1000;         // milliseconds
    public static int CONNECT_TRIES = CONNECT_TIMEOUT / CONNECT_TRY_DELAY;

    public static int THREAD_TIMEOUT = 2 * CONNECT_TRY_DELAY;  // milliseconds
    public static int PING_TIMEOUT = 30 * 1000;  // milliseconds

    public static int SOCKET_TIMEOUT = 2 * 1000;  // milliseconds
    public static int SOCKET_LINGER = 1;   // milliseconds

    private static int TRACE_LEVEL_PACKETS = 10;
    private static int TRACE_LEVEL_THREADS = 20;
    private static int TRACE_LEVEL_ACTIONS = 30;
    private static int TRACE_LEVEL_SOCKETS = 40;
    private static int TRACE_LEVEL_IO = 50;

    /**
     * Default message prefix for <code>Binder</code> object.
     */
    public static final String LOG_PREFIX = "binder> ";

    private DebugeeArgumentHandler argumentHandler = null;

    /**
     * Get version string.
     */
    public static String getVersion () {
        return "@(#)Binder.java %I% %E%";
    }

    // -------------------------------------------------- //

    private BindServerListener bindServerListener = null;
    private ServerSocket pipeServerSocket = null;

    // -------------------------------------------------- //

    /**
     * Incarnate new Binder obeying the given
     * <code>argumentHandler</code>; and assign the given
     * <code>log</code>.
     */
    public DebugeeBinder (DebugeeArgumentHandler argumentHandler, Log log) {
        super(log, LOG_PREFIX);
        this.argumentHandler = argumentHandler;
        Finalizer finalizer = new Finalizer(this);
        finalizer.activate();
    }

    /**
     * Get argument handler of this binder object.
     */
    DebugeeArgumentHandler getArgumentHandler() {
        return argumentHandler;
    }

    // -------------------------------------------------- //

    /**
     * Wait for given thread finished for THREAD_TIMEOUT timeout and
     * interrupt this thread if not finished.
     *
     * @param thr thread to wait for
     * @param logger to write log messages to
     */
    public static void waitForThread(Thread thr, Log.Logger logger) {
        waitForThread(thr, THREAD_TIMEOUT, logger);
    }

    /**
     * Wait for given thread finished for specified timeout and
     * interrupt this thread if not finished.
     *
     * @param thr thread to wait for
     * @param millisecs timeout in milliseconds
     * @param logger to write log messages to
     */
    public static void waitForThread(Thread thr, long millisecs, Log.Logger logger) {
        if (thr != null) {
            if (thr.isAlive()) {
                try {
                    logger.trace(TRACE_LEVEL_THREADS, "Waiting for thread: " + thr.getName());
                    thr.join(millisecs);
                } catch (InterruptedException e) {
                    e.printStackTrace(logger.getOutStream());
                    throw new Failure ("Thread interrupted while waiting for another thread:\n\t"
                                         + e);
                } finally {
                    if (thr.isAlive()) {
                        logger.trace(TRACE_LEVEL_THREADS, "Interrupting not finished thread: " + thr);
                        thr.interrupt();
                    }
                }
            }
        }
    }


    /**
     * Make preperation for IOPipe connection before starting debugee VM process.
     * May change options in the passed <code>argumentHandler</code>.
     */
    public void prepareForPipeConnection(DebugeeArgumentHandler argumentHandler) {
        if (argumentHandler.isTransportAddressDynamic()) {
            try {
                pipeServerSocket = new ServerSocket();
                pipeServerSocket.setReuseAddress(false);
                pipeServerSocket.bind(null);

            } catch (IOException e) {
                e.printStackTrace(getOutStream());
                throw new Failure("Caught IOException while binding for IOPipe connection: \n\t"
                                + e);
            }

            int port = pipeServerSocket.getLocalPort();
            argumentHandler.setPipePortNumber(port);
        }
    }

    /**
     * Return already bound ServerSocket for IOPipe connection or null.
     */
    protected ServerSocket getPipeServerSocket() {
        return pipeServerSocket;
    }

    /**
     * Close ServerSocket used for IOPipeConnection if any.
     */
    private void closePipeServerSocket() {
        if (pipeServerSocket != null) {
            try {
                pipeServerSocket.close();
            } catch (IOException e) {
                println("# WARNING: Caught IOException while closing ServerSocket used for IOPipe connection: \n\t"
                        + e);
            }
        }
    }

    // -------------------------------------------------- //

    /**
     * Make environment for launching JVM process.
     */
    public String[] makeProcessEnvironment() {
/*
        String env = new String[0];
        return env;
 */
        return null;
    }

    /**
     * Launch process by the specified command line.
     *
     * @throws IOException if I/O error occured while launching process
     */
    public Process launchProcess(String cmdLine) throws IOException {
        String env[] = makeProcessEnvironment();
        return Runtime.getRuntime().exec(cmdLine, env);
    }

    /**
     * Launch process by the arguments array.
     *
     * @throws IOException if I/O error occured while launching process
     */
    public Process launchProcess(String[] args) throws IOException {
        String env[] = makeProcessEnvironment();
        return Runtime.getRuntime().exec(args, env);
    }

    /**
     * Make string representation of debuggee VM transport address according
     * to current command line options.
     */
    public String makeTransportAddress() {
        String address = null;
        if (argumentHandler.isSocketTransport()) {
            if (argumentHandler.isListeningConnector()) {
                address = argumentHandler.getTestHost()
                        + ":" + argumentHandler.getTransportPort();
            } else {
                address = argumentHandler.getTransportPort();
            }
        } else if (argumentHandler.isShmemTransport() ) {
            address = argumentHandler.getTransportSharedName();
        } else {
            throw new TestBug("Undefined transport type: "
                        + argumentHandler.getTransportType());
        }
        return address;
    }

    /**
     * Make command line to launch debugee VM as a string using given quote symbol,
     * using specified <code>transportAddress</code> for JDWP connection.
     */
    public String makeCommandLineString(String classToExecute, String transportAddress, String quote) {
        String[] args = makeCommandLineArgs(classToExecute, transportAddress);
        return ArgumentParser.joinArguments(args, quote);
    }

    /**
     * Make command line to launch debugee VM as a string using given quote symbol.
     */
    public String makeCommandLineString(String classToExecute, String quote) {
        return makeCommandLineString(classToExecute, makeTransportAddress(), quote);
    }

    /**
     * Make command line to launch debugee VM as a string using default quote symbol,
     * using specified <code>transportAddress</code> for JDWP connection.
     */
/*
    public String makeCommandLineString(String classToExecute, String transportAddress) {
        return makeCommandLineString(classToExecute, transportAddress, "\"");
    }
 */

    /**
     * Make command line to launch debugee VM as a string using default quote symbol.
     */
/*
    public String makeCommandLineString(String classToExecute) {
        return makeCommandLineString(classToExecute, makeTransportAddress());
    }
 */

    /**
     * Make command line to launch debugee VM as an array of arguments,
     * using specified <code>transportAddress</code> for JDWP connection.
     */
    public String[] makeCommandLineArgs(String classToExecute, String transportAddress) {
        Vector<String> args = new Vector<String>();

        args.add(argumentHandler.getLaunchExecPath());

        String javaOpts = argumentHandler.getLaunchOptions();
        if (javaOpts != null && javaOpts.length() > 0) {
            StringTokenizer st = new StringTokenizer(javaOpts);

            while (st.hasMoreTokens()) {
                args.add(st.nextToken());
            }
        }

/*
        String classPath = System.getProperty("java.class.path");
        args.add("-classpath")
        args.add(classPath);
 */

        args.add("-Xdebug");

        String server;
        if (argumentHandler.isAttachingConnector()) {
            server = "y";
        } else {
            server = "n";
        }

        String jdwpArgs = "-Xrunjdwp:"
                        + "server=" + server
                        + ",transport=" + argumentHandler.getTransportName()
                        + ",address=" + transportAddress;

        if (! argumentHandler.isDefaultJVMDIStrictMode()) {
            if (argumentHandler.isJVMDIStrictMode())
                jdwpArgs += ",strict=y";
            else
                jdwpArgs += ",strict=n";
        }

        args.add(jdwpArgs);

        if (classToExecute != null) {
            StringTokenizer st = new StringTokenizer(classToExecute);

            while (st.hasMoreTokens()) {
                args.add(st.nextToken());
            }
        }

        String[] rawArgs = argumentHandler.getRawArguments();
        for (int i = 0; i < rawArgs.length; i++) {
            String rawArg = rawArgs[i];
            // " has to be escaped on windows
            if (IS_WINDOWS) {
                rawArg = rawArg.replace("\"", "\\\"");
            }
            args.add(rawArg);
        }

        String[] argsArray = new String[args.size()];
        for (int i = 0; i < args.size(); i++) {
            argsArray[i] = (String) args.elementAt(i);
        }

        return argsArray;
    }

    /**
     * Make command line to launch debugee VM as an array of arguments.
     */
    public String[] makeCommandLineArgs(String classToExecute) {
        return makeCommandLineArgs(classToExecute, makeTransportAddress());
    }

    /**
     * Make connection to remote BindServer and start BindServerListener thread.
     *
     * @throws IOException if I/O error occured while connecting
     */
    public void connectToBindServer(String taskID) {
        if (bindServerListener != null) {
            throw new Failure("Connection to BindServer already exists");
        }
        try {
            bindServerListener = new BindServerListener(this);
            bindServerListener.setDaemon(true);
            bindServerListener.connect(taskID);
            bindServerListener.start();
        } catch (IOException e) {
            e.printStackTrace(getOutStream());
            throw new Failure("Caught exception while connecting to BindServer:\n\t" + e);
        }
    }

    /**
     * Split string into list of substrings using specified separator.
     */
    private static String[] splitString(String givenString, String separator) {
        Vector<String> tmpList = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(givenString, separator);
        while(tokenizer.hasMoreTokens()) {
            tmpList.add(tokenizer.nextToken());
        }
        String[] list = new String[tmpList.size()];
        for (int i = 0; i < tmpList.size(); i++) {
            list[i] = tmpList.elementAt(i);
        }
        return list;
    }

    /**
     * Send command to remote <code>BindServer</code> and receive reply.
     *
     * @throws IOException if I/O error occured while launching process
     */
    public synchronized Object sendRemoteCommand(Object command) {
        try {
            bindServerListener.sendCommand(command);
            Object reply = bindServerListener.getReply();
            return reply;
        } catch (IOException e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("Unexpected exception while sending command to BindServer:\n\t"
                            + e);
        }
    }

    /**
     * Launch remote process using request to <code>BindServer</code>.
     *
     * @throws IOException if I/O error occured
     */
    public void launchRemoteProcess(String[] args) throws IOException {
        String pathSeparator = System.getProperty("path.separator");
        BindServer.LaunchDebugee command =
            new BindServer.LaunchDebugee(args,
                    System.getProperty("file.separator"),
                    System.getProperty("user.dir"),
                    splitString(System.getProperty("java.library.path"), pathSeparator),
                    splitString(System.getProperty("java.class.path"), pathSeparator),
                    splitString(System.getProperty("java.library.path"), pathSeparator));

        Object reply = sendRemoteCommand(command);
        if (reply instanceof BindServer.OK) {
            // do nothing
        } else if (reply instanceof BindServer.RequestFailed) {
            BindServer.RequestFailed castedReply = (BindServer.RequestFailed)reply;
            throw new Failure("BindServer error: " + castedReply.reason);
        } else {
            throw new Failure("Wrong reply from BindServer: " + reply);
        }
    }

    /**
     * Return exit status of the remotely launched process
     * using request to <code>BindServer</code>.
     */
    public int getRemoteProcessStatus () {
        Object reply = sendRemoteCommand(new BindServer.DebugeeExitCode());
        if (reply instanceof BindServer.OK) {
            BindServer.OK castedReply = (BindServer.OK)reply;
            return (int)castedReply.info;
        } else if (reply instanceof BindServer.CaughtException) {
            BindServer.CaughtException castedReply = (BindServer.CaughtException)reply;
            throw new IllegalThreadStateException(castedReply.reason);
        } else if (reply instanceof BindServer.RequestFailed) {
            BindServer.RequestFailed castedReply = (BindServer.RequestFailed)reply;
            throw new Failure("BindServer error: " + castedReply.reason);
        } else {
            throw new Failure("Wrong reply from BindServer: " + reply);
        }
    }

    /**
     * Check whether the remotely launched process has been terminated
     * using request to <code>BindServer</code>.
     */
    public boolean isRemoteProcessTerminated () {
        try {
            int value = getRemoteProcessStatus();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    // ---------------------------------------------- //

    /**
     * Kill the remotely launched process
     * using request to <code>BindServer</code>.
     */
    public void killRemoteProcess () {
        Object reply = sendRemoteCommand(new BindServer.KillDebugee());
        if (reply instanceof BindServer.OK) {
            return;
        } else if (reply instanceof BindServer.RequestFailed) {
            BindServer.RequestFailed castedReply = (BindServer.RequestFailed)reply;
            throw new Failure("BindServer error: " + castedReply.reason);
        } else {
            throw new Failure("Wrong reply from BindServer: " + reply);
        }
    }

    /**
     * Wait until the remotely launched process exits or crashes
     * using request to <code>BindServer</code>.
     */
    public int waitForRemoteProcess () {

        Object reply = sendRemoteCommand(new BindServer.WaitForDebugee(0));
        if (reply instanceof BindServer.OK) {
            BindServer.OK castedReply = (BindServer.OK)reply;
            return (int)castedReply.info;
        } else if (reply instanceof BindServer.RequestFailed) {
            BindServer.RequestFailed castedReply = (BindServer.RequestFailed)reply;
            throw new Failure("BindServer error: " + castedReply.reason);
        } else {
            throw new Failure("Wrong reply from BindServer: " + reply);
        }
    }

    /**
     * Close binder by closing all started threads.
     */
    public void close() {
        if (bindServerListener != null) {
            bindServerListener.close();
        }
        closePipeServerSocket();
    }

    /**
     * Finalize binder by invoking <code>close()</code>.
     *
     * @throws Throwable if any throwable exception is thrown during finalization
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Finalize binder at exit by invoking <code>finalize()</code>.
     *
     * @throws Throwable if any throwable exception is thrown during finalization
     */
    public void finalizeAtExit() throws Throwable {
        finalize();
    }

    /**
     * Separate thread for listening connection from <code>BindServer</code>.
     */
    private class BindServerListener extends Thread {
        private SocketConnection connection = null;
        private Log.Logger logger = null;

        /** List of received responses from <code>BindServer</code>. */
        private LinkedList<BindServer.Response> replies = new LinkedList<BindServer.Response>();

        /**
         * Make thread.
         */
        public BindServerListener(Log.Logger logger) {
            this.logger = logger;
        }

        /**
         * Establish connection to <code>BindServer</code>.
         */
        public void connect(String taskID) throws IOException {
            String host = argumentHandler.getDebugeeHost();
            int port = argumentHandler.getBindPortNumber();
            display("Connecting to BindServer: " + host + ":" + port);
            connection = new SocketConnection(logger, "BindServer");
//            connection.setPingTimeout(DebugeeBinder.PING_TIMEOUT);
            connection.attach(host, port);
            handshake(taskID);
        }

        /**
         * Receive OK(version) from BindServer and check received version number.
         */
        private void handshake(String taskID) {
            // receive OK(version)
            trace(TRACE_LEVEL_ACTIONS, "Waiting for initial OK(version) from BindServer");
            Object reply = connection.readObject();
            trace(TRACE_LEVEL_ACTIONS, "Got initial OK(version) from BindServer: " + reply);
            if (reply instanceof BindServer.RequestFailed) {
                BindServer.RequestFailed castedReply = (BindServer.RequestFailed)reply;
                trace(TRACE_LEVEL_ACTIONS, "Reply is RequestFailed: throw Failure");
                throw new Failure("BindServer error: " + castedReply.reason);
            } else if (reply instanceof BindServer.OK) {
                BindServer.OK castedReply = (BindServer.OK)reply;
                trace(TRACE_LEVEL_ACTIONS, "Reply is OK: check BindServer version");
                if (castedReply.info != BindServer.VERSION) {
                    throw new Failure("Wrong version of BindServer: " + castedReply.info
                                    + " (expected: " + BindServer.VERSION + ")");
                }
                display("Connected to BindServer: version " + castedReply.info);
            } else {
                trace(TRACE_LEVEL_ACTIONS, "Reply is unknown: throw Failure");
                throw new Failure("Wrong reply from BindServer: " + reply);
            }

            // send TaskID(id)
            try {
                trace(TRACE_LEVEL_ACTIONS, "Sending TaskID(id) to BindServer");
                sendCommand(new BindServer.TaskID(taskID));
                trace(TRACE_LEVEL_ACTIONS, "Sent TaskID(id) to BindServer");
            } catch (IOException e) {
                throw new Failure("Caught IOException while sending TaskID(id) to BindServer:\n\t"
                                + e);
            }
        }

        /**
         * Check if thread is connected to <code>BindServer</code>.
         */
        public boolean isConnected() {
            return (connection != null && connection.isConnected());
        }

        /**
         * Send a command to </code>BindServer</code>.
         */
        public synchronized void sendCommand(Object command) throws IOException {
            connection.writeObject(command);
        }

        /**
         * Receive response from <code>BindServer</code>.
         */
        public Object getReply() {
            synchronized (replies) {
                while (replies.isEmpty()) {
                    if (!isConnected()) {
                        throw new Failure("No reply from BindServer: connection lost");
                    }
                    try {
                        replies.wait(TRY_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace(getOutStream());
                        throw new Failure("Thread interrupted while waiting for reply from BindServer:\n\t"
                                        + e);
                    }
                }
                Object reply = replies.removeFirst();
                if (reply == null) {
                    throw new Failure("No reply from BindServer: connection lost");
                }
                return reply;
            }
        }

        /**
         * Add response object to the list of received responses.
         */
        private void addReply(BindServer.Response reply) {
            synchronized (replies) {
                replies.add(reply);
                replies.notifyAll();
            }
        }

        /**
         * Read packets from <code>BindServer<code> connection and
         * notify waiting thread if response or IOPipe message received.
         * Received lines of redirected streams are put into log.
         */
        public void run() {
            trace(TRACE_LEVEL_THREADS, "BindServerListener thread started");
            try {
                for (;;) {
                    Object reply = connection.readObject();
                    if (reply == null) {
                        break;
                    } else if (reply instanceof BindServer.Disconnect) {
                        reply = null;
                        trace(TRACE_LEVEL_ACTIONS, "Packet is Disconnect: close connection");
                        break;
                    } else if (reply instanceof BindServer.RedirectedStream) {
                        BindServer.RedirectedStream castedReply = (BindServer.RedirectedStream)reply;
                        trace(TRACE_LEVEL_ACTIONS, "Packet is RedirectedStream: put message into log");
                        log.println(castedReply.line);
                    } else if (reply instanceof BindServer.Response) {
                        BindServer.Response castedReply = (BindServer.Response)reply;
                        trace(TRACE_LEVEL_ACTIONS, "Packet is reply: notify all threads waiting for reply");
                        addReply(castedReply);
                    } else {
                        trace(TRACE_LEVEL_ACTIONS, "Packet is unknown: throw Failure");
                        throw new Failure("Wrong reply from BindServer: " + reply);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(getOutStream());
                complain("Caught exception while reading packets from BindServer:\n\t" + e);
            } finally {
                closeConnection();
                addReply(null);
                trace(TRACE_LEVEL_THREADS, "BindServerListener thread finished");
            }
        }

        /**
         * Send Disconnect command to </code>BindServer</code>.
         */
        public void disconnect() {
            if (connection == null) return;
            try {
                sendCommand(new BindServer.Disconnect());
            } catch (IOException e) {
                display("Caught IOException while requesting disconnection with BindServer");
            }
        }

        /**
         * Close socket connection.
         */
        public void closeConnection() {
            if (connection != null) {
                connection.close();
            }
        }

        /**
         * Wait for thread finished in the specified timeout or interrupt it.
         */
        public void waitForThread(long millis) {
            DebugeeBinder.waitForThread(this, millis, logger);
        }

        /**
         * Close this thread by waiting for it finishes or interrupt it
         * and close socket connection.
         */
        public void close() {
            disconnect();
            waitForThread(DebugeeBinder.THREAD_TIMEOUT);
            closeConnection();
        }

    } // BindServerListener

} // DebugeeBinder
