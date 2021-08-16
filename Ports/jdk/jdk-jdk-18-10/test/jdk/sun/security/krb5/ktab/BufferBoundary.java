/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8201867
 * @summary Kerberos keytabs with holes in certain places are parsed incorrectly
 * @modules java.security.jgss/sun.security.krb5
 *          java.security.jgss/sun.security.krb5.internal.ktab
 * @run main/othervm BufferBoundary
 */

import sun.security.krb5.PrincipalName;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;
import sun.security.krb5.internal.ktab.KeyTabInputStream;
import sun.security.krb5.internal.ktab.KeyTabOutputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BufferBoundary {
    public static void main(String[] args) throws Exception {

        // We force using one etype so the output is determined
        Files.write(Paths.get("krb5.conf"), List.of(
                "[libdefaults]",
                "default_tkt_enctypes = aes128-cts"
        ));
        System.setProperty("java.security.krb5.conf", "krb5.conf");

        KeyTab kt = KeyTab.create("ktab");
        int num = 0;
        while (true) {
            num++;
            PrincipalName pn = new PrincipalName(
                    String.format("user%03d@REALM", num));
            kt.addEntry(pn, "password".toCharArray(), num, true);
            int length = 2;
            for (KeyTabEntry e : kt.getEntries()) {
                length += e.entryLength() + 4;
            }
            // Create enough big keytab
            if (length > 8400) {
                break;
            }
        }
        kt.save();

        int count = 0;
        boolean written = false;
        try (KeyTabInputStream kin
                     = new KeyTabInputStream(new FileInputStream("ktab"));
                KeyTabOutputStream kout
                        = new KeyTabOutputStream(new FileOutputStream("ktab2"))) {
            kout.write(kin.readNBytes(2)); // version
            count += 2;
            while (true) {
                int len;

                try {
                    len = kin.read(4);
                } catch (IOException e) {
                    break; // reach end
                }
                count += 4;
                byte[] data = kin.readNBytes(len);
                count += len;
                if (count < 8192 || written) {
                    kout.write32(len);
                    kout.write(data);
                } else {
                    // Create a hole on the 8192 boundary
                    kout.write32(-len);
                    kout.write(new byte[len]);
                    written = true;
                }
            }
        }

        kt = KeyTab.getInstance("ktab2");
        if (!kt.isValid()) {
            throw new Exception("Should be valid");
        }
        if (kt.getEntries().length != num - 1) {
            throw new Exception("Should have " + (num - 1) + " entries");
        }
    }
}
