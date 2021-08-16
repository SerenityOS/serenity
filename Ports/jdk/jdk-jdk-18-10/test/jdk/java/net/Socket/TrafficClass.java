/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511783
 * @library /test/lib
 * @summary Test that setTrafficClass/getTraffiClass don't
 *          throw an exception
 * @run main TrafficClass
 * @run main/othervm -Djava.net.preferIPv4Stack=true TrafficClass
 */
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import jdk.test.lib.net.IPSupport;

public class TrafficClass {

    static final int IPTOS_RELIABILITY = 0x4;

    static int failures = 0;

    static void testDatagramSocket(DatagramSocket s) {
        try {
            s.setTrafficClass( IPTOS_RELIABILITY );
            int tc = s.getTrafficClass();
        } catch (Exception e) {
            failures++;
            System.err.println("testDatagramSocket failed: " + e);
        }
    }

    static void testSocket(Socket s) {
        try {
            s.setTrafficClass(IPTOS_RELIABILITY);
            int tc = s.getTrafficClass();
        } catch (Exception e) {
            failures++;
            System.err.println("testSocket failed: " + e);
        }

    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        DatagramSocket ds = new DatagramSocket();
        testDatagramSocket(ds);

        DatagramChannel dc = DatagramChannel.open();
        testDatagramSocket(dc.socket());

        Socket s = new Socket();
        testSocket(s);

        SocketChannel sc = SocketChannel.open();
        testSocket(sc.socket());

        if (failures > 0) {
            throw new Exception(failures + " sub-test(s) failed - " +
                "see log for details.");
        }
    }

}
