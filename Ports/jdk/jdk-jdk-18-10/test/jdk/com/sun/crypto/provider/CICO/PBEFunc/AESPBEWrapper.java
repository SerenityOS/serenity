/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.Security;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Wrapper class to test a given AES-based PBE algorithm.
 */
public class AESPBEWrapper extends AbstractPBEWrapper {
    /**
     * the algorithm parameters.
     */
    private AlgorithmParameters pbeParams;

    /**
     * the encryption key.
     */
    private final SecretKey key;

    /**
     * The Wrapper constructor. Instantiate Cipher using the given AES-based PBE
     * algorithm.
     *
     * @param algo AES-based PBE algorithm.
     * @param passwd password phrase.
     * @throws GeneralSecurityException all security exceptions are thrown.
     */
    public AESPBEWrapper(PBEAlgorithm algo, String passwd)
            throws GeneralSecurityException {
        // salt and iteration count will be generated during encryption
        super(algo, passwd, 0);

        // Generate secret key. We expect no mode and padding specified.
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algo.baseAlgo);
        key = skf.generateSecret(new PBEKeySpec(passwd.toCharArray()));
    }

    /**
     * Initiate the Cipher object using given "mode".
     * @return a cipher object.
     * @throws GeneralSecurityException all security exceptions are thrown.
     */
    @Override
    protected Cipher initCipher(int mode) throws GeneralSecurityException {
        Provider provider = Security.getProvider("SunJCE");
        if (provider == null) {
            throw new RuntimeException("SunJCE provider does not exist.");
        }
        // get Cipher instance
        Cipher ci = Cipher.getInstance(transformation, provider);
        if (Cipher.ENCRYPT_MODE == mode) {
            ci.init(Cipher.ENCRYPT_MODE, key);
            pbeParams = ci.getParameters();
        } else {
            ci.init(Cipher.DECRYPT_MODE, key, pbeParams);
        }
        return ci;
    }
}
