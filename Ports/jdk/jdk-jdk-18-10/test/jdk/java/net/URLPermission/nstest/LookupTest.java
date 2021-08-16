/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary A simple smoke test of the HttpURLPermission mechanism, which checks
 *          for either IOException (due to unknown host) or SecurityException
 *          due to lack of permission to connect
 * @run main/othervm -Djava.security.manager=allow -Djdk.net.hosts.file=LookupTestHosts LookupTest
 */

import java.io.BufferedWriter;
import java.io.FilePermission;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetPermission;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class LookupTest {

    static final Policy DEFAULT_POLICY = Policy.getPolicy();
    static int port;
    static volatile ServerSocket serverSocket;

    static void test(String url,
                     boolean throwsSecException,
                     boolean throwsIOException) {
        ProxySelector.setDefault(null);
        URL u;
        InputStream is = null;
        try {
            u = new URL(url);
            System.err.println("Connecting to " + u);
            URLConnection urlc = u.openConnection();
            is = urlc.getInputStream();
        } catch (SecurityException e) {
            if (!throwsSecException) {
                throw new RuntimeException("Unexpected SecurityException:", e);
            }
            return;
        } catch (IOException e) {
            if (!throwsIOException) {
                System.err.println("Unexpected IOException:" + e.getMessage());
                throw new RuntimeException(e);
            }
            return;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.err.println("Unexpected IOException:" + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        if (throwsSecException || throwsIOException) {
            System.err.printf("was expecting a %s\n", throwsSecException
                    ? "security exception" : "IOException");
            throw new RuntimeException("was expecting an exception");
        }
    }

    static final String HOSTS_FILE_NAME = System.getProperty("jdk.net.hosts.file");

    public static void main(String args[]) throws Exception {
        addMappingToHostsFile("allowedAndFound.com",
                              InetAddress.getLoopbackAddress().getHostAddress(),
                              HOSTS_FILE_NAME,
                              false);
        addMappingToHostsFile("notAllowedButFound.com",
                              "99.99.99.99",
                              HOSTS_FILE_NAME,
                              true);
        // name "notAllowedAndNotFound.com" is not in map
        // name "allowedButNotfound.com" is not in map
        Server server = new Server();
        try {
            Policy.setPolicy(new LookupTestPolicy());
            System.setSecurityManager(new SecurityManager());
            server.start();
            test("http://allowedAndFound.com:"       + port + "/foo", false, false);
            test("http://notAllowedButFound.com:"    + port + "/foo", true, false);
            test("http://allowedButNotfound.com:"    + port + "/foo", false, true);
            test("http://notAllowedAndNotFound.com:" + port + "/foo", true, false);
        } finally {
            server.terminate();
        }
    }

    static class Server extends Thread {
        private volatile boolean done;

        public Server() throws IOException {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(loopback, 0));
            port = serverSocket.getLocalPort();
        }

        public void run() {
            try {
                while (!done) {
                    try (Socket s = serverSocket.accept()) {
                        readOneRequest(s.getInputStream());
                        OutputStream o = s.getOutputStream();
                        String rsp = "HTTP/1.1 200 Ok\r\n" +
                                     "Connection: close\r\n" +
                                     "Content-length: 0\r\n\r\n";
                        o.write(rsp.getBytes(US_ASCII));
                    }
                }
            } catch (IOException e) {
                if (!done)
                    e.printStackTrace();
            }
        }

        void terminate() {
            done = true;
            try { serverSocket.close(); }
            catch (IOException unexpected) { unexpected.printStackTrace(); }
        }

        static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n' };

        // Read until the end of a HTTP request
        void readOneRequest(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            while ((r = is.read()) != -1) {
                if (r == requestEnd[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
        }
    }

    private static void addMappingToHostsFile(String host,
                                              String addr,
                                              String hostsFileName,
                                              boolean append)
        throws IOException
    {
        String mapping = addr + " " + host;
        try (FileWriter fr = new FileWriter(hostsFileName, append);
             PrintWriter hfPWriter = new PrintWriter(new BufferedWriter(fr))) {
            hfPWriter.println(mapping);
        }
    }

    static class LookupTestPolicy extends Policy {
        final PermissionCollection perms = new Permissions();

        LookupTestPolicy() throws Exception {
            perms.add(new NetPermission("setProxySelector"));
            perms.add(new SocketPermission("localhost:1024-", "resolve,accept"));
            perms.add(new URLPermission("http://allowedAndFound.com:" + port + "/-", "*:*"));
            perms.add(new URLPermission("http://allowedButNotfound.com:" + port + "/-", "*:*"));
            perms.add(new FilePermission("<<ALL FILES>>", "read,write,delete"));
            //perms.add(new PropertyPermission("java.io.tmpdir", "read"));
        }

        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return perms;
        }

        public PermissionCollection getPermissions(CodeSource codesource) {
            return perms;
        }

        public boolean implies(ProtectionDomain domain, Permission perm) {
            return perms.implies(perm) || DEFAULT_POLICY.implies(domain, perm);
        }
    }
}
