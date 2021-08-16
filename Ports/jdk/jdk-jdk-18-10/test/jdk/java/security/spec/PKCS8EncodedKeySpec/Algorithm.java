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

/**
 * @test
 * @bug 8047223
 * @summary Add algorithm parameter to PKCS8EncodedKeySpec class
 */
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Algorithm {

    private static String PKCS8PrivateKey =
        "MIICoTAbBgkqhkiG9w0BBQMwDgQIqQMPwbNEhOgCAggABIICgCwRkeLXVGdO7S1h\n" +
        "FAFUiwj1HCzqYFF2x9+FzjlXNwEWecZsor5eoKQlTtJ9dsPajQ/wFgY76lkXDQXE\n" +
        "hdm8ndWFgCwqFBshmAp4TOvO9GlaAloDTnLMUg715D5FujiElcV7vqIY2V/7uB21\n" +
        "YRanKUa21sZAFJGj6Hom1+5+k0Q7Xi4kHgt+ZIPNLwrNFPWVovbTJdScZuJaDp6m\n" +
        "Q1DJUIQOzthV11VI+MU/v5SSKhj/uCaxizazEi5lgdmR7rRGgMz2YipOIjXIsKgu\n" +
        "jKX5LYFAZ8nYq1hy8Q1JPR5VPuWMFqeyofO/teXJb8gI/4TC1ZoED8hXj07jpJqG\n" +
        "2NVO1Dwqab31qSAjfjBkSYHKun63BvZPq2mT+frJF1YzvQhCDnWN1zbMKFNTZJfd\n" +
        "cUaecH/fgNKwKpeKGgX7UlWxo26/lS8pBiJ5ihtbyFfMUBtlwEN5uOHqVFOeZp1Z\n" +
        "DwCc0o1JA7yOcazA2TtNT9pc58tFZ8pEeyLj7ZchOgv06N0hZJsI6AiwII4ljd+K\n" +
        "4WKvs/xiSZU3tcHaWzqlf+6/M5kC3Pihm9GhZbKBmvrZYiKyTlJEeVI3pFRNSqbE\n" +
        "nZUJgkmgzNT/ZfM2WsUJm03Rq0eNCU/FDscIZnCWSA6Bf/DJDQWmhMhg2QmTGzQM\n" +
        "hw/vy77q7jxV67s36HGxxR1oe8uoZ2zugBBxHWEdqyQyrVwZXJukdjrc2S7pvMln\n" +
        "/VSleEf91MEcDhztyhPSqlX+H95vMnVmh5oY2gwY+P0oD5Eki6/9K+BHfuqgtS4S\n" +
        "LIna1iSyLr17pRO1lmNtvuCMwmUjeI8w3JhLmxxx//bl/WCAekqj3nMplrJHZ7xd\n" +
        "6k0Stxo=";

    private static String keyAlg = "RSA";
    private static String password = "password";

    /*
     * This test checks that a PKCS8EncodedKeySpec is properly constructed
     * from an encrypted private key and that the key algorithm name can be
     * retrieved as expected.
     */
    public static void main(String[] argv) throws Exception {
        EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(
                Base64.getMimeDecoder().decode(PKCS8PrivateKey));
        PBEKeySpec pks = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory skf = SecretKeyFactory.getInstance(epki.getAlgName());
        SecretKey sk = skf.generateSecret(pks);
        PKCS8EncodedKeySpec keySpec = epki.getKeySpec(sk);

        // Get the key algorithm and make sure it's what we expect
        String alg = keySpec.getAlgorithm();
        if (!alg.equals(keyAlg)) {
            throw new Exception("Expected: " + keyAlg + ", Got: " + alg);
        }

        System.out.println("Test passed");
    }
}
