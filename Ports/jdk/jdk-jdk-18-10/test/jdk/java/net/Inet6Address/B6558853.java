/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @key intermittent
 * @bug 6558853
 * @summary  getHostAddress() on connections using IPv6 link-local addrs should have zone id
 *           This test needs to bind to the wildcard address and as such is succeptible to
 *           fail intermittently because of port reuse issues.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main B6558853
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Optional;
import jdk.test.lib.NetworkConfiguration;

public class B6558853 implements Runnable {
    private InetAddress addr = null;
    private int port = 0;

    public static void main(String[] args) throws Exception {
        Optional<Inet6Address> oaddr = NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(a -> a.isLinkLocalAddress())
                .findFirst();

        if (!oaddr.isPresent()) {
            System.out.println("No suitable interface found. Exiting.");
            return;
        }

        Inet6Address dest = oaddr.get();
        System.out.println("Using " + dest);

        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            B6558853 test = new B6558853(dest, port);
            Thread thread = new Thread(test);
            thread.start();
            Socket s = ss.accept();
            InetAddress a = s.getInetAddress();
            OutputStream out = s.getOutputStream();
            out.write(1);
            out.close();
            if (!(a instanceof Inet6Address) || a.getHostAddress().indexOf("%") == -1) {
                // No Scope found in the address String
                throw new RuntimeException("Wrong address: " + a.getHostAddress());
            }
        }
    }

    public B6558853(InetAddress a, int port) {
        addr = a;
        this.port = port;
    }

    public void run() {
        try {
            Socket s = new Socket(addr, port);
            InputStream in = s.getInputStream();
            int i = in.read();
            in.close();
        } catch (IOException iOException) {
        }
    }
}
