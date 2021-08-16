/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/*
 * A SSL socket server.
 */
public class JSSEServer {

    private SSLServerSocket server = null;

    public JSSEServer(SSLContext context, String constraint,
            boolean needClientAuth) throws Exception {
        TLSRestrictions.setConstraint("Server", constraint);

        SSLServerSocketFactory serverFactory = context.getServerSocketFactory();
        server = (SSLServerSocket) serverFactory.createServerSocket(0);
        server.setSoTimeout(TLSRestrictions.TIMEOUT);
        server.setNeedClientAuth(needClientAuth); // for dual authentication
        System.out.println("Server: port=" + getPort());
    }

    public Exception start() {
        System.out.println("Server: started");
        Exception exception = null;
        try (SSLSocket socket = (SSLSocket) server.accept()) {
            System.out.println("Server: accepted connection");
            socket.setSoTimeout(TLSRestrictions.TIMEOUT);
            InputStream sslIS = socket.getInputStream();
            OutputStream sslOS = socket.getOutputStream();
            sslIS.read();
            sslOS.write('S');
            sslOS.flush();
            System.out.println("Server: finished");
        } catch (Exception e) {
            exception = e;
            e.printStackTrace(System.out);
            System.out.println("Server: failed");
        }

        return exception;
    }

    public int getPort() {
        return server.getLocalPort();
    }
}
