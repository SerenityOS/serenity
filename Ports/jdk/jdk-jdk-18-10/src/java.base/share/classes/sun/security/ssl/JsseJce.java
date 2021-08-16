/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.ssl;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import javax.crypto.*;

/**
 * This class contains a few static methods for interaction with the JCA/JCE
 * to obtain implementations, etc.
 *
 * @author  Andreas Sterbenz
 */
final class JsseJce {
    static final boolean ALLOW_ECC =
            Utilities.getBooleanProperty("com.sun.net.ssl.enableECC", true);

    /**
     * JCE transformation string for RSA with PKCS#1 v1.5 padding.
     * Can be used for encryption, decryption, signing, verifying.
     */
    static final String CIPHER_RSA_PKCS1 = "RSA/ECB/PKCS1Padding";

    /**
     * JCE transformation string for the stream cipher RC4.
     */
    static final String CIPHER_RC4 = "RC4";

    /**
     * JCE transformation string for DES in CBC mode without padding.
     */
    static final String CIPHER_DES = "DES/CBC/NoPadding";

    /**
     * JCE transformation string for (3-key) Triple DES in CBC mode
     * without padding.
     */
    static final String CIPHER_3DES = "DESede/CBC/NoPadding";

    /**
     * JCE transformation string for AES in CBC mode
     * without padding.
     */
    static final String CIPHER_AES = "AES/CBC/NoPadding";

    /**
     * JCE transformation string for AES in GCM mode
     * without padding.
     */
    static final String CIPHER_AES_GCM = "AES/GCM/NoPadding";

    /**
     * JCE transformation string for ChaCha20-Poly1305
     */
    static final String CIPHER_CHACHA20_POLY1305 = "ChaCha20-Poly1305";

    /**
     * JCA identifier string for DSA, i.e. a DSA with SHA-1.
     */
    static final String SIGNATURE_DSA = "DSA";

    /**
     * JCA identifier string for ECDSA, i.e. a ECDSA with SHA-1.
     */
    static final String SIGNATURE_ECDSA = "SHA1withECDSA";

    /**
     * JCA identifier for EdDSA signatures.
     */
    static final String SIGNATURE_EDDSA = "EdDSA";

    /**
     * JCA identifier string for Raw DSA, i.e. a DSA signature without
     * hashing where the application provides the SHA-1 hash of the data.
     * Note that the standard name is "NONEwithDSA" but we use "RawDSA"
     * for compatibility.
     */
    static final String SIGNATURE_RAWDSA = "RawDSA";

    /**
     * JCA identifier string for Raw ECDSA, i.e. a DSA signature without
     * hashing where the application provides the SHA-1 hash of the data.
     */
    static final String SIGNATURE_RAWECDSA = "NONEwithECDSA";

    /**
     * JCA identifier string for Raw RSA, i.e. a RSA PKCS#1 v1.5 signature
     * without hashing where the application provides the hash of the data.
     * Used for RSA client authentication with a 36 byte hash.
     */
    static final String SIGNATURE_RAWRSA = "NONEwithRSA";

    /**
     * JCA identifier string for the SSL/TLS style RSA Signature. I.e.
     * an signature using RSA with PKCS#1 v1.5 padding signing a
     * concatenation of an MD5 and SHA-1 digest.
     */
    static final String SIGNATURE_SSLRSA = "MD5andSHA1withRSA";

    private JsseJce() {
        // no instantiation of this class
    }

    static boolean isEcAvailable() {
        return EcAvailability.isAvailable;
    }

    static int getRSAKeyLength(PublicKey key) {
        BigInteger modulus;
        if (key instanceof RSAPublicKey) {
            modulus = ((RSAPublicKey)key).getModulus();
        } else {
            RSAPublicKeySpec spec = getRSAPublicKeySpec(key);
            modulus = spec.getModulus();
        }
        return modulus.bitLength();
    }

    static RSAPublicKeySpec getRSAPublicKeySpec(PublicKey key) {
        if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey)key;
            return new RSAPublicKeySpec(rsaKey.getModulus(),
                                        rsaKey.getPublicExponent());
        }
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.getKeySpec(key, RSAPublicKeySpec.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // lazy initialization holder class idiom for static default parameters
    //
    // See Effective Java Second Edition: Item 71.
    private static class EcAvailability {
        // Is EC crypto available?
        private static final boolean isAvailable;

        static {
            boolean mediator = true;
            try {
                Signature.getInstance(SIGNATURE_ECDSA);
                Signature.getInstance(SIGNATURE_RAWECDSA);
                KeyAgreement.getInstance("ECDH");
                KeyFactory.getInstance("EC");
                KeyPairGenerator.getInstance("EC");
                AlgorithmParameters.getInstance("EC");
            } catch (Exception e) {
                mediator = false;
            }

            isAvailable = mediator;
        }
    }
}
