/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8243099
 * @library /test/lib
 * @modules jdk.net
 * @summary Check ExtendedSocketOption NAPI_ID support for Socket and
 *          ServerSocket
 * @run testng SocketNAPITest
 * @run testng/othervm -Djava.net.preferIPv4Stack=true SocketNAPITest
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import jdk.test.lib.net.IPSupport;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import static jdk.net.ExtendedSocketOptions.SO_INCOMING_NAPI_ID;

public class SocketNAPITest {
    private InetAddress hostAddr;
    private static final Class<SocketException> SE = SocketException.class;
    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    private static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @BeforeTest
    public void setup() throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();
        try (var s = new Socket();
             var ss = new ServerSocket()) {
            if (!s.supportedOptions().contains(SO_INCOMING_NAPI_ID)) {
                assertThrows(UOE, () -> s.getOption(SO_INCOMING_NAPI_ID));
                assertThrows(UOE, () -> s.setOption(SO_INCOMING_NAPI_ID, 42));
                assertThrows(UOE, () -> s.setOption(SO_INCOMING_NAPI_ID, null));
                assertThrows(UOE, () -> ss.getOption(SO_INCOMING_NAPI_ID));
                assertThrows(UOE, () -> ss.setOption(SO_INCOMING_NAPI_ID, 42));
                assertThrows(UOE, () -> ss.setOption(SO_INCOMING_NAPI_ID, null));
                throw new SkipException("NAPI ID not supported on this system");
            }
        }
        hostAddr = InetAddress.getLocalHost();
    }

    @Test
    public void testSetGetOptionSocket() throws IOException {
        try (var s = new Socket()) {
            assertEquals((int) s.getOption(SO_INCOMING_NAPI_ID), 0);
            assertThrows(SE, () -> s.setOption(SO_INCOMING_NAPI_ID, 42));
            assertThrows(IAE, () -> s.setOption(SO_INCOMING_NAPI_ID, null));
        }
    }

    @Test
    public void testSetGetOptionServerSocket() throws IOException {
        try (var ss = new ServerSocket()) {
            assertEquals((int) ss.getOption(SO_INCOMING_NAPI_ID), 0);
            assertThrows(SE, () -> ss.setOption(SO_INCOMING_NAPI_ID, 42));
            assertThrows(IAE, () -> ss.setOption(SO_INCOMING_NAPI_ID, null));
        }
    }

    @Test
    public void testSocket() throws Exception {
        int cID, sID;
        int temp_sID = 0, temp_cID = 0;
        boolean initialRun = false;
        // server socket
        try (var ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress(hostAddr, 0));
            var port = ss.getLocalPort();
            var addr = ss.getInetAddress();

            //client socket
            try (var c = new Socket()) {
                c.connect(new InetSocketAddress(addr, port));
                var cisr = new InputStreamReader(c.getInputStream());
                var cbr = new BufferedReader(cisr);
                var cps = new PrintStream(c.getOutputStream());

                //accepting socket
                try (var s = ss.accept()) {
                    var sisr = new InputStreamReader(s.getInputStream());
                    var sbr = new BufferedReader(sisr);
                    var sps = new PrintStream(s.getOutputStream());

                    for (int i = 0; i < 10; i++) {
                        cps.println("client");
                        sbr.readLine();
                        cps.flush();

                        sps.println("server");
                        cbr.readLine();
                        sps.flush();

                        // check ID remains consistent
                        sID = s.getOption(SO_INCOMING_NAPI_ID);
                        cID = c.getOption(SO_INCOMING_NAPI_ID);
                        if(initialRun) {
                            assertTrue(sID >= 0, "Socket: Server");
                            assertTrue(cID >= 0, "Socket: Client");
                        } else {
                            assertEquals(temp_cID, cID);
                            assertEquals(temp_sID, sID);
                            initialRun = false;
                        }
                        temp_sID = sID;
                        temp_cID = cID;
                    }
                }
            }
        }
    }
}
