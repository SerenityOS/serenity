/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug 8224997
 * @summary ChaCha20-Poly1305 TLS cipher suite decryption throws ShortBufferException
 * @library /test/lib
 * @build jdk.test.lib.Convert
 * @run main OutputSizeTest
 */

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class OutputSizeTest {

    private static final SecureRandom SR = new SecureRandom();

    public static void main(String args[]) throws Exception {
        testCC20GetOutSize();
        testCC20P1305GetOutSize();
        testMultiPartAEADDec();
    }

    private static void testCC20GetOutSize()
            throws GeneralSecurityException {
        boolean result = true;
        KeyGenerator kg = KeyGenerator.getInstance("ChaCha20", "SunJCE");
        kg.init(256);

        // ChaCha20 encrypt
        Cipher cc20 = Cipher.getInstance("ChaCha20", "SunJCE");
        cc20.init(Cipher.ENCRYPT_MODE, kg.generateKey(),
                new ChaCha20ParameterSpec(getRandBuf(12), 10));

        testOutLen(cc20, 0, 0);
        testOutLen(cc20, 5, 5);
        testOutLen(cc20, 5120, 5120);
        // perform an update, then test with a final block
        byte[] input = new byte[5120];
        SR.nextBytes(input);
        cc20.update(input);
        testOutLen(cc20, 1024, 1024);

        // Decryption lengths should be calculated the same way as encryption
        cc20.init(Cipher.DECRYPT_MODE, kg.generateKey(),
                new ChaCha20ParameterSpec(getRandBuf(12), 10));
        testOutLen(cc20, 0, 0);
        testOutLen(cc20, 5, 5);
        testOutLen(cc20, 5120, 5120);
        // perform an update, then test with a final block
        cc20.update(input);
        testOutLen(cc20, 1024, 1024);
    }

    private static void testCC20P1305GetOutSize()
            throws GeneralSecurityException {
        KeyGenerator kg = KeyGenerator.getInstance("ChaCha20", "SunJCE");
        kg.init(256);

        // ChaCha20 encrypt
        Cipher cc20 = Cipher.getInstance("ChaCha20-Poly1305", "SunJCE");
        cc20.init(Cipher.ENCRYPT_MODE, kg.generateKey(),
                new IvParameterSpec(getRandBuf(12)));

        // Encryption lengths are calculated as the input length plus the tag
        // length (16).
        testOutLen(cc20, 0, 16);
        testOutLen(cc20, 5, 21);
        testOutLen(cc20, 5120, 5136);
        // perform an update, then test with a final block
        byte[] input = new byte[5120];
        SR.nextBytes(input);
        cc20.update(input);
        testOutLen(cc20, 1024, 1040);

        // Decryption lengths are handled differently for AEAD mode.  The length
        // should be zero for anything up to and including the first 16 bytes
        // (since that's the tag).  Anything above that should be the input
        // length plus any unprocessed input (via update calls), minus the
        // 16 byte tag.
        cc20.init(Cipher.DECRYPT_MODE, kg.generateKey(),
                new IvParameterSpec(getRandBuf(12)));
        testOutLen(cc20, 0, 0);
        testOutLen(cc20, 5, 0);
        testOutLen(cc20, 16, 0);
        testOutLen(cc20, 5120, 5104);
        // Perform an update, then test with a the length of a final chunk
        // of data.
        cc20.update(input);
        testOutLen(cc20, 1024, 6128);
    }

    private static void testMultiPartAEADDec() throws GeneralSecurityException {
        KeyGenerator kg = KeyGenerator.getInstance("ChaCha20", "SunJCE");
        kg.init(256);
        Key key = kg.generateKey();
        IvParameterSpec ivps = new IvParameterSpec(getRandBuf(12));

        // Encrypt some data so we can test decryption.
        byte[] pText = getRandBuf(2048);
        ByteBuffer pTextBase = ByteBuffer.wrap(pText);

        Cipher enc = Cipher.getInstance("ChaCha20-Poly1305", "SunJCE");
        enc.init(Cipher.ENCRYPT_MODE, key, ivps);
        ByteBuffer ctBuf = ByteBuffer.allocateDirect(
                enc.getOutputSize(pText.length));
        enc.doFinal(pTextBase, ctBuf);

        // Create a new direct plain text ByteBuffer which will catch the
        // decrypted data.
        ByteBuffer ptBuf = ByteBuffer.allocateDirect(pText.length);

        // Set the cipher text buffer limit to roughly half the data so we can
        // do an update/final sequence.
        ctBuf.position(0).limit(1024);

        Cipher dec = Cipher.getInstance("ChaCha20-Poly1305", "SunJCE");
        dec.init(Cipher.DECRYPT_MODE, key, ivps);
        dec.update(ctBuf, ptBuf);
        System.out.println("CTBuf: " + ctBuf);
        System.out.println("PTBuf: " + ptBuf);
        ctBuf.limit(ctBuf.capacity());
        dec.doFinal(ctBuf, ptBuf);

        ptBuf.flip();
        pTextBase.flip();
        System.out.println("PT Base:" + pTextBase);
        System.out.println("PT Actual:" + ptBuf);

        if (pTextBase.compareTo(ptBuf) != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Plaintext mismatch: Original: ").
                    append(pTextBase.toString()).append("\nActual :").
                    append(ptBuf);
            throw new RuntimeException(sb.toString());
        }
    }

    private static void testOutLen(Cipher c, int inLen, int expOut) {
        int actualOut = c.getOutputSize(inLen);
        if (actualOut != expOut) {
            throw new RuntimeException("Cipher " + c + ", in: " + inLen +
                    ", expOut: " + expOut + ", actual: " + actualOut);
        }
    }

    private static byte[] getRandBuf(int len) {
        byte[] buf = new byte[len];
        SR.nextBytes(buf);
        return buf;
    }
}

