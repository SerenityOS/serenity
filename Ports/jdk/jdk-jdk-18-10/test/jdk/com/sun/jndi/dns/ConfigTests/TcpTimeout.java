/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import jtreg.SkippedException;

import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static jdk.test.lib.Utils.adjustTimeout;

/*
 * @test
 * @bug 8228580
 * @summary Tests that we get a DNS response when the UDP DNS server returns a
 *          truncated response and the TCP DNS server does not respond at all
 *          after connect.
 * @library ../lib/
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @run main TcpTimeout
 * @run main TcpTimeout -Dcom.sun.jndi.dns.timeout.initial=5000
 */

public class TcpTimeout extends DNSTestBase {
    private TcpDnsServer tcpDnsServer;

    /* The acceptable variation in timeout measurement. */
    private static final long TOLERANCE = adjustTimeout(5_000);

    /* The acceptable variation of early returns from timed socket operations. */
    private static final long PREMATURE_RETURN = adjustTimeout(100);

    public static void main(String[] args) throws Exception {
        new TcpTimeout().run(args);
    }

    @Override
    public void runTest() throws Exception {
        /* The default timeout value is 1 second, as stated in the
           jdk.naming.dns module docs. */
        long timeout = 1_000;
        var envTimeout = env().get("com.sun.jndi.dns.timeout.initial");
        if (envTimeout != null)
            timeout = Long.parseLong(String.valueOf(envTimeout));

        setContext(new InitialDirContext(env()));

        long startNanos = System.nanoTime();

        /* perform query */
        var attrs = context().getAttributes("host1");

        long elapsed = NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        if (elapsed < timeout - PREMATURE_RETURN || elapsed > timeout + TOLERANCE) {
            throw new RuntimeException(String.format(
                    "elapsed=%s, timeout=%s, TOLERANCE=%s, PREMATURE_RETURN=%s",
                    elapsed, timeout, TOLERANCE, PREMATURE_RETURN));
        }

        DNSTestUtils.debug(attrs);

        /* Note that the returned attributes are truncated and the response
        is not valid. */
        var txtAttr = attrs.get("TXT");
        if (txtAttr == null)
            throw new RuntimeException("TXT attribute missing.");
    }

    @Override
    public void initTest(String[] args) {
        /* We need to bind the TCP server on the same port the UDP server is
        listening to. This may not be possible if that port is in use. Retry
        MAX_RETRIES times relying on UDP port randomness. */
        final int MAX_RETRIES = 5;
        for (int i = 0; i < MAX_RETRIES; i++) {
            super.initTest(args);
            var udpServer = (Server) env().get(DNSTestUtils.TEST_DNS_SERVER_THREAD);
            int port = udpServer.getPort();
            try {
                tcpDnsServer = new TcpDnsServer(port);
                break; // success
            } catch (BindException be) {
                DNSTestUtils.debug("Failed to bind server socket on port " + port
                                           + ", retry no. " + (i + 1) + ", " + be.getMessage());
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception during initTest", ex);
            } finally {
                if (tcpDnsServer == null) { // cleanup behind exceptions
                    super.cleanupTest();
                }
            }
        }

        if (tcpDnsServer == null) {
            throw new SkippedException("Cannot start TCP server after "
                                               + MAX_RETRIES
                                               + " tries, skip the test");
        }
    }

    @Override
    public void cleanupTest() {
        super.cleanupTest();
        if (tcpDnsServer != null)
            tcpDnsServer.stopServer();
    }

    /**
     * A TCP server that accepts a connection and does nothing else: causes read
     * timeout on client side.
     */
    private static class TcpDnsServer {
        final ServerSocket serverSocket;

        TcpDnsServer(int port) throws IOException {
            serverSocket = new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
            System.out.println("TcpDnsServer: listening on port " + port);
        }

        void stopServer() {
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (Exception ignored) { }
        }
    }
}
