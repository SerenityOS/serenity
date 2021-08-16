/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* Test utilities
 *
 */

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.Random;


public class TestUtil {

    // Test hosts used by the channels tests - change these when
    // executing in a different network.
    public static final String UNRESOLVABLE_HOST = "blah-blah.blah-blah.blah";

    private TestUtil() { }

    // Repeatedly try random ports until we bind to one.  You might be tempted
    // to do this:
    //
    //     ServerSocketChannel ssc = ServerSocketChannel.open();
    //     ssc.socket().bind(new InetSocketAddress(0));
    //     SocketAddress sa = ssc.socket().getLocalSocketAddress();
    //
    // but unfortunately it doesn't work on NT 4.0.
    //
    // Returns the bound port.
    //
    static int bind(ServerSocketChannel ssc) throws IOException {
        InetAddress lh = InetAddress.getLocalHost();
        Random r = new Random();
        for (;;) {
            int p = r.nextInt((1 << 16) - 1024) + 1024;
            InetSocketAddress isa = new InetSocketAddress(lh, p);
            try {
                ssc.socket().bind(isa);
            } catch (IOException x) {
                continue;
            }
            return p;
        }
    }

    // A more convenient form of bind(ServerSocketChannel) that returns a full
    // socket address.
    //
    static InetSocketAddress bindToRandomPort(ServerSocketChannel ssc)
        throws IOException
    {
        int p = bind(ssc);
        return new InetSocketAddress(InetAddress.getLocalHost(), p);
    }

    private static String osName = System.getProperty("os.name");

    static boolean onWindows() {
        return osName.startsWith("Windows");
    }
}
