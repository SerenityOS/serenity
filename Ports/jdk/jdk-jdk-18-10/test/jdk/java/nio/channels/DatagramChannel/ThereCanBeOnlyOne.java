/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4630914
 * @summary Should only be one PUE even if multiple ICMPs were received
 */
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class ThereCanBeOnlyOne {

    static void doTest(InetAddress ia, boolean testSend) throws Exception {
        DatagramChannel dc1 = DatagramChannel.open();
        dc1.socket().bind((SocketAddress)null);
        int port = dc1.socket().getLocalPort();
        InetSocketAddress isa = new InetSocketAddress(ia, port);
        dc1.connect(isa);

        ByteBuffer bb = ByteBuffer.allocateDirect(512);
        bb.put("hello".getBytes());
        bb.flip();

        /*
         * Send a bunch of packets to the destination
         */
        int outstanding = 0;
        for (int i=0; i<20; i++) {
            try {
                bb.rewind();
                dc1.write(bb);
                outstanding++;
            } catch (PortUnreachableException e) {
                /* PUE throw => assume none outstanding now */
                outstanding = 0;
            }
            if (outstanding > 1) {
                break;
            }
        }
        if (outstanding < 1) {
            System.err.println("Insufficient exceptions outstanding - Test Skipped (Passed).");
            dc1.close();
            return;
        }

        /*
         * Give time for ICMP port unreachables to return
         */
        Thread.currentThread().sleep(5000);

        /*
         * The next send or receive should cause a PUE to be thrown
         */
        boolean gotPUE = false;
        boolean gotTimeout = false;
        dc1.configureBlocking(false);

        try {
            if (testSend) {
                bb.rewind();
                dc1.write(bb);
            } else {
                bb.clear();
                dc1.receive(bb);
            }
        } catch (PortUnreachableException pue) {
            System.err.println("Got one PUE...");
            gotPUE = true;
        }

        /*
         * The next receive should not get another PUE
         */
        if (gotPUE) {
            try {
                dc1.receive(bb);
            } catch (PortUnreachableException pue) {
                throw new Exception("PUs should have been consumed");
            }
        } else {
            // packets discarded. Okay
        }

        dc1.close();
    }


    public static void main(String args[]) throws Exception {
        InetAddress ia = InetAddress.getLocalHost();
        doTest(ia, true);
        doTest(ia, false);
    }

}
