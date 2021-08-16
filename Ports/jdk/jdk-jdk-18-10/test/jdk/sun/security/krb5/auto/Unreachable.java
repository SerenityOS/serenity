/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7162687 8015595 8194486
 * @summary enhance KDC server availability detection
 * @library /test/lib
 * @compile -XDignore.symbol.file Unreachable.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts Unreachable
 */
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import javax.security.auth.login.LoginException;
import sun.security.krb5.Config;

public class Unreachable {

    // Wait for 20 second until unreachable KDC throws PortUnreachableException.
    private static final int TIMEOUT = 20;
    private static final String REALM = "RABBIT.HOLE";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 13434;
    private static final String KRB_CONF = "unreachable.krb5.conf";

    public static void main(String[] args) throws Exception {

        // - Only PortUnreachableException will allow to continue execution.
        // - SocketTimeoutException may occur on Mac because it will not throw
        // PortUnreachableException for unreachable port in which case the Test
        // execution will be skipped.
        // - For Reachable port, the Test execution will get skipped.
        // - Any other Exception will be treated as Test failure.
        if (!findPortUnreachableExc()) {
            System.out.println(String.format("WARNING: Either a reachable "
                    + "connection found to %s:%s or SocketTimeoutException "
                    + "occured which means PortUnreachableException not thrown"
                    + " by the platform.", HOST, PORT));
            return;
        }
        KDC kdc = KDC.existing(REALM, HOST, PORT);
        KDC.saveConfig(KRB_CONF, kdc);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Exception> future = executor.submit(new Callable<Exception>() {
            @Override
            public Exception call() {
                System.setProperty("java.security.krb5.conf", KRB_CONF);
                try {
                    Config.refresh();
                    // If PortUnreachableException is not received, the login
                    // will consume about 3*3*30 seconds and the test will
                    // timeout.
                    try {
                        Context.fromUserPass("name", "pass".toCharArray(), true);
                    } catch (LoginException le) {
                        // This is OK
                    }
                    System.out.println("Execution successful.");
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        });
        try {
            Exception ex = null;
            if ((ex = future.get(TIMEOUT, TimeUnit.SECONDS)) != null) {
                throw new RuntimeException(ex);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("PortUnreachableException not thrown.");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * If the remote destination to which the socket is connected does not
     * exist, or is otherwise unreachable, and if an ICMP destination unreachable
     * packet has been received for that address, then a subsequent call to
     * send or receive may throw a PortUnreachableException. Note, there is no
     * guarantee that the exception will be thrown.
     */
    private static boolean findPortUnreachableExc() throws Exception {
        try {
            InetSocketAddress iaddr = new InetSocketAddress(HOST, PORT);
            DatagramSocket dgSocket = new DatagramSocket();
            dgSocket.setSoTimeout(5000);
            dgSocket.connect(iaddr);
            byte[] data = new byte[]{};
            dgSocket.send(new DatagramPacket(data, data.length, iaddr));
            dgSocket.receive(new DatagramPacket(data, data.length));
        } catch (PortUnreachableException e) {
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        }
        return false;
    }
}
