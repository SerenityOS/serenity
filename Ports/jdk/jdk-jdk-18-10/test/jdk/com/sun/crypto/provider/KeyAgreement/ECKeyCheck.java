/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8261502
 * @summary Check that ECPrivateKey's that are not ECPrivateKeyImpl can use
 * ECDHKeyAgreement
 */

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

public class ECKeyCheck {

    public static final void main(String args[]) throws Exception {
        ECGenParameterSpec spec = new ECGenParameterSpec("secp256r1");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(spec);

        ECPrivateKey privKey = (ECPrivateKey) kpg.generateKeyPair().getPrivate();
        ECPublicKey pubKey = (ECPublicKey) kpg.generateKeyPair().getPublic();
        generateECDHSecret(privKey, pubKey);
        generateECDHSecret(new newPrivateKeyImpl(privKey), pubKey);
    }

    private static byte[] generateECDHSecret(ECPrivateKey privKey,
        ECPublicKey pubKey) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(privKey);
        ka.doPhase(pubKey, true);
        return ka.generateSecret();
    }

    // Test ECPrivateKey class
    private static class newPrivateKeyImpl implements ECPrivateKey {
        private ECPrivateKey p;

        newPrivateKeyImpl(ECPrivateKey p) {
            this.p = p;
        }

        public BigInteger getS() {
            return p.getS();
        }

        public byte[] getEncoded() {
            return p.getEncoded();
        }

        public String getFormat() {
            return p.getFormat();
        }

        public String getAlgorithm() {
            return p.getAlgorithm();
        }

        public ECParameterSpec getParams() {
            return p.getParams();
        }
    }
}
