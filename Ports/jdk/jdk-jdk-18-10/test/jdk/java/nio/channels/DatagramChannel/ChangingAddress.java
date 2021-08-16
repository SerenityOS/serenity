/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6431343
 * @summary Test that DatagramChannel.getLocalAddress returns the right local
 *    address after connect/disconnect.
 * @run main/othervm -Djdk.net.usePlainDatagramSocketImpl=false ChangingAddress
 */
import java.net.*;
import java.nio.channels.DatagramChannel;

public class ChangingAddress {

    // Checks that the given DatagramSocket and DatagramChannel are bound to the
    // same local address.
    static void check(DatagramSocket ds, DatagramChannel dc) {
        InetAddress expected = ds.getLocalAddress();
        InetAddress actual = dc.socket().getLocalAddress();
        // okay if one bound to 0.0.0.0 and the other to ::0
        if ((expected.isAnyLocalAddress() != actual.isAnyLocalAddress()) &&
            !expected.equals(actual))
        {
            throw new RuntimeException("Expected: " + expected + ", actual: " + actual);
        }
    }

    public static void main(String[] args) throws Exception {
        InetAddress lh = InetAddress.getLocalHost();
        SocketAddress remote = new InetSocketAddress(lh, 1234);

        DatagramSocket ds = null;
        DatagramChannel dc = null;
        try {

            ds = new DatagramSocket();
            dc = DatagramChannel.open().bind(new InetSocketAddress(0));
            check(ds, dc);

            ds.connect(remote);
            dc.connect(remote);
            check(ds, dc);

            ds.disconnect();
            dc.disconnect();
            check(ds, dc);

            // repeat tests using socket adapter
            ds.connect(remote);
            dc.socket().connect(remote);
            check(ds, dc);

            ds.disconnect();
            dc.socket().disconnect();
            check(ds, dc);

       } finally {
            if (ds != null) ds.close();
            if (dc != null) dc.close();
       }
    }
}
