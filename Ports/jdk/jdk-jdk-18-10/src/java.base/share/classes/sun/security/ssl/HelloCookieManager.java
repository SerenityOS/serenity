/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import static sun.security.ssl.ClientHello.ClientHelloMessage;

/**
 *  (D)TLS handshake cookie manager
 */
abstract class HelloCookieManager {

    static class Builder {

        final SecureRandom secureRandom;

        private volatile D10HelloCookieManager d10HelloCookieManager;
        private volatile D13HelloCookieManager d13HelloCookieManager;
        private volatile T13HelloCookieManager t13HelloCookieManager;

        private final ReentrantLock managerLock = new ReentrantLock();

        Builder(SecureRandom secureRandom) {
            this.secureRandom = secureRandom;
        }

        HelloCookieManager valueOf(ProtocolVersion protocolVersion) {
            if (protocolVersion.isDTLS) {
                if (protocolVersion.useTLS13PlusSpec()) {
                    if (d13HelloCookieManager != null) {
                        return d13HelloCookieManager;
                    }

                    managerLock.lock();
                    try {
                        if (d13HelloCookieManager == null) {
                            d13HelloCookieManager =
                                    new D13HelloCookieManager(secureRandom);
                        }
                    } finally {
                        managerLock.unlock();
                    }

                    return d13HelloCookieManager;
                } else {
                    if (d10HelloCookieManager != null) {
                        return d10HelloCookieManager;
                    }

                    managerLock.lock();
                    try {
                        if (d10HelloCookieManager == null) {
                            d10HelloCookieManager =
                                    new D10HelloCookieManager(secureRandom);
                        }
                    } finally {
                        managerLock.unlock();
                    }

                    return d10HelloCookieManager;
                }
            } else {
                if (protocolVersion.useTLS13PlusSpec()) {
                    if (t13HelloCookieManager != null) {
                        return t13HelloCookieManager;
                    }

                    managerLock.lock();
                    try {
                        if (t13HelloCookieManager == null) {
                            t13HelloCookieManager =
                                    new T13HelloCookieManager(secureRandom);
                        }
                    } finally {
                        managerLock.unlock();
                    }

                    return t13HelloCookieManager;
                }
            }

            return null;
        }
    }

    abstract byte[] createCookie(ServerHandshakeContext context,
                ClientHelloMessage clientHello) throws IOException;

    abstract boolean isCookieValid(ServerHandshakeContext context,
            ClientHelloMessage clientHello, byte[] cookie) throws IOException;

    // DTLS 1.0/1.2
    private static final
            class D10HelloCookieManager extends HelloCookieManager {

        final SecureRandom secureRandom;
        private int               cookieVersion;  // allow to wrap, version + sequence
        private final byte[]      cookieSecret;
        private final byte[]      legacySecret;

        private final ReentrantLock d10ManagerLock = new ReentrantLock();

        D10HelloCookieManager(SecureRandom secureRandom) {
            this.secureRandom = secureRandom;

            this.cookieVersion = secureRandom.nextInt();
            this.cookieSecret = new byte[32];
            this.legacySecret = new byte[32];

            secureRandom.nextBytes(cookieSecret);
            System.arraycopy(cookieSecret, 0, legacySecret, 0, 32);
        }

        @Override
        byte[] createCookie(ServerHandshakeContext context,
                ClientHelloMessage clientHello) throws IOException {
            int version;
            byte[] secret;

            d10ManagerLock.lock();
            try {
                version = cookieVersion;
                secret = cookieSecret;

                // the cookie secret usage limit is 2^24
                if ((cookieVersion & 0xFFFFFF) == 0) {  // reset the secret
                    System.arraycopy(cookieSecret, 0, legacySecret, 0, 32);
                    secureRandom.nextBytes(cookieSecret);
                }

                cookieVersion++;
            } finally {
                d10ManagerLock.unlock();
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                    "MessageDigest algorithm SHA-256 is not available", nsae);
            }
            byte[] helloBytes = clientHello.getHelloCookieBytes();
            md.update(helloBytes);
            byte[] cookie = md.digest(secret);      // 32 bytes
            cookie[0] = (byte)((version >> 24) & 0xFF);

            return cookie;
        }

        @Override
        boolean isCookieValid(ServerHandshakeContext context,
            ClientHelloMessage clientHello, byte[] cookie) throws IOException {
            // no cookie exchange or not a valid cookie length
            if ((cookie == null) || (cookie.length != 32)) {
                return false;
            }

            byte[] secret;
            d10ManagerLock.lock();
            try {
                if (((cookieVersion >> 24) & 0xFF) == cookie[0]) {
                    secret = cookieSecret;
                } else {
                    secret = legacySecret;  // including out of window cookies
                }
            } finally {
                d10ManagerLock.unlock();
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                    "MessageDigest algorithm SHA-256 is not available", nsae);
            }
            byte[] helloBytes = clientHello.getHelloCookieBytes();
            md.update(helloBytes);
            byte[] target = md.digest(secret);      // 32 bytes
            target[0] = cookie[0];

