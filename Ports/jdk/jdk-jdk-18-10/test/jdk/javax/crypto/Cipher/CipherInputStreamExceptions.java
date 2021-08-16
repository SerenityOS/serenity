/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8064546
 * @summary Throw exceptions during reading but not closing of a
 * CipherInputStream:
 * - Make sure authenticated algorithms continue to throwing exceptions
 *   when the authentication tag fails verification.
 * - Make sure other algorithms do not throw exceptions when the stream
 *   calls close() and only throw when read() errors.
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.RuntimeException;
import java.lang.Throwable;
import java.security.AlgorithmParameters;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;

public class CipherInputStreamExceptions {

    static SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
    static GCMParameterSpec gcmspec = new GCMParameterSpec(128, new byte[16]);
    static IvParameterSpec iv = new IvParameterSpec(new byte[16]);
    static boolean failure = false;

    /* Full read stream, check that getMoreData() is throwing an exception
     * This test
     *   1) Encrypt 100 bytes with AES/GCM/NoPadding
     *   2) Changes the last byte to invalidate the authetication tag.
     *   3) Fully reads CipherInputStream to decrypt the message and closes
     */

    static void gcm_AEADBadTag() throws Exception {
        Cipher c;
        byte[] read = new byte[200];

        System.out.println("Running gcm_AEADBadTag");

        // Encrypt 100 bytes with AES/GCM/NoPadding
        byte[] ct = encryptedText("GCM", 100);
        // Corrupt the encrypted message
        ct = corruptGCM(ct);
        // Create stream for decryption
        CipherInputStream in = getStream("GCM", ct);

        try {
            int size = in.read(read);
            throw new RuntimeException("Fail: CipherInputStream.read() " +
                    "returned " + size + " and didn't throw an exception.");
        } catch (IOException e) {
            Throwable ec = e.getCause();
            if (ec instanceof AEADBadTagException) {
                System.out.println("  Pass.");
            } else {
                System.out.println("  Fail: " + ec.getMessage());
                throw new RuntimeException(ec);
            }
        } finally {
            in.close();
        }
    }

    /* Short read stream,
     * This test
     *   1) Encrypt 100 bytes with AES/GCM/NoPadding
     *   2) Reads 100 bytes from stream to decrypt the message and closes
     *   3) Make sure no value is returned by read()
     *   4) Make sure no exception is thrown
     */

