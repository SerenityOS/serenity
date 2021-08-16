/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009875
 * @summary Provide a default udp_preference_limit for krb5.conf
 * @modules java.security.jgss/sun.security.krb5:+open
 * @run main/othervm DefUdpLimit -1 1465
 * @run main/othervm DefUdpLimit 0 0
 * @run main/othervm DefUdpLimit 1234 1234
 * @run main/othervm DefUdpLimit 12345 12345
 * @run main/othervm DefUdpLimit 123456 32700
 *
 */

import sun.security.krb5.KdcComm;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DefUdpLimit {

    public static void main(String[] args) throws Exception {
        int set = Integer.valueOf(args[0]);
        int expected = Integer.valueOf(args[1]);
        Field f = KdcComm.class.getDeclaredField("defaultUdpPrefLimit");
        f.setAccessible(true);
        writeConf(set);
        int actual = (Integer)f.get(null);
        if (actual != expected) {
            throw new Exception("Expected: " + expected + ", get " + actual);
        }
    }

    static void writeConf(int i) throws Exception {
        String file = "krb5.conf." + i;
        String content = "[libdefaults]\n";
        if (i >= 0) {
            content += "udp_preference_limit = " + i;
        }
        Files.write(Paths.get(file), content.getBytes());
        System.setProperty("java.security.krb5.conf", file);
    }
}

