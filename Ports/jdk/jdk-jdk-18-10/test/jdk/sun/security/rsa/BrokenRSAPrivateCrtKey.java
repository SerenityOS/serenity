/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4503229 8220016
 * @summary default RSA KeyFactory can return broken RSAPrivateCrtKey objects
 *      This test was taken directly from the bug report, which
 *      was fixed in the crippled JSAFE provider, and needed
 *      to be brought forward into SunRsaSign (was JSSE).
 * @author Brad Wetmore
 */

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.math.BigInteger;

public class BrokenRSAPrivateCrtKey {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        generator.initialize(512);

        KeyPair pair = generator.generateKeyPair();

        RSAPrivateCrtKey privatekey = (RSAPrivateCrtKey) pair.getPrivate();

        RSAPrivateCrtKeySpec spec =
                new RSAPrivateCrtKeySpec(privatekey.getModulus(),
                privatekey.getPublicExponent(),
                privatekey.getPrivateExponent(),
                privatekey.getPrimeP(), privatekey.getPrimeQ(),
                privatekey.getPrimeExponentP(),
                privatekey.getPrimeExponentQ(),
                privatekey.getCrtCoefficient());

        KeyFactory factory = KeyFactory.getInstance("RSA", "SunRsaSign");

        PrivateKey privatekey2 = factory.generatePrivate(spec);

        BigInteger pe =
                ((RSAPrivateCrtKey) privatekey2).getPublicExponent();

        System.out.println("public exponent: " + pe);
    }
}