            return Arrays.equals(target, cookie);
        }
    }

    private static final
            class D13HelloCookieManager extends HelloCookieManager {
        D13HelloCookieManager(SecureRandom secureRandom) {
        }

        @Override
        byte[] createCookie(ServerHandshakeContext context,
                ClientHelloMessage clientHello) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        boolean isCookieValid(ServerHandshakeContext context,
            ClientHelloMessage clientHello, byte[] cookie) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final
            class T13HelloCookieManager extends HelloCookieManager {

        final SecureRandom secureRandom;
        private int             cookieVersion;      // version + sequence
        private final byte[]    cookieSecret;
        private final byte[]    legacySecret;

        private final ReentrantLock t13ManagerLock = new ReentrantLock();

        T13HelloCookieManager(SecureRandom secureRandom) {
            this.secureRandom = secureRandom;
            this.cookieVersion = secureRandom.nextInt();
            this.cookieSecret = new byte[64];
            this.legacySecret = new byte[64];

            secureRandom.nextBytes(cookieSecret);
            System.arraycopy(cookieSecret, 0, legacySecret, 0, 64);
        }

        @Override
        byte[] createCookie(ServerHandshakeContext context,
                ClientHelloMessage clientHello) throws IOException {
            int version;
            byte[] secret;

            t13ManagerLock.lock();
            try {
                version = cookieVersion;
                secret = cookieSecret;

                // the cookie secret usage limit is 2^24
                if ((cookieVersion & 0xFFFFFF) == 0) {  // reset the secret
                    System.arraycopy(cookieSecret, 0, legacySecret, 0, 64);
                    secureRandom.nextBytes(cookieSecret);
                }

                cookieVersion++;        // allow wrapped version number
            } finally {
                t13ManagerLock.unlock();
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance(
                    context.negotiatedCipherSuite.hashAlg.name);
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                        "MessageDigest algorithm " +
                        context.negotiatedCipherSuite.hashAlg.name +
                        " is not available", nsae);
            }
            byte[] headerBytes = clientHello.getHeaderBytes();
            md.update(headerBytes);
            byte[] headerCookie = md.digest(secret);

            // hash of ClientHello handshake message
            context.handshakeHash.update();
            byte[] clientHelloHash = context.handshakeHash.digest();

            // version and cipher suite
            //
            // Store the negotiated cipher suite in the cookie as well.
            // cookie[0]/[1]: cipher suite
            // cookie[2]: cookie version
            // + (hash length): Mac(ClientHello header)
            // + (hash length): Hash(ClientHello)
            byte[] prefix = new byte[] {
                    (byte)((context.negotiatedCipherSuite.id >> 8) & 0xFF),
                    (byte)(context.negotiatedCipherSuite.id & 0xFF),
                    (byte)((version >> 24) & 0xFF)
                };

            byte[] cookie = Arrays.copyOf(prefix,
                prefix.length + headerCookie.length + clientHelloHash.length);
            System.arraycopy(headerCookie, 0, cookie,
                prefix.length, headerCookie.length);
            System.arraycopy(clientHelloHash, 0, cookie,
                prefix.length + headerCookie.length, clientHelloHash.length);

            return cookie;
        }

        @Override
        boolean isCookieValid(ServerHandshakeContext context,
            ClientHelloMessage clientHello, byte[] cookie) throws IOException {
            // no cookie exchange or not a valid cookie length
            if ((cookie == null) || (cookie.length <= 32)) {    // 32: roughly
                return false;
            }

            int csId = ((cookie[0] & 0xFF) << 8) | (cookie[1] & 0xFF);
            CipherSuite cs = CipherSuite.valueOf(csId);
            if (cs == null || cs.hashAlg == null || cs.hashAlg.hashLength == 0) {
                return false;
            }

            int hashLen = cs.hashAlg.hashLength;
            if (cookie.length != (3 + hashLen * 2)) {
                return false;
            }

            byte[] prevHeadCookie =
                    Arrays.copyOfRange(cookie, 3, 3 + hashLen);
            byte[] prevClientHelloHash =
                    Arrays.copyOfRange(cookie, 3 + hashLen, cookie.length);

            byte[] secret;
            t13ManagerLock.lock();
            try {
                if ((byte)((cookieVersion >> 24) & 0xFF) == cookie[2]) {
                    secret = cookieSecret;
                } else {
                    secret = legacySecret;  // including out of window cookies
                }
            } finally {
                t13ManagerLock.unlock();
            }

            MessageDigest md;
            try {
                md = MessageDigest.getInstance(cs.hashAlg.name);
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                        "MessageDigest algorithm " +
                        cs.hashAlg.name + " is not available", nsae);
            }
            byte[] headerBytes = clientHello.getHeaderBytes();
            md.update(headerBytes);
            byte[] headerCookie = md.digest(secret);

            if (!Arrays.equals(headerCookie, prevHeadCookie)) {
                return false;
            }

            // Use the ClientHello hash in the cookie for transtript
            // hash calculation for stateless HelloRetryRequest.
            //
            // Transcript-Hash(ClientHello1, HelloRetryRequest, ... Mn) =
            //   Hash(message_hash ||    /* Handshake type */
            //     00 00 Hash.length ||  /* Handshake message length (bytes) */
            //     Hash(ClientHello1) || /* Hash of ClientHello1 */
            //     HelloRetryRequest || ... || Mn)

            // Reproduce HelloRetryRequest handshake message
            byte[] hrrMessage =
                    ServerHello.hrrReproducer.produce(context, clientHello);
            context.handshakeHash.push(hrrMessage);

            // Construct the 1st ClientHello message for transcript hash
            byte[] hashedClientHello = new byte[4 + hashLen];
            hashedClientHello[0] = SSLHandshake.MESSAGE_HASH.id;
            hashedClientHello[1] = (byte)0x00;
            hashedClientHello[2] = (byte)0x00;
            hashedClientHello[3] = (byte)(hashLen & 0xFF);
            System.arraycopy(prevClientHelloHash, 0,
                    hashedClientHello, 4, hashLen);

            context.handshakeHash.push(hashedClientHello);

            return true;
        }
    }
}