    static void gcm_shortReadAEAD() throws Exception {
        Cipher c;
        byte[] read = new byte[100];

        System.out.println("Running gcm_shortReadAEAD");

        byte[] pt = new byte[600];
        pt[0] = 1;
        // Encrypt provided 600 bytes with AES/GCM/NoPadding
        byte[] ct = encryptedText("GCM", pt);
        // Create stream for decryption
        CipherInputStream in = getStream("GCM", ct);

        int size = 0;
        try {
            size = in.read(read);
            in.close();
            if (read.length != 100) {
                throw new RuntimeException("Fail: read size = " + read.length +
                        "should be 100.");
            }
            if (read[0] != 1) {
                throw new RuntimeException("Fail: The decrypted text does " +
                        "not match the plaintext: '" + read[0] +"'");
            }
        } catch (IOException e) {
            System.out.println("  Fail: " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
        System.out.println("  Pass.");
    }

    /*
     * Verify doFinal() exception is suppressed when input stream is not
     * read before it is closed.
     * This test:
     *   1) Encrypt 100 bytes with AES/GCM/NoPadding
     *   2) Changes the last byte to invalidate the authetication tag.
     *   3) Opens a CipherInputStream and the closes it. Never reads from it.
     *
     * There should be no exception thrown.
     */
    static void gcm_suppressUnreadCorrupt() throws Exception {
        Cipher c;
        byte[] read = new byte[200];

        System.out.println("Running supressUnreadCorrupt test");

        // Encrypt 100 bytes with AES/GCM/NoPadding
        byte[] ct = encryptedText("GCM", 100);
        // Corrupt the encrypted message
        ct = corruptGCM(ct);
        // Create stream for decryption
        CipherInputStream in = getStream("GCM", ct);

        try {
            in.close();
            System.out.println("  Pass.");
        } catch (IOException e) {
            System.out.println("  Fail: " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    /*
     * Verify noexception thrown when 1 byte is read from a GCM stream
     * and then closed
     * This test:
     *   1) Encrypt 100 bytes with AES/GCM/NoPadding
     *   2) Read one byte from the stream, expect no exception thrown.
     *   4) Close stream,expect no exception thrown.
     */
    static void gcm_oneReadByte() throws Exception {

        System.out.println("Running gcm_oneReadByte test");

        // Encrypt 100 bytes with AES/GCM/NoPadding
        byte[] ct = encryptedText("GCM", 100);
        // Create stream for decryption
        CipherInputStream in = getStream("GCM", ct);

        try {
            in.read();
            System.out.println("  Pass.");
        } catch (Exception e) {
            System.out.println("  Fail: " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    /*
     * Verify exception thrown when 1 byte is read from a corrupted GCM stream
     * and then closed
     * This test:
     *   1) Encrypt 100 bytes with AES/GCM/NoPadding
     *   2) Changes the last byte to invalidate the authetication tag.
     *   3) Read one byte from the stream, expect exception thrown.
     *   4) Close stream,expect no exception thrown.
     */
    static void gcm_oneReadByteCorrupt() throws Exception {

        System.out.println("Running gcm_oneReadByteCorrupt test");

        // Encrypt 100 bytes with AES/GCM/NoPadding
        byte[] ct = encryptedText("GCM", 100);
        // Corrupt the encrypted message
        ct = corruptGCM(ct);
        // Create stream for decryption
        CipherInputStream in = getStream("GCM", ct);

        try {
            in.read();
            System.out.println("  Fail. No exception thrown.");
        } catch (IOException e) {
            Throwable ec = e.getCause();
            if (ec instanceof AEADBadTagException) {
                System.out.println("  Pass.");
            } else {
                System.out.println("  Fail: " + ec.getMessage());
                throw new RuntimeException(ec);
            }
        }
    }

    /* Check that close() does not throw an exception with full message in
     * CipherInputStream's ibuffer.
     * This test:
     *   1) Encrypts a 97 byte message with AES/CBC/PKCS5Padding
     *   2) Create a stream that sends 96 bytes.
     *   3) Read stream once,
     *   4) Close and expect no exception
     */

    static void cbc_shortStream() throws Exception {
        Cipher c;
        AlgorithmParameters params;
        byte[] read = new byte[200];

        System.out.println("Running cbc_shortStream");

        // Encrypt 97 byte with AES/CBC/PKCS5Padding
        byte[] ct = encryptedText("CBC", 97);
        // Create stream with only 96 bytes of encrypted data
        CipherInputStream in = getStream("CBC", ct, 96);

        try {
            int size = in.read(read);
            in.close();
            if (size != 80) {
                throw new RuntimeException("Fail: CipherInputStream.read() " +
                        "returned " + size + ". Should have been 80");
            }
            System.out.println("  Pass.");
        } catch (IOException e) {
            System.out.println("  Fail:  " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    /* Check that close() does not throw an exception when the whole message is
     * inside the internal buffer (ibuffer) in CipherInputStream and we read
     * one byte and close the stream.
     * This test:
     *   1) Encrypts a 400 byte message with AES/CBC/PKCS5Padding
     *   2) Read one byte from the stream
     *   3) Close and expect no exception
     */

    static void cbc_shortRead400() throws Exception {
        System.out.println("Running cbc_shortRead400");

        // Encrypt 400 byte with AES/CBC/PKCS5Padding
        byte[] ct = encryptedText("CBC", 400);
        // Create stream with encrypted data
        CipherInputStream in = getStream("CBC", ct);

        try {
            in.read();
            in.close();
            System.out.println("  Pass.");
        } catch (IOException e) {
            System.out.println("  Fail:  " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    /* Check that close() does not throw an exception when the  inside the
     * internal buffer (ibuffer) in CipherInputStream does not contain the
     * whole message.
     * This test:
     *   1) Encrypts a 600 byte message with AES/CBC/PKCS5Padding
     *   2) Read one byte from the stream
     *   3) Close and expect no exception
     */

    static void cbc_shortRead600() throws Exception {
        System.out.println("Running cbc_shortRead600");

        // Encrypt 600 byte with AES/CBC/PKCS5Padding
        byte[] ct = encryptedText("CBC", 600);
        // Create stream with encrypted data
        CipherInputStream in = getStream("CBC", ct);

        try {
            in.read();
            in.close();
            System.out.println("  Pass.");
        } catch (IOException e) {
            System.out.println("  Fail:  " + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    /* Check that exception is thrown when message is fully read
     * This test:
     *   1) Encrypts a 96 byte message with AES/CBC/PKCS5Padding
     *   2) Create a stream that sends 95 bytes.
     *   3) Read stream to the end
     *   4) Expect IllegalBlockSizeException thrown
     */

    static void cbc_readAllIllegalBlockSize() throws Exception {
        byte[] read = new byte[200];

        System.out.println("Running cbc_readAllIllegalBlockSize test");

        // Encrypt 96 byte with AES/CBC/PKCS5Padding
        byte[] ct = encryptedText("CBC", 96);
        // Create a stream with only 95 bytes of encrypted data
        CipherInputStream in = getStream("CBC", ct, 95);

        try {
            int s, size = 0;
            while ((s = in.read(read)) != -1) {
                size += s;
            }
            throw new RuntimeException("Fail: No IllegalBlockSizeException. " +
                    "CipherInputStream.read() returned " + size);

        } catch (IOException e) {
            Throwable ec = e.getCause();
            if (ec instanceof IllegalBlockSizeException) {
                System.out.println("  Pass.");
            } else {
                System.out.println("  Fail: " + ec.getMessage());
                throw new RuntimeException(ec);
            }
        }
    }

    /* Generic method to create encrypted text */
    static byte[] encryptedText(String mode, int length) throws Exception{
        return encryptedText(mode, new byte[length]);
    }

    /* Generic method to create encrypted text */
    static byte[] encryptedText(String mode, byte[] pt) throws Exception{
        Cipher c;
        if (mode.compareTo("GCM") == 0) {
            c = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
            c.init(Cipher.ENCRYPT_MODE, key, gcmspec);
        } else if (mode.compareTo("CBC") == 0) {
            c = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
            c.init(Cipher.ENCRYPT_MODE, key, iv);
        } else {
            return null;
        }

        return c.doFinal(pt);
    }

    /* Generic method to get a properly setup CipherInputStream */
    static CipherInputStream getStream(String mode, byte[] ct) throws Exception {
        return getStream(mode, ct, ct.length);
    }

    /* Generic method to get a properly setup CipherInputStream */
    static CipherInputStream getStream(String mode, byte[] ct, int length)
            throws Exception {
        Cipher c;

        if (mode.compareTo("GCM") == 0) {
            c = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
            c.init(Cipher.DECRYPT_MODE, key, gcmspec);
        } else if (mode.compareTo("CBC") == 0) {
            c = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
            c.init(Cipher.DECRYPT_MODE, key, iv);
        } else {
            return null;
        }

        return new CipherInputStream(new ByteArrayInputStream(ct, 0, length), c);

    }

    /* Generic method for corrupting a GCM message.  Change the last
     * byte on of the authentication tag
     */
    static byte[] corruptGCM(byte[] ct) {
        ct[ct.length - 1] = (byte) (ct[ct.length - 1] + 1);
        return ct;
    }

    public static void main(String[] args) throws Exception {
        gcm_AEADBadTag();
        gcm_shortReadAEAD();
        gcm_suppressUnreadCorrupt();
        gcm_oneReadByte();
        gcm_oneReadByteCorrupt();
        cbc_shortStream();
        cbc_shortRead400();
        cbc_shortRead600();
        cbc_readAllIllegalBlockSize();
    }
}
