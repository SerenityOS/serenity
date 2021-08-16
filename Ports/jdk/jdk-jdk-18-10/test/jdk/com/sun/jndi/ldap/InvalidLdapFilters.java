/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6916202 7041125
 * @library /test/lib
 * @summary More cases of invalid ldap filters accepted and processed
 *      LDAP API does not catch malformed filters that contain two operands
 *      for the ! operator
 * @run main/othervm InvalidLdapFilters valid (cn=Babs)
 * @run main/othervm InvalidLdapFilters valid (&(cn=Bob))
 * @run main/othervm InvalidLdapFilters valid (&(objectClass=*)(uid=*))
 * @run main/othervm InvalidLdapFilters valid (|(cn=Bob))
 * @run main/othervm InvalidLdapFilters valid (|(objectClass=*)(uid=*))
 * @run main/othervm InvalidLdapFilters valid (!(cn=Tim))
 * @run main/othervm InvalidLdapFilters valid (!(!(cn=Tim)))
 * @run main/othervm InvalidLdapFilters valid (!(&(objectClass=*)(uid=*)))
 * @run main/othervm InvalidLdapFilters valid (!(|(objectClass=*)(uid=*)))
 * @run main/othervm InvalidLdapFilters valid (&(objectClass=*)(!(uid=*)))
 * @run main/othervm InvalidLdapFilters valid (o=univ*of*mich*)
 * @run main/othervm InvalidLdapFilters valid (seeAlso=)
 * @run main/othervm InvalidLdapFilters valid (cn:caseExactMatch:=Flintstone)
 * @run main/othervm InvalidLdapFilters valid (cn:=Betty)
 * @run main/othervm InvalidLdapFilters valid (sn:dn:2.4.6.8.10:=Barney)
 * @run main/othervm InvalidLdapFilters valid (o:dn:=Ace)
 * @run main/othervm InvalidLdapFilters valid (:1.2.3:=Wilma)
 * @run main/othervm InvalidLdapFilters valid (:DN:2.4.6.8.10:=Dino)
 * @run main/othervm InvalidLdapFilters valid (1.2.3=abc)
 * @run main/othervm InvalidLdapFilters valid (cn;lang-de;lang-en=abc)
 * @run main/othervm InvalidLdapFilters valid (owner=abc)
 * @run main/othervm InvalidLdapFilters valid (sn;lang-en:dn:2.4.6.8.10:=Barney)
 * @run main/othervm InvalidLdapFilters valid
         (&(objectClass=Person)(|(sn=Jensen)(cn=Bab*)))
 * @run main/othervm InvalidLdapFilters valid
         (orcluserapplnprovstatus;EMAIL_email=PROVISIONING_FAILURE)
 * @run main/othervm InvalidLdapFilters invalid "(&(cn=Robert Dean)))"
 * @run main/othervm InvalidLdapFilters invalid (&|(cn=Bob))
 * @run main/othervm InvalidLdapFilters invalid (&&(cn=Bob))
 * @run main/othervm InvalidLdapFilters invalid (|&(cn=Bob))
 * @run main/othervm InvalidLdapFilters invalid (||(cn=Bob))
 * @run main/othervm InvalidLdapFilters invalid (:1.2.:=Wilma)
 * @run main/othervm InvalidLdapFilters invalid (::DN:2.4.6.8.10:=Dino)
 * @run main/othervm InvalidLdapFilters invalid (:DN::2.4.6.8.10:=Dino)
 * @run main/othervm InvalidLdapFilters invalid (:DN:2.4.6.8.10::=Dino)
 * @run main/othervm InvalidLdapFilters invalid (:DN:2.4.6..8.10:=Dino)
 * @run main/othervm InvalidLdapFilters invalid (:DN:2.4.6.8.:=Dino)
 * @run main/othervm InvalidLdapFilters invalid (1.2.;::=abc)
 * @run main/othervm InvalidLdapFilters invalid (1.2.3;::=abc)
 * @run main/othervm InvalidLdapFilters invalid (1.2.3;x;=abc)
 * @run main/othervm InvalidLdapFilters invalid (1.2.3:x::=abc)
 * @run main/othervm InvalidLdapFilters invalid (1.2.3:x=abc)
 * @run main/othervm InvalidLdapFilters invalid (sn;:dn:2.4.6.8.10:=Barney)
 * @run main/othervm InvalidLdapFilters invalid "\"((objectClass=*)&(uid=*))\""
 * @run main/othervm InvalidLdapFilters invalid "&(objectClass=*)(uid=*)"
 * @run main/othervm InvalidLdapFilters invalid "(:DN:2.4.6.8.10:cn:=Dino)"
 * @run main/othervm InvalidLdapFilters invalid "(:DN:2.4.6.8.10:cn=Dino)"
 * @run main/othervm InvalidLdapFilters invalid
         "((objectCategory=person)(cn=u)(!(cn=u2*)))"
 * @run main/othervm InvalidLdapFilters invalid
         "((&(objectClass=user)(cn=andy*)(cn=steve*)(cn=bob*)))"
 * @run main/othervm InvalidLdapFilters invalid
         (&(objectClass=Person)(!(sn=Jensen)(cn=Bab)))
 *
 * @author Xuelei Fan
 */

