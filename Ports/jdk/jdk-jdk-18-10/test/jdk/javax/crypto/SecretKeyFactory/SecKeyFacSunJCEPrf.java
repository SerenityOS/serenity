/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218723
 * @summary Use SunJCE Mac in SecretKeyFactory PBKDF2 implementation
 * @library evilprov.jar
 * @library /test/lib
 * @run main/othervm SecKeyFacSunJCEPrf
 */

import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import java.security.Provider;
import java.security.Security;
import com.evilprovider.*;

public class SecKeyFacSunJCEPrf {

    // One of the PBKDF2 HMAC-SHA1 test vectors from RFC 6070
    private static final byte[] SALT = "salt".getBytes();
    private static final char[] PASS = "password".toCharArray();
    private static final int ITER = 4096;
    private static final byte[] EXP_OUT =
            HexFormat.of().parseHex("4B007901B765489ABEAD49D926F721D065A429C1");

    public static void main(String[] args) throws Exception {
        // Instantiate the Evil Provider and insert it in the
        // most-preferred position.
        Provider evilProv = new EvilProvider();
        System.out.println("3rd Party Provider: " + evilProv);
        Security.insertProviderAt(evilProv, 1);

        SecretKeyFactory pbkdf2 =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1", "SunJCE");
        PBEKeySpec pbks = new PBEKeySpec(PASS, SALT, ITER, 160);

        SecretKey secKey1 = pbkdf2.generateSecret(pbks);
        System.out.println("PBKDF2WithHmacSHA1:\n" +
                    HexFormat.of().withUpperCase().formatHex(secKey1.getEncoded()));
        if (Arrays.equals(secKey1.getEncoded(), EXP_OUT)) {
            System.out.println("Test Vector Passed");
        } else {
            System.out.println("Test Vector Failed");
            System.out.println("Expected Output:\n" +
                    HexFormat.of().withUpperCase().formatHex(EXP_OUT));
            throw new RuntimeException();
        }
    }
}

