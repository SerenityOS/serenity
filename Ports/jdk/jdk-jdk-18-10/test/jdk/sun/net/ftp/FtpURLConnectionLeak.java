/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7167293
 * @summary FtpURLConnection doesn't close FTP connection when FileNotFoundException is thrown
 * @library ../www/ftptest/
 * @build FtpServer FtpCommandHandler FtpAuthHandler FtpFileSystemHandler
 * @run main/othervm FtpURLConnectionLeak
 */
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.Proxy;

public class FtpURLConnectionLeak {

    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        FtpServer server = new FtpServer(loopback, 0);
        server.setFileSystemHandler(new CustomFileSystemHandler("/"));
        server.setAuthHandler(new MyAuthHandler());
        String authority = server.getAuthority();
        server.start();
        URL url = new URL("ftp://" + authority + "/filedoesNotExist.txt");
        try (server) {
            for (int i = 0; i < 3; i++) {
                try {
                    InputStream stream = url.openConnection(Proxy.NO_PROXY).getInputStream();
                } catch (FileNotFoundException expected) {
                    // should always reach this point since the path does not exist
                    System.out.println("caught expected " + expected);
                    int times = 1;
                    do {
                        // give some time to close the connection...
                        System.out.println("sleeping... " + times);
                        Thread.sleep(times * 1000);
                    } while (server.activeClientsCount() > 0 && times++ < 5);

                    if (server.activeClientsCount() > 0) {
                        server.killClients();
                        throw new RuntimeException("URLConnection didn't close the" +
                                " FTP connection on FileNotFoundException");
                    }
                }
            }
        }
    }

    static class CustomFileSystemHandler implements FtpFileSystemHandler {

        private String currentDir;

        public CustomFileSystemHandler(String path) {
            currentDir = path;
        }

        @Override
        public boolean cd(String path) {
            currentDir = path;
            return true;
        }

        @Override
        public boolean cdUp() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String pwd() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean fileExists(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InputStream getFile(String name) {
            return null; //return null so that server will return 550 File not found.
        }

        @Override
        public long getFileSize(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public InputStream listCurrentDir() {
            return null;
        }

        @Override
        public OutputStream putFile(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeFile(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean mkdir(String name) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean rename(String from, String to) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    static class MyAuthHandler implements FtpAuthHandler {

        @Override
        public int authType() {
            return 0;
        }

        @Override
        public boolean authenticate(String user, String password) {
            return true;
        }

        @Override
        public boolean authenticate(String user, String password, String account) {
            return true;
        }
    }
}
