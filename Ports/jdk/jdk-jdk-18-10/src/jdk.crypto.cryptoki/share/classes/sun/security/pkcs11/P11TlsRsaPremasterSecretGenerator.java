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
import javax.crypto.spec.*;

import sun.security.internal.spec.TlsRsaPremasterSecretParameterSpec;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * KeyGenerator for the SSL/TLS RSA premaster secret.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
final class P11TlsRsaPremasterSecretGenerator extends KeyGeneratorSpi {

    private static final String MSG = "TlsRsaPremasterSecretGenerator must be "
        + "initialized using a TlsRsaPremasterSecretParameterSpec";

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private long mechanism;

    @SuppressWarnings("deprecation")
    private TlsRsaPremasterSecretParameterSpec spec;

    // whether SSLv3 is supported
    private final boolean supportSSLv3;

    P11TlsRsaPremasterSecretGenerator(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;

        // Given the current lookup order specified in SunPKCS11.java,
        // if CKM_SSL3_PRE_MASTER_KEY_GEN is not used to construct this object,
        // it means that this mech is disabled or unsupported.
        this.supportSSLv3 = (mechanism == CKM_SSL3_PRE_MASTER_KEY_GEN);
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    @SuppressWarnings("deprecation")
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(params instanceof TlsRsaPremasterSecretParameterSpec)) {
            throw new InvalidAlgorithmParameterException(MSG);
        }

        TlsRsaPremasterSecretParameterSpec spec =
            (TlsRsaPremasterSecretParameterSpec) params;
        int tlsVersion = (spec.getMajorVersion() << 8) | spec.getMinorVersion();

        if ((tlsVersion == 0x0300 && !supportSSLv3) ||
                (tlsVersion < 0x0300) || (tlsVersion > 0x0303)) {
             throw new InvalidAlgorithmParameterException
                    ("Only" + (supportSSLv3? " SSL 3.0,": "") +
                     " TLS 1.0, TLS 1.1 and TLS 1.2 are supported (" +
                     tlsVersion + ")");
        }
        this.spec = spec;
    }

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    // Only can be used in client side to generate TLS RSA premaster secret.
    protected SecretKey engineGenerateKey() {
        if (spec == null) {
            throw new IllegalStateException
                        ("TlsRsaPremasterSecretGenerator must be initialized");
        }

        CK_VERSION version = new CK_VERSION(
                        spec.getMajorVersion(), spec.getMinorVersion());
        Session session = null;
        try {
            session = token.getObjSession();
            CK_ATTRIBUTE[] attributes = token.getAttributes(
                    O_GENERATE, CKO_SECRET_KEY,
                    CKK_GENERIC_SECRET, new CK_ATTRIBUTE[0]);
            long keyID = token.p11.C_GenerateKey(session.id(),
                    new CK_MECHANISM(mechanism, version), attributes);
            SecretKey key = P11Key.secretKey(session,
                    keyID, "TlsRsaPremasterSecret", 48 << 3, attributes);
            return key;
        } catch (PKCS11Exception e) {
            throw new ProviderException(
                    "Could not generate premaster secret", e);
        } finally {
            token.releaseSession(session);
        }
    }

}
