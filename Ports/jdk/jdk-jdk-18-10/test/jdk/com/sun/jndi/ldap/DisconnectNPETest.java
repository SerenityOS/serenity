/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import jdk.test.lib.net.URIBuilder;

/*
 * @test
 * @bug 8205330
 * @summary Test that If a connection has already been established and then
 *          the LDAP directory server sends an (unsolicited)
 *          "Notice of Disconnection", make sure client handle it correctly,
 *          no NPE been thrown.
 * @library lib/ /test/lib
 * @run main/othervm DisconnectNPETest
 */

public class DisconnectNPETest {
    // Normally the NPE bug should be hit less than 100 times run, but just in
    // case, we set repeat count to 1000 here.
    private static final int REPEAT_COUNT = 1000;

    // "Notice of Disconnection" message
    private static final byte[] DISCONNECT_MSG = { 0x30, 0x4C, 0x02, 0x01,
            0x00, 0x78, 0x47, 0x0A, 0x01, 0x34, 0x04, 0x00, 0x04, 0x28,
            0x55, 0x4E, 0x41, 0x56, 0x41, 0x49, 0x4C, 0x41, 0x42, 0x4C,
            0x45, 0x3A, 0x20, 0x54, 0x68, 0x65, 0x20, 0x73, 0x65, 0x72,
            0x76, 0x65, 0x72, 0x20, 0x77, 0x69, 0x6C, 0x6C, 0x20, 0x64,
            0x69, 0x73, 0x63, 0x6F, 0x6E, 0x6E, 0x65, 0x63, 0x74, 0x21,
            (byte) 0x8A, 0x16, 0x31, 0x2E, 0x33, 0x2E, 0x36, 0x2E, 0x31,
            0x2E, 0x34, 0x2E, 0x31, 0x2E, 0x31, 0x34, 0x36, 0x36, 0x2E,
            0x32, 0x30, 0x30, 0x33, 0x36 };
    private static final byte[] BIND_RESPONSE = { 0x30, 0x0C, 0x02, 0x01,
            0x01, 0x61, 0x07, 0x0A, 0x01, 0x00, 0x04, 0x00, 0x04, 0x00 };

    public static void main(String[] args) throws IOException {
        new DisconnectNPETest().run();
    }

    private ServerSocket serverSocket;
    private Hashtable<Object, Object> env;

    private void initRes() throws IOException {
        serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
    }

    private void initTest() {
        env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, URIBuilder.newBuilder()
                        .scheme("ldap")
                        .loopback()
                        .port(serverSocket.getLocalPort())
                        .buildUnchecked().toString());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL,
                "cn=8205330,ou=Client6,ou=Vendor1,o=IMC,c=US");
        env.put(Context.SECURITY_CREDENTIALS, "secret123");
    }

    private void run() throws IOException {
        initRes();
        initTest();
        int count = 0;
        try (var ignore = new BaseLdapServer(serverSocket) {
            @Override
            protected void handleRequest(Socket socket, LdapMessage request,
                    OutputStream out) throws IOException {
                if (request.getOperation()
                        == LdapMessage.Operation.BIND_REQUEST) {
                    out.write(BIND_RESPONSE);
                    out.write(DISCONNECT_MSG);
                }
            }
        }.start()) {
            while (count < REPEAT_COUNT) {
                count++;
                InitialDirContext context = null;
                try {
                    context = new InitialDirContext(env);
                } catch (NamingException ne) {
                    System.out.println("(" + count + "/" + REPEAT_COUNT
                            + ") It's ok to get NamingException: " + ne);
                    // for debug
                    ne.printStackTrace(System.out);
                } finally {
                    cleanupContext(context);
                }
            }
        } finally {
            System.out.println("Test count: " + count + "/" + REPEAT_COUNT);
        }
    }

    private void cleanupContext(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException e) {
                // ignore
            }
        }
    }
}
