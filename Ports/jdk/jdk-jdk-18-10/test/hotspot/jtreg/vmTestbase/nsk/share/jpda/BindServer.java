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

package nsk.share.jpda;

import java.io.*;
import java.net.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;

/**
 * <code>BindServer</code> is an utility to perform JPDA tests
 * in remote mode across network.
 * <p>
 * This utility should be started on remote host. It listens for connection
 * from JPDA tests and launches debuggee VM on this host.
 * <p>
 * <code>BindServer</code> works together with <code>Binder</code> used in
 * the tests to incapsulate actions required for launching debuggee VM.
 * See <code>ProcessBinder</code> and <code>DebugeeArgumentHandler</code>
 * to know how run tests in local or remote mode across network or
 * on an single host.
 * <p>
 * <code>BindServer</code> is started on the debuggee host.
 * It recognizes following command line options:
 * <ul>
 *   <li><code>-bind.file=<i>filename</i></code> - configuration file
 *   <li><code>-verbose</code> - print verbose messages
 * </ul>
 * <p>
 * Only required option is <code>-bind.file</code>, which points to the file
 * where pairs of particular pathes are presented as they are seen from
 * both hosts along with some other <code>BindServer</code> options.
 * See <i>execution.html</i> to read more about format of bind-file.
 *
 * @see DebugeeBinder
 * @see DebugeeArgumentHandler
 */
public class BindServer implements Finalizable {

    /** Version of <code>BindServer</code> implementation. */
    public static final long VERSION = 2;

    /** Timeout in milliseconds used for waiting for inner threads. */
    private static long THREAD_TIMEOUT = DebugeeBinder.THREAD_TIMEOUT;      // milliseconds

    private static int PASSED = 0;
    private static int FAILED = 2;
    private static int JCK_BASE = 95;

    private static int TRACE_LEVEL_PACKETS = 10;
    private static int TRACE_LEVEL_THREADS = 20;
    private static int TRACE_LEVEL_ACTIONS = 30;
    private static int TRACE_LEVEL_SOCKETS = 40;
    private static int TRACE_LEVEL_IO = 50;

    private static String pathSeparator = System.getProperty("path.separator");
    private static String fileSeparator = System.getProperty("file.separator");

    private static char pathSeparatorChar = pathSeparator.charAt(0);
    private static char fileSeparatorChar = fileSeparator.charAt(0);

    private static Log log = null;
    private static Log.Logger logger = null;
    private static ArgumentHandler argHandler = null;

    private static String pathConvertions[][] = null;

    private ListeningThread listeningThread = null;

    private int totalRequests = 0;
    private int acceptedRequests = 0;
    private int unauthorizedRequests = 0;
    private int busyRequests = 0;

    /**
     * Start <code>BindServer</code> utility from command line.
     * This method invokes <code>run()</code> and redirects output
     * to <code>System.err</code>.
     *
     * @param argv list of command line arguments
     */
    public static void main (String argv[]) {
        System.exit(run(argv,System.err) + JCK_BASE);
    }

    /**
     * Start <code>BindServer</code> utility from JCK-compatible
     * environment.
     *
     * @param argv list of command line arguments
     * @param out outpur stream for log messages
     *
     * @return FAILED if error occured
     *         PASSED oterwise
     */
    public static int run(String argv[], PrintStream out) {
        return new BindServer().runIt(argv, out);
    }

    /**
     * Perform execution of <code>BindServer</code>.
     * This method handles command line arguments, starts seperate
     * thread for listening connection from test on remote host,
     * and waits for command "exit" from a user.
     * Finally it closes all conections and prints connections
     * statiscs.
     *
     * @param argv list of command line arguments
     * @param out outpur stream for log messages
     *
     * @return FAILED if error occured
     *         PASSED oterwise
     */
    private int runIt(String argv[], PrintStream out) {
        try {
            argHandler = new ArgumentHandler(argv);
        } catch (ArgumentHandler.BadOption e) {
            out.println("ERROR: " + e.getMessage());
            return FAILED;
        }

        if (argHandler.getArguments().length > 0) {
            out.println("ERROR: " + "Too many positional arguments in command line");
            return FAILED;
        }

        log = new Log(out, argHandler);
        log.enableErrorsSummary(false);
        log.enableVerboseOnError(false);
        logger = new Log.Logger(log, "");

        Finalizer bindFinalizer = new Finalizer(this);
        bindFinalizer.activate();

        logger.trace(TRACE_LEVEL_THREADS, "BindServer: starting main thread");

        logger.display("Listening to port: " + argHandler.getBindPortNumber());
        logger.display("Authorizing host: " + argHandler.getDebuggerHost());

        pathConvertions = new String[][] {
            { "TESTED_JAVA_HOME", argHandler.getDebuggerJavaHome(), argHandler.getDebugeeJavaHome() },
            { "TESTBASE", argHandler.getDebuggerTestbase(), argHandler.getDebugeeTestbase() },
            { "WORKDIR", argHandler.getDebuggerWorkDir(), argHandler.getDebugeeWorkDir() }
        };

        logger.display("Translating pathes:");
        for (int i = 0; i < pathConvertions.length; i++) {
            logger.display(pathConvertions[i][0] + ":" +"\n"
                         + "  " + pathConvertions[i][1] + "\n"
                         + "  =>" + "\n"
                         + "  " + pathConvertions[i][2]);
        }

        String windir = argHandler.getDebugeeWinDir();
        if (!(windir == null || windir.equals(""))) {
            logger.display("Using WINDIR: \n"
                         + "  " + argHandler.getDebugeeWinDir());
        }

        BufferedReader stdIn = new BufferedReader(
            new InputStreamReader(System.in));

        listeningThread = new ListeningThread(this);
        listeningThread.bind();
        listeningThread.start();

        System.out.println("\n"
                          + "BindServer started" + "\n"
                          + "Type \"exit\" to shut down BindServer"
                          + "\n");

        for (;;) {
            try {
                String userInput = stdIn.readLine();
                if (userInput == null || userInput.equals("exit")
                        || userInput.equals("quit")) {
                    logger.display("Shutting down BindServer");
                    stdIn.close();
                    stdIn = null;
                    break;
                } else if (userInput.trim().equals("")) {
                    continue;
                } else {
                    System.out.println("ERROR: Unknown command: " + userInput);
                }
            } catch(IOException e) {
                e.printStackTrace(log.getOutStream());
                throw new Failure("Caught exception while reading console command:\n\t"
                                    + e);
            }
        }

        printSummary(System.out);

        logger.trace(TRACE_LEVEL_THREADS, "BindServer: exiting main thread");
        try {
            finalize();
        } catch (Throwable e) {
            e.printStackTrace(log.getOutStream());
            logger.complain("Caught exception while finalization of BindServer:\n\t" + e);
        }

        return PASSED;
    }

