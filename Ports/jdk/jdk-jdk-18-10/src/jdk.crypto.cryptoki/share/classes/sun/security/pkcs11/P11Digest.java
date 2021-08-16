/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.nio.ByteBuffer;

import java.security.*;

import javax.crypto.SecretKey;

import sun.nio.ch.DirectBuffer;

import sun.security.util.MessageDigestSpi2;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * MessageDigest implementation class. This class currently supports
 * MD2, MD5, SHA-1, SHA-2 family (SHA-224, SHA-256, SHA-384, and SHA-512)
 * and SHA-3 family (SHA3-224, SHA3-256, SHA3-384, and SHA3-512) of digests.
 *
 * Note that many digest operations are on fairly small amounts of data
 * (less than 100 bytes total). For example, the 2nd hashing in HMAC or
 * the PRF in TLS. In order to speed those up, we use some buffering to
 * minimize number of the Java->native transitions.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11Digest extends MessageDigestSpi implements Cloneable,
    MessageDigestSpi2 {

    /* fields initialized, no session acquired */
    private static final int S_BLANK    = 1;

    /* data in buffer, session acquired, but digest not initialized */
    private static final int S_BUFFERED = 2;

    /* session initialized for digesting */
    private static final int S_INIT     = 3;

    private static final int BUFFER_SIZE = 96;

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id object
    private final CK_MECHANISM mechanism;

    // length of the digest in bytes
    private final int digestLength;

    // associated session, if any
    private Session session;

    // current state, one of S_* above
    private int state;

    // buffer to reduce number of JNI calls
    private byte[] buffer;

    // offset into the buffer
    private int bufOfs;

    P11Digest(Token token, String algorithm, long mechanism) {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = new CK_MECHANISM(mechanism);
        switch ((int)mechanism) {
        case (int)CKM_MD2:
        case (int)CKM_MD5:
            digestLength = 16;
            break;
        case (int)CKM_SHA_1:
            digestLength = 20;
            break;
        case (int)CKM_SHA224:
        case (int)CKM_SHA512_224:
        case (int)CKM_SHA3_224:
            digestLength = 28;
            break;
        case (int)CKM_SHA256:
        case (int)CKM_SHA512_256:
        case (int)CKM_SHA3_256:
            digestLength = 32;
            break;
        case (int)CKM_SHA384:
        case (int)CKM_SHA3_384:
            digestLength = 48;
            break;
        case (int)CKM_SHA512:
        case (int)CKM_SHA3_512:
            digestLength = 64;
            break;
        default:
            throw new ProviderException("Unknown mechanism: " + mechanism);
        }
        buffer = new byte[BUFFER_SIZE];
        state = S_BLANK;
    }

    // see JCA spec
    protected int engineGetDigestLength() {
        return digestLength;
    }

    private void fetchSession() {
        token.ensureValid();
        if (state == S_BLANK) {
            try {
                session = token.getOpSession();
                state = S_BUFFERED;
            } catch (PKCS11Exception e) {
                throw new ProviderException("No more session available", e);
            }
        }
    }

    // see JCA spec
    protected void engineReset() {
        token.ensureValid();

        if (session != null) {
            if (state == S_INIT && token.explicitCancel == true
                    && session.hasObjects() == false) {
                session = token.killSession(session);
            } else {
                session = token.releaseSession(session);
            }
        }
        state = S_BLANK;
        bufOfs = 0;
    }

    // see JCA spec
    protected byte[] engineDigest() {
        try {
            byte[] digest = new byte[digestLength];
            int n = engineDigest(digest, 0, digestLength);
            return digest;
        } catch (DigestException e) {
            throw new ProviderException("internal error", e);
        }
    }

    // see JCA spec
    protected int engineDigest(byte[] digest, int ofs, int len)
            throws DigestException {
        if (len < digestLength) {
            throw new DigestException("Length must be at least " +
                    digestLength);
        }

        fetchSession();
        try {
            int n;
            if (state == S_BUFFERED) {
                n = token.p11.C_DigestSingle(session.id(), mechanism, buffer, 0,
                        bufOfs, digest, ofs, len);
                bufOfs = 0;
            } else {
                if (bufOfs != 0) {
                    token.p11.C_DigestUpdate(session.id(), 0, buffer, 0,
                            bufOfs);
                    bufOfs = 0;
                }
                n = token.p11.C_DigestFinal(session.id(), digest, ofs, len);
            }
            if (n != digestLength) {
                throw new ProviderException("internal digest length error");
            }
            return n;
        } catch (PKCS11Exception e) {
            throw new ProviderException("digest() failed", e);
        } finally {
            engineReset();
        }
    }

    // see JCA spec
    protected void engineUpdate(byte in) {
        byte[] temp = { in };
        engineUpdate(temp, 0, 1);
    }

    // see JCA spec
    protected void engineUpdate(byte[] in, int ofs, int len) {
        if (len <= 0) {
            return;
        }

        fetchSession();
        try {
            if (state == S_BUFFERED) {
                token.p11.C_DigestInit(session.id(), mechanism);
                state = S_INIT;
            }
            if ((bufOfs != 0) && (bufOfs + len > buffer.length)) {
                // process the buffered data
                token.p11.C_DigestUpdate(session.id(), 0, buffer, 0, bufOfs);
                bufOfs = 0;
            }
            if (bufOfs + len > buffer.length) {
                // process the new data
                token.p11.C_DigestUpdate(session.id(), 0, in, ofs, len);
             } else {
                // buffer the new data
                System.arraycopy(in, ofs, buffer, bufOfs, len);
                bufOfs += len;
            }
        } catch (PKCS11Exception e) {
            engineReset();
            throw new ProviderException("update() failed", e);
        }
    }

    // Called by SunJSSE via reflection during the SSL 3.0 handshake if
    // the master secret is sensitive.
    // Note: Change to protected after this method is moved from
    // sun.security.util.MessageSpi2 interface to
    // java.security.MessageDigestSpi class
    public void engineUpdate(SecretKey key) throws InvalidKeyException {
        // SunJSSE calls this method only if the key does not have a RAW
        // encoding, i.e. if it is sensitive. Therefore, no point in calling
        // SecretKeyFactory to try to convert it. Just verify it ourselves.
        if (key instanceof P11Key == false) {
            throw new InvalidKeyException("Not a P11Key: " + key);
        }
        P11Key p11Key = (P11Key)key;
        if (p11Key.token != token) {
            throw new InvalidKeyException("Not a P11Key of this provider: " +
                    key);
        }

        fetchSession();
        long p11KeyID = p11Key.getKeyID();
        try {
            if (state == S_BUFFERED) {
                token.p11.C_DigestInit(session.id(), mechanism);
                state = S_INIT;
            }

            if (bufOfs != 0) {
                token.p11.C_DigestUpdate(session.id(), 0, buffer, 0, bufOfs);
                bufOfs = 0;
            }
            token.p11.C_DigestKey(session.id(), p11KeyID);
        } catch (PKCS11Exception e) {
            engineReset();
            throw new ProviderException("update(SecretKey) failed", e);
        } finally {
            p11Key.releaseKeyID();
        }
    }

    // see JCA spec
    protected void engineUpdate(ByteBuffer byteBuffer) {
        int len = byteBuffer.remaining();
        if (len <= 0) {
            return;
        }

        if (byteBuffer instanceof DirectBuffer == false) {
            super.engineUpdate(byteBuffer);
            return;
        }

        fetchSession();
        long addr = ((DirectBuffer)byteBuffer).address();
        int ofs = byteBuffer.position();
        try {
            if (state == S_BUFFERED) {
                token.p11.C_DigestInit(session.id(), mechanism);
                state = S_INIT;
            }
            if (bufOfs != 0) {
                token.p11.C_DigestUpdate(session.id(), 0, buffer, 0, bufOfs);
                bufOfs = 0;
            }
            token.p11.C_DigestUpdate(session.id(), addr + ofs, null, 0, len);
            byteBuffer.position(ofs + len);
        } catch (PKCS11Exception e) {
            engineReset();
            throw new ProviderException("update() failed", e);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        P11Digest copy = (P11Digest) super.clone();
        copy.buffer = buffer.clone();
        try {
            if (session != null) {
                copy.session = copy.token.getOpSession();
            }
            if (state == S_INIT) {
                byte[] stateValues =
                    token.p11.C_GetOperationState(session.id());
                token.p11.C_SetOperationState(copy.session.id(),
                                              stateValues, 0, 0);
            }
        } catch (PKCS11Exception e) {
            throw (CloneNotSupportedException)
                (new CloneNotSupportedException(algorithm).initCause(e));
        }
        return copy;
    }
}
