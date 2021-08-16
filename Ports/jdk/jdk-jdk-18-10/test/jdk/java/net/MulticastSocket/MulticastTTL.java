/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4089415
 * @summary Test MulticastSocket send for modification of ttl
 *
 */
import java.io.*;
import java.net.*;

public class MulticastTTL {

    public static void main(String args[]) throws Exception {
        MulticastSocket soc = null;
        DatagramPacket pac = null;
        InetAddress sin = null;
        byte [] array = new byte[65537];
        int port = 0;
        byte old_ttl = 0;
        byte new_ttl = 64;
        byte ttl = 0;

        sin = InetAddress.getByName("224.80.80.80");
        soc = new MulticastSocket();
        port = soc.getLocalPort();
        old_ttl = soc.getTTL();
        pac = new DatagramPacket(array, array.length, sin, port);

        try {
            soc.send(pac, new_ttl);
        } catch(java.io.IOException e) {
            ttl = soc.getTTL();
            soc.close();
            if(ttl != old_ttl)
                throw new RuntimeException("TTL ");
        }
        soc.close();
    }
}
