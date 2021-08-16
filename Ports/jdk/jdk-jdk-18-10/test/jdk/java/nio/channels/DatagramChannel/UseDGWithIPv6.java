/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @bug 6435300
 * @summary Check using IPv6 address does not crash the VM
 * @run main/othervm UseDGWithIPv6
 * @run main/othervm -Djava.net.preferIPv4Stack=true UseDGWithIPv6
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnsupportedAddressTypeException;

import jdk.test.lib.net.IPSupport;

public class UseDGWithIPv6 {
    static String[] targets = {
        "3ffe:e00:811:b::21:5",
        "15.70.186.80"
    };
    static int BUFFER_LEN = 10;
    static int port = 12345;

    public static void main(String[] args) throws IOException
    {
        IPSupport.throwSkippedExceptionIfNonOperational();

        ByteBuffer data = ByteBuffer.wrap("TESTING DATA".getBytes());
        DatagramChannel dgChannel = DatagramChannel.open();

        for(int i = 0; i < targets.length; i++){
            data.rewind();
            SocketAddress sa = new InetSocketAddress(targets[i], port);
            System.out.println("-------------\nDG_Sending data:" +
                               "\n    remaining:" + data.remaining() +
                               "\n     position:" + data.position() +
                               "\n        limit:" + data.limit() +
                               "\n     capacity:" + data.capacity() +
                               " bytes on DG channel to " + sa);
            try {
                int n = dgChannel.send(data, sa);
                System.out.println("DG_Sent " + n + " bytes");
            } catch (UnsupportedAddressTypeException e) {
                System.out.println("Ignoring unsupported address type");
            } catch (IOException e) {
                //This regression test is to check vm crash only, so ioe is OK.
                e.printStackTrace();
            }
        }
        dgChannel.close();
    }
}
