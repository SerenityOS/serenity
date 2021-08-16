/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8001326 8218482
 * @run main/othervm ReplayCachePrecise
 * @summary when there are 2 two AuthTime with the same time but different hash,
 * it's not a replay.
*/

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import sun.security.krb5.KrbException;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.ReplayCache;
import sun.security.krb5.internal.rcache.AuthTimeWithHash;

public class ReplayCachePrecise {
    static final String client = "dummy@REALM";
    static final String server = "server/localhost@REALM";

    public static void main(String[] args) throws Exception {

        int time = (int)(System.currentTimeMillis()/1000);

        AuthTimeWithHash a1 = new AuthTimeWithHash(client, server, time, 0,
                "HASH", "1111111111111111");
        AuthTimeWithHash a2 = new AuthTimeWithHash(client, server, time, 0,
                "HASH", "2222222222222222");
        KerberosTime now = new KerberosTime(time*1000L);

        // When all new styles, must exact match
        ReplayCache cache = ReplayCache.getInstance("dfl:./c1");
        cache.checkAndStore(now, a1);
        cache.checkAndStore(now, a2);

        // When only old style in cache, partial match
        cache = ReplayCache.getInstance("dfl:./c2");
        cache.checkAndStore(now, a1);
        // A small surgery to remove the new style from the cache file
        SeekableByteChannel ch = Files.newByteChannel(Paths.get("c2"),
                StandardOpenOption.WRITE,
                StandardOpenOption.READ);
        ch.position(6);
        ch.write(ByteBuffer.wrap(a1.encode(false)));
        ch.truncate(ch.position());
        ch.close();
        try {
            cache.checkAndStore(now, a2);
            throw new Exception();
        } catch (KrbException ke) {
            // Correct
            System.out.println(ke);
        }
    }
}
