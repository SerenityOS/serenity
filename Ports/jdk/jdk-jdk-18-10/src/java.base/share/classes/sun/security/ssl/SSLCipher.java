/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import sun.security.ssl.Authenticator.MAC;
import static sun.security.ssl.CipherType.*;
import static sun.security.ssl.JsseJce.*;

enum SSLCipher {
    // exportable ciphers
    @SuppressWarnings({"unchecked", "rawtypes"})
    B_NULL("NULL", NULL_CIPHER, 0, 0, 0, 0, true, true,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new NullReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_NONE
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new NullReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_13
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new NullWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_NONE
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new NullWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_13
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC4_40(CIPHER_RC4, STREAM_CIPHER, 5, 16, 0, 0, true, true,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC2_40("RC2", BLOCK_CIPHER, 5, 16, 8, 0, false, true,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_DES_40(CIPHER_DES,  BLOCK_CIPHER, 5, 8, 8, 0, true, true,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            )
        })),

    // domestic strength ciphers
    @SuppressWarnings({"unchecked", "rawtypes"})
    B_RC4_128(CIPHER_RC4, STREAM_CIPHER, 16, 16, 0, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new StreamReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new StreamWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_DES(CIPHER_DES, BLOCK_CIPHER, 8, 8, 8, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_11
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_11
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_3DES(CIPHER_3DES, BLOCK_CIPHER, 24, 24, 8, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_IDEA("IDEA", BLOCK_CIPHER, 16, 16, 8, 0, false, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                null,
                ProtocolVersion.PROTOCOLS_TO_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                null,
                ProtocolVersion.PROTOCOLS_TO_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128(CIPHER_AES, BLOCK_CIPHER, 16, 16, 16, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256(CIPHER_AES, BLOCK_CIPHER, 32, 32, 16, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T10BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T11BlockReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T10BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_TO_10
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T11BlockWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_11_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128_GCM(CIPHER_AES_GCM, AEAD_CIPHER, 16, 16, 12, 4, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256_GCM(CIPHER_AES_GCM, AEAD_CIPHER, 32, 32, 12, 4, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_128_GCM_IV(CIPHER_AES_GCM, AEAD_CIPHER, 16, 16, 12, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_AES_256_GCM_IV(CIPHER_AES_GCM, AEAD_CIPHER, 32, 32, 12, 0, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13GcmReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13GcmWriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        })),

    @SuppressWarnings({"unchecked", "rawtypes"})
    B_CC20_P1305(CIPHER_CHACHA20_POLY1305, AEAD_CIPHER, 32, 32, 12,
            12, true, false,
        (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T12CC20P1305ReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            ),
            new SimpleImmutableEntry<ReadCipherGenerator, ProtocolVersion[]>(
                new T13CC20P1305ReadCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }),
        (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]>[])(new Map.Entry[] {
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T12CC20P1305WriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_12
            ),
            new SimpleImmutableEntry<WriteCipherGenerator, ProtocolVersion[]>(
                new T13CC20P1305WriteCipherGenerator(),
                ProtocolVersion.PROTOCOLS_OF_13
            )
        }));

    // descriptive name including key size, e.g. AES/128
    final String description;

    // JCE cipher transformation string, e.g. AES/CBC/NoPadding
    final String transformation;

    // algorithm name, e.g. AES
    final String algorithm;

    // supported and compile time enabled. Also see isAvailable()
    final boolean allowed;

    // number of bytes of entropy in the key
    final int keySize;

    // length of the actual cipher key in bytes.
    // for non-exportable ciphers, this is the same as keySize
    final int expandedKeySize;

    // size of the IV
    final int ivSize;

    // size of fixed IV
    //
    // record_iv_length = ivSize - fixedIvSize
    final int fixedIvSize;

    // exportable under 512/40 bit rules
    final boolean exportable;

    // Is the cipher algorithm of Cipher Block Chaining (CBC) mode?
    final CipherType cipherType;

    // size of the authentication tag, only applicable to cipher suites in
    // Galois Counter Mode (GCM)
    //
    // As far as we know, all supported GCM cipher suites use 128-bits
    // authentication tags.
    final int tagSize = 16;

    // runtime availability
    private final boolean isAvailable;

    private final Map.Entry<ReadCipherGenerator,
            ProtocolVersion[]>[] readCipherGenerators;
    private final Map.Entry<WriteCipherGenerator,
            ProtocolVersion[]>[] writeCipherGenerators;

    // Map of Ciphers listed in jdk.tls.keyLimits
    private static final HashMap<String, Long> cipherLimits = new HashMap<>();

    // Keywords found on the jdk.tls.keyLimits security property.
    static final String[] tag = {"KEYUPDATE"};

    static  {
        final long max = 4611686018427387904L; // 2^62
        @SuppressWarnings("removal")
        String prop = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty("jdk.tls.keyLimits");
            }
        });

        if (prop != null) {
            String[] propvalue = prop.split(",");

            for (String entry : propvalue) {
                int index;
                // If this is not a UsageLimit, goto to next entry.
                String[] values = entry.trim().toUpperCase().split(" ");

                if (values[1].contains(tag[0])) {
                    index = 0;
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.fine("jdk.tls.keyLimits:  Unknown action:  " +
                                entry);
                    }
                    continue;
                }

                long size;
                int i = values[2].indexOf("^");
                try {
                    if (i >= 0) {
                        size = (long) Math.pow(2,
                                Integer.parseInt(values[2].substring(i + 1)));
                    } else {
                        size = Long.parseLong(values[2]);
                    }
                    if (size < 1 || size > max) {
                        throw new NumberFormatException(
                            "Length exceeded limits");
                    }
                } catch (NumberFormatException e) {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                        SSLLogger.fine("jdk.tls.keyLimits:  " + e.getMessage() +
                                ":  " +  entry);
                    }
                    continue;
                }
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("jdk.tls.keyLimits:  entry = " + entry +
                            ". " + values[0] + ":" + tag[index] + " = " + size);
                }
                cipherLimits.put(values[0] + ":" + tag[index], size);
            }
        }
    }

    private SSLCipher(String transformation,
            CipherType cipherType, int keySize,
            int expandedKeySize, int ivSize,
            int fixedIvSize, boolean allowed, boolean exportable,
            Map.Entry<ReadCipherGenerator,
                    ProtocolVersion[]>[] readCipherGenerators,
            Map.Entry<WriteCipherGenerator,
                    ProtocolVersion[]>[] writeCipherGenerators) {
        this.transformation = transformation;
        String[] splits = transformation.split("/");
        this.algorithm = splits[0];
        this.cipherType = cipherType;
        this.description = this.algorithm + "/" + (keySize << 3);
        this.keySize = keySize;
        this.ivSize = ivSize;
        this.fixedIvSize = fixedIvSize;
        this.allowed = allowed;

        this.expandedKeySize = expandedKeySize;
        this.exportable = exportable;

        // availability of this bulk cipher
        //
        // AES/256 is unavailable when the default JCE policy jurisdiction files
        // are installed because of key length restrictions.
        this.isAvailable = allowed && isUnlimited(keySize, transformation) &&
                isTransformationAvailable(transformation);

        this.readCipherGenerators = readCipherGenerators;
        this.writeCipherGenerators = writeCipherGenerators;
    }

    private static boolean isTransformationAvailable(String transformation) {
        if (transformation.equals("NULL")) {
            return true;
        }
        try {
            Cipher.getInstance(transformation);
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                SSLLogger.fine("Transformation " + transformation + " is" +
                        " not available.");
            }
        }
        return false;
    }

    SSLReadCipher createReadCipher(Authenticator authenticator,
            ProtocolVersion protocolVersion,
            SecretKey key, IvParameterSpec iv,
            SecureRandom random) throws GeneralSecurityException {
        if (readCipherGenerators.length == 0) {
            return null;
        }

        ReadCipherGenerator rcg = null;
        for (Map.Entry<ReadCipherGenerator,
                ProtocolVersion[]> me : readCipherGenerators) {
            for (ProtocolVersion pv : me.getValue()) {
                if (protocolVersion == pv) {
                    rcg = me.getKey();
                    break;
                }
            }
        }

        if (rcg != null) {
            return rcg.createCipher(this, authenticator,
                    protocolVersion, transformation, key, iv, random);
        }
        return null;
    }

    SSLWriteCipher createWriteCipher(Authenticator authenticator,
            ProtocolVersion protocolVersion,
            SecretKey key, IvParameterSpec iv,
            SecureRandom random) throws GeneralSecurityException {
        if (writeCipherGenerators.length == 0) {
            return null;
        }

        WriteCipherGenerator wcg = null;
        for (Map.Entry<WriteCipherGenerator,
                ProtocolVersion[]> me : writeCipherGenerators) {
            for (ProtocolVersion pv : me.getValue()) {
                if (protocolVersion == pv) {
                    wcg = me.getKey();
                    break;
                }
            }
        }

        if (wcg != null) {
            return wcg.createCipher(this, authenticator,
                    protocolVersion, transformation, key, iv, random);
        }
        return null;
    }

    /**
     * Test if this bulk cipher is available. For use by CipherSuite.
     */
    boolean isAvailable() {
        return this.isAvailable;
    }

    private static boolean isUnlimited(int keySize, String transformation) {
        int keySizeInBits = keySize * 8;
        if (keySizeInBits > 128) {    // need the JCE unlimited
                                      // strength jurisdiction policy
            try {
                if (Cipher.getMaxAllowedKeyLength(
                        transformation) < keySizeInBits) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return description;
    }

    interface ReadCipherGenerator {
        SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException;
    }

    abstract static class SSLReadCipher {
        final Authenticator authenticator;
        final ProtocolVersion protocolVersion;
        boolean keyLimitEnabled = false;
        long keyLimitCountdown = 0;
        SecretKey baseSecret;

        SSLReadCipher(Authenticator authenticator,
                ProtocolVersion protocolVersion) {
            this.authenticator = authenticator;
            this.protocolVersion = protocolVersion;
        }

        static final SSLReadCipher nullTlsReadCipher() {
            try {
                return B_NULL.createReadCipher(
                        Authenticator.nullTlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw new RuntimeException("Cannot create NULL SSLCipher", gse);
            }
        }

        static final SSLReadCipher nullDTlsReadCipher() {
            try {
                return B_NULL.createReadCipher(
                        Authenticator.nullDtlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw new RuntimeException("Cannot create NULL SSLCipher", gse);
            }
        }

        abstract Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException;

        void dispose() {
            // blank
        }

        abstract int estimateFragmentSize(int packetSize, int headerSize);

        boolean isNullCipher() {
            return false;
        }

        /**
         * Check if processed bytes have reached the key usage limit.
         * If key usage limit is not be monitored, return false.
         */
        public boolean atKeyLimit() {
            if (keyLimitCountdown >= 0) {
                return false;
            }

            // Turn off limit checking as KeyUpdate will be occurring
            keyLimitEnabled = false;
            return true;
        }
    }

    interface WriteCipherGenerator {
        SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException;
    }

    abstract static class SSLWriteCipher {
        final Authenticator authenticator;
        final ProtocolVersion protocolVersion;
        boolean keyLimitEnabled = false;
        long keyLimitCountdown = 0;
        SecretKey baseSecret;

        SSLWriteCipher(Authenticator authenticator,
                ProtocolVersion protocolVersion) {
            this.authenticator = authenticator;
            this.protocolVersion = protocolVersion;
        }

        abstract int encrypt(byte contentType, ByteBuffer bb);

        static final SSLWriteCipher nullTlsWriteCipher() {
            try {
                return B_NULL.createWriteCipher(
                        Authenticator.nullTlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw new RuntimeException(
                        "Cannot create NULL SSL write Cipher", gse);
            }
        }

        static final SSLWriteCipher nullDTlsWriteCipher() {
            try {
                return B_NULL.createWriteCipher(
                        Authenticator.nullDtlsMac(),
                        ProtocolVersion.NONE, null, null, null);
            } catch (GeneralSecurityException gse) {
                // unlikely
                throw new RuntimeException(
                        "Cannot create NULL SSL write Cipher", gse);
            }
        }

        void dispose() {
            // blank
        }

        abstract int getExplicitNonceSize();
        abstract int calculateFragmentSize(int packetLimit, int headerSize);
        abstract int calculatePacketSize(int fragmentSize, int headerSize);

        boolean isCBCMode() {
            return false;
        }

        boolean isNullCipher() {
            return false;
        }

        /**
         * Check if processed bytes have reached the key usage limit.
         * If key usage limit is not be monitored, return false.
         */
        public boolean atKeyLimit() {
            if (keyLimitCountdown >= 0) {
                return false;
            }

            // Turn off limit checking as KeyUpdate will be occurring
            keyLimitEnabled = false;
            return true;
        }
    }

    private static final
            class NullReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new NullReadCipher(authenticator, protocolVersion);
        }

        static final class NullReadCipher extends SSLReadCipher {
            NullReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion) {
                super(authenticator, protocolVersion);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    checkStreamMac(signer, bb, contentType, sequence);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetSize - headerSize - macLen;
            }

            @Override
            boolean isNullCipher() {
                return true;
            }
        }
    }

    private static final
            class NullWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new NullWriteCipher(authenticator, protocolVersion);
        }

        static final class NullWriteCipher extends SSLWriteCipher {
            NullWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion) {
                super(authenticator, protocolVersion);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                // add message authentication code
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                int len = bb.remaining();
                bb.position(bb.limit());
                return len;
            }


            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetLimit - headerSize - macLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return fragmentSize + headerSize + macLen;
            }

            @Override
            boolean isNullCipher() {
                return true;
            }
        }
    }

    private static final
            class StreamReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new StreamReadCipher(authenticator, protocolVersion,
                    algorithm, key, params, random);
        }

        static final class StreamReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            StreamReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                int len = bb.remaining();
                int pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                bb.position(pos);
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", bb.duplicate());
                }

                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    checkStreamMac(signer, bb, contentType, sequence);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetSize - headerSize - macLen;
            }
        }
    }

    private static final
            class StreamWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new StreamWriteCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class StreamWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;

            StreamWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                // add message authentication code
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.finest(
                        "Padded plaintext before ENCRYPTION", bb.duplicate());
                }

                int len = bb.remaining();
                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }
                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return packetLimit - headerSize - macLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                return fragmentSize + headerSize + macLen;
            }
        }
    }

    private static final
            class T10BlockReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockReadCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class BlockReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            BlockReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                BadPaddingException reservedBPE = null;

                // sanity check length of the ciphertext
                MAC signer = (MAC)authenticator;
                int cipheredLength = bb.remaining();
                int tagLen = signer.macAlg().size;
                if (tagLen != 0) {
                    if (!sanityCheck(tagLen, bb.remaining())) {
                        reservedBPE = new BadPaddingException(
                                "ciphertext sanity check failed");
                    }
                }
                // decryption
                int len = bb.remaining();
                int pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext after DECRYPTION",
                            bb.duplicate().position(pos));
                }

                // remove the block padding
                int blockSize = cipher.getBlockSize();
                bb.position(pos);
                try {
                    removePadding(bb, tagLen, blockSize, protocolVersion);
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                // Requires message authentication code for null, stream and
                // block cipher suites.
                try {
                    if (tagLen != 0) {
                        checkCBCMac(signer, bb,
                                contentType, cipheredLength, sequence);
                    } else {
                        authenticator.increaseSequenceNumber();
                    }
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                // Is it a failover?
                if (reservedBPE != null) {
                    throw reservedBPE;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;

                // No padding for a maximum fragment.
                //
                // 1 byte padding length field: 0x00
                return packetSize - headerSize - macLen - 1;
            }

            /**
             * Sanity check the length of a fragment before decryption.
             *
             * In CBC mode, check that the fragment length is one or multiple
             * times of the block size of the cipher suite, and is at least
             * one (one is the smallest size of padding in CBC mode) bigger
             * than the tag size of the MAC algorithm except the explicit IV
             * size for TLS 1.1 or later.
             *
             * In non-CBC mode, check that the fragment length is not less than
             * the tag size of the MAC algorithm.
             *
             * @return true if the length of a fragment matches above
             *         requirements
             */
            private boolean sanityCheck(int tagLen, int fragmentLen) {
                int blockSize = cipher.getBlockSize();
                if ((fragmentLen % blockSize) == 0) {
                    int minimal = tagLen + 1;
                    minimal = (minimal >= blockSize) ? minimal : blockSize;

                    return (fragmentLen >= minimal);
                }

                return false;
            }
        }
    }

    private static final
            class T10BlockWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockWriteCipher(authenticator,
                    protocolVersion, algorithm, key, params, random);
        }

        static final class BlockWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;

            BlockWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                int pos = bb.position();

                // add message authentication code
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                int blockSize = cipher.getBlockSize();
                int len = addPadding(bb, blockSize);
                bb.position(pos);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int fragLen = packetLimit - headerSize;
                fragLen -= (fragLen % blockSize);   // cannot hold a block
                // No padding for a maximum fragment.
                fragLen -= 1;       // 1 byte padding length field: 0x00
                fragLen -= macLen;
                return fragLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int paddedLen = fragmentSize + macLen + 1;
                if ((paddedLen % blockSize)  != 0) {
                    paddedLen += blockSize - 1;
                    paddedLen -= paddedLen % blockSize;
                }

                return headerSize + paddedLen;
            }

            @Override
            boolean isCBCMode() {
                return true;
            }
        }
    }

    // For TLS 1.1 and 1.2
    private static final
            class T11BlockReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class BlockReadCipher extends SSLReadCipher {
            private final Cipher cipher;

            BlockReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                if (params == null) {
                    params = new IvParameterSpec(new byte[sslCipher.ivSize]);
                }
                cipher.init(Cipher.DECRYPT_MODE, key, params, random);
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                BadPaddingException reservedBPE = null;

                // sanity check length of the ciphertext
                MAC signer = (MAC)authenticator;
                int cipheredLength = bb.remaining();
                int tagLen = signer.macAlg().size;
                if (tagLen != 0) {
                    if (!sanityCheck(tagLen, bb.remaining())) {
                        reservedBPE = new BadPaddingException(
                                "ciphertext sanity check failed");
                    }
                }

                // decryption
                int len = bb.remaining();
                int pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext after DECRYPTION",
                            bb.duplicate().position(pos));
                }

                // Ignore the explicit nonce.
                bb.position(pos + cipher.getBlockSize());
                pos = bb.position();

                // remove the block padding
                int blockSize = cipher.getBlockSize();
                bb.position(pos);
                try {
                    removePadding(bb, tagLen, blockSize, protocolVersion);
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                // Requires message authentication code for null, stream and
                // block cipher suites.
                try {
                    if (tagLen != 0) {
                        checkCBCMac(signer, bb,
                                contentType, cipheredLength, sequence);
                    } else {
                        authenticator.increaseSequenceNumber();
                    }
                } catch (BadPaddingException bpe) {
                    if (reservedBPE == null) {
                        reservedBPE = bpe;
                    }
                }

                // Is it a failover?
                if (reservedBPE != null) {
                    throw reservedBPE;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;

                // No padding for a maximum fragment.
                //
                // 1 byte padding length field: 0x00
                int nonceSize = cipher.getBlockSize();
                return packetSize - headerSize - nonceSize - macLen - 1;
            }

            /**
             * Sanity check the length of a fragment before decryption.
             *
             * In CBC mode, check that the fragment length is one or multiple
             * times of the block size of the cipher suite, and is at least
             * one (one is the smallest size of padding in CBC mode) bigger
             * than the tag size of the MAC algorithm except the explicit IV
             * size for TLS 1.1 or later.
             *
             * In non-CBC mode, check that the fragment length is not less than
             * the tag size of the MAC algorithm.
             *
             * @return true if the length of a fragment matches above
             *         requirements
             */
            private boolean sanityCheck(int tagLen, int fragmentLen) {
                int blockSize = cipher.getBlockSize();
                if ((fragmentLen % blockSize) == 0) {
                    int minimal = tagLen + 1;
                    minimal = (minimal >= blockSize) ? minimal : blockSize;
                    minimal += blockSize;

                    return (fragmentLen >= minimal);
                }

                return false;
            }
        }
    }

    // For TLS 1.1 and 1.2
    private static final
            class T11BlockWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new BlockWriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class BlockWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final SecureRandom random;

            BlockWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.random = random;
                if (params == null) {
                    params = new IvParameterSpec(new byte[sslCipher.ivSize]);
                }
                cipher.init(Cipher.ENCRYPT_MODE, key, params, random);
            }

            @Override
            public int encrypt(byte contentType, ByteBuffer bb) {
                // To be unique and aware of overflow-wrap, sequence number
                // is used as the nonce_explicit of block cipher suites.
                int pos = bb.position();

                // add message authentication code
                MAC signer = (MAC)authenticator;
                if (signer.macAlg().size != 0) {
                    addMac(signer, bb, contentType);
                } else {
                    authenticator.increaseSequenceNumber();
                }

                // DON'T WORRY, the nonce spaces are considered already.
                byte[] nonce = new byte[cipher.getBlockSize()];
                random.nextBytes(nonce);
                pos = pos - nonce.length;
                bb.position(pos);
                bb.put(nonce);
                bb.position(pos);

                int blockSize = cipher.getBlockSize();
                int len = addPadding(bb, blockSize);
                bb.position(pos);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Padded plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                try {
                    if (len != cipher.update(dup, bb)) {
                        // catch BouncyCastle buffering error
                        throw new RuntimeException(
                                "Unexpected number of plaintext bytes");
                    }

                    if (bb.position() != dup.position()) {
                        throw new RuntimeException(
                                "Unexpected ByteBuffer position");
                    }
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return cipher.getBlockSize();
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int fragLen = packetLimit - headerSize - blockSize;
                fragLen -= (fragLen % blockSize);   // cannot hold a block
                // No padding for a maximum fragment.
                fragLen -= 1;       // 1 byte padding length field: 0x00
                fragLen -= macLen;
                return fragLen;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                int macLen = ((MAC)authenticator).macAlg().size;
                int blockSize = cipher.getBlockSize();
                int paddedLen = fragmentSize + macLen + 1;
                if ((paddedLen % blockSize)  != 0) {
                    paddedLen += blockSize - 1;
                    paddedLen -= paddedLen % blockSize;
                }

                return headerSize + blockSize + paddedLen;
            }

            @Override
            boolean isCBCMode() {
                return true;
            }
        }
    }

    private static final
            class T12GcmReadCipherGenerator implements ReadCipherGenerator {
        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmReadCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        static final class GcmReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] fixedIv;
            private final int recordIvSize;
            private final SecureRandom random;

            GcmReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.fixedIv = ((IvParameterSpec)params).getIV();
                this.recordIvSize = sslCipher.ivSize - sslCipher.fixedIvSize;
                this.random = random;

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (bb.remaining() < (recordIvSize + tagSize)) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than or equal to IV size (" + recordIvSize +
                         ") + tag size (" + tagSize + ")");
                }

                // initialize the AEAD cipher for the unique IV
                byte[] iv = Arrays.copyOf(fixedIv,
                                    fixedIv.length + recordIvSize);
                bb.get(iv, fixedIv.length, recordIvSize);
                GCMParameterSpec spec = new GCMParameterSpec(tagSize * 8, iv);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                // update the additional authentication data
                byte[] aad = authenticator.acquireAuthenticationBytes(
                        contentType, bb.remaining() - tagSize,
                        sequence);
                cipher.updateAAD(aad);

                // DON'T decrypt the nonce_explicit for AEAD mode. The buffer
                // position has moved out of the nonce_explicit range.
                int len, pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                // reset the limit to the end of the decrypted data
                bb.position(pos);
                bb.limit(pos + len);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", bb.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - recordIvSize - tagSize;
            }
        }
    }

    private static final
            class T12GcmWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator,
                ProtocolVersion protocolVersion, String algorithm,
                Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmWriteCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        private static final class GcmWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] fixedIv;
            private final int recordIvSize;
            private final SecureRandom random;

            GcmWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.fixedIv = ((IvParameterSpec)params).getIV();
                this.recordIvSize = sslCipher.ivSize - sslCipher.fixedIvSize;
                this.random = random;

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                // To be unique and aware of overflow-wrap, sequence number
                // is used as the nonce_explicit of AEAD cipher suites.
                byte[] nonce = authenticator.sequenceNumber();

                // initialize the AEAD cipher for the unique IV
                byte[] iv = Arrays.copyOf(fixedIv,
                                            fixedIv.length + nonce.length);
                System.arraycopy(nonce, 0, iv, fixedIv.length, nonce.length);

                GCMParameterSpec spec = new GCMParameterSpec(tagSize * 8, iv);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), null);
                cipher.updateAAD(aad);

                // DON'T WORRY, the nonce spaces are considered already.
                bb.position(bb.position() - nonce.length);
                bb.put(nonce);

                // DON'T encrypt the nonce for AEAD mode.
                int len, pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                int outputSize = cipher.getOutputSize(dup.remaining());
                if (outputSize > bb.remaining()) {
                    // Need to expand the limit of the output buffer for
                    // the authentication tag.
                    //
                    // DON'T worry about the buffer's capacity, we have
                    // reserved space for the authentication tag.
                    bb.limit(pos + outputSize);
                }

                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                return len + nonce.length;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return recordIvSize;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - recordIvSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + recordIvSize + tagSize;
            }
        }
    }

    private static final
            class T13GcmReadCipherGenerator implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmReadCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        static final class GcmReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            GcmReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                        algorithm.toUpperCase() + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("KeyLimit read side: algorithm = " +
                            algorithm.toUpperCase() + ":" + tag[0] +
                            "\ncountdown value = " + keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }
                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                // An implementation may receive an unencrypted record of type
                // change_cipher_spec consisting of the single byte value 0x01
                // at any time after the first ClientHello message has been
                // sent or received and before the peer's Finished message has
                // been received and MUST simply drop it without further
                // processing.
                if (contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
                }

                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = iv.clone();
                int offset = nonce.length - sn.length;
                for (int i = 0; i < sn.length; i++) {
                    nonce[offset + i] ^= sn[i];
                }

                // initialize the AEAD cipher for the unique IV
                GCMParameterSpec spec =
                        new GCMParameterSpec(tagSize * 8, nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), sn);
                cipher.updateAAD(aad);

                int len, pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                // reset the limit to the end of the decrypted data
                bb.position(pos);
                bb.limit(pos + len);

                // remove inner plaintext padding
                int i = bb.limit() - 1;
                for (; i > 0 && bb.get(i) == 0; i--) {
                    // blank
                }
                if (i < (pos + 1)) {
                    throw new BadPaddingException(
                            "Incorrect inner plaintext: no content type");
                }
                contentType = bb.get(i);
                bb.limit(i);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", bb.duplicate());
                }
                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final
            class T13GcmWriteCipherGenerator implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new GcmWriteCipher(authenticator, protocolVersion, sslCipher,
                    algorithm, key, params, random);
        }

        private static final class GcmWriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            GcmWriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                        algorithm.toUpperCase() + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("KeyLimit write side: algorithm = "
                            + algorithm.toUpperCase() + ":" + tag[0] +
                            "\ncountdown value = " + keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = iv.clone();
                int offset = nonce.length - sn.length;
                for (int i = 0; i < sn.length; i++) {
                    nonce[offset + i] ^= sn[i];
                }

                // initialize the AEAD cipher for the unique IV
                GCMParameterSpec spec =
                        new GCMParameterSpec(tagSize * 8, nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in GCM mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                int outputSize = cipher.getOutputSize(bb.remaining());
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, outputSize, sn);
                cipher.updateAAD(aad);

                int len, pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                if (outputSize > bb.remaining()) {
                    // Need to expand the limit of the output buffer for
                    // the authentication tag.
                    //
                    // DON'T worry about the buffer's capacity, we have
                    // reserved space for the authentication tag.
                    bb.limit(pos + outputSize);
                }

                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }
                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static final class T12CC20P1305ReadCipherGenerator
            implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305ReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class CC20P1305ReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305ReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                // initialize the AEAD cipher with the unique IV
                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                // update the additional authentication data
                byte[] aad = authenticator.acquireAuthenticationBytes(
                        contentType, bb.remaining() - tagSize, sequence);
                cipher.updateAAD(aad);

                // DON'T decrypt the nonce_explicit for AEAD mode. The buffer
                // position has moved out of the nonce_explicit range.
                int len;
                int pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                // reset the limit to the end of the decrypted data
                bb.position(pos);
                bb.limit(pos + len);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", bb.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final class T12CC20P1305WriteCipherGenerator
            implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305WriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        private static final class CC20P1305WriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305WriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                        algorithm.toUpperCase() + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("algorithm = " + algorithm.toUpperCase() +
                            ":" + tag[0] + "\ncountdown value = " +
                            keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                // initialize the AEAD cipher for the unique IV
                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), null);
                cipher.updateAAD(aad);

                // DON'T encrypt the nonce for AEAD mode.
                int pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                int outputSize = cipher.getOutputSize(dup.remaining());
                if (outputSize > bb.remaining()) {
                    // Need to expand the limit of the output buffer for
                    // the authentication tag.
                    //
                    // DON'T worry about the buffer's capacity, we have
                    // reserved space for the authentication tag.
                    bb.limit(pos + outputSize);
                }

                int len;
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static final class T13CC20P1305ReadCipherGenerator
            implements ReadCipherGenerator {

        @Override
        public SSLReadCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305ReadCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        static final class CC20P1305ReadCipher extends SSLReadCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305ReadCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public Plaintext decrypt(byte contentType, ByteBuffer bb,
                    byte[] sequence) throws GeneralSecurityException {
                // An implementation may receive an unencrypted record of type
                // change_cipher_spec consisting of the single byte value 0x01
                // at any time after the first ClientHello message has been
                // sent or received and before the peer's Finished message has
                // been received and MUST simply drop it without further
                // processing.
                if (contentType == ContentType.CHANGE_CIPHER_SPEC.id) {
                    return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
                }

                if (bb.remaining() <= tagSize) {
                    throw new BadPaddingException(
                        "Insufficient buffer remaining for AEAD cipher " +
                        "fragment (" + bb.remaining() + "). Needs to be " +
                        "more than tag size (" + tagSize + ")");
                }

                byte[] sn = sequence;
                if (sn == null) {
                    sn = authenticator.sequenceNumber();
                }
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                // initialize the AEAD cipher with the unique IV
                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, bb.remaining(), sn);
                cipher.updateAAD(aad);

                int len;
                int pos = bb.position();
                ByteBuffer dup = bb.duplicate();
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                        "Cipher error in AEAD mode \"" + ibse.getMessage() +
                        " \"in JCE provider " + cipher.getProvider().getName());
                } catch (ShortBufferException sbe) {
                    // catch BouncyCastle buffering error
                    throw new RuntimeException("Cipher buffering error in " +
                        "JCE provider " + cipher.getProvider().getName(), sbe);
                }
                // reset the limit to the end of the decrypted data
                bb.position(pos);
                bb.limit(pos + len);

                // remove inner plaintext padding
                int i = bb.limit() - 1;
                for (; i > 0 && bb.get(i) == 0; i--) {
                    // blank
                }
                if (i < (pos + 1)) {
                    throw new BadPaddingException(
                            "Incorrect inner plaintext: no content type");
                }
                contentType = bb.get(i);
                bb.limit(i);

                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext after DECRYPTION", bb.duplicate());
                }

                return new Plaintext(contentType,
                        ProtocolVersion.NONE.major, ProtocolVersion.NONE.minor,
                        -1, -1L, bb.slice());
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int estimateFragmentSize(int packetSize, int headerSize) {
                return packetSize - headerSize - tagSize;
            }
        }
    }

    private static final class T13CC20P1305WriteCipherGenerator
            implements WriteCipherGenerator {
        @Override
        public SSLWriteCipher createCipher(SSLCipher sslCipher,
                Authenticator authenticator, ProtocolVersion protocolVersion,
                String algorithm, Key key, AlgorithmParameterSpec params,
                SecureRandom random) throws GeneralSecurityException {
            return new CC20P1305WriteCipher(authenticator, protocolVersion,
                    sslCipher, algorithm, key, params, random);
        }

        private static final class CC20P1305WriteCipher extends SSLWriteCipher {
            private final Cipher cipher;
            private final int tagSize;
            private final Key key;
            private final byte[] iv;
            private final SecureRandom random;

            CC20P1305WriteCipher(Authenticator authenticator,
                    ProtocolVersion protocolVersion,
                    SSLCipher sslCipher, String algorithm,
                    Key key, AlgorithmParameterSpec params,
                    SecureRandom random) throws GeneralSecurityException {
                super(authenticator, protocolVersion);
                this.cipher = Cipher.getInstance(algorithm);
                this.tagSize = sslCipher.tagSize;
                this.key = key;
                this.iv = ((IvParameterSpec)params).getIV();
                this.random = random;

                keyLimitCountdown = cipherLimits.getOrDefault(
                        algorithm.toUpperCase() + ":" + tag[0], 0L);
                if (SSLLogger.isOn && SSLLogger.isOn("ssl")) {
                    SSLLogger.fine("algorithm = " + algorithm.toUpperCase() +
                            ":" + tag[0] + "\ncountdown value = " +
                            keyLimitCountdown);
                }
                if (keyLimitCountdown > 0) {
                    keyLimitEnabled = true;
                }

                // DON'T initialize the cipher for AEAD!
            }

            @Override
            public int encrypt(byte contentType,
                    ByteBuffer bb) {
                byte[] sn = authenticator.sequenceNumber();
                byte[] nonce = new byte[iv.length];
                System.arraycopy(sn, 0, nonce, nonce.length - sn.length,
                        sn.length);
                for (int i = 0; i < nonce.length; i++) {
                    nonce[i] ^= iv[i];
                }

                // initialize the AEAD cipher for the unique IV
                AlgorithmParameterSpec spec = new IvParameterSpec(nonce);
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, key, spec, random);
                } catch (InvalidKeyException |
                            InvalidAlgorithmParameterException ikae) {
                    // unlikely to happen
                    throw new RuntimeException(
                                "invalid key or spec in AEAD mode", ikae);
                }

                // Update the additional authentication data, using the
                // implicit sequence number of the authenticator.
                int outputSize = cipher.getOutputSize(bb.remaining());
                byte[] aad = authenticator.acquireAuthenticationBytes(
                                        contentType, outputSize, sn);
                cipher.updateAAD(aad);

                int pos = bb.position();
                if (SSLLogger.isOn && SSLLogger.isOn("plaintext")) {
                    SSLLogger.fine(
                            "Plaintext before ENCRYPTION",
                            bb.duplicate());
                }

                ByteBuffer dup = bb.duplicate();
                if (outputSize > bb.remaining()) {
                    // Need to expand the limit of the output buffer for
                    // the authentication tag.
                    //
                    // DON'T worry about the buffer's capacity, we have
                    // reserved space for the authentication tag.
                    bb.limit(pos + outputSize);
                }

                int len;
                try {
                    len = cipher.doFinal(dup, bb);
                } catch (IllegalBlockSizeException |
                            BadPaddingException | ShortBufferException ibse) {
                    // unlikely to happen
                    throw new RuntimeException(
                            "Cipher error in AEAD mode in JCE provider " +
                            cipher.getProvider().getName(), ibse);
                }

                if (len != outputSize) {
                    throw new RuntimeException(
                            "Cipher buffering error in JCE provider " +
                            cipher.getProvider().getName());
                }

                if (keyLimitEnabled) {
                    keyLimitCountdown -= len;
                }
                return len;
            }

            @Override
            void dispose() {
                if (cipher != null) {
                    try {
                        cipher.doFinal();
                    } catch (Exception e) {
                        // swallow all types of exceptions.
                    }
                }
            }

            @Override
            int getExplicitNonceSize() {
                return 0;
            }

            @Override
            int calculateFragmentSize(int packetLimit, int headerSize) {
                return packetLimit - headerSize - tagSize;
            }

            @Override
            int calculatePacketSize(int fragmentSize, int headerSize) {
                return fragmentSize + headerSize + tagSize;
            }
        }
    }

    private static void addMac(MAC signer,
            ByteBuffer destination, byte contentType) {
        if (signer.macAlg().size != 0) {
            int dstContent = destination.position();
            byte[] hash = signer.compute(contentType, destination, false);

            /*
             * position was advanced to limit in MAC compute above.
             *
             * Mark next area as writable (above layers should have
             * established that we have plenty of room), then write
             * out the hash.
             */
            destination.limit(destination.limit() + hash.length);
            destination.put(hash);

            // reset the position and limit
            destination.position(dstContent);
        }
    }

    // for null and stream cipher
    private static void checkStreamMac(MAC signer, ByteBuffer bb,
            byte contentType,  byte[] sequence) throws BadPaddingException {
        int tagLen = signer.macAlg().size;

        // Requires message authentication code for null, stream and
        // block cipher suites.
        if (tagLen != 0) {
            int contentLen = bb.remaining() - tagLen;
            if (contentLen < 0) {
                throw new BadPaddingException("bad record");
            }

            // Run MAC computation and comparison on the payload.
            //
            // MAC data would be stripped off during the check.
            if (checkMacTags(contentType, bb, signer, sequence, false)) {
                throw new BadPaddingException("bad record MAC");
            }
        }
    }

    // for CBC cipher
    private static void checkCBCMac(MAC signer, ByteBuffer bb,
            byte contentType, int cipheredLength,
            byte[] sequence) throws BadPaddingException {
        BadPaddingException reservedBPE = null;
        int tagLen = signer.macAlg().size;
        int pos = bb.position();

        if (tagLen != 0) {
            int contentLen = bb.remaining() - tagLen;
            if (contentLen < 0) {
                reservedBPE = new BadPaddingException("bad record");

                // set offset of the dummy MAC
                contentLen = cipheredLength - tagLen;
                bb.limit(pos + cipheredLength);
            }

            // Run MAC computation and comparison on the payload.
            //
            // MAC data would be stripped off during the check.
            if (checkMacTags(contentType, bb, signer, sequence, false)) {
                if (reservedBPE == null) {
                    reservedBPE =
                            new BadPaddingException("bad record MAC");
                }
            }

            // Run MAC computation and comparison on the remainder.
            int remainingLen = calculateRemainingLen(
                    signer, cipheredLength, contentLen);

            // NOTE: remainingLen may be bigger (less than 1 block of the
            // hash algorithm of the MAC) than the cipheredLength.
            //
            // Is it possible to use a static buffer, rather than allocate
            // it dynamically?
            remainingLen += signer.macAlg().size;
            ByteBuffer temporary = ByteBuffer.allocate(remainingLen);

            // Won't need to worry about the result on the remainder. And
            // then we won't need to worry about what's actual data to
            // check MAC tag on.  We start the check from the header of the
            // buffer so that we don't need to construct a new byte buffer.
            checkMacTags(contentType, temporary, signer, sequence, true);
        }

        // Is it a failover?
        if (reservedBPE != null) {
            throw reservedBPE;
        }
    }

    /*
     * Run MAC computation and comparison
     */
    private static boolean checkMacTags(byte contentType, ByteBuffer bb,
            MAC signer, byte[] sequence, boolean isSimulated) {
        int tagLen = signer.macAlg().size;
        int position = bb.position();
        int lim = bb.limit();
        int macOffset = lim - tagLen;

        bb.limit(macOffset);
        byte[] hash = signer.compute(contentType, bb, sequence, isSimulated);
        if (hash == null || tagLen != hash.length) {
            // Something is wrong with MAC implementation.
            throw new RuntimeException("Internal MAC error");
        }

        bb.position(macOffset);
        bb.limit(lim);
        try {
            int[] results = compareMacTags(bb, hash);
            return (results[0] != 0);
        } finally {
            // reset to the data
            bb.position(position);
            bb.limit(macOffset);
        }
    }

    /*
     * A constant-time comparison of the MAC tags.
     *
     * Please DON'T change the content of the ByteBuffer parameter!
     */
    private static int[] compareMacTags(ByteBuffer bb, byte[] tag) {
        // An array of hits is used to prevent Hotspot optimization for
        // the purpose of a constant-time check.
        int[] results = {0, 0};     // {missed #, matched #}

        // The caller ensures there are enough bytes available in the buffer.
        // So we won't need to check the remaining of the buffer.
        for (byte t : tag) {
            if (bb.get() != t) {
                results[0]++;       // mismatched bytes
            } else {
                results[1]++;       // matched bytes
            }
        }

        return results;
    }

    /*
     * Calculate the length of a dummy buffer to run MAC computation
     * and comparison on the remainder.
     *
     * The caller MUST ensure that the fullLen is not less than usedLen.
     */
    private static int calculateRemainingLen(
            MAC signer, int fullLen, int usedLen) {

        int blockLen = signer.macAlg().hashBlockSize;
        int minimalPaddingLen = signer.macAlg().minimalPaddingSize;

        // (blockLen - minimalPaddingLen) is the maximum message size of
        // the last block of hash function operation. See FIPS 180-4, or
        // MD5 specification.
        fullLen += 13 - (blockLen - minimalPaddingLen);
        usedLen += 13 - (blockLen - minimalPaddingLen);

        // Note: fullLen is always not less than usedLen, and blockLen
        // is always bigger than minimalPaddingLen, so we don't worry
        // about negative values. 0x01 is added to the result to ensure
        // that the return value is positive.  The extra one byte does
        // not impact the overall MAC compression function evaluations.
        return 0x01 + (int)(Math.ceil(fullLen/(1.0d * blockLen)) -
                Math.ceil(usedLen/(1.0d * blockLen))) * blockLen;
    }

    private static int addPadding(ByteBuffer bb, int blockSize) {

        int     len = bb.remaining();
        int     offset = bb.position();

        int     newlen = len + 1;
        byte    pad;
        int     i;

        if ((newlen % blockSize) != 0) {
            newlen += blockSize - 1;
            newlen -= newlen % blockSize;
        }
        pad = (byte) (newlen - len);

        /*
         * Update the limit to what will be padded.
         */
        bb.limit(newlen + offset);

        /*
         * TLS version of the padding works for both SSLv3 and TLSv1
         */
        for (i = 0, offset += len; i < pad; i++) {
            bb.put(offset++, (byte) (pad - 1));
        }

        bb.position(offset);
        bb.limit(offset);

        return newlen;
    }

    private static int removePadding(ByteBuffer bb,
            int tagLen, int blockSize,
            ProtocolVersion protocolVersion) throws BadPaddingException {
        int len = bb.remaining();
        int offset = bb.position();

        // last byte is length byte (i.e. actual padding length - 1)
        int padOffset = offset + len - 1;
        int padLen = bb.get(padOffset) & 0xFF;

        int newLen = len - (padLen + 1);
        if ((newLen - tagLen) < 0) {
            // If the buffer is not long enough to contain the padding plus
            // a MAC tag, do a dummy constant-time padding check.
            //
            // Note that it is a dummy check, so we won't care about what is
            // the actual padding data.
            checkPadding(bb.duplicate(), (byte)(padLen & 0xFF));

            throw new BadPaddingException("Invalid Padding length: " + padLen);
        }

        // The padding data should be filled with the padding length value.
        int[] results = checkPadding(
                bb.duplicate().position(offset + newLen),
                (byte)(padLen & 0xFF));
        if (protocolVersion.useTLS10PlusSpec()) {
            if (results[0] != 0) {          // padding data has invalid bytes
                throw new BadPaddingException("Invalid TLS padding data");
            }
        } else { // SSLv3
            // SSLv3 requires 0 <= length byte < block size
            // some implementations do 1 <= length byte <= block size,
            // so accept that as well
            // v3 does not require any particular value for the other bytes
            if (padLen > blockSize) {
                throw new BadPaddingException("Padding length (" +
                padLen + ") of SSLv3 message should not be bigger " +
                "than the block size (" + blockSize + ")");
            }
        }

        // Reset buffer limit to remove padding.
        bb.limit(offset + newLen);

        return newLen;
    }

    /*
     * A constant-time check of the padding.
     *
     * NOTE that we are checking both the padding and the padLen bytes here.
     *
     * The caller MUST ensure that the bb parameter has remaining.
     */
    private static int[] checkPadding(ByteBuffer bb, byte pad) {
        if (!bb.hasRemaining()) {
            throw new RuntimeException("hasRemaining() must be positive");
        }

        // An array of hits is used to prevent Hotspot optimization for
        // the purpose of a constant-time check.
        int[] results = {0, 0};    // {missed #, matched #}
        bb.mark();
        for (int i = 0; i <= 256; bb.reset()) {
            for (; bb.hasRemaining() && i <= 256; i++) {
                if (bb.get() != pad) {
                    results[0]++;       // mismatched padding data
                } else {
                    results[1]++;       // matched padding data
                }
            }
        }

        return results;
    }
}
