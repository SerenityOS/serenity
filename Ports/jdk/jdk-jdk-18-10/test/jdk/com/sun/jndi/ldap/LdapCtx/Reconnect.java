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

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/*
 * @test
 * @bug 8217606
 * @summary The LdapContext.reconnect method allows LDAP clients to initiate an
 *          LDAP bind operation on the existing connection. Invoking this method
 *          should not open a new connection under those circumstances.
 *
 * @library ../lib/
 * @run main Reconnect
 */
public class Reconnect {

    private static final byte[] BIND_RESPONSE = {
            0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A,
            0x01, 0x00, 0x04, 0x00, 0x04, 0x00
    };

    /*
     * This test checks that there's only one connection from the client to
     * the server.
     *
     * The mechanics is as follows. The first connection is awaited for some
     * generous timeout to factor in a possibility of running on a slow system.
     * Once the connection has been made, the second timeout begins. This
     * second timeout is smaller. The test then verifies that no further
     * connections have been made for that amount of time.
     */
    public static void main(String[] args) throws Exception {

        final Semaphore s = new Semaphore(0);

        BaseLdapServer server = new BaseLdapServer() {

            @Override
            protected void beforeConnectionHandled(Socket socket) {
                // Increment the number of connections from LDAP client
                s.release(1);
            }

            @Override
            protected void handleRequest(Socket socket,
                                         LdapMessage msg,
                                         OutputStream out)
                    throws IOException
            {
                switch (msg.getOperation()) {
                    case BIND_REQUEST:
                        out.write(BIND_RESPONSE);
                    default:
                        break;
                }
            }
        };

        try (var s1 = server.start()) {
            Hashtable<String, Object> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL,
                    "ldap://" + InetAddress.getLoopbackAddress().getHostName()
                            + ":" + server.getPort());
            env.put("java.naming.ldap.version", "3");

            // open connection
            InitialLdapContext context = new InitialLdapContext(env, null);

            // send bind request
            context.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
            context.addToEnvironment(Context.SECURITY_PRINCIPAL, "test");
            context.addToEnvironment(Context.SECURITY_CREDENTIALS, "secret");

            context.reconnect(null);
        }

        if (!s.tryAcquire(60L, TimeUnit.SECONDS)) {
            throw new RuntimeException("No connection has been made");
        }

        if (s.tryAcquire(5L, TimeUnit.SECONDS)) {
            throw new RuntimeException("Expected 1 connection, but found: "
                                               + (s.availablePermits() + 2));
        }
    }
}
