/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

import sun.security.util.ECUtil;
import static sun.security.util.SecurityProviderConstants.DEF_EC_KEY_SIZE;

/**
 * A fake EC keypair generator.
 */
public final class ECKeyPairGenerator extends KeyPairGeneratorSpi {

    private int keySize;

    public ECKeyPairGenerator() {
        initialize(DEF_EC_KEY_SIZE, null);
    }

    @Override
    public void initialize(int keySize, SecureRandom random) {
        this.keySize = keySize;
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    @Override
    public KeyPair generateKeyPair() {
        BigInteger s, x, y;
        switch (keySize) {
            case 384:
                s = new BigInteger("230878276322370828604837367594276033697165"
                        + "328633328282930557390817326627704675451851870430805"
                        + "90262886393892128915463");
                x = new BigInteger("207573127814711182089888821916296502977037"
                        + "557291394001491584185306092085745595207966563387890"
                        + "64848861531410731137896");
                y = new BigInteger("272903686539605964684771543637437742229808"
                        + "792287657810480793861620950159864617021540168828129"
                        + "97920015041145259782242");
                break;
            default:
                throw new AssertionError("SunEC ECKeyPairGenerator" +
                    "has been patched. Key size " + keySize +
                    " is not supported");
        }
        ECParameterSpec ecParams = ECUtil.getECParameterSpec(null, keySize);
        try {
            return new KeyPair(new ECPublicKeyImpl(new ECPoint(x, y), ecParams),
                    new ECPrivateKeyImpl(s, ecParams));
        } catch (Exception ex) {
            throw new ProviderException(ex);
        }
    }
}
