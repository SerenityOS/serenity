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

final class SSLBasicKeyDerivation implements SSLKeyDerivation {
    private final String hashAlg;
    private final SecretKey secret;
    private final byte[] hkdfInfo;

    SSLBasicKeyDerivation(SecretKey secret, String hashAlg,
            byte[] label, byte[] context, int length) {
        this.hashAlg = hashAlg.replace("-", "");
        this.secret = secret;
        this.hkdfInfo = createHkdfInfo(label, context, length);
    }

    @Override
    public SecretKey deriveKey(String algorithm,
            AlgorithmParameterSpec keySpec) throws IOException {
        try {
            HKDF hkdf = new HKDF(hashAlg);
            return hkdf.expand(secret, hkdfInfo,
                    ((SecretSizeSpec)keySpec).length, algorithm);
        } catch (GeneralSecurityException gse) {
            throw (SSLHandshakeException) new SSLHandshakeException(
                "Could not generate secret").initCause(gse);
        }
    }

    private static byte[] createHkdfInfo(
            byte[] label, byte[] context, int length) {
        byte[] info = new byte[4 + label.length + context.length];
        ByteBuffer m = ByteBuffer.wrap(info);
        try {
            Record.putInt16(m, length);
            Record.putBytes8(m, label);
            Record.putBytes8(m, context);
        } catch (IOException ioe) {
            // unlikely
        }
        return info;
    }

    static class SecretSizeSpec implements AlgorithmParameterSpec {
        final int length;

        SecretSizeSpec(int length) {
            this.length = length;
        }
    }
}
