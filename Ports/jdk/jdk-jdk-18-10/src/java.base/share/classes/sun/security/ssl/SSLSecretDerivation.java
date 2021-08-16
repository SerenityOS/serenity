/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLHandshakeException;
import sun.security.ssl.CipherSuite.HashAlg;

final class SSLSecretDerivation implements SSLKeyDerivation {
    private static final byte[] sha256EmptyDigest = new byte[] {
        (byte)0xE3, (byte)0xB0, (byte)0xC4, (byte)0x42,
        (byte)0x98, (byte)0xFC, (byte)0x1C, (byte)0x14,
        (byte)0x9A, (byte)0xFB, (byte)0xF4, (byte)0xC8,
        (byte)0x99, (byte)0x6F, (byte)0xB9, (byte)0x24,
        (byte)0x27, (byte)0xAE, (byte)0x41, (byte)0xE4,
        (byte)0x64, (byte)0x9B, (byte)0x93, (byte)0x4C,
        (byte)0xA4, (byte)0x95, (byte)0x99, (byte)0x1B,
        (byte)0x78, (byte)0x52, (byte)0xB8, (byte)0x55
    };

    private static final byte[] sha384EmptyDigest = new byte[] {
        (byte)0x38, (byte)0xB0, (byte)0x60, (byte)0xA7,
        (byte)0x51, (byte)0xAC, (byte)0x96, (byte)0x38,
        (byte)0x4C, (byte)0xD9, (byte)0x32, (byte)0x7E,
        (byte)0xB1, (byte)0xB1, (byte)0xE3, (byte)0x6A,
        (byte)0x21, (byte)0xFD, (byte)0xB7, (byte)0x11,
        (byte)0x14, (byte)0xBE, (byte)0x07, (byte)0x43,
        (byte)0x4C, (byte)0x0C, (byte)0xC7, (byte)0xBF,
        (byte)0x63, (byte)0xF6, (byte)0xE1, (byte)0xDA,
        (byte)0x27, (byte)0x4E, (byte)0xDE, (byte)0xBF,
        (byte)0xE7, (byte)0x6F, (byte)0x65, (byte)0xFB,
        (byte)0xD5, (byte)0x1A, (byte)0xD2, (byte)0xF1,
        (byte)0x48, (byte)0x98, (byte)0xB9, (byte)0x5B
    };

    private final HashAlg hashAlg;
    private final SecretKey secret;
    private final byte[] transcriptHash;  // handshake messages transcript hash

    SSLSecretDerivation(
            HandshakeContext context, SecretKey secret) {
        this.secret = secret;
        this.hashAlg = context.negotiatedCipherSuite.hashAlg;
        String hkdfAlg = "HKDF-Expand/Hmac" + hashAlg.name.replace("-", "");
        context.handshakeHash.update();
        this.transcriptHash = context.handshakeHash.digest();
    }

    SSLSecretDerivation forContext(HandshakeContext context) {
        return new SSLSecretDerivation(context, secret);
    }

    @Override
    public SecretKey deriveKey(String algorithm,
            AlgorithmParameterSpec params) throws IOException {
        SecretSchedule ks = SecretSchedule.valueOf(algorithm);
        try {
            byte[] expandContext;
            if (ks == SecretSchedule.TlsSaltSecret) {
                if (hashAlg == HashAlg.H_SHA256) {
                    expandContext = sha256EmptyDigest;
                } else if (hashAlg == HashAlg.H_SHA384) {
                    expandContext = sha384EmptyDigest;
                } else {
                    // unlikely, but please update if more hash algorithm
                    // get supported in the future.
                    throw new SSLHandshakeException(
                            "Unexpected unsupported hash algorithm: " +
                            algorithm);
                }
            } else {
                expandContext = transcriptHash;
            }
            byte[] hkdfInfo = createHkdfInfo(ks.label,
                    expandContext, hashAlg.hashLength);

            HKDF hkdf = new HKDF(hashAlg.name);
            return hkdf.expand(secret, hkdfInfo, hashAlg.hashLength, algorithm);
        } catch (GeneralSecurityException gse) {
            throw (SSLHandshakeException) new SSLHandshakeException(
                "Could not generate secret").initCause(gse);
        }
    }

    public static byte[] createHkdfInfo(
            byte[] label, byte[] context, int length) {
        byte[] info = new byte[4 + label.length + context.length];
        ByteBuffer m = ByteBuffer.wrap(info);
        try {
            Record.putInt16(m, length);
            Record.putBytes8(m, label);
            Record.putBytes8(m, context);
        } catch (IOException ioe) {
            // unlikely
            throw new RuntimeException("Unexpected exception", ioe);
        }

        return info;
    }

    private enum SecretSchedule {
        // Note that we use enum name as the key/secret name.
        TlsSaltSecret                       ("derived"),
        TlsExtBinderKey                     ("ext binder"),
        TlsResBinderKey                     ("res binder"),
        TlsClientEarlyTrafficSecret         ("c e traffic"),
        TlsEarlyExporterMasterSecret        ("e exp master"),
        TlsClientHandshakeTrafficSecret     ("c hs traffic"),
        TlsServerHandshakeTrafficSecret     ("s hs traffic"),
        TlsClientAppTrafficSecret           ("c ap traffic"),
        TlsServerAppTrafficSecret           ("s ap traffic"),
        TlsExporterMasterSecret             ("exp master"),
        TlsResumptionMasterSecret           ("res master");

        private final byte[] label;

        private SecretSchedule(String label) {
            this.label = ("tls13 " + label).getBytes();
        }
    }
}
