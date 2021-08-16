/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6997561
 * @summary A request for better error handling in JNDI
 * @library ../lib/ /test/lib
 */

import java.net.Socket;
import java.io.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.util.Collections;
import java.util.Hashtable;
import jdk.test.lib.net.URIBuilder;

public class EmptyNameSearch {

    private static final byte[] bindResponse = {
            0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A,
            0x01, 0x00, 0x04, 0x00, 0x04, 0x00
    };
    private static final byte[] searchResponse = {
            0x30, 0x0C, 0x02, 0x01, 0x02, 0x65, 0x07, 0x0A,
            0x01, 0x02, 0x04, 0x00, 0x04, 0x00
    };

    public static void main(String[] args) throws Exception {

        // Start the LDAP server
        var ldapServer = new BaseLdapServer() {
            @Override
            protected void handleRequest(Socket socket, LdapMessage request,
                    OutputStream out) throws IOException {
                switch (request.getOperation()) {
                    case BIND_REQUEST:
                        out.write(bindResponse);
                        break;
                    case SEARCH_REQUEST:
                        out.write(searchResponse);
                        break;
                    default:
                        break;
                }
            }
        }.start();
        Thread.sleep(3000);

        // Setup JNDI parameters
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(ldapServer.getPort())
                .build().toString());

        try (ldapServer) {

            // Create initial context
            System.out.println("Client: connecting...");
            DirContext ctx = new InitialDirContext(env);

            System.out.println("Client: performing search...");
            ctx.search(new LdapName(Collections.emptyList()), "cn=*", null);
            ctx.close();

            // Exit
            throw new RuntimeException();

        } catch (NamingException e) {
            System.err.println("Passed: caught the expected Exception - " + e);

        } catch (Exception e) {
            System.err.println("Failed: caught an unexpected Exception - " + e);
            throw e;
        }
    }
}
