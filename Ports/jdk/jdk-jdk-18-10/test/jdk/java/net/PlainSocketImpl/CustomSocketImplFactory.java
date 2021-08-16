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

/*
 * @test
 * @bug 8024952
 * @summary ClassCastException in SocketImpl.accept() when using custom socketImpl
 * @run main/othervm CustomSocketImplFactory
 */

import java.net.*;
import java.io.*;

public class CustomSocketImplFactory implements SocketImplFactory {

    @Override
    public SocketImpl createSocketImpl() {
        try {
            SocketImpl s = new CustomSocketImpl();
            System.out.println("Created " + s);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {

        Socket.setSocketImplFactory(new CustomSocketImplFactory());
        try (ServerSocket ss = new ServerSocket(0)) {
            ss.setSoTimeout(1);
            ss.accept();
            System.out.println("PASS");
        } catch (SocketTimeoutException | NullPointerException e) {
            // Not a real socket impl
        }
    }

    class CustomSocketImpl extends SocketImpl {

        public void create(boolean stream) throws IOException {
        }

        public void connect(String host, int port) throws IOException {
        }

        public void connect(InetAddress addr, int port) throws IOException {
        }

        public void connect(SocketAddress addr, int timeout) throws IOException {
        }

        public void bind(InetAddress host, int port) throws IOException {
        }

        public void listen(int backlog) throws IOException {
        }

        public void accept(SocketImpl s) throws IOException {
        }

        public InputStream getInputStream() throws IOException {
            return null;
        }

        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        public int available() throws IOException {
            return 0;
        }

        public void close() throws IOException {
        }

        public void sendUrgentData(int data) throws IOException {
        }

        public Object getOption(int i) throws SocketException {
            return null;
        }

        public void setOption(int i, Object o) throws SocketException {
        }
    }
}
