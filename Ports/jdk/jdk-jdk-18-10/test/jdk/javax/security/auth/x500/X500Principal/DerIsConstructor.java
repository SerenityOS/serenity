/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4426033
 * @summary Should add an X500Principal(InputStream) constructor
 */

import java.io.*;
import javax.security.auth.x500.X500Principal;

public class DerIsConstructor {
    public static void main(String[] args) {

        try {

            // create 2 different X500Principals
            X500Principal p = new X500Principal("o=sun, cn=duke");
            X500Principal p2 = new X500Principal("o=sun, cn=dukette");

            // get the encoded bytes for the 2 principals
            byte[] encoded = p.getEncoded();
            byte[] encoded2 = p2.getEncoded();

            // create a ByteArrayInputStream with the
            // encodings from the 2 principals
            byte[] all = new byte[encoded.length + encoded2.length];
            System.arraycopy(encoded, 0, all, 0, encoded.length);
            System.arraycopy(encoded2, 0, all, encoded.length, encoded2.length);
            ByteArrayInputStream bais = new ByteArrayInputStream(all);

            // create 2 new X500Principals from the ByteArrayInputStream
            X500Principal pp = new X500Principal(bais);
            X500Principal pp2 = new X500Principal(bais);

            // sanity check the 2 new principals
            if (p.equals(pp) && p2.equals(pp2) && !pp.equals(pp2)) {
                System.out.println("Test 1 passed");
            } else {
                throw new SecurityException("Test 1 failed");
            }

            // corrupt the ByteArrayInputStream and see if the
            // mark/reset worked
            byte[] all2 = new byte[all.length];
            System.arraycopy(all, 0, all2, 0, all.length);
            all2[encoded.length + 2] = (byte)-1;
            bais = new ByteArrayInputStream(all2);

            // this should work
            X500Principal ppp = new X500Principal(bais);

            // this should throw an IOException due to stream corruption
            int origAvailable = bais.available();
            try {
                X500Principal ppp2 = new X500Principal(bais);
                throw new SecurityException("Test 2 (part a) failed");
            } catch (IllegalArgumentException iae) {
                if (bais.available() == origAvailable) {
                    System.out.println("Test 2 passed");
                } else {
                    throw new SecurityException("Test 2 (part b) failed");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException(e.getMessage());
        }
    }
}
