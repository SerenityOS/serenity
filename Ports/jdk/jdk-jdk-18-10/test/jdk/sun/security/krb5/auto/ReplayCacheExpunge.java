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
 * @bug 8001326
 * @run main/othervm ReplayCacheExpunge 16
 * @run main/othervm/fail ReplayCacheExpunge 15
 * @summary when number of expired entries minus number of good entries
 * is more than 30, expunge occurs, and expired entries are forgotten.
*/

import java.util.Random;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ReplayCache;
import sun.security.krb5.internal.rcache.AuthTimeWithHash;

public class ReplayCacheExpunge {
    static final String client = "dummy@REALM";
    static final String server = "server/localhost@REALM";
    static final Random rand = new Random();

    public static void main(String[] args) throws Exception {
        // Make sure clockskew is default value
        System.setProperty("java.security.krb5.conf", "nothing");

        int count = Integer.parseInt(args[0]);
        ReplayCache cache = ReplayCache.getInstance("dfl:./");
        AuthTimeWithHash a1 =
                new AuthTimeWithHash(client, server, time(-400), 0, "HASH", hash("1"));
        AuthTimeWithHash a2 =
                new AuthTimeWithHash(client, server, time(0), 0, "HASH", hash("4"));
        KerberosTime now = new KerberosTime(time(0)*1000L);
        KerberosTime then = new KerberosTime(time(-300)*1000L);

        // Once upon a time, we added a lot of events
        for (int i=0; i<count; i++) {
            a1 = new AuthTimeWithHash(client, server, time(-400), 0, "HASH", hash(""));
            cache.checkAndStore(then, a1);
        }

        // Now, we add a new one. If some conditions hold, the old ones
        // will be expunged.
        cache.checkAndStore(now, a2);

        // and adding an old one will not trigger any error
        cache.checkAndStore(now, a1);
    }

    private static String hash(String s) {
        char[] data = new char[16];
        for (int i=0; i<16; i++) {
            if (i < s.length()) data[i] = s.charAt(i);
            else data[i] = (char)('0' + rand.nextInt(10));
        }
        return new String(data);
    }

    private static int time(int x) {
        return (int)(System.currentTimeMillis()/1000) + x;
    }
}
