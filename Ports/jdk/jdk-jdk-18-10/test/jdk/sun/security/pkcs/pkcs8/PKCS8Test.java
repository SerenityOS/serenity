/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048357 8244565
 * @summary PKCS8 Standards Conformance Tests
 * @library /test/lib
 * @modules java.base/sun.security.pkcs
 *          java.base/sun.security.util
 *          java.base/sun.security.provider
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file PKCS8Test.java
 * @run testng PKCS8Test
 */

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HexFormat;

import jdk.test.lib.hexdump.ASN1Formatter;
import jdk.test.lib.hexdump.HexPrinter;
import org.testng.Assert;
import org.testng.annotations.Test;
import sun.security.pkcs.PKCS8Key;
import sun.security.provider.DSAPrivateKey;
import sun.security.util.DerValue;

public class PKCS8Test {

    static final String FORMAT = "PKCS#8";
    static final String EXPECTED_ALG_ID_CHRS = "DSA, \n" +
            "\tp:     02\n\tq:     03\n\tg:     04\n";
    static final String ALGORITHM = "DSA";

    static final byte[] EXPECTED = HexFormat.of().parseHex(
            "301e" + // SEQUENCE
                "020100" +  // Version int 0
                "3014" +    // PrivateKeyAlgorithmIdentifier
                    "06072a8648ce380401" +      // OID DSA 1.2.840.10040.4.1
                    "3009020102020103020104" +  // p=2, q=3, g=4
                "0403020101");  // PrivateKey OCTET int x = 1

    @Test
    public void test() throws IOException {

        byte[] encodedKey = new DSAPrivateKey(
                BigInteger.valueOf(1),
                BigInteger.valueOf(2),
                BigInteger.valueOf(3),
                BigInteger.valueOf(4)).getEncoded();

        Assert.assertTrue(Arrays.equals(encodedKey, EXPECTED),
                HexPrinter.simple()
                        .formatter(ASN1Formatter.formatter())
                        .toString(encodedKey));

        PKCS8Key decodedKey = (PKCS8Key)PKCS8Key.parseKey(encodedKey);

        Assert.assertEquals(decodedKey.getAlgorithm(), ALGORITHM);
        Assert.assertEquals(decodedKey.getFormat(), FORMAT);
        Assert.assertEquals(decodedKey.getAlgorithmId().toString(),
                EXPECTED_ALG_ID_CHRS);

        byte[] encodedOutput = decodedKey.getEncoded();
        Assert.assertTrue(Arrays.equals(encodedOutput, EXPECTED),
                HexPrinter.simple()
                        .formatter(ASN1Formatter.formatter())
                        .toString(encodedOutput));

        // Test additional fields
        enlarge(0, "8000");    // attributes
        enlarge(1, "810100");  // public key for v2
        enlarge(1, "8000", "810100");  // both

        Assert.assertThrows(() -> enlarge(2));  // bad ver
        Assert.assertThrows(() -> enlarge(0, "8000", "8000")); // no dup
        Assert.assertThrows(() -> enlarge(0, "810100")); // no public in v1
        Assert.assertThrows(() -> enlarge(1, "810100", "8000")); // bad order
        Assert.assertThrows(() -> enlarge(1, "820100")); // bad tag
    }

    /**
     * Add more fields to EXPECTED and see if it's still valid PKCS8.
     *
     * @param newVersion new version
     * @param fields extra fields to add, in hex
     */
    static void enlarge(int newVersion, String... fields) throws IOException {
        byte[] original = EXPECTED.clone();
        int length = original.length;
        for (String field : fields) {   // append fields
            byte[] add = HexFormat.of().parseHex(field);
            original = Arrays.copyOf(original, length + add.length);
            System.arraycopy(add, 0, original, length, add.length);
            length += add.length;
        }
        Assert.assertTrue(length < 127);
        original[1] = (byte)(length - 2);   // the length field inside DER
        original[4] = (byte)newVersion;     // the version inside DER
        PKCS8Key.parseKey(original);
    }
}
