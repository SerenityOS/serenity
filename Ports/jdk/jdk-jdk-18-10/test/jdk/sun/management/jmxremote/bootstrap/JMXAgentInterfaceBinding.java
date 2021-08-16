/*
 * Copyright (c) 2015, Red Hat Inc
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
 * Tests client connections to the JDK's built-in JMX agent server on the given
 * ports/interface combinations.
 *
 * @see JMXInterfaceBindingTest
 *
 * @author Severin Gehwolf <sgehwolf@redhat.com>
 *
 * Usage:
 *
 * SSL:
 *        java -Dcom.sun.management.jmxremote.ssl.need.client.auth=true \
 *             -Dcom.sun.management.jmxremote.host=127.0.0.1 \
 *             -Dcom.sun.management.jmxremote.port=9111 \
 *             -Dcom.sun.management.jmxremote.rmi.port=9112 \
 *             -Dcom.sun.management.jmxremote.authenticate=false \
 *             -Dcom.sun.management.jmxremote.ssl=true \
 *             -Dcom.sun.management.jmxremote.registry.ssl=true
 *             -Djavax.net.ssl.keyStore=... \
 *             -Djavax.net.ssl.keyStorePassword=... \
 *             JMXAgentInterfaceBinding 127.0.0.1 9111 9112 true
 *
 * Non-SSL:
 *        java -Dcom.sun.management.jmxremote.host=127.0.0.1 \
 *             -Dcom.sun.management.jmxremote.port=9111 \
 *             -Dcom.sun.management.jmxremote.rmi.port=9112 \
 *             -Dcom.sun.management.jmxremote.authenticate=false \
 *             -Dcom.sun.management.jmxremote.ssl=false \
 *             JMXAgentInterfaceBinding 127.0.0.1 9111 9112 false
 *
 */
public class JMXAgentInterfaceBinding {

    private final MainThread mainThread;

    public JMXAgentInterfaceBinding(InetAddress bindAddress,
                                   int jmxPort,
                                   int rmiPort,
                                   boolean useSSL) {
        this.mainThread = new MainThread(bindAddress, jmxPort, rmiPort, useSSL);
    }

