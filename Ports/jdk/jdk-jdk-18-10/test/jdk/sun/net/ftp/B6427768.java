/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6427768
 * @summary FtpURLConnection doesn't close FTP connection when login fails
 * @modules java.base/sun.net.ftp
 * @library ../www/ftptest/
 * @build FtpServer FtpCommandHandler FtpAuthHandler FtpFileSystemHandler
 * @run main/othervm/timeout=500 B6427768
 */

import java.net.*;
import java.io.*;

public class B6427768 {
    // Need to test when login fails, so AuthHandler should always return
    // false
    static class MyAuthHandler implements FtpAuthHandler {
        public int authType() {
                return 2;
        }

        public boolean authenticate(String user, String password) {
                return false;
        }

        public boolean authenticate(String user, String password, String account) {
                return false;
        }
    }

    static class MyFileSystemHandler implements FtpFileSystemHandler {
        private String currentDir = "/";

        public MyFileSystemHandler(String path) {
                currentDir = path;
        }

        public boolean cd(String path) {
            currentDir = path;
            return true;
        }

        public boolean cdUp() {
            return true;
        }

        public String pwd() {
            return currentDir;
        }

        public InputStream getFile(String name) {
            return null;
        }

        public long getFileSize(String name) {
            return -1;
        }

        public boolean fileExists(String name) {
            return false;
        }

        public InputStream listCurrentDir() {
            return null;
        }

        public OutputStream putFile(String name) {
            return null;
        }

        public boolean removeFile(String name) {
            return false;
        }

        public boolean mkdir(String name) {
            return false;
        }

        public boolean rename(String from, String to) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        FtpServer server = new FtpServer(loopback, 0);
        int port = server.getLocalPort();
        server.setFileSystemHandler(new MyFileSystemHandler("/"));
        server.setAuthHandler(new MyAuthHandler());
        server.start();
        URL url = new URL("ftp://user:passwd@" + server.getAuthority() + "/foo.txt");
        URLConnection con = url.openConnection(Proxy.NO_PROXY);
        // triggers the connection
        try {
            con.getInputStream();
        } catch(sun.net.ftp.FtpLoginException e) {
            // Give some time to the client thread to properly terminate.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                // shouldn't happen
            }
            if (server.activeClientsCount() > 0) {
                // If there are still active clients attached to the FTP
                // server, it means we didn't quit properly
                server.killClients();
                throw new RuntimeException("URLConnection didn't close the ftp connection on failure to login");
            }
        } finally {
            server.terminate();
        }
    }
}
