/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Demonstrate JNDI using an LDAP request/response control.
 * The Authorization Identity controls and their associated control factory
 * is supplied by a third-party module.
 */

package test;

import java.io.PrintStream;
import java.net.*;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;

import org.example.authz.AuthzIdRequestControl;
import org.example.authz.AuthzIdResponseControl;

public class ConnectWithAuthzId {

    static {
        final PrintStream out = new PrintStream(System.out, true);
        final PrintStream err = new PrintStream(System.err, true);

        System.setOut(out);
        System.setErr(err);
    }

    // LDAP capture file
    private static final String LDAP_CAPTURE_FILE =
        System.getProperty("test.src") +
        "/src/test/test/ConnectWithAuthzId.ldap";

    public static void main(String[] args) throws Exception {

        /*
         * Process arguments
         */

        int argc = args.length;
        if ((argc < 1) ||
            ((argc == 1) && (args[0].equalsIgnoreCase("-help")))) {

            System.err.println("\nUsage:   ConnectWithAuthzId <ldapurl>\n");
            System.err.println("        <ldapurl> is the LDAP URL of the parent entry\n");
            System.err.println("example:");
            System.err.println("        java ConnectWithAuthzId ldap://oasis/o=airius.com");
            return;
        }

        /*
         * Launch the LDAP server with the ConnectWithAuthzId.ldap capture file
         */

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new LDAPServer(serverSocket, LDAP_CAPTURE_FILE);
                    } catch (Exception e) {
                        System.out.println("ERROR: unable to launch LDAP server");
                        e.printStackTrace();
                    }
                }
            }).start();

            /*
             * Connect to the LDAP directory
             */

            Hashtable<String,Object> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    "com.sun.jndi.ldap.LdapCtxFactory");
            URI ldapUri = new URI(args[0]);
            if (ldapUri.getPort() == -1) {
                ldapUri = new URI(ldapUri.getScheme(), null, ldapUri.getHost(),
                        serverSocket.getLocalPort(), ldapUri.getPath(), null, null);
            }
            env.put(Context.PROVIDER_URL, ldapUri.toString());
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "cn=admin,dc=ie,dc=oracle,dc=com");
            env.put(Context.SECURITY_CREDENTIALS, "changeit");
            env.put(LdapContext.CONTROL_FACTORIES,
                    "org.example.authz.AuthzIdResponseControlFactory");
            if (args[args.length - 1].equalsIgnoreCase("-trace")) {
                env.put("com.sun.jndi.ldap.trace.ber", System.out);
            }

            System.out.println("ConnectWithAuthzId: connecting to " + ldapUri);
            LdapContext ctx = null;
            Control[] connectionControls = { new AuthzIdRequestControl(false) };

            try {
                ctx = new InitialLdapContext(env, connectionControls);
                System.out.println("ConnectWithAuthzId: connected");
                // Retrieve the response controls
                Control[] responseControls = ctx.getResponseControls();
                if (responseControls != null) {
                    for (Control responseControl : responseControls) {
                        System.out.println("ConnectWithAuthzId: received response" +
                                " control: " + responseControl.getID());
                        if (responseControl instanceof AuthzIdResponseControl) {
                            AuthzIdResponseControl authzId =
                                    (AuthzIdResponseControl)responseControl;
                            System.out.println("ConnectWithAuthzId: identity is  " +
                                    authzId.getIdentity());
                        }
                    }
                }
            } catch (NamingException e) {
                System.err.println("ConnectWithAuthzId: error connecting " + e);
            } finally {
                if (ctx != null) {
                    ctx.close();
                }
            }
        }
    }
}
