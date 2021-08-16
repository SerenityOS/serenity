/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.*;

import sun.security.internal.spec.*;

import static com.sun.crypto.provider.TlsPrfGenerator.*;

/**
 * KeyGenerator implementation for the SSL/TLS master secret derivation.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
public final class TlsKeyMaterialGenerator extends KeyGeneratorSpi {

    private static final String MSG = "TlsKeyMaterialGenerator must be "
        + "initialized using a TlsKeyMaterialParameterSpec";

    @SuppressWarnings("deprecation")
    private TlsKeyMaterialParameterSpec spec;

    private int protocolVersion;

    public TlsKeyMaterialGenerator() {
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    @SuppressWarnings("deprecation")
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof TlsKeyMaterialParameterSpec == false) {
            throw new InvalidAlgorithmParameterException(MSG);
        }
        this.spec = (TlsKeyMaterialParameterSpec)params;
        if ("RAW".equals(spec.getMasterSecret().getFormat()) == false) {
            throw new InvalidAlgorithmParameterException(
                "Key format must be RAW");
        }
        protocolVersion = (spec.getMajorVersion() << 8)
            | spec.getMinorVersion();
        if ((protocolVersion < 0x0300) || (protocolVersion > 0x0303)) {
            throw new InvalidAlgorithmParameterException(
                "Only SSL 3.0, TLS 1.0/1.1/1.2 supported");
        }
    }

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    protected SecretKey engineGenerateKey() {
        if (spec == null) {
            throw new IllegalStateException(
                "TlsKeyMaterialGenerator must be initialized");
        }
        byte[] masterSecret = spec.getMasterSecret().getEncoded();
        try {
            return engineGenerateKey0(masterSecret);
        } catch (GeneralSecurityException e) {
            throw new ProviderException(e);
        } finally {
            Arrays.fill(masterSecret, (byte)0);
        }
    }

    @SuppressWarnings("deprecation")
    private SecretKey engineGenerateKey0(byte[] masterSecret) throws GeneralSecurityException {

        byte[] clientRandom = spec.getClientRandom();
        byte[] serverRandom = spec.getServerRandom();

        SecretKey clientMacKey = null;
        SecretKey serverMacKey = null;
        SecretKey clientCipherKey = null;
        SecretKey serverCipherKey = null;
        IvParameterSpec clientIv = null;
        IvParameterSpec serverIv = null;

        int macLength = spec.getMacKeyLength();
        int expandedKeyLength = spec.getExpandedCipherKeyLength();
        boolean isExportable = (expandedKeyLength != 0);
        int keyLength = spec.getCipherKeyLength();
        int ivLength = spec.getIvLength();

        int keyBlockLen = macLength + keyLength
            + (isExportable ? 0 : ivLength);
        keyBlockLen <<= 1;
        byte[] keyBlock;

        // These may be used again later for exportable suite calculations.
        MessageDigest md5 = null;
        MessageDigest sha = null;

        // generate key block
        if (protocolVersion >= 0x0303) {
            // TLS 1.2
            byte[] seed = concat(serverRandom, clientRandom);
            keyBlock = doTLS12PRF(masterSecret, LABEL_KEY_EXPANSION, seed,
                        keyBlockLen, spec.getPRFHashAlg(),
                        spec.getPRFHashLength(), spec.getPRFBlockSize());
        } else if (protocolVersion >= 0x0301) {
            // TLS 1.0/1.1
            md5 = MessageDigest.getInstance("MD5");
            sha = MessageDigest.getInstance("SHA1");
            byte[] seed = concat(serverRandom, clientRandom);
            keyBlock = doTLS10PRF(masterSecret, LABEL_KEY_EXPANSION, seed,
                        keyBlockLen, md5, sha);
        } else {
            // SSL
            md5 = MessageDigest.getInstance("MD5");
            sha = MessageDigest.getInstance("SHA1");
            keyBlock = new byte[keyBlockLen];

            byte[] tmp = new byte[20];
            for (int i = 0, remaining = keyBlockLen;
                 remaining > 0;
                 i++, remaining -= 16) {

                sha.update(SSL3_CONST[i]);
                sha.update(masterSecret);
                sha.update(serverRandom);
                sha.update(clientRandom);
                sha.digest(tmp, 0, 20);

                md5.update(masterSecret);
                md5.update(tmp);

                if (remaining >= 16) {
                    md5.digest(keyBlock, i << 4, 16);
                } else {
                    md5.digest(tmp, 0, 16);
                    System.arraycopy(tmp, 0, keyBlock, i << 4, remaining);
                }
            }
        }

        // partition keyblock into individual secrets

        int ofs = 0;
        if (macLength != 0) {
            // mac keys
            clientMacKey = new SecretKeySpec(keyBlock, ofs, macLength, "Mac");
            ofs += macLength;
            serverMacKey = new SecretKeySpec(keyBlock, ofs, macLength, "Mac");
            ofs += macLength;
        }

        if (keyLength == 0) { // SSL_RSA_WITH_NULL_* ciphersuites
            return new TlsKeyMaterialSpec(clientMacKey, serverMacKey);
        }

        String alg = spec.getCipherAlgorithm();

        // cipher keys
        byte[] clientKeyBytes = new byte[keyLength];
        System.arraycopy(keyBlock, ofs, clientKeyBytes, 0, keyLength);
        ofs += keyLength;

        byte[] serverKeyBytes = new byte[keyLength];
        System.arraycopy(keyBlock, ofs, serverKeyBytes, 0, keyLength);
        ofs += keyLength;

        try {
            if (isExportable == false) {
                // cipher keys
                clientCipherKey = new SecretKeySpec(clientKeyBytes, alg);
                serverCipherKey = new SecretKeySpec(serverKeyBytes, alg);

                // IV keys if needed.
                if (ivLength != 0) {
                    clientIv = new IvParameterSpec(keyBlock, ofs, ivLength);
                    ofs += ivLength;
                    serverIv = new IvParameterSpec(keyBlock, ofs, ivLength);
                    ofs += ivLength;
                }
            } else {
                // if exportable suites, calculate the alternate
                // cipher key expansion and IV generation
                if (protocolVersion >= 0x0302) {
                    // TLS 1.1+
                    throw new RuntimeException(
                            "Internal Error:  TLS 1.1+ should not be negotiating" +
                                    "exportable ciphersuites");
                } else if (protocolVersion == 0x0301) {
                    // TLS 1.0
                    byte[] seed = concat(clientRandom, serverRandom);

                    byte[] tmp = doTLS10PRF(clientKeyBytes,
                            LABEL_CLIENT_WRITE_KEY, seed, expandedKeyLength, md5, sha);
                    clientCipherKey = new SecretKeySpec(tmp, alg);
                    Arrays.fill(tmp, (byte) 0);

                    tmp = doTLS10PRF(serverKeyBytes, LABEL_SERVER_WRITE_KEY, seed,
                            expandedKeyLength, md5, sha);
                    serverCipherKey = new SecretKeySpec(tmp, alg);
                    Arrays.fill(tmp, (byte) 0);

                    if (ivLength != 0) {
                        byte[] block = doTLS10PRF(null, LABEL_IV_BLOCK, seed,
                                ivLength << 1, md5, sha);
                        clientIv = new IvParameterSpec(block, 0, ivLength);
                        serverIv = new IvParameterSpec(block, ivLength, ivLength);
                    }
                } else {
                    // SSLv3
                    byte[] tmp = new byte[expandedKeyLength];
                    byte[] digest;

                    md5.update(clientKeyBytes);
                    md5.update(clientRandom);
                    md5.update(serverRandom);
                    digest = md5.digest();
                    System.arraycopy(digest, 0, tmp, 0, expandedKeyLength);
                    clientCipherKey = new SecretKeySpec(tmp, alg);
                    Arrays.fill(digest, (byte) 0);

                    md5.update(serverKeyBytes);
                    md5.update(serverRandom);
                    md5.update(clientRandom);
                    digest = md5.digest();
                    System.arraycopy(digest, 0, tmp, 0, expandedKeyLength);
                    serverCipherKey = new SecretKeySpec(tmp, alg);
                    Arrays.fill(digest, (byte) 0);

                    Arrays.fill(tmp, (byte) 0);

                    if (ivLength != 0) {
                        tmp = new byte[ivLength];

                        md5.update(clientRandom);
                        md5.update(serverRandom);
                        System.arraycopy(md5.digest(), 0, tmp, 0, ivLength);
                        clientIv = new IvParameterSpec(tmp);

                        md5.update(serverRandom);
                        md5.update(clientRandom);
                        System.arraycopy(md5.digest(), 0, tmp, 0, ivLength);
                        serverIv = new IvParameterSpec(tmp);
                    }
                }
            }
        } finally {
            Arrays.fill(serverKeyBytes, (byte) 0);
            Arrays.fill(clientKeyBytes, (byte) 0);
            Arrays.fill(keyBlock, (byte) 0);
        }

        return new TlsKeyMaterialSpec(clientMacKey, serverMacKey,
            clientCipherKey, clientIv, serverCipherKey, serverIv);
    }

}
