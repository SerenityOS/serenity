/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6213702
 * @summary OOB data causes a SocketChannel, with OOBINLINE disabled, to be selected
 */

/* @test
 * @requires (os.family == "windows")
 * @run main/othervm -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider OutOfBand
 */

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.io.IOException;

public class OutOfBand {

    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = null;
        SocketChannel sc = null;
        Selector sel = null;
        Socket s = null;

        try {
            // establish loopback connection.
            ssc = ServerSocketChannel.open().bind(new InetSocketAddress(0));
            s = new Socket(InetAddress.getLocalHost(),
                           ssc.socket().getLocalPort());
            sc = ssc.accept();

            sel = Selector.open();
            sc.configureBlocking(false);
            sc.register(sel, SelectionKey.OP_READ);

            // OOB data should be disabled by default
            if (sc.socket().getOOBInline())
                throw new RuntimeException("SO_OOBINLINE enabled");
            test(s, false, 0,   0,   sel);
            test(s, false, 512, 0,   sel);
            test(s, false, 0,   512, sel);
            test(s, false, 512, 512, sel);

            // enable SO_OOBINLINE
            sc.socket().setOOBInline(true);

            // OOB data should be received
            test(s, true, 0,   0,   sel);
            test(s, true, 512, 0,   sel);
            test(s, true, 0,   512, sel);
            test(s, true, 512, 512, sel);

        } finally {
            if (sel != null) sel.close();
            if (sc != null) sc.close();
            if (ssc != null) ssc.close();
            if (s != null) sc.close();
        }
    }

    static void test(Socket s, boolean urgentExpected,
                     int bytesBefore, int bytesAfter,
                     Selector sel)
        throws IOException
    {
        // send data
        int bytesExpected = 0;
        if (bytesBefore > 0) {
            s.getOutputStream().write(new byte[bytesBefore]);
            bytesExpected += bytesBefore;
        }
        s.sendUrgentData(0xff);
        if (urgentExpected)
            bytesExpected++;
        if (bytesAfter > 0) {
            s.getOutputStream().write(new byte[bytesAfter]);
            bytesExpected += bytesAfter;
        }

        // receive data, checking for spurious wakeups and reads
        int spuriousWakeups = 0;
        int spuriousReads = 0;
        int bytesRead = 0;
        ByteBuffer bb = ByteBuffer.allocate(100);
        for (;;) {
            int n = sel.select(2000);
            if (n == 0) {
                if (bytesRead == bytesExpected) {
                    System.out.format("Selector wakeups %d\tSpurious reads %d%n",
                            spuriousWakeups, spuriousReads);
                    return;
                }
                if (++spuriousWakeups >= 3)
                    throw new RuntimeException("Selector appears to be spinning" +
                        " or data not received");
                continue;
            }
            if (n > 1)
                throw new RuntimeException("More than one key selected????");
            SelectionKey key = sel.selectedKeys().iterator().next();
            bb.clear();
            n = ((SocketChannel)key.channel()).read(bb);
            if (n == 0) {
                if (++spuriousReads >=3)
                    throw new RuntimeException("Too many spurious reads");
            } else {
                bytesRead += n;
                if (bytesRead > bytesExpected)
                    throw new RuntimeException("Received more than expected");
            }
            sel.selectedKeys().clear();
        }
    }
}
