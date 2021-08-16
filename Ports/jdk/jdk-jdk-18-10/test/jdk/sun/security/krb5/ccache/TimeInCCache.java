/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6590930
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ccache:+open
 * @run main/othervm TimeInCCache
 * @summary read/write does not match for ccache
 */

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

import sun.security.krb5.internal.ccache.CCacheInputStream;
import sun.security.krb5.internal.ccache.Credentials;

public class TimeInCCache {
    // Attention: this field is also used by 2 other tests:
    // CorruptedCC.java and EmptyRealmCC.java
    public static byte[] ccache;

    static {
        // A trivial cache file, with startdate and renewTill being zero.
        // The endtime is set to sometime in year 2022, so that isValid()
        // will always check starttime.
        String var =
            /*0000*/ "05 04 00 0C 00 01 00 08 FF FF FF 13 FF FE 59 33 " +
            /*0010*/ "00 00 00 01 00 00 00 01 00 00 00 0A 4D 41 58 49 " +
            /*0020*/ "2E 4C 4F 43 41 4C 00 00 00 05 64 75 6D 6D 79 00 " +
            /*0030*/ "00 00 01 00 00 00 01 00 00 00 0A 4D 41 58 49 2E " +
            /*0040*/ "4C 4F 43 41 4C 00 00 00 05 64 75 6D 6D 79 00 00 " +
            /*0050*/ "00 00 00 00 00 02 00 00 00 0A 4D 41 58 49 2E 4C " +
            /*0060*/ "4F 43 41 4C 00 00 00 06 6B 72 62 74 67 74 00 00 " +
            /*0070*/ "00 0A 4D 41 58 49 2E 4C 4F 43 41 4C 00 11 00 00 " +
            /*0080*/ "00 10 B2 AB A6 CE BC 73 44 08 D9 93 5B 3D EF E5 " +
            /*0090*/ "86 88 47 45 10 87 00 00 00 00 62 45 10 87 00 00 " +
            /*00A0*/ "00 00 00 40 E0 00 00 00 00 00 00 00 00 00 00 00 " +
            /*00B0*/ "00 01 00 61 81 FD 30 81 FA A0 03 02 01 05 A1 0C " +
            /*00C0*/ "1B 0A 4D 41 58 49 2E 4C 4F 43 41 4C A2 1F 30 1D " +
            /*00D0*/ "A0 03 02 01 00 A1 16 30 14 1B 06 6B 72 62 74 67 " +
            /*00E0*/ "74 1B 0A 4D 41 58 49 2E 4C 4F 43 41 4C A3 81 C3 " +
            /*00F0*/ "30 81 C0 A0 03 02 01 11 A1 03 02 01 01 A2 81 B3 " +
            /*0100*/ "04 81 B0 2B 41 BE 22 15 DE 25 23 20 32 F2 7A 4D " +
            /*0110*/ "FD E3 25 63 32 7D D5 A0 B2 55 17 29 B0 44 02 93 " +
            /*0120*/ "E5 26 D7 B8 E0 7F 3F B4 EA 51 21 8E E2 68 7D AF " +
            /*0130*/ "E3 46 E7 17 64 B5 E7 3E 88 B2 C3 9C B6 32 8B 81 " +
            /*0140*/ "F0 4F 96 3E D9 5B 64 F6 17 A8 EE D1 33 ED 71 12 " +
            /*0150*/ "62 9B 1F 62 16 AF 0B D7 D6 43 57 5C FE 2A CA 4F " +
            /*0160*/ "31 A6 2B DB 5A 9A 7D 3E A8 B3 64 66 17 C7 CD 26 " +
            /*0170*/ "44 D4 C7 9A 67 FA 55 C6 4A 8B A9 43 99 DC 6E 86 " +
            /*0180*/ "73 0C 76 96 8E CD 4F 44 20 A5 CB FB CD 59 48 46 " +
            /*0190*/ "7B F4 A1 09 28 E2 8B 4A 4D 26 5B 7E AE 11 62 62 " +
            /*01A0*/ "CF 4E 24 24 67 B4 9C E9 76 A4 F8 50 67 E9 9E 38 " +
            /*01B0*/ "15 41 B3 00 00 00 00 ";
        ccache = new byte[var.length()/3];
        for (int i=0; i<ccache.length; i++) {
            ccache[i] = Integer.valueOf(var.substring(3*i,3*i+2), 16).byteValue();
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.security.krb5.debug", "true");  // test code changes in DEBUG
        CCacheInputStream cis = new CCacheInputStream(new ByteArrayInputStream(ccache));
        cis.readVersion();
        cis.readTag();
        cis.readPrincipal(0x504);
        Method m = CCacheInputStream.class.getDeclaredMethod("readCred", Integer.TYPE);
        m.setAccessible(true);
        Credentials c = (Credentials) m.invoke(cis, new Integer(0x504));
        sun.security.krb5.Credentials cc = c.setKrbCreds();

        // 1. Make sure starttime is still null
        if (cc.getStartTime() != null) {
            throw new Exception("Fail, starttime should be zero here");
        }

        // 2. Make sure renewTill is still null
        if (cc.getRenewTill() != null) {
            throw new Exception("Fail, renewTill should be zero here");
        }

        // 3. Make sure isValid works
        c.isValid();
    }
}
