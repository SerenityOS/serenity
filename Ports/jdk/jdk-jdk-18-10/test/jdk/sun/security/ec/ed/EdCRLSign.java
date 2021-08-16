/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.util.Date;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;

/*
 * @test
 * @bug 8209632
 * @summary CRL Sign
 * @modules java.base/sun.security.x509
 * @run main EdCRLSign
 */
public class EdCRLSign {

    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String OIDN25519 = "1.3.101.112";
    private static final String OID25519 = "OID.1.3.101.112";
    private static final String OIDN448 = "1.3.101.113";
    private static final String OID448 = "OID.1.3.101.113";
    private static final String PROVIDER = "SunEC";
    private static final SecureRandom S_RND = new SecureRandom(new byte[]{0x1});

    public static void main(String[] args) throws Exception {

        for (boolean initWithRandom : new boolean[]{true, false}) {
            // Default Parameter
            test(PROVIDER, ED25519, null, initWithRandom);
            test(PROVIDER, ED448, null, initWithRandom);

            // With named parameter
            test(PROVIDER, ED25519, ED25519, initWithRandom);
            test(PROVIDER, OIDN25519, ED25519, initWithRandom);
            test(PROVIDER, OID25519, ED25519, initWithRandom);
            test(PROVIDER, ED448, ED448, initWithRandom);
            test(PROVIDER, OIDN448, ED448, initWithRandom);
            test(PROVIDER, OID448, ED448, initWithRandom);

            // With size parameter
            test(PROVIDER, ED25519, 255, initWithRandom);
            test(PROVIDER, OIDN25519, 255, initWithRandom);
            test(PROVIDER, OID25519, 255, initWithRandom);
            test(PROVIDER, ED448, 448, initWithRandom);
            test(PROVIDER, OIDN448, 448, initWithRandom);
            test(PROVIDER, OID448, 448, initWithRandom);
        }
    }

    // Test CRL signature using a KeyPair.
    private static void test(String provider, String name, Object param,
            boolean initWithRandom) throws Exception {

        System.out.printf("Case Algo:%s, Param:%s, Intitiate with random:%s%n",
                name, param, initWithRandom);
        KeyPair kp = genKeyPair(provider, name, param, initWithRandom);
        X509CRLImpl crl = new X509CRLImpl(
                new X500Name("CN=Issuer"), new Date(), new Date());
        crl.sign(kp.getPrivate(), name);
        crl.verify(kp.getPublic());
        System.out.println("Passed.");
    }

    private static KeyPair genKeyPair(String provider, String name,
            Object param, boolean initWithRandom) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(name, provider);
        if (initWithRandom) {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param, S_RND);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param), S_RND);
            }
        } else {
            if (param instanceof Integer) {
                kpg.initialize((Integer) param);
            } else if (param instanceof String) {
                kpg.initialize(new NamedParameterSpec((String) param));
            }
        }
        return kpg.generateKeyPair();
    }

}
