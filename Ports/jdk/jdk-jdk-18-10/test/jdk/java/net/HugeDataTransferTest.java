/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8185072
 * @summary network006 times out in many configs in JDK10-hs nightly
 * @run main/othervm/manual HugeDataTransferTest 1
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * This test makes huge number of data transfers between 2 Java virtual machines
 * using the TCP/IP protocol, and checks if those data are transfered correctly.
 * Both client and server VMs run on the same local computer and attach TCP/IP
 * sockets to the local host, or to the loopback domain
 * ``<code>localhost</code>'' (having IP address <code>127.0.0.1</code>).
 *
 * <p>
 * In this test, 128 client/server connections are established. Once a
 * connection is established, client passes a data parcel to server, and server
 * reads that parcel and checks if it is same as expected (byte-to-byte equality
 * is desired). Then server passes (some other) parcel to the client, and client
 * reads and verifies those bytes. This ping-pong game is repeated 128 times;
 * and after that each pair of sockets checks if there are no extra bytes
 * accedentally passed through their connection.
 *
 * <p>
 * Parcels lengths and contents are chosen randomly, and average parcel length
 * is 128 bytes. So totally, each pair of sockets passes ~16Kb of data to each
 * other, and thus ~32Kb of data are transfered by each sockets pair. Totally,
 * ~4Mb of data are transfered by all client/server pairs.
 *
 * @author vtewari
 */
public class HugeDataTransferTest {

    /**
     * Timeout for TCP/IP sockets (currently set to 1 min).
     */
    private static int SO_TIMEOUT;// = 2*60*1000;

    /**
     * Maximal number of connections this test should open simultaneously.
     */
    private final static int MAX_CONNECTIONS = 128;

    /**
     * Check few more connections to make sure that MAX_CONNECTIONS are safe.
     */
    private final static int CONNECTIONS_RESERVE = 10;

    /**
     * The test used to fail with connection reset by peer set to 50. (and once
     * in a three if it was set to 10). So now we set it to MAX_CONNECTIONS
     * (128).
     */
    private final static int BACKLOG_QUEUE_LENGTH = MAX_CONNECTIONS;

    /**
     * Number of parcels to be sent/recieved.
     */
    private final static int DATA_PARCELS = 128;

    /**
     * Maximal length of data parcel to be sent/recieved (it equals to 256 bytes
     * now).
     */
    private final static int MAX_PARCEL = 1 << 8;

    /**
     * Either actually display optional reports or not.
     */
    static private final boolean DEBUG_MODE = false;

    /**
     * How many IP sockets can we open simultaneously? Check if
     * <code>MAX_CONNECTIONS</code> connections can be open simultaneously.
     */
    private static int detectOSLimitation() {
        final int CONNECTIONS_TO_TRY = MAX_CONNECTIONS + CONNECTIONS_RESERVE;
        display("--- Trying to open " + CONNECTIONS_TO_TRY + " connections:");

        InetAddress address;
        ServerSocket serverSocket;
        try {
            address = InetAddress.getLocalHost();
            int anyPort = 0;
            int defaultBacklog = BACKLOG_QUEUE_LENGTH;
            serverSocket = new ServerSocket(anyPort, defaultBacklog, address);
        } catch (IOException ioe) {
            throw new Error("FATAL error while loading the test: " + ioe);
        }
        display(serverSocket.toString());

        Socket server[] = new Socket[CONNECTIONS_TO_TRY];
        Socket client[] = new Socket[CONNECTIONS_TO_TRY];

        int i, port = serverSocket.getLocalPort();
        for (i = 0; i < CONNECTIONS_TO_TRY; i++) {
            try {
                client[i] = new Socket(address, port);
                display(">Open: client[" + i + "] = " + client[i]);
                server[i] = serverSocket.accept();
                display(">Open: server[" + i + "] = " + server[i]);
            } catch (IOException ioe) {
                display(">OOPS! -- failed to open connection #" + i);
                break;
            }
        }
        display("> Could open "
                + (i < CONNECTIONS_TO_TRY ? "only " : "") + i + " connections.");
        display(">Closing them:");
        for (int j = 0; j < i; j++) {
            try {
                server[j].close();
                client[j].close();
            } catch (IOException ioe) {
                throw new Error("FATAL error while loading the test: " + ioe);
            }
        }
        display(">OK.");
        int safeConnections = i - CONNECTIONS_RESERVE;
        if (safeConnections < 1) {
            safeConnections = 1;
        }
        if (safeConnections < MAX_CONNECTIONS) {
            complain("------------------------- CAUTION: -------------------");
            complain("While checking the OS limitations, the test found that");
            complain("only " + i + " TCP/IP socket connections could be safely open");
            complain("simultaneously. However, possibility to open at least");
            complain("" + MAX_CONNECTIONS + "+" + CONNECTIONS_RESERVE
                    + " connections were expected.");
            complain("");
            complain("So, the test will check only " + safeConnections + " connection"
                    + (safeConnections == 1 ? "" : "s") + " which seem");
            complain("safe to be open simultaneously.");
            complain("------------------------------------------------------");
        }
        return safeConnections;
    }