    public void startEndpoint() {
        mainThread.start();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Test failed", e);
        }
        if (mainThread.isFailed()) {
            mainThread.rethrowException();
        }
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            throw new RuntimeException(
                    "Test failed. usage: java JMXInterfaceBindingTest <BIND_ADDRESS> <JMX_PORT> <RMI_PORT> {true|false}");
        }
        int jmxPort = parsePortFromString(args[1]);
        int rmiPort = parsePortFromString(args[2]);
        boolean useSSL = Boolean.parseBoolean(args[3]);
        String strBindAddr = args[0];
        System.out.println(
                "DEBUG: Running test for triplet (hostname,jmxPort,rmiPort) = ("
                        + strBindAddr + "," + jmxPort + "," + rmiPort + "), useSSL = " + useSSL);
        InetAddress bindAddress;
        try {
            bindAddress = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Test failed. Unknown ip: " + args[0]);
        }
        JMXAgentInterfaceBinding test = new JMXAgentInterfaceBinding(bindAddress,
                jmxPort, rmiPort, useSSL);
        test.startEndpoint(); // Expect for main test to terminate process
    }

    private static int parsePortFromString(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Invalid port specified. Not an integer! Value was: "
                            + port);
        }
    }

    private static class JMXConnectorThread extends Thread {

        private final String addr;
        private final int jmxPort;
        private final int rmiPort;
        private final boolean useSSL;
        private final CountDownLatch latch;
        private volatile boolean failed;
        private volatile boolean jmxConnectWorked;
        private volatile boolean rmiConnectWorked;

        private JMXConnectorThread(String addr,
                                   int jmxPort,
                                   int rmiPort,
                                   boolean useSSL,
                                   CountDownLatch latch) {
            this.addr = addr;
            this.jmxPort = jmxPort;
            this.rmiPort = rmiPort;
            this.latch = latch;
            this.useSSL = useSSL;
        }

        @Override
        public void run() {
            try {
                connect();
            } catch (IOException e) {
                e.printStackTrace();
                failed = true;
            }
        }

        private void connect() throws IOException {
            System.out.println(
                    "JMXConnectorThread: Attempting JMX connection on: "
                            + addr + " on port " + jmxPort);
            JMXServiceURL url;
            try {
                url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"
                        + addr + ":" + jmxPort + "/jmxrmi");
            } catch (MalformedURLException e) {
                throw new RuntimeException("Test failed.", e);
            }
            Map<String, Object> env = new HashMap<>();
            if (useSSL) {
                SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
                env.put("com.sun.jndi.rmi.factory.socket", csf);
                env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
            }
            // connect and immediately close
            JMXConnector c = JMXConnectorFactory.connect(url, env);
            c.close();
            System.out.println("JMXConnectorThread: connection to JMX worked");
            jmxConnectWorked = true;
            checkRmiSocket();
            latch.countDown(); // signal we are done.
        }

        private void checkRmiSocket() throws IOException {
            Socket rmiConnection;
            if (useSSL) {
                rmiConnection = SSLSocketFactory.getDefault().createSocket();
            } else {
                rmiConnection = new Socket();
            }
            SocketAddress target = new InetSocketAddress(addr, rmiPort);
            rmiConnection.connect(target);
            if (useSSL) {
                ((SSLSocket)rmiConnection).startHandshake();
            }
            System.out.println(
                    "JMXConnectorThread: connection to rmi socket worked host/port = "
                            + addr + "/" + rmiPort);
            rmiConnectWorked = true;
            // Closing the channel without sending any data will cause an
            // java.io.EOFException on the server endpoint. We don't care about this
            // though, since we only want to test if we can connect.
            rmiConnection.close();
        }

        public boolean isFailed() {
            return failed;
        }

        public boolean jmxConnectionWorked() {
            return jmxConnectWorked;
        }

        public boolean rmiConnectionWorked() {
            return rmiConnectWorked;
        }
    }

    private static class MainThread extends Thread {

        private static final String EXP_TERM_MSG_REG = "Exit: ([0-9]+)";
        private static final Pattern EXIT_PATTERN = Pattern.compile(EXP_TERM_MSG_REG);
        private static final String COOP_EXIT = "MainThread: Cooperative Exit";
        private static final int WAIT_FOR_JMX_AGENT_TIMEOUT_MS = 20_000;
        private final String addr;
        private final int jmxPort;
        private final int rmiPort;
        private final boolean useSSL;
        private boolean jmxAgentStarted = false;
        private volatile Exception excptn;

        private MainThread(InetAddress bindAddress, int jmxPort, int rmiPort, boolean useSSL) {
            this.addr = wrapAddress(bindAddress.getHostAddress());
            this.jmxPort = jmxPort;
            this.rmiPort = rmiPort;
            this.useSSL = useSSL;
        }

        @Override
        public void run() {
            try {
                waitUntilReadyForConnections();

                // Wait for termination message
                String actualTerm = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).trim();
                System.err.println("DEBUG: MainThread: actualTerm: '" + actualTerm + "'");
                Matcher matcher = EXIT_PATTERN.matcher(actualTerm);
                if (matcher.matches()) {
                    int expExitCode = Integer.parseInt(matcher.group(1));
                    System.out.println(COOP_EXIT);
                    System.exit(expExitCode); // The main test expects this exit value
                }
            } catch (Exception e) {
                this.excptn = e;
            }
        }

        private void waitUntilReadyForConnections() {
            CountDownLatch latch = new CountDownLatch(1);
            JMXConnectorThread connectionTester = new JMXConnectorThread(
                    addr, jmxPort, rmiPort, useSSL, latch);
            connectionTester.start();
            boolean expired = false;
            try {
                expired = !latch.await(WAIT_FOR_JMX_AGENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                System.out.println(
                        "MainThread: Finished waiting for JMX agent to become available: expired == "
                                + expired);
                jmxAgentStarted = !expired;
            } catch (InterruptedException e) {
                throw new RuntimeException("Test failed", e);
            }
            if (!jmxAgentStarted) {
                throw new RuntimeException(
                        "Test failed. JMX server agents not becoming available.");
            }
            if (connectionTester.isFailed()
                    || !connectionTester.jmxConnectionWorked()
                    || !connectionTester.rmiConnectionWorked()) {
                throw new RuntimeException(
                        "Test failed. JMX agent does not seem ready. See log output for details.");
            }
            // The main test expects this exact message being printed
            System.out.println("MainThread: Ready for connections");
        }

        private boolean isFailed() {
            return excptn != null;
        }

        private void rethrowException() throws RuntimeException {
            throw new RuntimeException(excptn);
        }
    }

    /**
     * Will wrap IPv6 address in '[]'
     */
    static String wrapAddress(String address) {
        if (address.contains(":")) {
            return "[" + address + "]";
        }
        return address;
    }
}
