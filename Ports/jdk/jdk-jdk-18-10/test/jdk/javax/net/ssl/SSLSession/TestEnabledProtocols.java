/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 4416068 4478803 4479736
 * @summary 4273544 JSSE request for function forceV3ClientHello()
 *          4479736 setEnabledProtocols API does not work correctly
 *          4478803 Need APIs to determine the protocol versions used in an SSL
 *                  session
 *          4701722 protocol mismatch exceptions should be consistent between
 *                  SSLv3 and TLSv1
 * @library /javax/net/ssl/templates
 * @run main/othervm TestEnabledProtocols
 * @author Ram Marti
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.Security;
import java.util.Arrays;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class TestEnabledProtocols extends SSLSocketTemplate {

    private final String[] serverProtocols;
    private final String[] clientProtocols;
    private final boolean exceptionExpected;
    private final String selectedProtocol;

    public TestEnabledProtocols(String[] serverProtocols,
            String[] clientProtocols, boolean exceptionExpected,
            String selectedProtocol) {
        this.serverProtocols = serverProtocols;
        this.clientProtocols = clientProtocols;
        this.exceptionExpected = exceptionExpected;
        this.selectedProtocol = selectedProtocol;
        this.serverAddress = InetAddress.getLoopbackAddress();
    }

    @Override
    protected void configureServerSocket(SSLServerSocket sslServerSocket) {
        sslServerSocket.setEnabledProtocols(serverProtocols);
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        try {
            socket.startHandshake();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.write(280);
            in.read();
        } catch (SSLHandshakeException se) {
            // ignore it; this is part of the testing
            // log it for debugging
            System.out.println("Server SSLHandshakeException:");
            se.printStackTrace(System.out);
        } catch (InterruptedIOException ioe) {
            // must have been interrupted, no harm
        } catch (SSLException | SocketException se) {
            // The client side may have closed the socket.
            System.out.println("Server SSLException:");
            se.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("Server exception:");
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void runClientApplication(SSLSocket sslSocket) throws Exception {
        try {
            System.out.println("=== Starting new test run ===");
            showProtocols("server", serverProtocols);
            showProtocols("client", clientProtocols);

            sslSocket.setEnabledProtocols(clientProtocols);
            sslSocket.startHandshake();

            String protocolName = sslSocket.getSession().getProtocol();
            System.out.println("Protocol name after getSession is " +
                protocolName);

            if (protocolName.equals(selectedProtocol)) {
                System.out.println("** Success **");
            } else {
                System.out.println("** FAILURE ** ");
                throw new RuntimeException
                    ("expected protocol " + selectedProtocol +
                     " but using " + protocolName);
            }

            InputStream in = sslSocket.getInputStream();
            OutputStream out = sslSocket.getOutputStream();
            in.read();
            out.write(280);
        } catch (SSLHandshakeException e) {
            if (!exceptionExpected) {
                failTest(e, "Client got UNEXPECTED SSLHandshakeException:");
            } else {
                System.out.println(
                        "Client got expected SSLHandshakeException:");
                e.printStackTrace(System.out);
                System.out.println("** Success **");
            }
        } catch (SSLException | SocketException se) {
            // The server side may have closed the socket.
            if (isConnectionReset(se)) {
                System.out.println("Client SocketException:");
                se.printStackTrace(System.out);
            } else {
                failTest(se, "Client got UNEXPECTED Exception:");
            }

        } catch (Exception e) {
            failTest(e, "Client got UNEXPECTED Exception:");
        }
    }

    private boolean isConnectionReset(IOException ioe) {
        Throwable cause = ioe instanceof SSLException se ? se.getCause() : ioe;
        return cause instanceof SocketException
                && "Connection reset".equals(cause.getMessage());
    }

    private void failTest(Exception e, String message) {
        System.out.println(message);
        e.printStackTrace(System.out);
        System.out.println("** FAILURE **");
        throw new RuntimeException(e);
    }

    public static void main(String[] args) throws Exception {
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        runCase(new String[] { "TLSv1" },
                new String[] { "TLSv1" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1" },
                new String[] { "TLSv1", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "TLSv1" },
                new String[] { "TLSv1", "SSLv3" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1" },
                new String[] { "SSLv3", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "TLSv1" },
                new String[] { "SSLv3" },
                true, null);
        runCase(new String[] { "TLSv1" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                true, null);

        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "TLSv1" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv2Hello" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "SSLv3", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "SSLv3" },
                true, null);
        runCase(new String[] { "TLSv1", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                false, "TLSv1");

        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "TLSv1" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "TLSv1", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "TLSv1", "SSLv3" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "SSLv3", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "TLSv1", "SSLv3" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                true, null);

        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1" },
                true, null);
        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "SSLv3", "SSLv2Hello" },
                false, "SSLv3");
        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                false, "SSLv3");

        runCase(new String[] { "SSLv3" },
                new String[] { "TLSv1" },
                true, null);
        runCase(new String[] { "SSLv3" },
                new String[] { "TLSv1", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "SSLv3" },
                new String[] { "TLSv1", "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "SSLv3" },
                new String[] { "SSLv3", "SSLv2Hello" },
                true, null);
        runCase(new String[] { "SSLv3" },
                new String[] { "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "SSLv3" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                true, null);

        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv2Hello" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3" },
                false, "TLSv1");
        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "SSLv3", "SSLv2Hello" },
                false, "SSLv3");
        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "SSLv3" },
                false, "SSLv3");
        runCase(new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                new String[] { "TLSv1", "SSLv3", "SSLv2Hello" },
                false, "TLSv1");
    }

    private static void runCase(
            String[] serverProtocols,
            String[] clientProtocols,
            boolean exceptionExpected,
            String selectedProtocol) throws Exception {
        new TestEnabledProtocols(
                serverProtocols,
                clientProtocols,
                exceptionExpected,
                selectedProtocol).run();
    }

    private static void showProtocols(String name, String[] protocols) {
        System.out.printf("Enabled protocols on the %s are: %s%n",
                name,
                Arrays.asList(protocols));
    }
}
