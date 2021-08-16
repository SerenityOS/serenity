/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

/*
 * @test
 * @summary Call decrypt doFinal() with different output values to see if the
 * the operation can complete after a ShortBufferException
 */
public class GCMShortBuffer {
    static Cipher c;
    static final GCMParameterSpec iv = new GCMParameterSpec(128, new byte[16]);
    static final SecretKeySpec keySpec = new SecretKeySpec(new byte[16], "AES");
    static byte cipherText[], plaintext[] = new byte[51];
    boolean error = false;

    GCMShortBuffer(byte[] out) throws Exception {
        int len = cipherText.length - 1;

        c.init(Cipher.DECRYPT_MODE, keySpec, iv);
        byte[] pt = new byte[c.getOutputSize(cipherText.length)];
        c.update(cipherText, 0, 1);
        try {
            c.doFinal(cipherText, 1, len, out, 0);
        } catch (ShortBufferException e) {
            System.out.println("ShortBuffer caught");
        } catch (Exception e) {
            throw e;
        }
        int r = c.doFinal(cipherText, 1, len, pt, 0);
        if (r != pt.length) {
            System.out.println(
                "doFinal() return ( " + r + ") is not the same" +
                    "as getOutputSize returned" + pt.length);
            error = true;
        }
        if (Arrays.compare(pt, plaintext) != 0) {
            System.out.println("output  : " + HexFormat.of().formatHex(pt));
            System.out.println("expected: " +
                HexFormat.of().formatHex(plaintext));
            System.out.println("output and plaintext do not match");
            error = true;
        }
        if (error) {
            throw new Exception("An error has occurred");
        }
    }

    GCMShortBuffer(ByteBuffer dst) throws Exception {
        int len = cipherText.length - 1;
        ByteBuffer out = ByteBuffer.allocate(plaintext.length);

        c.init(Cipher.DECRYPT_MODE, keySpec, iv);
        c.update(cipherText, 0, 1);
        ByteBuffer ct = ByteBuffer.wrap(cipherText, 1, len);
        try {
            c.doFinal(ct , dst);
        } catch (ShortBufferException e) {
            System.out.println("ShortBuffer caught");
        } catch (Exception e) {
            throw e;
        }
        int r = c.doFinal(ByteBuffer.wrap(cipherText, 1, len), out);
        out.flip();
        if (r != out.capacity()) {
            System.out.println(
                "doFinal() return ( " + r + ") is not the same" +
                    " as getOutputSize returned" + out.capacity());
            error = true;
        }
        if (out.compareTo(ByteBuffer.wrap(plaintext)) != 0) {
            System.out.println("output and plaintext do not match");
            System.out.println("output  : " +
                HexFormat.of().formatHex(out.array()));
            System.out.println("expected: " +
                HexFormat.of().formatHex(plaintext));
            error = true;
        }
        if (error) {
            throw new Exception("An error has occurred");
        }
    }

    public static void main(String args[]) throws Exception {
        c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        cipherText = c.doFinal(plaintext);

        new GCMShortBuffer(new byte[13]);
        new GCMShortBuffer(new byte[50]);
        new GCMShortBuffer(ByteBuffer.allocate(13));
        new GCMShortBuffer(ByteBuffer.allocate(50));
    }


}
