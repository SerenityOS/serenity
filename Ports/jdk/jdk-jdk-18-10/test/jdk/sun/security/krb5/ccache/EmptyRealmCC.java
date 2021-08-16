/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048073
 * @summary Cannot read ccache entry with a realm-less service name
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ccache
 * @compile -XDignore.symbol.file EmptyRealmCC.java
 * @run main EmptyRealmCC
 */
import java.nio.file.Files;
import java.nio.file.Paths;

import sun.security.krb5.internal.ccache.CredentialsCache;

public class EmptyRealmCC {
    public static void main(String[] args) throws Exception {
        byte[] ccache = TimeInCCache.ccache;

        // The service name starts at 0x52:
        //
        //    0050:    00 00 00 02 00 00 00 0A 4D 41 58 49 2E 4C
        //             ----------- -----------
        //    0060: 4F 43 41 4C 00 00 00 06 6B 72 62 74 67 74 00 00
        //                      -----------                   -----
        //    0070: 00 0A 4D 41 58 49 2E 4C 4F 43 41 4C
        //          -----
        //
        // which contains 2 (the length of names), a 10-byte realm, a 6-byte
        // name[0], and a 10-byte name[1].

        // We will empty the realm, and pack the realm string to another
        // name (6-byte ".LOCAL"). Finally "krbtgt/MAXI.LOCAL@MAXI.LOCAL"
        // becomes ".LOCAL/krbtgt/MAXI.LOCAL@".

        // length of names is now 3
        ccache[0x55] = 3;
        // The empty realm
        System.arraycopy(new byte[4], 0, ccache, 0x56, 4);
        // Length of inserted name is 6
        System.arraycopy(new byte[]{0,0,0,6}, 0, ccache, 0x5A, 4);

        Files.write(Paths.get("tmpcc"), TimeInCCache.ccache);
        if (CredentialsCache.getInstance("tmpcc").getCredsList() != null) {
            throw new Exception("Nothing should be there");
        }
    }
}
