/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug 4091012
 *
 * @summary Tests MulticastSocket send for new set/getTimeToLived
 *
 */
import java.net.*;

public class TimeToLive {

    static int[] new_ttls = { 0, 1, 127, 254, 255 };
    static int[] bad_ttls = { -1, 256 };

    public static void main(String[] args) throws Exception {
        try (MulticastSocket socket = new MulticastSocket()) {
            int ttl = socket.getTimeToLive();
            System.out.println("default ttl: " + ttl);
            for (int i = 0; i < new_ttls.length; i++) {
                socket.setTimeToLive(new_ttls[i]);
                if (!(new_ttls[i] == socket.getTimeToLive())) {
                    throw new RuntimeException("test failure, set/get differ: " +
                            new_ttls[i] + " /  " +
                            socket.getTimeToLive());
                }
            }
            for (int j = 0; j < bad_ttls.length; j++) {
                boolean exception = false;
                try {
                    socket.setTimeToLive(bad_ttls[j]);
                } catch (IllegalArgumentException e) {
                    exception = true;
                }
                if (!exception) {
                    throw new RuntimeException("bad argument accepted: " + bad_ttls[j]);
                }
            }
        }
    }
}