import java.io.*;
import javax.naming.*;
import javax.naming.directory.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Hashtable;

import java.net.Socket;
import java.net.ServerSocket;

import jdk.test.lib.net.URIBuilder;

public class InvalidLdapFilters {
    // Should we run the client or server in a separate thread?
    //
    // Both sides can throw exceptions, but do you have a preference
    // as to which side should be the main thread.
    static boolean separateServerThread = true;

    // use any free port by default
    volatile int serverPort = 0;

    // Is the server ready to serve?
    volatile static boolean serverReady = false;

    // Define the server side of the test.
    //
    // If the server prematurely exits, serverReady will be set to true
    // to avoid infinite hangs.
    void doServerSide() throws Exception {
        ServerSocket serverSock = new ServerSocket();
        SocketAddress sockAddr = new InetSocketAddress(
                InetAddress.getLoopbackAddress(), serverPort);
        // Bind server socket
        serverSock.bind(sockAddr);

        // signal client, it's ready to accept connection
        serverPort = serverSock.getLocalPort();
        serverReady = true;

        // accept a connection
        Socket socket = serverSock.accept();
        System.out.println("Server: Connection accepted");

        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        // read the bindRequest
        while (is.read() != -1) {
            // ignore
            is.skip(is.available());
            break;
        }

        byte[] bindResponse = {0x30, 0x0C, 0x02, 0x01, 0x01, 0x61, 0x07, 0x0A,
                               0x01, 0x00, 0x04, 0x00, 0x04, 0x00};
        // write bindResponse
        os.write(bindResponse);
        os.flush();

        // ignore any more request.
        while (is.read() != -1) {
            // ignore
            is.skip(is.available());
        }

        is.close();
        os.close();
        socket.close();
        serverSock.close();
    }

    //  Define the client side of the test.
    //
    // If the server prematurely exits, serverReady will be set to true
    // to avoid infinite hangs.
    void doClientSide() throws Exception {
        // Wait for server to get started.
        while (!serverReady) {
            Thread.sleep(50);
        }

        // set up the environment for creating the initial context
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                                "com.sun.jndi.ldap.LdapCtxFactory");
        String providerUrl = URIBuilder.newBuilder()
                .scheme("ldap")
                .loopback()
                .port(serverPort)
                .build()
                .toString();
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put("com.sun.jndi.ldap.read.timeout", "1000");

        // env.put(Context.SECURITY_AUTHENTICATION, "simple");
        // env.put(Context.SECURITY_PRINCIPAL,"cn=root");
        // env.put(Context.SECURITY_CREDENTIALS,"root");

        // create initial context
        DirContext context = null;
        int i = 0;
        while (true) {
            try {
                context = new InitialDirContext(env);
                break;
            } catch (NamingException ne) {
                // may be a connection or read timeout, try again
                // no more than 5 times
                if (i++ > 5) {
                    throw new Exception(
                        "Maybe timeout during context initialization", ne);
                }
            }
        }

        // searching
        SearchControls scs = new SearchControls();
        scs.setSearchScope(SearchControls.SUBTREE_SCOPE);

        try {
            NamingEnumeration<SearchResult> answer =
                    context.search("o=sun,c=us", searchFilter, scs);
        } catch (InvalidSearchFilterException isfe) {
            if (filterIsValid) {
                // unexpected filter exception.
                throw new Exception("Unexpected ISFE", isfe);
            } else {
                // ignore, it is the expected filter exception.
                System.out.println("Expected exception: " + isfe.getMessage());
            }
        } catch (NamingException ne) {
            // maybe a read timeout exception, as the server does not response.
            if (filterIsValid) {
                System.out.println("Expected exception: " + ne.getMessage());
            } else {
                throw new Exception("Not an InvalidSearchFilterException", ne);
            }
        }

        context.close();
    }

    private static boolean filterIsValid;
    private static String searchFilter;

    private static void parseArguments(String[] args) {
        System.out.println("arguments length: " + args.length);
        if (args[0].equals("valid")) {
          filterIsValid = true;
        }

        searchFilter = args[1];
    }

    /*
     * ============================================================
     * The remainder is just support stuff
     */

    // client and server thread
    Thread clientThread = null;
    Thread serverThread = null;

    // client and server exceptions
    volatile Exception serverException = null;
    volatile Exception clientException = null;

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        System.err.println(e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }

    // Primary constructor, used to drive remainder of the test.
    InvalidLdapFilters() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            System.out.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.out.print("Client Exception:");
            throw clientException;
        }
    }

    public static void main(String[] args) throws Exception {
        // parse the customized arguments
        parseArguments(args);

        // start the test
        new InvalidLdapFilters();
    }

}
