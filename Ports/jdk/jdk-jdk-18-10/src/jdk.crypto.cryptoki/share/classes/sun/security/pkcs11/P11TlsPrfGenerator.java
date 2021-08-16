/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import sun.security.internal.spec.TlsPrfParameterSpec;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * KeyGenerator for the TLS PRF. Note that although the PRF is used in a number
 * of places during the handshake, this class is usually only used to calculate
 * the Finished messages. The reason is that for those other uses more specific
 * PKCS#11 mechanisms have been defined (CKM_SSL3_MASTER_KEY_DERIVE, etc.).
 *
 * <p>This class supports the CKM_TLS_PRF mechanism from PKCS#11 v2.20 and
 * the older NSS private mechanism.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
final class P11TlsPrfGenerator extends KeyGeneratorSpi {

    private static final String MSG =
            "TlsPrfGenerator must be initialized using a TlsPrfParameterSpec";

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private final long mechanism;

    @SuppressWarnings("deprecation")
    private TlsPrfParameterSpec spec;

    private P11Key p11Key;

    P11TlsPrfGenerator(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    @SuppressWarnings("deprecation")
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof TlsPrfParameterSpec == false) {
            throw new InvalidAlgorithmParameterException(MSG);
        }
        this.spec = (TlsPrfParameterSpec)params;
        SecretKey key = spec.getSecret();
        if (key == null) {
            key = NULL_KEY;
        }
        try {
            p11Key = P11SecretKeyFactory.convertKey(token, key, null);
        } catch (InvalidKeyException e) {
            throw new InvalidAlgorithmParameterException("init() failed", e);
        }
    }

    // SecretKeySpec does not allow zero length keys, so we define our
    // own class.
    //
    // As an anonymous class cannot make any guarantees about serialization
    // compatibility, it is nonsensical for an anonymous class to define a
    // serialVersionUID. Suppress warnings relative to missing serialVersionUID
    // field in the anonymous subclass of serializable SecretKey.
    @SuppressWarnings("serial")
    private static final SecretKey NULL_KEY = new SecretKey() {
        public byte[] getEncoded() {
            return new byte[0];
        }
        public String getFormat() {
            return "RAW";
        }
        public String getAlgorithm() {
            return "Generic";
        }
    };

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    protected SecretKey engineGenerateKey() {
        if (spec == null) {
            throw new IllegalStateException("TlsPrfGenerator must be initialized");
        }

        byte[] seed = spec.getSeed();

        // TLS 1.2
        if (mechanism == CKM_TLS_MAC) {
            SecretKey k = null;
            int ulServerOrClient = 0;
            if (spec.getLabel().equals("server finished")) {
                ulServerOrClient = 1;
            }
            if (spec.getLabel().equals("client finished")) {
                ulServerOrClient = 2;
            }

            if (ulServerOrClient != 0) {
                // Finished message
                CK_TLS_MAC_PARAMS params = new CK_TLS_MAC_PARAMS(
                        Functions.getHashMechId(spec.getPRFHashAlg()),
                        spec.getOutputLength(), ulServerOrClient);
                Session session = null;
                long keyID = p11Key.getKeyID();
                try {
                    session = token.getOpSession();
                    token.p11.C_SignInit(session.id(),
                            new CK_MECHANISM(mechanism, params), keyID);
                    token.p11.C_SignUpdate(session.id(), 0, seed, 0, seed.length);
                    byte[] out = token.p11.C_SignFinal
                                        (session.id(), spec.getOutputLength());
                    return new SecretKeySpec(out, "TlsPrf");
                } catch (PKCS11Exception e) {
                    throw new ProviderException("Could not calculate PRF", e);
                } finally {
                    p11Key.releaseKeyID();
                    token.releaseSession(session);
                }
            } else {
                throw new ProviderException("Only Finished message authentication code"+
                                            " generation supported for TLS 1.2.");
            }
        }

        byte[] label = spec.getLabel().getBytes(UTF_8);

        if (mechanism == CKM_NSS_TLS_PRF_GENERAL) {
            Session session = null;
            long keyID = p11Key.getKeyID();
            try {
                session = token.getOpSession();
                token.p11.C_SignInit
                    (session.id(), new CK_MECHANISM(mechanism), keyID);
                token.p11.C_SignUpdate(session.id(), 0, label, 0, label.length);
                token.p11.C_SignUpdate(session.id(), 0, seed, 0, seed.length);
                byte[] out = token.p11.C_SignFinal
                                    (session.id(), spec.getOutputLength());
                return new SecretKeySpec(out, "TlsPrf");
            } catch (PKCS11Exception e) {
                throw new ProviderException("Could not calculate PRF", e);
            } finally {
                p11Key.releaseKeyID();
                token.releaseSession(session);
            }
        }

        // mechanism == CKM_TLS_PRF

        byte[] out = new byte[spec.getOutputLength()];
        CK_TLS_PRF_PARAMS params = new CK_TLS_PRF_PARAMS(seed, label, out);

        Session session = null;
        long keyID = p11Key.getKeyID();
        try {
            session = token.getOpSession();
            token.p11.C_DeriveKey(session.id(),
                new CK_MECHANISM(mechanism, params), keyID, null);
            return new SecretKeySpec(out, "TlsPrf");
        } catch (PKCS11Exception e) {
            throw new ProviderException("Could not calculate PRF", e);
        } finally {
            p11Key.releaseKeyID();
            token.releaseSession(session);
        }
    }

}
