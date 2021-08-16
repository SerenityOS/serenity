/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.ntlm;

import sun.security.action.GetBooleanAction;

import static com.sun.security.ntlm.Version.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * NTLM authentication implemented according to MS-NLMP, version 12.1
 * @since 1.7
 */
class NTLM {

    private final SecretKeyFactory fac;
    private final Cipher cipher;
    private final MessageDigest md4;
    private final Mac hmac;
    private final MessageDigest md5;
    private static final boolean DEBUG
            = GetBooleanAction.privilegedGetProperty("ntlm.debug");

    final Version v;

    final boolean writeLM;
    final boolean writeNTLM;

    protected NTLM(String version) throws NTLMException {
        if (version == null) version = "LMv2/NTLMv2";
        switch (version) {
            case "LM": v = NTLM; writeLM = true; writeNTLM = false; break;
            case "NTLM": v = NTLM; writeLM = false; writeNTLM = true; break;
            case "LM/NTLM": v = NTLM; writeLM = writeNTLM = true; break;
            case "NTLM2": v = NTLM2; writeLM = writeNTLM = true; break;
            case "LMv2": v = NTLMv2; writeLM = true; writeNTLM = false; break;
            case "NTLMv2": v = NTLMv2; writeLM = false; writeNTLM = true; break;
            case "LMv2/NTLMv2": v = NTLMv2; writeLM = writeNTLM = true; break;
            default: throw new NTLMException(NTLMException.BAD_VERSION,
                    "Unknown version " + version);
        }
        try {
            fac = SecretKeyFactory.getInstance ("DES");
            cipher = Cipher.getInstance ("DES/ECB/NoPadding");
            md4 = sun.security.provider.MD4.getInstance();
            hmac = Mac.getInstance("HmacMD5");
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchPaddingException e) {
            throw new AssertionError();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }

    /**
     * Prints out a formatted string, called in various places inside then NTLM
     * implementation for debugging/logging purposes. When the system property
     * "ntlm.debug" is set, <code>System.out.printf(format, args)</code> is
     * called. This method is designed to be overridden by child classes to
     * match their own debugging/logging mechanisms.
     * @param format a format string
     * @param args the arguments referenced by <code>format</code>
     * @see java.io.PrintStream#printf(java.lang.String, java.lang.Object[])
     */
    public void debug(String format, Object... args) {
        if (DEBUG) {
            System.out.printf(format, args);
        }
    }

    /**
     * Prints out the content of a byte array, called in various places inside
     * the NTLM implementation for debugging/logging purposes. When the system
     * property "ntlm.debug" is set, the hexdump of the array is printed into
     * System.out. This method is designed to be overridden by child classes to
     * match their own debugging/logging mechanisms.
     * @param bytes the byte array to print out
     */
    public void debug(byte[] bytes) {
        if (DEBUG) {
            try {
                new sun.security.util.HexDumpEncoder().encodeBuffer(bytes, System.out);
            } catch (IOException ioe) {
                // Impossible
            }
        }
    }

    /**
     * Reading an NTLM packet
     */
    static class Reader {

        private final byte[] internal;

        Reader(byte[] data) {
            internal = data;
        }

        int readInt(int offset) throws NTLMException {
            try {
                return (internal[offset] & 0xff) +
                        ((internal[offset+1] & 0xff) << 8) +
                        ((internal[offset+2] & 0xff) << 16) +
                        ((internal[offset+3] & 0xff) << 24);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new NTLMException(NTLMException.PACKET_READ_ERROR,
                        "Input message incorrect size");
            }
        }

        int readShort(int offset) throws NTLMException {
            try {
                return (internal[offset] & 0xff) +
                        (((internal[offset+1] & 0xff) << 8));
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new NTLMException(NTLMException.PACKET_READ_ERROR,
                        "Input message incorrect size");
            }
        }

        byte[] readBytes(int offset, int len) throws NTLMException {
            try {
                return Arrays.copyOfRange(internal, offset, offset + len);
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new NTLMException(NTLMException.PACKET_READ_ERROR,
                        "Input message incorrect size");
            }
        }

        byte[] readSecurityBuffer(int offset) throws NTLMException {
            int pos = readInt(offset+4);
            if (pos == 0) return new byte[0];
            try {
                return Arrays.copyOfRange(
                        internal, pos, pos + readShort(offset));
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new NTLMException(NTLMException.PACKET_READ_ERROR,
                        "Input message incorrect size");
            }
        }

        String readSecurityBuffer(int offset, boolean unicode)
                throws NTLMException {
            byte[] raw = readSecurityBuffer(offset);
            return raw == null ? null : new String(
                    raw, unicode ? StandardCharsets.UTF_16LE
                                 : StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Writing an NTLM packet
     */
    static class Writer {

        private byte[] internal;    // buffer
        private int current;        // current written content interface buffer

        /**
         * Starts writing a NTLM packet
         * @param type NEGOTIATE || CHALLENGE || AUTHENTICATE
         * @param len the base length, without security buffers
         */
        Writer(int type, int len) {
            assert len < 256;
            internal = new byte[256];
            current = len;
            System.arraycopy (
                    new byte[] {'N','T','L','M','S','S','P',0,(byte)type},
                    0, internal, 0, 9);
        }

        void writeShort(int offset, int number) {
            internal[offset] = (byte)(number);
            internal[offset+1] = (byte)(number >> 8);
        }

        void writeInt(int offset, int number) {
            internal[offset] = (byte)(number);
            internal[offset+1] = (byte)(number >> 8);
            internal[offset+2] = (byte)(number >> 16);
            internal[offset+3] = (byte)(number >> 24);
        }

        void writeBytes(int offset, byte[] data) {
            System.arraycopy(data, 0, internal, offset, data.length);
        }

        void writeSecurityBuffer(int offset, byte[] data) {
            if (data == null) {
                writeShort(offset+4, current);
            } else {
                int len = data.length;
                if (current + len > internal.length) {
                    internal = Arrays.copyOf(internal, current + len + 256);
                }
                writeShort(offset, len);
                writeShort(offset+2, len);
                writeShort(offset+4, current);
                System.arraycopy(data, 0, internal, current, len);
                current += len;
            }
        }

        void writeSecurityBuffer(int offset, String str, boolean unicode) {
            writeSecurityBuffer(offset, str == null ? null : str.getBytes(
                    unicode ? StandardCharsets.UTF_16LE
                            : StandardCharsets.ISO_8859_1));
        }

        byte[] getBytes() {
            return Arrays.copyOf(internal, current);
        }
    }

    // LM/NTLM

    /* Convert a 7 byte array to an 8 byte array (for a des key with parity)
     * input starts at offset off
     */
    byte[] makeDesKey (byte[] input, int off) {
        int[] in = new int [input.length];
        for (int i=0; i<in.length; i++ ) {
            in[i] = input[i]<0 ? input[i]+256: input[i];
        }
        byte[] out = new byte[8];
        out[0] = (byte)in[off+0];
        out[1] = (byte)(((in[off+0] << 7) & 0xFF) | (in[off+1] >> 1));
        out[2] = (byte)(((in[off+1] << 6) & 0xFF) | (in[off+2] >> 2));
        out[3] = (byte)(((in[off+2] << 5) & 0xFF) | (in[off+3] >> 3));
        out[4] = (byte)(((in[off+3] << 4) & 0xFF) | (in[off+4] >> 4));
        out[5] = (byte)(((in[off+4] << 3) & 0xFF) | (in[off+5] >> 5));
        out[6] = (byte)(((in[off+5] << 2) & 0xFF) | (in[off+6] >> 6));
        out[7] = (byte)((in[off+6] << 1) & 0xFF);
        return out;
    }

    byte[] calcLMHash (byte[] pwb) {
        byte[] magic = {0x4b, 0x47, 0x53, 0x21, 0x40, 0x23, 0x24, 0x25};
        byte[] pwb1 = new byte [14];
        int len = pwb.length;
        if (len > 14)
            len = 14;
        System.arraycopy (pwb, 0, pwb1, 0, len); /* Zero padded */

        try {
            DESKeySpec dks1 = new DESKeySpec (makeDesKey (pwb1, 0));
            DESKeySpec dks2 = new DESKeySpec (makeDesKey (pwb1, 7));

            SecretKey key1 = fac.generateSecret (dks1);
            SecretKey key2 = fac.generateSecret (dks2);
            cipher.init (Cipher.ENCRYPT_MODE, key1);
            byte[] out1 = cipher.doFinal (magic, 0, 8);
            cipher.init (Cipher.ENCRYPT_MODE, key2);
            byte[] out2 = cipher.doFinal (magic, 0, 8);
            byte[] result = new byte [21];
            System.arraycopy (out1, 0, result, 0, 8);
            System.arraycopy (out2, 0, result, 8, 8);
            return result;
        } catch (InvalidKeyException ive) {
            // Will not happen, all key material are 8 bytes
            assert false;
        } catch (InvalidKeySpecException ikse) {
            // Will not happen, we only feed DESKeySpec to DES factory
            assert false;
        } catch (IllegalBlockSizeException ibse) {
            // Will not happen, we encrypt 8 bytes
            assert false;
        } catch (BadPaddingException bpe) {
            // Will not happen, this is encryption
            assert false;
        }
        return null;    // will not happen, we returned already
    }

    byte[] calcNTHash (byte[] pw) {
        byte[] out = md4.digest (pw);
        byte[] result = new byte [21];
        System.arraycopy (out, 0, result, 0, 16);
        return result;
    }

    /* key is a 21 byte array. Split it into 3 7 byte chunks,
     * Convert each to 8 byte DES keys, encrypt the text arg with
     * each key and return the three results in a sequential []
     */
    byte[] calcResponse (byte[] key, byte[] text) {
        try {
            assert key.length == 21;
            DESKeySpec dks1 = new DESKeySpec(makeDesKey(key, 0));
            DESKeySpec dks2 = new DESKeySpec(makeDesKey(key, 7));
            DESKeySpec dks3 = new DESKeySpec(makeDesKey(key, 14));
            SecretKey key1 = fac.generateSecret(dks1);
            SecretKey key2 = fac.generateSecret(dks2);
            SecretKey key3 = fac.generateSecret(dks3);
            cipher.init(Cipher.ENCRYPT_MODE, key1);
            byte[] out1 = cipher.doFinal(text, 0, 8);
            cipher.init(Cipher.ENCRYPT_MODE, key2);
            byte[] out2 = cipher.doFinal(text, 0, 8);
            cipher.init(Cipher.ENCRYPT_MODE, key3);
            byte[] out3 = cipher.doFinal(text, 0, 8);
            byte[] result = new byte[24];
            System.arraycopy(out1, 0, result, 0, 8);
            System.arraycopy(out2, 0, result, 8, 8);
            System.arraycopy(out3, 0, result, 16, 8);
            return result;
        } catch (IllegalBlockSizeException ex) {    // None will happen
            assert false;
        } catch (BadPaddingException ex) {
            assert false;
        } catch (InvalidKeySpecException ex) {
            assert false;
        } catch (InvalidKeyException ex) {
            assert false;
        }
        return null;
    }

    // LMv2/NTLMv2

    byte[] hmacMD5(byte[] key, byte[] text) {
        try {
            SecretKeySpec skey =
                    new SecretKeySpec(Arrays.copyOf(key, 16), "HmacMD5");
            hmac.init(skey);
            return hmac.doFinal(text);
        } catch (InvalidKeyException ex) {
            assert false;
        } catch (RuntimeException e) {
            assert false;
        }
        return null;
    }

    byte[] calcV2(byte[] nthash, String text, byte[] blob, byte[] challenge) {
        byte[] ntlmv2hash = hmacMD5(nthash, text.getBytes(StandardCharsets.UTF_16LE));
        byte[] cn = new byte[blob.length+8];
        System.arraycopy(challenge, 0, cn, 0, 8);
        System.arraycopy(blob, 0, cn, 8, blob.length);
        byte[] result = new byte[16+blob.length];
        System.arraycopy(hmacMD5(ntlmv2hash, cn), 0, result, 0, 16);
        System.arraycopy(blob, 0, result, 16, blob.length);
        return result;
    }

    // NTLM2 LM/NTLM

    static byte[] ntlm2LM(byte[] nonce) {
        return Arrays.copyOf(nonce, 24);
    }

    byte[] ntlm2NTLM(byte[] ntlmHash, byte[] nonce, byte[] challenge) {
        byte[] b = Arrays.copyOf(challenge, 16);
        System.arraycopy(nonce, 0, b, 8, 8);
        byte[] sesshash = Arrays.copyOf(md5.digest(b), 8);
        return calcResponse(ntlmHash, sesshash);
    }

    // Password in ASCII and UNICODE

    static byte[] getP1(char[] password) {
        return new String(password).toUpperCase(Locale.ENGLISH)
                                   .getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] getP2(char[] password) {
        return new String(password).getBytes(StandardCharsets.UTF_16LE);
    }
}
