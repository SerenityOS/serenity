/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8201510
 * @library /test/lib
 * @summary Make sure IPv6 addresses are rejected when the System option
 *          java.net.preferIPv4Stack is set to true
 * @run main/othervm -Djava.net.preferIPv4Stack=true RejectIPv6
 */

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import jdk.test.lib.net.IPSupport;

public class RejectIPv6 {

    public static void main(String [] argv) throws Throwable {
        IPSupport.throwSkippedExceptionIfNonOperational();

        ServerSocket serverSocket = new ServerSocket(0);
        serverSocket.setSoTimeout(1000);
        int serverPort = serverSocket.getLocalPort();
        Socket clientSocket = new Socket();

        test("bind", () -> clientSocket.bind(
                new InetSocketAddress("::1", 0)));

        test("connect", () -> clientSocket.connect(
                new InetSocketAddress("::1", serverPort), 1000));
    }

    static void test(String msg, CodeToTest codeToTest) throws Throwable {
        Thread client = new Thread(() ->
            {
                try {
                    codeToTest.run();
                    throw new RuntimeException(msg +
                            " failed to reject IPv6 address");
                } catch (SocketException ok) {
                } catch (Exception exc) {
                    throw new RuntimeException("unexpected", exc);
                }
            });
        client.start();
        client.join();
    }

    interface CodeToTest {
        void run() throws Exception;
    }
}
