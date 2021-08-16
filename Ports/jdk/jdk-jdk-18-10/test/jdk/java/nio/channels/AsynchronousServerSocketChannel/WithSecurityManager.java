/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4607272
 * @summary Unit test for AsynchronousServerServerSocketChannel
 * @build WithSecurityManager
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager allow
 * @run main/othervm -Djava.security.manager=allow WithSecurityManager deny
 */

import java.nio.file.Paths;
import java.nio.channels.*;
import java.net.*;
import java.util.concurrent.*;

public class WithSecurityManager {
    public static void main(String[] args) throws Exception {
        boolean allow = false;
        String policy = (args[0].equals("allow")) ? "java.policy.allow" :
            "java.policy.deny";

        String testSrc = System.getProperty("test.src");
        if (testSrc == null)
            testSrc = ".";

        System.setProperty("java.security.policy",
            Paths.get(testSrc).resolve(policy).toString());
        System.setSecurityManager(new SecurityManager());

        AsynchronousServerSocketChannel listener =
            AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(0));

        InetAddress lh = InetAddress.getLocalHost();
        int port = ((InetSocketAddress)(listener.getLocalAddress())).getPort();

        // establish and accept connection
        SocketChannel sc = SocketChannel.open(new InetSocketAddress(lh, port));
        Future<AsynchronousSocketChannel> result = listener.accept();

        if (allow) {
            // no security exception
            result.get().close();
        } else {
            try {
                result.get();
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof SecurityException))
                    throw new RuntimeException("SecurityException expected");
            }
        }

        sc.close();
        listener.close();
    }
}
