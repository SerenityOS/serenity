/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

/*
 * Utilities for testing the signature algorithm OIDs.
 */
public class TestSignatureOidHelper {

    private static final byte[] INPUT = "1234567890".getBytes();

    private final String algorithm;

    private final String provider;

    private final int keySize;

    private final List<OidAlgorithmPair> data;

    public TestSignatureOidHelper(String algorithm, String provider,
            int keySize, List<OidAlgorithmPair> data) {
        this.algorithm = algorithm;
        this.provider = provider;
        this.keySize = keySize;
        this.data = data;
    }

    public void execute() throws Exception {
        KeyPair keyPair = createKeyPair();
        for (OidAlgorithmPair oidAlgorithmPair : data) {
            runTest(oidAlgorithmPair, keyPair);
            System.out.println("passed");
        }
        System.out.println("All tests passed");
    }

    private KeyPair createKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm,
                provider);
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    private void runTest(OidAlgorithmPair oidAlgorithmPair, KeyPair keyPair)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException {
        Signature sgAlgorithm =
                Signature.getInstance(oidAlgorithmPair.algorithm, provider);
        Signature sgOid = Signature.getInstance(oidAlgorithmPair.oid, provider);

        if (sgAlgorithm == null) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance failed.%n",
                    oidAlgorithmPair.algorithm));
        }

        if (sgOid == null) {
            throw new RuntimeException(
                    String.format("Test failed: OID %s getInstance failed.%n",
                            oidAlgorithmPair.oid));
        }

        if (!sgAlgorithm.getAlgorithm().equals(oidAlgorithmPair.algorithm)) {
            throw new RuntimeException(String.format(
                    "Test failed: algorithm string %s getInstance "
                            + "doesn't generate expected algorithm.%n",
                    oidAlgorithmPair.algorithm));
        }

        sgAlgorithm.initSign(keyPair.getPrivate());
        sgAlgorithm.update(INPUT);
        sgOid.initVerify(keyPair.getPublic());
        sgOid.update(INPUT);
        if (!sgOid.verify(sgAlgorithm.sign())) {
            throw new RuntimeException(
                    "Signature verification failed unexpectedly");
        }
    }
}

class OidAlgorithmPair {

    public final String oid;
    public final String algorithm;

    public OidAlgorithmPair(String oid, String algorithm) {
        this.oid = oid;
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return "[oid=" + oid + ", algorithm=" + algorithm + "]";
    }
}
