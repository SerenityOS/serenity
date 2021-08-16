/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6738532
 * @summary Check EllipticCurve.equals() does not compare seed value of curve.
 * @author Mike StJohns
 * @key randomness
 */

import java.security.spec.*;
import java.math.BigInteger;
import java.util.Random;

public class EllipticCurveMatch {
    static String primeP256 =
        "0FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF";
    static String aP256 =
        "0FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC";
    static String bP256 =
        "05AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B";
    static String seedP256 =
        "0C49D360886E704936A6678E1139D26B7819F7E90";

    private static EllipticCurve addSeedToCurve(EllipticCurve curve)
    {
        Random rand = new Random();
        byte[] seed = new byte[12];
        rand.nextBytes(seed);

        return new EllipticCurve (curve.getField(), curve.getA(), curve.getB(),
            seed);
    }

    private static EllipticCurve getP256Curve()
    {
        ECFieldFp field = new ECFieldFp(new BigInteger (primeP256,16));
        BigInteger a = new BigInteger (aP256, 16);
        BigInteger b = new BigInteger (bP256, 16);

        return new EllipticCurve (field, a, b);
    }

    public static void main (String[] argv) throws Exception {
        EllipticCurve firstCurve = getP256Curve();
        EllipticCurve secondCurve = addSeedToCurve(firstCurve);
        EllipticCurve thirdCurve = addSeedToCurve(firstCurve);

        if (!firstCurve.equals(firstCurve))
            throw new Exception("Original curve doesn't equal itself");

        if (!firstCurve.equals(secondCurve))
            throw new Exception ("Original curve doesn't equal seeded curve");

        if (!secondCurve.equals(secondCurve))
            throw new Exception ("Seeded curve doesn't equal itself");

        if (!secondCurve.equals(thirdCurve))
            throw new Exception ("Seeded curve doesn't equal differently " +
                "seeded curve");
        System.out.println("Curve equals test passed");
    }
}
