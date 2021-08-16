/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009650
 * @summary HttpClient available() check throws SocketException when connection
 * has been closed
 * @modules java.base/sun.net
 *          java.base/sun.net.www.http:+open
 * @library /test/lib
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.ServerSocket;
import sun.net.www.http.HttpClient;
import java.security.*;
import java.lang.reflect.Method;
import jdk.test.lib.net.URIBuilder;

public class IsAvailable {

    public static void main(String[] args) throws Exception {
        int readTimeout = 20;
        ServerSocket ss = new ServerSocket();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss.bind(new InetSocketAddress(loopback, 0));

        try (ServerSocket toclose = ss) {

            URL url1 = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .toURL();

            HttpClient c1 = HttpClient.New(url1);

            Method available = HttpClient.class.
                    getDeclaredMethod("available", null);
            available.setAccessible(true);

            c1.setReadTimeout(readTimeout);
            boolean a = (boolean) available.invoke(c1);
            if (!a) {
                throw new RuntimeException("connection should be available");
            }
            if (c1.getReadTimeout() != readTimeout) {
                throw new RuntimeException("read timeout has been altered");
            }

            c1.closeServer();

            a = (boolean) available.invoke(c1);
            if (a) {
                throw new RuntimeException("connection shouldn't be available");
            }
        }
    }
}
