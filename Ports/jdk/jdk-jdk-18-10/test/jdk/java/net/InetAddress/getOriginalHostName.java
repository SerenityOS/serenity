/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8133196
 * @summary test functionality of getOriginalHostName(InetAddress)
 * @modules java.base/jdk.internal.access
 */

import java.net.InetAddress;

import jdk.internal.access.JavaNetInetAddressAccess;
import jdk.internal.access.SharedSecrets;

public class getOriginalHostName {

    private static final JavaNetInetAddressAccess jna =
        SharedSecrets.getJavaNetInetAddressAccess();

    public static void main(String[] args) throws Exception {
        final String HOST = "dummyserver.java.net";
        InetAddress ia = null;
        ia = InetAddress.getByName(HOST);
        testInetAddress(ia, HOST);
        ia = InetAddress.getByName("255.255.255.0");
        testInetAddress(ia, null);
        ia = InetAddress.getByAddress(new byte[]{1,1,1,1});
        testInetAddress(ia, null);
        ia = InetAddress.getLocalHost();
        testInetAddress(ia, ia.getHostName());
        ia = InetAddress.getLoopbackAddress();
        testInetAddress(ia, ia.getHostName());
    }


    private static void testInetAddress(InetAddress ia, String expected)
        throws Exception {

        System.out.println("Testing InetAddress: " + ia);
        System.out.println("Expecting original hostname of : " + expected);
        String origHostName = jna.getOriginalHostName(ia);
        System.out.println("via JavaNetAccess: " + origHostName);
        if (origHostName == null && expected != null) {
            throw new RuntimeException("Unexpected null. Testing:" + expected);
        } else if (expected != null && !origHostName.equals(expected)) {
            throw new RuntimeException("Unexpected hostname :" + origHostName);
        } else if (expected == null && origHostName != null) {
            throw new RuntimeException("Unexpected origHostName: " + origHostName);
        }
    }
}
