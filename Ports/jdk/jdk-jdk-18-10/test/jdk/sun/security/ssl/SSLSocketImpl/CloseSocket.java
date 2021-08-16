/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4674913
 * @summary Verify that EOFException are correctly handled during the handshake
 * @library /javax/net/ssl/templates
 * @author Andreas Sterbenz
 * @run main/othervm CloseSocket
 */

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CloseSocket extends SSLSocketTemplate {

/*
 * SSLSocketImpl::startHandshake internally checks that the socket is not closed or
 * broken and still connected, so this test needs the server to close the socket
 * after those verifications are performed to reproduce the scenario. Using a
 * CountDownLatch in the test before calling startHandshake does not guarantee that.
 * Using a CountDownLatch after startHandshake does not work either since the client
 * keeps waiting for a server response, which is blocked waiting for the latch.
 *
 * Therefore, we can only guarantee the socket is not yet closed when the handshake
 * is requested by looking at the client thread stack
 */
    private volatile Thread clientThread = null;

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        clientThread = Thread.currentThread();
        boolean failed = false;
        for (TestCase testCase : getTestCases()) {
            try {
                testCase.test(socket);
                System.out.println("ERROR: no exception");
                failed = true;
            } catch (IOException e) {
                System.out.println("Failed as expected: " + e);
            }
        }
        if (failed) {
            throw new Exception("One or more tests failed");
        }
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        System.out.println("Server accepted connection");
        while (!isHandshakeStarted()) {
            // wait for a short time before checking again if handshake started
            TimeUnit.MILLISECONDS.sleep(100);
        }

        socket.close();
        System.out.println("Server closed socket, done.");
    }

    private List<TestCase> getTestCases() {
        List<TestCase> testCases = new ArrayList<>();

        testCases.add(SSLSocket::startHandshake);
        testCases.add(socket -> {
            InputStream in = socket.getInputStream();
            in.read();
        });
        testCases.add(socket -> {
            OutputStream out = socket.getOutputStream();
            out.write(43);
        });

        return testCases;
    }

    private boolean isHandshakeStarted() {
        if (clientThread == null) {
            return false;
        } else {
            StackTraceElement[] traces = clientThread.getStackTrace();
            return Arrays.stream(traces).anyMatch(stackElement ->
                    stackElement.getMethodName().equals("readHandshakeRecord"));
        }
    }

    public static void main(String[] args) throws Exception {
        new CloseSocket().run();
    }

    interface TestCase {
        void test(SSLSocket socket) throws IOException;
    }
}