    /**
     * Print usefull summary statistics about connections occured.
     *
     * @param out output stream for printing statistics
     */
    private void printSummary(PrintStream out) {
        out.println("\n"
                + "Connections summary:" + "\n"
                + "  Tolal connections:     " + totalRequests + "\n"
                + "  Accepted authorized:   " + acceptedRequests + "\n"
                + "  Rejected unauthorized  " + unauthorizedRequests + "\n"
                + "  Rejected being busy:   " + busyRequests + "\n");
    };

    /**
     * Check if given <code>path</code> starts with the specified prefix taking
     * into account difference between <code>slashChar<code> used in <code>path</code>
     * and <code>fileSeparatorChar</code> used in <code>prefix</code>.
     *
     * @param path path to check
     * @param prefix prefix to compare with
     * @param slashChar file separator used in <code>path</code>
     */
    private static boolean checkPathPrefix(String path, String prefix, char slashChar) {
        int prefixLength = prefix.length();
        if (prefixLength > path.length()) {
            return false;
        }
        for (int i = 0; i < prefixLength; i++) {
            char pathChar = path.charAt(i);
            char prefixChar = prefix.charAt(i);

            if (pathChar != prefixChar) {
                if ((pathChar == slashChar || pathChar == fileSeparatorChar
                         || pathChar == '\\' || pathChar == '/')
                    && (prefixChar == slashChar || prefixChar == fileSeparatorChar
                         || prefixChar == '\\' || prefixChar == '/')) {
                    // do nothing
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Convert given path according to list of prefixes from
     * <code>pathConvertions</code> table.
     *
     * @param path path for converting
     * @param slash file separator used in <code>path</code>
     * @param name path identifier used for error messages
     * @param strict force throwing Failure if path is not matched
     *
     * @return string with the converted path
     *
     * @throws Failure if path does not matched for translation
     */
    private static String convertPath(String path, String slash, String name, boolean strict) {
        if (path == null)
            return null;

        char slashChar = slash.charAt(0);

        for (int i = 0; i < pathConvertions.length; i++) {
            String from = pathConvertions[i][1];
            String to = pathConvertions[i][2];
            if (checkPathPrefix(path, from, slashChar)) {
                return (to + path.substring(from.length())).replace(slashChar, fileSeparatorChar);
            }
        }
        if (strict) {
            throw new Failure("Path not matched for translation " + name + ":\n\t" + path);
        }
        return path;
    }

    /**
     * Convert given list of pathes according to list of prefixes from
     * <code>pathConvertions</code> table by invoking <code>convertPath()</code>
     * for each path from the list.
     *
     * @param list list of pathes for converting
     * @param slash file separator used in pathes
     * @param name path identifier used for error messages
     * @param strict force throwing Failure if some path is not matched
     *
     * @return list of strings with converted pathes
     *
     * @throws Failure if some path does not matched for translation
     *
     * @see #convertPath()
     */
    private static String[] convertPathes(String[] list, String slash, String name, boolean strict) {
        String[] converted = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            converted[i] = convertPath(list[i], slash, name, strict);
        }
        return converted;
    }

    /**
     * Pause current thread for specified amount of time in milliseconds,
     * This method uses <code>Object.wait(long)</code> method as a reliable
     * method which prevents whole VM from suspending.
     *
     * @param millisecs - amount of time in milliseconds
     */
    private static void sleeping(int millisecs) {
        Object obj = new Object();

        synchronized(obj) {
            try {
                obj.wait(millisecs);
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                new Failure("Thread interrupted while sleeping:\n\t" + e);
            }
        }
    }

    /**
     * Wait for given thread finished for specified timeout or
     * interrupt this thread if not finished.
     *
     * @param thr thread to wait for
     * @param millisecs timeout in milliseconds
     */
    private static void waitInterruptThread(Thread thr, long millisecs) {
        if (thr != null) {
            String name = thr.getName();
            try {
                if (thr.isAlive()) {
                    logger.trace(TRACE_LEVEL_THREADS, "Waiting for thread: " + name);
                    thr.join(millisecs);
                }
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                throw new Failure ("Thread interrupted while waiting for another thread:\n\t"
                                     + e);
            } finally {
                if (thr.isAlive()) {
                    logger.trace(TRACE_LEVEL_THREADS, "Interrupting not finished thread: " + name);
                    thr.interrupt();
/*
                    logger.display("Stopping not finished thread: " + thr);
                    thr.stop();
 */
                }
            }
        }
    }

    /**
     * Wait for given thread finished for default timeout
     * <code>THREAD_TIMEOUT</code> and
     * interrupt this thread if not finished.
     *
     * @param thr thread to wait for
     */
    private static void waitInterruptThread(Thread thr) {
        waitInterruptThread(thr, THREAD_TIMEOUT);
    }

    /**
     * Close <code>BindServer</code> by finishing all threads and closing
     * all conections.
     */
    public synchronized void close() {
        if (listeningThread != null) {
            listeningThread.close();
            listeningThread = null;
        }
    }

    /**
     * Make finalization of <code>BindServer</code> object by invoking
     * method <code>close()</code>.
     *
     * @see #close()
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Make finalization of <code>BindServer</code> object at program exit
     * by invoking method <code>finalize()</code>.
     *
     * @see #finalize()
     */
    public void finalizeAtExit() throws Throwable {
        finalize();
        logger.trace(TRACE_LEVEL_THREADS, "BindServer: finalization at exit completed");
    }

///////// Thread listening a TCP/IP socket //////////

    /**
     * An inner thread used for listening connection from remote test
     * and starting separate serving thread for each accepted connection.
     *
     * @see ServingThread
     */
    private static class ListeningThread extends Thread {
        private volatile boolean shouldStop = false;
        private volatile boolean closed = false;

        private BindServer owner = null;
        private volatile ServingThread servingThread = null;
        private volatile int taskCount = 0;

        private ObjectOutputStream socOut = null;
        private ObjectInputStream socIn = null;

        private String autorizedHostName = argHandler.getDebuggerHost();
        private InetAddress autorizedInetAddresses[] = null;
        private int port = argHandler.getBindPortNumber();
        private Socket socket = null;
        private ServerSocket serverSocket = null;
        private InetAddress clientInetAddr = null;
        private String clientHostName = null;
        private SocketConnection connection = null;

        /**
         * Make listening thread for given <code>BindServer</code> object
         * as an owner and bind it to listening port by invoking method
         * <code>bind()</code>.
         *
         * @see bind()
         */
        public ListeningThread(BindServer owner) {
            super("ListeningThread");
            this.owner = owner;
            try {
                autorizedInetAddresses = InetAddress.getAllByName(autorizedHostName);
            } catch (UnknownHostException e) {
                e.printStackTrace(log.getOutStream());
                throw new Failure("Cannot resolve DEBUGGER_HOST value: " + autorizedHostName);
            }
        }

        /**
         * Bind ServerSocket to the specified port.
         */
        public void bind() {
            for (int i = 0; !shouldStop && i < DebugeeBinder.CONNECT_TRIES; i++) {
                try {
                    logger.trace(TRACE_LEVEL_SOCKETS, "ListeningThread: binding to server socket ...");
                    // length of the queue = 2
                    serverSocket = new ServerSocket(port, 2);
                    // timeout for the ServerSocket.accept()
                    serverSocket.setSoTimeout(DebugeeBinder.CONNECT_TRY_DELAY);
                    logger.trace(TRACE_LEVEL_SOCKETS, "ListeningThread: socket bound: " + serverSocket);
                    logger.display("Bound to listening port");
                    return;
                } catch (BindException e) {
                    logger.display("Socket binding try #" + i + " failed:\n\t" + e);
                    sleeping(DebugeeBinder.CONNECT_TRY_DELAY);
                } catch (IOException e) {
                    e.printStackTrace(log.getOutStream());
                    throw new Failure("Caught exception while binding to socket:\n\t"
                                    + e);
                }
            }
            throw new Failure("Unable to bind to socket after "
                + DebugeeBinder.CONNECT_TRIES + " tries");
        }

        /**
         * Accept socket connection from authorized remote host and
         * start separate <code>SrvingThread</code> to handle each connection.
         * Connection from unauthorized hosts or connections made while
         * current connection is alive are rejected.
         *
         * @see ServingThread
         * @see #llowConnection()
         * @see allowServing()
         */
        public void run() {
            String reply = null;

            logger.trace(TRACE_LEVEL_THREADS, "ListeningThread: started");
            logger.display("Listening for connection from remote host");
            while(!(shouldStop || isInterrupted())) {
                try {
                    try {
                        logger.trace(TRACE_LEVEL_SOCKETS, "ListeningThread: waiting for connection from test");
                        socket = serverSocket.accept();
                        logger.trace(TRACE_LEVEL_SOCKETS, "ListeningThread: connection accepted");
                    } catch(InterruptedIOException e) {
//                        logger.trace(TRACE_LEVEL_SOCKETS, "ListeningThread: timeout of waiting for connection from test");
                        continue;
                    }
                    owner.totalRequests++;
                    logger.display("");
                    clientInetAddr = socket.getInetAddress();
                    clientHostName = clientInetAddr.getHostName();
                    logger.display("Connection #" + owner.totalRequests
                                    + " requested from host: " + clientHostName);
                    connection = new SocketConnection(logger, "BindServer");
//                    connection.setPingTimeout(DebugeeBinder.PING_TIMEOUT);
                    connection.setSocket(socket);
                    socket = null;
                    if (allowConnection()) {
                        if (allowServing()) {
                            owner.acceptedRequests++;
                            reply = "host authorized: " + clientHostName;
                            logger.display("Accepting connection #" + owner.acceptedRequests
                                            + ": " + reply);
                            servingThread = new ServingThread(this, connection);
                            servingThread.start();
                            cleanHostConnection();
                        } else {
                            owner.busyRequests++;
                            reply = "BindServer is busy";
                            logger.complain("Rejecting connection #" + owner.busyRequests
                                            + ": " + reply);
                            connection.writeObject(new RequestFailed(reply));
                            closeHostConnection();
                        }
                    } else {
                        owner.unauthorizedRequests++;
                        reply = "host unauthorized: " + clientHostName;
                        logger.complain("Rejecting connection #" + owner.unauthorizedRequests
                                            + ": " + reply);
                        connection.writeObject(new RequestFailed(reply));
                        closeHostConnection();
                    }
                } catch (Exception e) {
                    logger.complain("Caught exception while accepting connection:\n" + e);
                    e.printStackTrace(log.getOutStream());
                }
            }
            logger.trace(TRACE_LEVEL_THREADS, "ListeningThread: exiting");
            closeConnection();
        }

        /**
         * Check if the connection made is from authorized host.
         *
         * @return true if connection is allowed because host authorized
         *         false if connection is rejected because host unauthorized
         */
        private boolean allowConnection() {
            // check if local host from loopback address
            if (autorizedHostName.equals("localhost"))
                return clientInetAddr.isLoopbackAddress();

            // check if equal hostname
            if (autorizedHostName.equals(clientHostName))
                return true;

            // check if equal host address
            for (int i = 0; i < autorizedInetAddresses.length; i++) {
                if (clientInetAddr.equals(autorizedInetAddresses[i])) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check if no current connection exists or it is dead.
         * If current connection presents it will be tested by pinging
         * remote host and aborted if host sends no reply. If an alive
         * connection exists, new connection will be rejected.
         *
         * @return true if no alive connection exists
         *         false otherwise
         */
        private boolean allowServing() {
            if (servingThread == null) {
                return true;
            }
            if (servingThread.done) {
                return true;
            }
            if (!servingThread.isConnectionAlive()) {
                logger.display("# WARNING: Previous connection from remote host is dead: aborting connection");
                servingThread.close();
                servingThread = null;
                return true;
            }

/*
            logger.complain("Previous connection from remote host is alive: starting new connection");
            servingThread = null;
            return true;
 */
            logger.complain("Previous connection from remote host is alive: reject new connection");
            return false;
        }

        /**
         * Wait for this thread finished
         * for specified timeout or interrupt it.
         *
         * @param millis timeout in milliseconds
         */
        public void waitForThread(long millis) {
            shouldStop = true;
            waitInterruptThread(this, millis);
        }

        /**
         * Close socket connection from remote host.
         */
        private void closeHostConnection() {
            if (connection != null) {
                connection.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.complain("Caught IOException while closing socket:\n\t"
                                    + e);
                }
                socket = null;
            }
        }

        /**
         * Assign <null> to connection and socket objects
         * but do not close them.
         */
        private void cleanHostConnection() {
            connection = null;
            socket = null;
        }

        /**
         * Close all connections and sockets.
         */
        private void closeConnection() {
            closeHostConnection();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.complain("Caught IOException while closing ServerSocket:\n\t"
                                    + e);
                }
                serverSocket = null;
            }
        }

        /**
         * Close thread by closing all connections and waiting
         * foor thread finished.
         *
         * @see #closeConnection()
         */
        public synchronized void close() {
            if (closed) {
                return;
            }
            closeHostConnection();
            if (servingThread != null) {
                servingThread.close();
                servingThread = null;
            }
            waitForThread(THREAD_TIMEOUT);
            closeConnection();
            closed = true;
            logger.trace(TRACE_LEVEL_THREADS, "ListeningThread closed");
        }

    } // ListeningThread

///////// Thread working with a communication channel //////////

    /**
     * An internal thread for handling each connection from a test
     * on remote host. It reads requests from test and starts separate
     * <code>LaunchingThread</code> to execute each request.
     *
     * @see LaunchingThread
     */
    private static class ServingThread extends Thread {
        private volatile boolean shouldStop = false;
        private volatile boolean closed = false;
        private volatile boolean done = false;

        private ListeningThread owner = null;
        private LaunchingThread launchingThread = null;

        private SocketConnection connection = null;

        /**
         * Make serving thread with specified input/output connection streams
         * and given <code>Listenerthread</code> as an owner.
         *
         * @param owner owner of this thread
         * @param connection established socket connection with test
         */
        public ServingThread(ListeningThread owner, SocketConnection connection) {
            super("ServingThread");
            this.owner = owner;
            this.connection = connection;
        }

        /**
         * Read requests from socket connection and start <code>LaunchingThread</code>
         * to perform each requested action.
         */
        public void run() {
            logger.trace(TRACE_LEVEL_THREADS, "ServingThread: starting handling requests from debugger");
            try {
                // sending OK(version)
                logger.trace(TRACE_LEVEL_ACTIONS, "ServingThread: sending initial OK(VERSION) to debugger");
                connection.writeObject(new OK(VERSION));

                // receiving TaskID(id)
                logger.trace(TRACE_LEVEL_IO, "ServingThread: waiting for TaskID from debugger");
                Object taskID = connection.readObject();
                logger.trace(TRACE_LEVEL_IO, "ServingThread: received TaskID from debugger: " + taskID);
                if (taskID instanceof TaskID) {
                    String id = ((TaskID)taskID).id;
                    owner.taskCount++;
                    logger.println("[" + owner.taskCount + "/" + owner.owner.totalRequests + "]: " + id);
                } else {
                    throw new Failure("Unexpected TaskID received form debugger: " + taskID);
                }

                // starting launching thread
                launchingThread = new LaunchingThread(this, connection);
                launchingThread.start();

                // receiving and handling requests
                while(!(shouldStop || isInterrupted())) {
                    logger.trace(TRACE_LEVEL_IO, "ServingThread: waiting for request from debugger");
                    Object request = connection.readObject();
                    logger.trace(TRACE_LEVEL_IO, "ServingThread: received request from debugger: " + request);
                    if (request == null) {
                        logger.display("Connection closed");
                        break;
                    } else if (request instanceof Disconnect) {
                        logger.display("Closing connection by request");
                        request = null;
                        break;
                    } else {
                        boolean success = false;
                        long timeToFinish = System.currentTimeMillis() + THREAD_TIMEOUT;
                        while (System.currentTimeMillis() < timeToFinish) {
                            if (launchingThread.doneRequest()) {
                                success = true;
                                logger.trace(TRACE_LEVEL_ACTIONS, "ServingThread: asking launching thread to handle request: " + request);
                                launchingThread.handleRequest(request);
                                break;
                            }
                            try {
                                launchingThread.join(DebugeeBinder.TRY_DELAY);
                            } catch (InterruptedException e) {
                                throw new Failure("ServingThread interrupted while waiting for LaunchingThread:\n\t"
                                                + e);
                            }
                        }
                        if (!success) {
                            logger.complain("Rejecting request because of being busy:\n" + request);
                            connection.writeObject(
                                new RequestFailed("Busy with handling previous request"));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(log.getOutStream());
                logger.complain("Caught exception while handling request:\n\t" + e);
            } finally {
                logger.trace(TRACE_LEVEL_THREADS, "ServingThread: exiting");
                closeConnection();
                done = true;
            }
        }

        /**
         * Check if present socket connection is alive.
         */
        private boolean isConnectionAlive() {
            return (connection != null && connection.isConnected());
        }

        /**
         * Wait for this thread finished
         * for specified timeout or interrupt it.
         *
         * @param millis timeout in milliseconds
         */
        public void waitForThread(long millis) {
            shouldStop = true;
            waitInterruptThread(this, millis);
        }

        /**
         * Close socket connection from remote host.
         */
        private void closeConnection() {
            if (connection != null) {
                connection.close();
            }
            if (launchingThread != null) {
                launchingThread.handleRequest(null);
            }
        }

        /**
         * Close thread closing socket connection and
         * waiting for thread finished.
         */
        public synchronized void close() {
            if (closed) {
                return;
            }
            closeConnection();
            if (launchingThread != null) {
                launchingThread.close();
                launchingThread = null;
            }
            waitForThread(THREAD_TIMEOUT);
            closed = true;
            logger.trace(TRACE_LEVEL_THREADS, "ServingThread closed");
        }

    } // ServingThread

///////// Thread serving a particular Binder's request //////////

    /**
     * An internal thread to execute each request from a test on remote host.
     * Requests are coming from ServingThread by invoking handleRequest(Object)
     * method.
     */
    private static class LaunchingThread extends Thread {
        private volatile boolean shouldStop = false;
        private volatile boolean closed = false;
        public volatile boolean done = false;

        private ServingThread owner = null;
//        private ProcessWaitingThread waitingThread = null;
        private Process process = null;

        private StreamRedirectingThread stdoutRedirectingThread = null;
        private StreamRedirectingThread stderrRedirectingThread = null;

        /** Notification about request occurence. */
        private volatile Object notification = new Object();
        /** Request to execute. */
        private volatile Object request = null;
        /** Socket stream to send replies to. */
        private SocketConnection connection = null;

        /**
         * Make thread for executing requests from a test and
         * send reply.
         *
         * @param owner owner of this thread
         * @connection socket connection for sending replies
         */
        public LaunchingThread(ServingThread owner, SocketConnection connection) {
            super("LaunchingThread");
            this.owner = owner;
            this.connection = connection;
        }

        /**
         * Notify this thread that new request has come.
         *
         * @param request request to execute
         */
        public void handleRequest(Object request) {
            synchronized (notification) {
                this.request = request;
                notification.notifyAll();
            }
        }

        /**
         * Check if request has been executed.
         */
        public boolean doneRequest() {
            return done;
        }

        /**
         * Wait for request notification from <code>ServingThread</code>
         * and execute an action according to the request.
         * Request <i>null</code> means thread should finish.
         */
        public void run() {
            logger.trace(TRACE_LEVEL_THREADS, "LaunchingThread: started to handle request");
            done = true;
            while (!isInterrupted()) {
                // wait for new request notification
                logger.trace(TRACE_LEVEL_ACTIONS, "LaunchingThread: waiting for request");
                synchronized (notification) {
                    try {
                        notification.wait();
                    } catch (InterruptedException e) {
                        logger.complain("LaunchingThread interrupted while waiting for request:\n\t"
                                        + e);
                        break;
                    }
                }

                // execute the request
                try {
                    logger.trace(TRACE_LEVEL_ACTIONS, "LaunchingThread: handling request: " + request);
                    if (request == null) {
                        break;
                    } else if (request instanceof LaunchDebugee) {
                        launchDebugee((LaunchDebugee)request);
                    } else if (request instanceof WaitForDebugee) {
                        waitForDebugee((WaitForDebugee)request);
                    } else if (request instanceof DebugeeExitCode) {
                        debugeeExitCode((DebugeeExitCode)request);
                    } else if (request instanceof KillDebugee) {
                        killDebugee((KillDebugee)request);
                    } else {
                        String reason = "Unknown request: " + request;
                        logger.complain(reason);
                        sendReply(new RequestFailed(reason));
                    }
                } catch (Exception e) {
                    e.printStackTrace(log.getOutStream());
                    logger.complain("Caught exception while handling request:\n\t" + e);
                }
                done = true;
            }
            done = true;
            logger.trace(TRACE_LEVEL_THREADS, "LaunchingThread: exiting");
            closeConnection();
        }

        /**
         * Send given reply to remote test.
         *
         * @param reply reply object to send
         */
        public void sendReply(Object reply) throws IOException {
            connection.writeObject(reply);
        }

        /**
         * Send given output line to remote test.
         *
         * @param reply wrapper object for output line to send
         */
        public void sendStreamMessage(RedirectedStream wrapper) throws IOException {
            logger.trace(TRACE_LEVEL_ACTIONS, "Sending output line wrapper to debugger: " + wrapper);
            if (connection.isConnected()) {
                sendReply(wrapper);
            } else {
                logger.complain("NOT redirected: " + wrapper.line);
            }
        }

        /**
         * Launch two <code>StreamRedirectingThread</code> threads to redirect
         * stdin/stderr output of debuggee VM process via <code>BindServer</code>
         * connection.
         *
         * @param process debuggee VM process
         */
        private void launchStreamRedirectors(Process process) {
            stdoutRedirectingThread =
                new StdoutRedirectingThread(this, process.getInputStream(),
                                            DebugeeProcess.DEBUGEE_STDOUT_LOG_PREFIX);
            stdoutRedirectingThread.start();
            stderrRedirectingThread =
                new StderrRedirectingThread(this, process.getErrorStream(),
                                            DebugeeProcess.DEBUGEE_STDERR_LOG_PREFIX);
            stderrRedirectingThread.start();
        }

        /**
         * Execute request for launching debuggee.
         *
         * @param request request to execute
         */
        private void launchDebugee(LaunchDebugee request) throws IOException {
            logger.trace(TRACE_LEVEL_ACTIONS, "LaunchDebugee: handle request: " + request);

            if (process != null) {
                logger.complain("Unable to launch debuggee: process already launched");
                sendReply(new RequestFailed("Debuggee process already launched"));
                return;
            }

            try {
                String[] cmd = request.cmd;
                cmd[0] = convertPath(cmd[0], request.slash, "TESTED_JAVA_HOME", true);
                for (int i = 1; i < cmd.length; i++) {
                    cmd[i] = convertPath(cmd[i], request.slash, "JAVA_ARGS", false);
                }
                String workDir = convertPath(request.workDir, request.slash, "WORKDIR", true);
                String[] classPathes = convertPathes(request.classPathes, request.slash, "CLASSPATH", true);
                String windir = argHandler.getDebugeeWinDir();

                boolean win = (!(windir == null || windir.equals("")));
                String[] envp = new String[win ? 3 : 1] ;
                envp[0] = "CLASSPATH=" + ArgumentParser.joinArguments(classPathes, "", pathSeparator);
                if (win) {
                    envp[1] = "WINDIR=" + windir;
                    envp[2] = "SystemRoot=" + windir;
                }

                logger.display("Setting environment:\n"
                                + "  " + ArgumentHandler.joinArguments(envp, "", "\n  "));
                logger.display("Setting work dir:\n"
                                + "  " + workDir);
                logger.display("Launching debuggee:\n"
                                + "  " + ArgumentHandler.joinArguments(cmd, "\""));

                process = Runtime.getRuntime().exec(cmd, envp, new File(workDir));
                logger.display("  debuggee launched successfully");

                launchStreamRedirectors(process);
            } catch (Exception e) {
                if (!(e instanceof Failure)) {
                    e.printStackTrace(log.getOutStream());
                }
                logger.complain("Caught exception while launching debuggee:\n\t" + e);
                sendReply(new CaughtException(e));
                return;
            }

            sendReply(new OK());
        }

        /**
         * Execute request for waiting for debuggee exited.
         *
         * @param request request to execute
         */
        private void waitForDebugee(WaitForDebugee request) throws IOException {
            logger.trace(TRACE_LEVEL_ACTIONS, "WaitForDebugee: handle request: " + request);

            if (process == null) {
                String reply = "No debuggee process to wait for";
                logger.complain(reply);
                sendReply(new RequestFailed(reply));
                return;
            }

            logger.display("Waiting for debuggee to exit");
/*
            // because timeout is not supported now
            // we do not use separate thread for waiting for process
            // and so following lines are commented out

            waitingThread = new ProcessWaitingThread();
            logger.trace(TRACE_LEVEL_ACTIONS, "LaunchingThread: starting thread for waiting for debugee process");
            waitingThread.start();
            try {
                waitingThread.join(request.timeout);
                if (waitingThread.isAlive()) {
                    String reply = "Timeout exceeded while waiting for debuggee to exit";
                    logger.complain(reply);
                    waitingThread.interrupt();
                    sendReply(socOut, new RequestFailed(reply));
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                logger.complain("Caught exception while waiting for debuggee:\n\t" + e);
                sendReply(new CaughtException(e));
                return;
            }
            int exitStatus = waitingThread.exitStatus;
            waitingThread = null;
 */
            int exitStatus;
            try {
                exitStatus = process.waitFor();
                waitForRedirectors(THREAD_TIMEOUT);
                process.destroy();
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                logger.complain("Caught exception while waiting for debuggee process to exit:\n\t"
                                + e);
                sendReply(new CaughtException(e));
                return;
            }
            logger.display("  debuggee exited with exit status: " + exitStatus);
            sendReply(new OK(exitStatus));
        }

        /**
         * Execute request for returning debuggee exit code.
         *
         * @param request request to execute
         */
        private void debugeeExitCode(DebugeeExitCode request) throws IOException {
            logger.trace(TRACE_LEVEL_ACTIONS, "DebugeeExitCode: handle request: " + request);

            if (process == null) {
                String reply = "No debuggee process to get exit code for";
                logger.complain(reply);
                sendReply(new RequestFailed(reply));
                return;
            }

            int exitStatus = 0;
            try {
                exitStatus = process.exitValue();
            } catch (IllegalThreadStateException e) {
                logger.display("# WARNING: Caught exception while getting exit status of debuggee:\n\t"
                                + e);
                sendReply(new CaughtException(e));
                return;
            }
            logger.trace(TRACE_LEVEL_ACTIONS, "DebugeeExitCode: return debuggee exit status: " + exitStatus);
            sendReply(new OK(exitStatus));
        }

        /**
         * Execute request for unconditional terminating debuggee process.
         *
         * @param request request to execute
         */
        private void killDebugee(KillDebugee request) throws IOException {
            logger.trace(TRACE_LEVEL_ACTIONS, "killDebugee: handle request: " + request);

            if (process == null) {
                String reply = "No debuggee process to kill";
                logger.complain(reply);
                sendReply(new RequestFailed(reply));
                return;
            }

            logger.trace(TRACE_LEVEL_ACTIONS, "killDebugee: killing debuggee process");
            process.destroy();

            logger.trace(TRACE_LEVEL_ACTIONS, "killDebugee: debuggee process killed");
            sendReply(new OK());
        }

        /**
         * Terminate debigee VM process if still alive.
         */
        private void terminateDebugeeAtExit() {
            if (process != null) {
                logger.trace(TRACE_LEVEL_ACTIONS, "Checking that debuggee process has exited correctly");
                try {
                    int value = process.exitValue();
                } catch (IllegalThreadStateException e) {
                    logger.complain("Debuggee process has not exited correctly: trying to kill it");
                    process.destroy();
                    try {
                        int value = process.exitValue();
                    } catch (IllegalThreadStateException ie) {
                        logger.complain("Debuggee process is alive after killing it");
                    }
                    process = null;
                    return;
                }
                logger.trace(TRACE_LEVEL_ACTIONS, "Debuggee process has exited correctly");
            }
        }

        /**
         * Wait for stream redirecting threads finished
         * for specified timeout.
         *
         * @param millis timeout in milliseconds
         */
        private void waitForRedirectors(long millis) {
            try {
                if (stdoutRedirectingThread != null) {
                    stdoutRedirectingThread.join(millis);
                }
                if (stderrRedirectingThread != null) {
                    stderrRedirectingThread.join(millis);
                }
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                logger.complain("Caught exception while waiting for debuggee process exited:\n\t"
                                + e);
            }
        }

        /**
         * Wait for this thread finished
         * for specified timeout or interrupt it.
         *
         * @param millis timeout in milliseconds
         */
        public void waitForThread(long millis) {
            shouldStop = true;
            handleRequest(null);
            waitInterruptThread(this, millis);
        }

        /**
         * Close connection with debuggee.
         */
        public void closeConnection() {
            // no connections to close
        }

        /**
         * Close thread by closing all connections with debuggee,
         * finishing all redirectors and wait for thread finished.
         */
        public synchronized void close() {
            if (closed) {
                return;
            }
            closeConnection();
            terminateDebugeeAtExit();
            if (stdoutRedirectingThread != null) {
                stdoutRedirectingThread.close();
                stdoutRedirectingThread = null;
            }
            if (stderrRedirectingThread != null) {
                stderrRedirectingThread.close();
                stderrRedirectingThread = null;
            }
            waitForThread(THREAD_TIMEOUT);
            closed = true;
            logger.trace(TRACE_LEVEL_THREADS, "LaunchingThread closed");
        }

        /**
         * An inner thread for waiting for debuggee process exited
         * and saving its exit status. (currently not used)
         */
/*
        private class ProcessWaitingThread extends Thread {
            int exitStatus = 0;

            ProcessWaitingThread() {
                super("ProcessWaitingThread");
            }

            public void run() {
                logger.trace(TRACE_LEVEL_THREADS, "ProcessWaitingThread: starting waiting for process");
                try {
                    exitStatus = process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace(log.getOutStream());
                    logger.complain("Caught exception while waiting for debuggee process:\n\t"
                                    + e);
                }
                logger.trace(TRACE_LEVEL_ACTIONS, "ProcessWaitingThread: process finished with status: " + exitStatus);
                logger.trace(TRACE_LEVEL_THREADS, "ProcessWaitingThread: exiting");
            }

            public synchronized void close() {
                logger.trace(TRACE_LEVEL_THREADS, "ProcessWaitingThread closed");
            }

        } // ProcessWaitingThread
 */
    } // LaunchingThread

///////// Redirecting threads /////////

    /**
     * An abstract base class for internal threads which redirects stderr/stdout
     * output from debuggee process via <code>BindServer</code> connection.
     * <p>
     * Two derived classes will redirect <i>stderr</i> or </i>stdout</i> stream
     * by enwrapping stream line by <code>DebugeeStderr</code> or
     * <code>DebugeeStderr</code> objects. They should implement only one
     * abstract method <code>enwrapLine(String)</code> to make the difference.
     */
    public static abstract class StreamRedirectingThread extends Thread {
        private volatile boolean shouldStop = false;
        private volatile boolean closed = false;

        private LaunchingThread owner = null;

        private BufferedReader bin = null;
        private String prefix = null;

        /**
         * Make a thread to enwrap and redirect lines from specified
         * input stream with given prefix.
         *
         * @param owner owner of this thread
         * @param is input stream to redirect lines from
         * @param prefix prefix to add to each line
         */
        public StreamRedirectingThread(LaunchingThread owner, InputStream is, String prefix) {
            super("StreamRedirectingThread");
            this.prefix = prefix;
            this.owner = owner;
            bin = new BufferedReader(new InputStreamReader(is));
        }

        /**
         * Read lines from an input stream, enwrap them, and send to remote
         * test via <code>BindServer</code> connection.
         */
        public void run() {
            logger.trace(TRACE_LEVEL_THREADS, "StreamRedirectingThread: starting redirect output stream");
            try {
                String line;
                logger.trace(TRACE_LEVEL_IO, "StreamRedirectingThread: waiting for line from debuggee output");
                while(!shouldStop) {
                    line = bin.readLine();
                    if (line == null)
                        break;
                    owner.sendStreamMessage(enwrapLine(prefix + line));
                }
            } catch (EOFException e) {
                logger.display("Debuggee output stream closed by process");
            } catch (IOException e) {
                e.printStackTrace(log.getOutStream());
                logger.display("# WARNING: Connection to debuggee output stream aborted:\n\t" + e);
            } catch (Exception e) {
                e.printStackTrace(log.getOutStream());
                logger.complain("Caught exception while redirecting debuggee output stream:\n\t"
                                + e);
            }
            logger.trace(TRACE_LEVEL_THREADS, "StreamRedirectingThread: exiting");
            closeConnection();
        }

        /**
         * Envrap output line by the appropriate wrapper.
         * @param line line to enwrap
         */
        protected abstract RedirectedStream enwrapLine(String line);

        /**
         * Wait for this thread finished or interrupt it.
         *
         * @param millis timeout in milliseconds
         */
        public void waitForThread(long millis) {
            shouldStop = true;
            waitInterruptThread(this, millis);
        }

        /**
         * Close redirected process output stream.
         */
        public void closeConnection() {
            if (closed) {
                return;
            }
            if (bin != null) {
                try {
                    bin.close();
                } catch (IOException e) {
                    e.printStackTrace(log.getOutStream());
                    logger.complain("Caught exception while closing debuggee output stream:\n\t"
                                    + e);
                }
                bin = null;
            }
            closed = true;
            logger.trace(TRACE_LEVEL_THREADS, "StreamRedirectingThread closed");
        }

        /**
         * Close thread by waiting redirected stream closed
         * and finish the thread.
         */
        public synchronized void close() {
            if (closed) {
                return;
            }
            waitForThread(THREAD_TIMEOUT);
            closeConnection();
            closed = true;
            logger.trace(TRACE_LEVEL_THREADS, "StreamRedirectingThread closed");
        }

    } // StreamRedirectingThread

    /**
     * Particalar case of <code>StreamRedirectingThread</code> to redirect
     * <i>stderr</i> stream by enwrapping lines into <code>DebugeeStderr</code>
     * objects.
     */
    private static class StderrRedirectingThread extends StreamRedirectingThread {

        /**
         * Make a thread to redirect <i>stderr</i> output stream.
         */
        StderrRedirectingThread(LaunchingThread owner, InputStream is, String prefix) {
            super(owner, is, prefix);
            setName("StderrRedirectingThread");
        }

        /**
         * Enwrap given line into <code>DebugeeStderr</code> object.
         */
        protected RedirectedStream enwrapLine(String line) {
            return new DebugeeStderr(line);
        }

    }

    /**
     * Particalar case of <code>StreamRedirectingThread</code> to redirect
     * <i>stdout</i> stream by enwrapping lines into <code>DebugeeStdout</code>
     * objects.
     */
    private static class StdoutRedirectingThread extends StreamRedirectingThread {

        /**
         * Make a thread to redirect <i>stdout</i> output stream.
         */
        StdoutRedirectingThread(LaunchingThread owner, InputStream is, String prefix) {
            super(owner, is, prefix);
            setName("StdoutRedirectingThread");
        }

        /**
         * Enwrap given line into <code>DebugeeStdout</code> object.
         */
        protected RedirectedStream enwrapLine(String line) {
            return new DebugeeStdout(line);
        }

    }

///////// BinderServer's packets //////////

    /**
     * Base serializable object to transmit request or reply
     * via <code>BindServer</code> connection.
     */
    public static class Packet implements Serializable {}

    ///////// Binder's requests //////////

    /**
     * Base class to represent request to <code>BindServer</code>.
     */
    public static abstract class Request extends Packet {}

    /**
     * This class implements task identification command.
     */
    public static class TaskID extends Request {
        public String id;

        public TaskID(String id) {
            this.id = id;
        }

        public String toString() {
            return "TaskID: id=" + id;
        }
    }

    /**
     * This class implements a request for launching a debugee.
     */
    public static class LaunchDebugee extends Request {
        public String slash;         // slash symbol used on debugger host
        public String[] cmd;         // command line arguments as seen on debugger host
        public String workDir;       // path to working directory as seen on debugger host
        public String[] classPathes; // list of class pathes as seen on debugger host

        public LaunchDebugee(String[] cmd, String slash, String workDir,
                            String[] pathes, String[] classPathes,
                            String[] libPathes) {
            this.cmd = cmd;
            this.slash = slash;
            this.workDir = workDir;
            this.classPathes = classPathes;
        }

        public String toString() {
            return "LaunchDebugee:"
                + "\n\tcommand=" + ArgumentParser.joinArguments(cmd, "\"")
                + "\n\tWORKDIR=" + workDir
                + "\n\tCLASSPATH=" + ArgumentParser.joinArguments(classPathes, "", ":")
                + "\n\tslash=" + slash;
        }
    }

    /**
     * This class implements a request for waiting for debugee
     * termination.
     */
    public static class WaitForDebugee extends Request {
        public long timeout = 0; // timeout in minutes for waiting

        public WaitForDebugee(long value) {
            timeout = value;
        }

        public String toString() {
            return "WaitForDebugee: timeout=" + timeout;
        }
    }

    /**
     * This class implements a request for exit code of
     * debugee process.
     */
    public static class DebugeeExitCode extends Request {
        public String toString() {
            return "SebugeeExitCode";
        }
    }

    /**
     * This class implements a request for killing debugee process.
     */
    public static class KillDebugee extends Request {
        public String toString() {
            return "KillDebugee";
        }
    }

    /**
     * This class implements a request to disconnect connection with test.
     */
    public static class Disconnect extends Request {
        public String toString() {
            return "Disconnect";
        }
    }

    ///////// BindServer's responses //////////

    /**
     * Base class to represent response from <code>BindServer</code>.
     */
    public static abstract class Response extends Packet {}

    /**
     * This class implements a response that a previoulsy received
     * request has been successfully performed.
     */
    public static class OK extends Response {
        public long info = BindServer.VERSION; // optional additional info

        public OK() {
        }

        public OK(long value) {
            info = value;
        }

        public String toString() {
            return "OK(" + info + ")";
        }
    }

    /**
     * This class implements a response that the BindServer is
     * unable to serve a previoulsy received request.
     */
    public static class RequestFailed extends Response {
        public String reason; // the short explanation of failure

        public RequestFailed(String reason) {
            this.reason = reason;
        }

        public String toString() {
            return "RequestFailed(" + reason + ")";
        }
    }

    /**
     * This class implements a response that the BindServer is
     * unable to serve a previoulsy received request because of
     * caught exception.
     */
    public static class CaughtException extends RequestFailed {
        public CaughtException(Exception cause) {
            super("Caught exception: " + cause);
        }
    }

    ///////// Wrappers for redirected messages //////////

    /**
     * Base class to represent wrappers for redirected streams.
     */
    public static class RedirectedStream extends Packet {
        public String line; // line containing line from redirected stream

        public RedirectedStream(String str) {
            line = str;
        }

        public String toString() {
            return "RedirectedStream(" + line + ")";
        }
    }

    /**
     * This class enwraps redirected line of <i>stdout</i> stream.
     */
    public static class DebugeeStdout extends RedirectedStream {

        public DebugeeStdout(String str) {
            super(str);
        }

        public String toString() {
            return "DebugeeStdout(" + line + ")";
        }
    }

    /**
     * This class enwraps redirected line of <i>stderr</i> stream.
     */
    public static class DebugeeStderr extends RedirectedStream {
        public DebugeeStderr(String str) {
            super(str);
        }

        public String toString() {
            return "DebugeeStderr(" + line + ")";
        }
    }

/////// ArgumentHandler for BindServer command line /////////

    /**
     * This class is used to parse arguments from command line
     * and specified <i>bind-file</i>,
     */
    private static class ArgumentHandler extends ArgumentParser {

        protected Properties fileOptions;

        /**
         * Make parser object for command line arguments.
         *
         * @param args list of command line arguments
         */
        public ArgumentHandler(String[] args) {
            super(args);
        }

        /**
         * Check if given command line option is aloowed.
         *
         * @param option option name
         * @param value option value
         */
        protected boolean checkOption(String option, String value) {
            if (option.equals("bind.file")) {
                // accept any file name
                return true;
            }
            return super.checkOption(option, value);
        }

        /**
         * Check if all recignized options are compatible.
         */
        protected void checkOptions() {
            if (getBindFileName() == null) {
                throw new BadOption("Option -bind.file is requred ");
            }
            super.checkOptions();
        }

        /**
         * Check if value of this option points to a existing directory.
         *
         * @param option option name
         * @param dir option value
         */
        private void checkDir(String option, String dir) {
            File file = new File(dir);
            if (!file.exists()) {
                throw new BadOption(option + " does not exist: " + dir);
            }
            if (!file.isAbsolute()) {
                throw new BadOption(option + " is not absolute pathname: " + dir);
            }
            if (!file.isDirectory()) {
                throw new BadOption(option + " is not directory: " + dir);
            }
        }

        /**
         * Check if option from <i>bind-file</i> is allowed.
         *
         * @param option option name
         * @param value option value
         */
        protected boolean checkAdditionalOption(String option, String value) {

            if (option.equals("DEBUGGER_HOST")) {
                // accept any hostname
                return true;
            }

            if (option.equals("BINDSERVER_PORT")) {
                // accept only integer value
                try {
                    int port = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new Failure("Not integer value of bind-file option " + option
                                        + ": " + value);
                }
                return true;
            }

            if (option.equals("DEBUGGER_TESTED_JAVA_HOME")
                    || option.equals("DEBUGGER_WORKDIR")
                    || option.equals("DEBUGGER_TESTBASE")) {
                if (value == null || value.equals("")) {
                    throw new BadOption("Empty value of bind-file option " + option);
                }
                return true;
            }

            if (option.equals("DEBUGGEE_TESTED_JAVA_HOME")
                    || option.equals("DEBUGGEE_WORKDIR")
                    || option.equals("DEBUGGEE_TESTBASE")) {
                if (value == null || value.equals("")) {
                    throw new BadOption("Empty value of bind-file option " + option);
                }
                checkDir(option, value);
                return true;
            }

            if (option.equals("DEBUGGEE_WINDIR")) {
                if (!(value == null || value.equals(""))) {
                    checkDir(option, value);
                }
                return true;
            }

            return false;
        }

        /**
         * Check if all recignized options form <i>bind-file</i> are compatible.
         */
        protected void checkAdditionalOptions() {

            if (getDebuggerJavaHome() == null) {
                throw new BadOption("Option DEBUGGER_JAVA_HOME missed from bind-file");
            }
            if (getDebuggerWorkDir() == null) {
                throw new BadOption("Option DEBUGGER_WORKDIR missed from bind-file");
            }
            if (getDebuggerTestbase() == null) {
                throw new BadOption("Option DEBUGGER_TESTBASE missed from bind-file");
            }

            if (getDebugeeJavaHome() == null) {
                throw new BadOption("Option DEBUGGEE_JAVA_HOME missed from bind-file");
            }
            if (getDebugeeWorkDir() == null) {
                throw new BadOption("Option DEBUGGEE_WORKDIR missed from bind-file");
            }
            if (getDebugeeTestbase() == null) {
                throw new BadOption("Option DEBUGGEE_TESTBASE missed from bind-file");
            }
        }

        /**
         * Parse options form specified <i>bind-file</i>.
         */
        protected void parseAdditionalOptions() {
            Enumeration keys = fileOptions.keys();
            while (keys.hasMoreElements()) {
                String option = (String)keys.nextElement();
                String value = fileOptions.getProperty(option);
                if (! checkAdditionalOption(option, value)) {
                    throw new BadOption("Unrecognized bind-file option: " + option);
                }
            }
            checkAdditionalOptions();
        }

        /**
         * Parse all options from command line and specified <i>bind-file</i>.
         */
        protected void parseArguments() {
            super.parseArguments();
            String fileName = getBindFileName();
            try {
                FileInputStream bindFile = new FileInputStream(fileName);
                fileOptions = new Properties();
                fileOptions.load(bindFile);
                bindFile.close();
            } catch(FileNotFoundException e) {
                throw new BadOption("Unable to open bind-file " + fileName + ": " + e);
            } catch(IOException e) {
                e.printStackTrace(log.getOutStream());
                throw new Failure("Caught exception while reading bind-file:\n" + e);
            }
            parseAdditionalOptions();
        }

        /** Return name of specified <i>bind-file<i>. */
        public String getBindFileName() {
            return options.getProperty("bind.file");
        }

        /** Return specified debuggee host name . */
        public String getDebuggerHost() {
            return fileOptions.getProperty("DEBUGGER_HOST", "localhost");
        }

        /** Return string representation of port number for <code>BindServer<code> connection. */
        public String getBindPort() {
            return fileOptions.getProperty("BINDSERVER_PORT", "9000");
        }

        /** Return specified port number for <code>BindServer<code> connection. */
        public int getBindPortNumber() {
            try {
                return Integer.parseInt(getBindPort());
            } catch (NumberFormatException e) {
                throw new Failure("Not integer value of BindServer port");
            }
        }

        /** Return specified path to tested JDK used for debuggee VM. */
        public String getDebugeeJavaHome() {
            return fileOptions.getProperty("DEBUGGEE_TESTED_JAVA_HOME");
        }

        /** Return specified path to tested JDK used for debugger. */
        public String getDebuggerJavaHome() {
            return fileOptions.getProperty("DEBUGGER_TESTED_JAVA_HOME");
        }

        /** Return specified path to working dir from debuggee host. */
        public String getDebugeeWorkDir() {
            return fileOptions.getProperty("DEBUGGEE_WORKDIR");
        }

        /** Return specified path to working dir from debugger host. */
        public String getDebuggerWorkDir() {
            return fileOptions.getProperty("DEBUGGER_WORKDIR");
        }

        /** Return specified path to testbase dir from debuggee host. */
        public String getDebugeeTestbase() {
            return fileOptions.getProperty("DEBUGGEE_TESTBASE");
        }

        /** Return specified path to testbase dir from debugger host. */
        public String getDebuggerTestbase() {
            return fileOptions.getProperty("DEBUGGER_TESTBASE");
        }

        /** Return specified path to system directory on Wimdows platform. */
        public String getDebugeeWinDir() {
            return fileOptions.getProperty("DEBUGGEE_WINDIR");
        }

    } // ArgumentHandler

} // BindServer
