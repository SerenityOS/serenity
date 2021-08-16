/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.*;
import sun.security.internal.spec.TlsMasterSecretParameterSpec;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;

import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * KeyGenerator for the SSL/TLS master secret.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
public final class P11TlsMasterSecretGenerator extends KeyGeneratorSpi {

    private static final String MSG = "TlsMasterSecretGenerator must be "
        + "initialized using a TlsMasterSecretParameterSpec";

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private long mechanism;

    private int tlsVersion;

    @SuppressWarnings("deprecation")
    private TlsMasterSecretParameterSpec spec;
    private P11Key p11Key;

    CK_VERSION ckVersion;

    // whether SSLv3 is supported
    private final boolean supportSSLv3;

    P11TlsMasterSecretGenerator(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;

        // Given the current lookup order specified in SunPKCS11.java, if
        // CKM_SSL3_MASTER_KEY_DERIVE is not used to construct this object,
        // it means that this mech is disabled or unsupported.
        supportSSLv3 = (mechanism == CKM_SSL3_MASTER_KEY_DERIVE);
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    @SuppressWarnings("deprecation")
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof TlsMasterSecretParameterSpec == false) {
            throw new InvalidAlgorithmParameterException(MSG);
        }

        TlsMasterSecretParameterSpec spec = (TlsMasterSecretParameterSpec)params;
        tlsVersion = (spec.getMajorVersion() << 8) | spec.getMinorVersion();
        if ((tlsVersion == 0x0300 && !supportSSLv3) ||
                (tlsVersion < 0x0300) || (tlsVersion > 0x0303)) {
             throw new InvalidAlgorithmParameterException
                    ("Only" + (supportSSLv3? " SSL 3.0,": "") +
                     " TLS 1.0, TLS 1.1 and TLS 1.2 are supported (" +
                     tlsVersion + ")");
        }

        SecretKey key = spec.getPremasterSecret();
        // algorithm should be either TlsRsaPremasterSecret or TlsPremasterSecret,
        // but we omit the check
        try {
            p11Key = P11SecretKeyFactory.convertKey(token, key, null);
        } catch (InvalidKeyException e) {
            throw new InvalidAlgorithmParameterException("init() failed", e);
        }
        this.spec = spec;
        final boolean isTlsRsaPremasterSecret =
                p11Key.getAlgorithm().equals("TlsRsaPremasterSecret");
        if (tlsVersion == 0x0300) {
            mechanism = isTlsRsaPremasterSecret ?
                    CKM_SSL3_MASTER_KEY_DERIVE : CKM_SSL3_MASTER_KEY_DERIVE_DH;
        } else if (tlsVersion == 0x0301 || tlsVersion == 0x0302) {
            mechanism = isTlsRsaPremasterSecret ?
                    CKM_TLS_MASTER_KEY_DERIVE : CKM_TLS_MASTER_KEY_DERIVE_DH;
        } else if (tlsVersion == 0x0303) {
            mechanism = isTlsRsaPremasterSecret ?
                    CKM_TLS12_MASTER_KEY_DERIVE : CKM_TLS12_MASTER_KEY_DERIVE_DH;
        }
        if (isTlsRsaPremasterSecret) {
            ckVersion = new CK_VERSION(0, 0);
        } else {
            // Note: we use DH for all non-RSA premaster secrets. That includes
            // Kerberos. That should not be a problem because master secret
            // calculation is always a straightforward application of the
            // TLS PRF (or the SSL equivalent).
            // The only thing special about RSA master secret calculation is
            // that it extracts the version numbers from the premaster secret.
            ckVersion = null;
        }
    }

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    protected SecretKey engineGenerateKey() {
        if (spec == null) {
            throw new IllegalStateException
                ("TlsMasterSecretGenerator must be initialized");
        }
        byte[] clientRandom = spec.getClientRandom();
        byte[] serverRandom = spec.getServerRandom();
        CK_SSL3_RANDOM_DATA random =
                new CK_SSL3_RANDOM_DATA(clientRandom, serverRandom);
        CK_MECHANISM ckMechanism = null;
        if (tlsVersion < 0x0303) {
            CK_SSL3_MASTER_KEY_DERIVE_PARAMS params =
                    new CK_SSL3_MASTER_KEY_DERIVE_PARAMS(random, ckVersion);
            ckMechanism = new CK_MECHANISM(mechanism, params);
        } else if (tlsVersion == 0x0303) {
            CK_TLS12_MASTER_KEY_DERIVE_PARAMS params =
                    new CK_TLS12_MASTER_KEY_DERIVE_PARAMS(random, ckVersion,
                            Functions.getHashMechId(spec.getPRFHashAlg()));
            ckMechanism = new CK_MECHANISM(mechanism, params);
        }
        Session session = null;
        long p11KeyID = p11Key.getKeyID();
        try {
            session = token.getObjSession();
            CK_ATTRIBUTE[] attributes = token.getAttributes(O_GENERATE,
                CKO_SECRET_KEY, CKK_GENERIC_SECRET, new CK_ATTRIBUTE[0]);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    ckMechanism, p11KeyID, attributes);
            int major, minor;
            if (ckVersion == null) {
                major = -1;
                minor = -1;
            } else {
                major = ckVersion.major;
                minor = ckVersion.minor;
            }
            return P11Key.masterSecretKey(session, keyID,
                "TlsMasterSecret", 48 << 3, attributes, major, minor);
        } catch (Exception e) {
            throw new ProviderException("Could not generate key", e);
        } finally {
            p11Key.releaseKeyID();
            token.releaseSession(session);
        }
    }
}
