/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4866927
 * @summary InetAddress.getByName behaves differently on windows
 */
import java.net.*;

public class IPv4Formats {
    public static void main(String[] args) {
        InetAddress ad1, ad2;
        String adds[][] = {
            {"0", "0.0.0.0"},
            {"126.1", "126.0.0.1"},
            {"128.50.65534", "128.50.255.254"},
            {"192.168.1.2", "192.168.1.2"},
            {"invalidhost.invalid", null},
            {"1024.1.2.3", null},
            {"128.14.66000", null }
        };
        for (int i = 0; i < adds.length; i++) {
            if (adds[i][1] != null) {
                try {
                    ad1 = InetAddress.getByName(adds[i][0]);
                    ad2 = InetAddress.getByName(adds[i][1]);
                } catch (UnknownHostException ue) {
                    throw new RuntimeException("Wrong conversion: " + adds[i][0] + " should be " + adds[i][1] + " But throws " + ue);
                }
                if (! ad1.equals(ad2))
                    throw new RuntimeException("Wrong conversion: " + adds[i][0] + " should be " + adds[i][1] + " But is " + ad1);
            } else {
                try {
                    ad1 = InetAddress.getByName(adds[i][0]);
                    // should throw an UnknownHostException
                    throw new RuntimeException(adds[i][0] + " should throw UnknownHostException!");
                } catch (UnknownHostException e) {
                    // This is what we expect!
                }
            }
        }
    }
}