    //----------------------------------------------------------------//
    /**
     * Re-calls to the method <code>run(args[],out)</code> actually performing
     * the test. After <code>run(args[],out)</code> stops, follow JDK-like
     * convention for exit codes. I.e.: stop with exit status 95 if the test has
     * passed, or with status 97 if the test has failed.
     *
     * @see #run(String[],PrintStream)
     */
    public static void main(String args[]) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
        // JCK-like exit status.
    }

    public static int run(String args[], PrintStream out) {
        HugeDataTransferTest.out = out;

        //
        // Get the Internet address of the local machine.
        //
        InetAddress address = null;
        try {
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException exception) {
            complain(exception.toString());
            return 2; // FAILED
        }
        display("Host: " + address);

        //
        // Detect if it is safe to open MAX_CONNETIONS simultaneously:
        //
        final int CONNECTIONS = detectOSLimitation();

        //
        // Assign ServerSocket, and start client VM which should open
        // the prescribed number of CONNECTIONS to that ServerSocket.
        //
        ServerSocket serverSocket;
        try {
            final int anyPort = 0;
            final int defaultBacklog = BACKLOG_QUEUE_LENGTH;
            serverSocket = new ServerSocket(anyPort, defaultBacklog, address);
        } catch (IOException exception) {
            complain("Cannot assign a ServerSocket on: " + address);
            return 2;
        }

        //
        // Start the client process on different VM.
        //
        String jdkPath = System.getProperty("test.jdk");
        Path toolName = Paths.get("bin", "java" + (isWindows() ? ".exe" : ""));
        Path jdkTool = Paths.get(jdkPath, toolName.toString());

        String IPAddress = address.getHostAddress();
        int localPort = serverSocket.getLocalPort();
        String arguments = " " + CONNECTIONS + " " + IPAddress + " " + localPort;
        //String command = args[0] + " " + network006.class.getName() + "$Client " + arguments;
        String command = jdkTool.toAbsolutePath().toString() + " " + Client.class.getName() + " " + arguments;
        try {
            SO_TIMEOUT = Integer.parseInt(args[0]) * 60 * 1000;
        } catch (NumberFormatException e) {
            complain("Wrong timeout argument: " + e);
            return 2;
        }

        Runtime runtime = Runtime.getRuntime();

        Process client = null;
        IORedirector redirectOut = null;
        IORedirector redirectErr = null;

        try {
            // Start clients on different JVM:
            client = runtime.exec(command);

            // Provide clients with access to stderr and stdout:
            InputStream clientOut = client.getInputStream();
            InputStream clientErr = client.getErrorStream();
            redirectOut = new IORedirector(clientOut, DEBUG_MODE ? out : null);
            redirectErr = new IORedirector(clientErr, out);
            redirectOut.start();
            redirectErr.start();

        } catch (IOException exception) {
            complain("Failed to start client: " + exception);
            return 2;
        }
        //
        // Start the server threads (and let them establish connections):
        //

        Server server[] = new Server[CONNECTIONS];
        for (int i = 0; i < CONNECTIONS; i++) {
            server[i] = new Server(serverSocket);
            display("Server #" + i + ": " + server[i]);
            server[i].start();
        }

        //
        // Wait for the servers and the clients:
        //
        boolean testFailed = false;

        try {
            client.waitFor();
            int clientStatus = client.exitValue();
            display("Client VM exitCode=" + clientStatus);

            // Let I/O redirectors to flush:
            if (redirectOut.isAlive()) {
                redirectOut.join();
            }
            if (redirectErr.isAlive()) {
                redirectErr.join();
            }

            // If client has crashed, also terminate the server (to avoid hangup).
            if (clientStatus != 95) {
                complain("Client VM has crashed: exit status=" + clientStatus);
                testFailed = true;
            }

            // Client has finished OK; wait for the server.
            for (int i = 0; i < CONNECTIONS; i++) {
                display("Server: waiting for #" + i);
                if (server[i].isAlive()) {
                    display("Server #" + i + ": (joining...)" + server[i]);
                    server[i].join();
                }
                if (server[i].exception != null) {
                    if (server[i].message != null) {
                        complain("Server #" + i + "(finished): with message:" + server[i].message);
                    }

                    complain("Server #" + i + "(finished): " + server[i].exception);
                    server[i].exception.printStackTrace(out);
                    out.flush();
//                    complain("Server #"+i+": "+server[i].exception.getStackTrace());
                    testFailed = true;
                }
            }

        } catch (InterruptedException exception) {
            complain("Test interrupted: " + exception);
            testFailed = true;
        }

        if (testFailed) {
            complain("Test failed.");
        } else {
            display("Test passed.");
        }
        return testFailed ? 2 : 0;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }
    //----------------------------------------------------------------//
    /**
     * Log stream for error messages and/or (optional) execution trace.
     */
    private static PrintStream out;

    /**
     * Print error message.
     */
    private static synchronized void complain(Object message) {
        out.println("# " + message);
        out.flush();
    }

    ;

    /**
     * Display optional report: comment ca va?
     */
    private static synchronized void display(Object report) {
        if (DEBUG_MODE) {
            out.println(report.toString());
        }
        out.flush(); //todo shouldn't this be inside if??
    }

    ;

    //----------------------------------------------------------------//

    /**
     * Server thread should reply to data parcels sent by Client VM.
     */
    private static class Server extends Thread {

        /**
         * The socket is assigned at the Server instantiation.
         */
        private ServerSocket serverSocket;

        /**
         * The socket is assigned at the Server runtime.
         */
        private Socket socket;

        /**
         * Display the server socket.
         */
        @Override
        public String toString() {

            return "ServerSocket: " + serverSocket.toString();
//                    + " socket: " + socket.toString();
        }

        /**
         * Which port is this socket listening?
         */
        int getPort() {
            return serverSocket.getLocalPort();
        }

        /**
         * Find some free port at the given <code>address</code> and attach new
         * server to hear that port. // lidsten to??
         */
        public Server(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        /**
         * Exception just arisen while the server was working, or
         * <code>null</code> if it was OK with the server.
         */
        Exception exception = null;
        String message = null;

        /**
         * Accept connection, then reply to client's parcels.
         */
        @Override
        public void run() {
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(SO_TIMEOUT);

                InputStream istream = socket.getInputStream();
                OutputStream ostream = socket.getOutputStream();

                Random random = new Random(getPort());

                for (int i = 0; i < DATA_PARCELS; i++) {
                    Parcel etalon = new Parcel(random);
                    message = "reading parcel number " + i;
                    Parcel sample = new Parcel(istream); // read
                    if (!sample.equals(etalon)) {
                        complain("Server thread for port #"
                                + getPort() + " got unexpected parcel:\n"
                                + "sample=" + sample + "\n"
                                + "etalon=" + etalon);
                        throw new TestFailure( //received??
                                "server has read unexpected parcel");
                    }
                    message = "sending parcel number " + i;
                    etalon.send(ostream);
                    ostream.flush();
                }

                int datum = istream.read(); // wait for client close()
                if (datum >= 0) {
                    throw new TestFailure(
                            "server has read ambigous byte: " + datum);
                }

                ostream.close(); // implies: socket.close();

            } catch (Exception oops) {
                exception = oops;
            }
        }
    }

    //----------------------------------------------------------------//
    /**
     * Client VM should send data parcels to Server VM and recieve and verify
     * the server's replies.
     */
    private static class Client extends Thread {

        /**
         * This thread uses the single client socket.
         */
        private Socket socket;

        /**
         * Address and port of this socket.
         */
        @Override
        public String toString() {
            return socket.toString();
        }

        /**
         * Did the thread failed? If yes, what is the failure's reason.
         */
        Exception exception = null;
        String message = null;

        public static java.io.PrintStream complainStream = System.out;
        public static java.io.PrintStream displayStream = System.err;

        /**
         * Connect client socket on the given <code>address</code> and
         * <code>port</code>.
         */
        Client(InetAddress address, int port) throws IOException {
            socket = new Socket(address, port);
            socket.setSoTimeout(SO_TIMEOUT);
        }

        /**
         * What is the port number this socket is listening for?
         */
        int getPort() {
            return socket.getPort();
        }

        /**
         * Establish connection, then read/respond <code>DATA_PARCELS</code>
         * parcels of random data. Set initial seed for pseudo-random numbers
         * generator to the value of the local port number.
         *
         * @see #DATA_PARCELS
         * @see #getPort()
         */
        @Override
        public void run() {
            try {
                InputStream istream = socket.getInputStream();
                OutputStream ostream = socket.getOutputStream();

                Random random = new Random(getPort());
                // suggested by Oleg -- to avoid race conditions
                /* try{
                    Thread.sleep(500);
                }
                catch (java.lang.InterruptedException e)
                {
                }*/

                for (int i = 0; i < DATA_PARCELS; i++) {
                    Parcel etalon = new Parcel(random);
                    message = "sending parcel number: " + i;
                    etalon.send(ostream);
                    ostream.flush();

                    message = "reading parcel number: " + i;
                    Parcel sample = new Parcel(istream); // read
                    if (!sample.equals(etalon)) {
                        complain("Client thread for port #"
                                + getPort() + " got unexpected parcel:\n"
                                + "sample=" + sample + "\n"
                                + "etalon=" + etalon);
                        throw new TestFailure(
                                "parcel context is unexpected to client");
                    }
                }

                if (istream.available() > 0) {
                    int datum = istream.read();
                    throw new TestFailure(
                            "client has read ambigous byte: " + datum);
                }
                ostream.close(); // implies: socket.close()

            } catch (Exception oops) {
                exception = oops;
            }
        }

        /**
         * Establish lots of connections to server socket, attack servers with
         * huge data parcels, and check if they reply correctly. The number of
         * connections to try, the address and port number for the server socket
         * are passed through <code>args[]</code>, like:
         * <pre>
         *    java network006$Client connections_to_try address port
         * </pre>
         */
        public static void main(String args[]) {
            if (DEBUG_MODE) {
                try {
                    String filename = "Client" + ((args.length == 3) ? args[2] : "new");
                    displayStream = new PrintStream(filename + ".out");
                    complainStream = new PrintStream(filename + ".err");
                } catch (FileNotFoundException exception) {
                    complain(exception);
                }

            }

            if (args.length != 3) {
                complain("Client expects 3 paramenets:");
                complain("    java " + Client.class.getName() + " connections_to_try address port");
                exit(1); // FAILED
            }

            int CONNECTIONS = Integer.parseInt(args[0]);
            display("Client VM: will try " + CONNECTIONS + " connections.");
            InetAddress address;
            try {
                address = InetAddress.getByName(args[1]);
            } catch (UnknownHostException exception) {
                address = null;
                complain("Client: cannot find host: \"" + args[1] + "\"");
                exit(4);
            }
            display("Client: host to contact: " + address);
            int port = Integer.parseInt(args[2]);
            display("Client: port to contact: " + port);

            //
            // Establish connections, and start client processes:
            //
            Client client[] = new Client[CONNECTIONS];
            for (int i = 0; i < CONNECTIONS; i++) {
                try {
                    client[i] = new Client(address, port);
                    display("Client #" + i + ": " + client[i]);

                } catch (IOException ioe) {
                    complain("Client #" + i + "(creation): " + ioe);
                    ioe.printStackTrace(complainStream);
                    complainStream.flush();
//                    complain("Client #" + i + "(creation): " + ioe.getStackTrace());
                    exit(3);
                }
            }

            for (int i = 0; i < CONNECTIONS; i++) {
                client[i].start();
            }

            //
            // Wait until testing is not finished:
            //
            int status = 0;
            for (int i = 0; i < CONNECTIONS; i++) {
                display("Client: waiting for #" + i);
                if (client[i].isAlive()) {
                    display("Client #" + i + ": (joining...)" + client[i]);

                    try {
                        client[i].join();
                    } catch (InterruptedException ie) {
                        complain("Client #" + i + ": " + ie);
                        status = 3;
                    }
                }
                if (client[i].exception != null) {
                    if (client[i].message != null) {
                        complain("Client #" + i + "(finished) with message: " + client[i].message);
                    }
                    complain("Client #" + i + "(finished): " + client[i].exception);
                    client[i].exception.printStackTrace(complainStream);
                    complainStream.flush();
                    if (status == 0) {
                        status = 2;
                    }
                }
            }

            exit(status);
        }

        /**
         * Print error message.
         */
        private static synchronized void complain(Object message) {
            complainStream.println("# " + message);
            complainStream.flush();
        }

        /**
         * Display execution trace.
         */
        private static synchronized void display(Object message) {
            if (!DEBUG_MODE) {
                return;
            }
            displayStream.println(message.toString());
            displayStream.flush();
        }

        /**
         * Exit with JCK-like status.
         */
        private static void exit(int exitCode) {
            int status = exitCode + 95;
//          display("Client: exiting with code=" + status);
            System.exit(status);
        }
    }

    /**
     * Two of such threads should redirect <code>out</code> and <code>err</code>
     * streams of client VM.
     */
    private static class IORedirector extends Thread {

        /**
         * Source stream.
         */
        InputStream in;
        /**
         * Destination stream.
         */
        OutputStream out;

        /**
         * Redirect <code>in</code> to <code>out</code>.
         */
        public IORedirector(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        /**
         * Read input stream until the EOF, and write everithing to output
         * stream. If output stream is assigned to <code>null</code>, do not
         * print anything, but read the input stream anywhere.
         */
        @Override
        public void run() {
            try {
                for (;;) {
                    int symbol = in.read();
                    if (symbol < 0) {
                        break; // EOF
                    }
                    if (out != null) {
                        out.write(symbol);
                    }
                }

                if (out != null) {
                    out.flush();
                }

            } catch (IOException exception) {
                throw new TestFailure("IORedirector exception: " + exception);
            }
        }
    }

    //----------------------------------------------------------------//
    /**
     * A data parcel to be sent/recieved between Client VM and Server thread.
     * When data parcel is sent, first 4 bytes are transfered which encode the
     * <code>int</code> number equal to size of the parcel minus 1. I.e.: if
     * number of data bytes in the parcel's contents is <code>N</code>, then the
     * first 4 bytes encode the number <code>N-1</code>. After that, the
     * parcel's contents bytes are transered.
     */
    static class Parcel {

        private final byte[] parcel;

        /**
         * Display all bytes as integer values from 0 to 255; or return
         * ``<tt>null</tt>'' if this Parcel is not yet initialized.
         */
        @Override
        public String toString() {
            if (parcel == null) {
                return "null";
            }
            String s = "{";
            for (int i = 0; i < parcel.length; i++) {
                s += (i > 0 ? ", " : "") + ((int) parcel[i] & 0xFF);
            }
            return s + "}";
        }

        /**
         * Generate new <code>parcel[]</code> array using the given
         * <code>random</code> numbers generator. Client and Server threads
         * should use identical <code>random</code> generators, so that those
         * threads could generate equal data parcels and check the parcel just
         * transfered.
         */
        public Parcel(Random random) {
            int size = random.nextInt(MAX_PARCEL) + 1;
            parcel = new byte[size];
            for (int i = 0; i < size; i++) {
                parcel[i] = (byte) random.nextInt(256);
            }
        }

        ;

        /**
         * Read exactly <code>size</code> bytes from the <code>istream</code>
         * if possible, or throw <code>TestFailure</code> if unexpected end of
         * <code>istream</code> occurs.
         */
        private static byte[] readBytes(int size, InputStream istream)
                throws IOException {

            byte data[] = new byte[size];
            for (int i = 0; i < size; i++) {
                int datum = istream.read();
                if (datum < 0) {
                    throw new TestFailure(
                            "unexpected EOF: have read: " + i + " bytes of " + size);
                }
                data[i] = (byte) datum;
            }
            return data;
        }

        /**
         * Read 4 bytes from <code>istream</code> and threat them to encode size
         * of data parcel following these 4 bytes.
         */
        private static int getSize(InputStream istream) throws IOException {
            byte data[] = readBytes(4, istream);
            int data0 = (int) data[0] & 0xFF;
            int data1 = (int) data[1] & 0xFF;
            int data2 = (int) data[2] & 0xFF;
            int data3 = (int) data[3] & 0xFF;
            int sizeWord = data0 + (data1 << 8) + (data2 << 16) + (data3 << 24);
            int size = sizeWord + 1;
            if (size <= 0) {
                throw new TestFailure("illegal size: " + size);
            }
            return size;
        }

        /**
         * Send 4 bytes encoding actual size of the parcel just to be
         * transfered.
         */
        private static void putSize(OutputStream ostream, int size)
                throws IOException {

            if (size <= 0) {
                throw new TestFailure("illegal size: " + size);
            }

            int sizeWord = size - 1;
            byte data[] = new byte[4];
            data[0] = (byte) sizeWord;
            data[1] = (byte) (sizeWord >> 8);
            data[2] = (byte) (sizeWord >> 16);
            data[3] = (byte) (sizeWord >> 24);
            ostream.write(data);
        }

        /**
         * Recieve data parcel.
         */
        public Parcel(InputStream istream) throws IOException {
            int size = getSize(istream);
            parcel = readBytes(size, istream);
        }

        /**
         * Send <code>this</code> data parcel.
         */
        public void send(OutputStream ostream) throws IOException {
            int size = parcel.length;
            putSize(ostream, size);
            ostream.write(parcel);
        }

        /**
         * Check byte-to-byte equality between <code>this</code> and the
         * <code>other</code> parcels.
         */
        public boolean equals(Parcel other) {
            if (this.parcel.length != other.parcel.length) {
                return false;
            }
            int size = parcel.length;
            for (int i = 0; i < size; i++) {
                if (this.parcel[i] != other.parcel[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Server or Client may throw this exception to report the test failure.
     */
    static class TestFailure extends RuntimeException {

        /**
         * Report particular <code>purpose</code> of the test failure.
         */
        public TestFailure(String purpose) {
            super(purpose);
        }
    }
}
