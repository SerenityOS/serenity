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

/*
 * @test
 * @bug 8261462
 * @summary Verify that after the first doFinal() decryption op, the ByteBuffer
 * is properly set for the second operation.
 */

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;

public class ResetByteBuffer {

    Cipher c;
    SecretKey key;
    ByteBuffer in, out;
    byte[] data = new byte[1500];
    byte encrypted[];

    public static final void main(String args[]) throws Exception {
        // Cannot do encryption back to back with AES/GCM
        // Tests GCM's ByteBuffer code
        String algo = "AES/GCM/NoPadding";
        new ResetByteBuffer(algo).decrypt(true).updateTest().updateTest();
        new ResetByteBuffer(algo).decrypt(false).updateTest().updateTest();
        new ResetByteBuffer(algo).decrypt(true).updateTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(false).updateTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(true).doFinalTest().updateTest();
        new ResetByteBuffer(algo).decrypt(false).doFinalTest().updateTest();
        new ResetByteBuffer(algo).decrypt(true).doFinalTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(false).doFinalTest().doFinalTest();

        // Tests CipherCore code.  Testing CBC should be enough to cover the
        // other algorithms that use CipherCore
        algo = "AES/CBC/PKCS5Padding";
        new ResetByteBuffer(algo).encrypt(true).updateTest().updateTest();
        new ResetByteBuffer(algo).encrypt(false).updateTest().updateTest();
        new ResetByteBuffer(algo).encrypt(true).updateTest().doFinalTest();
        new ResetByteBuffer(algo).encrypt(false).updateTest().doFinalTest();
        new ResetByteBuffer(algo).encrypt(true).doFinalTest().updateTest();
        new ResetByteBuffer(algo).encrypt(false).doFinalTest().updateTest();
        new ResetByteBuffer(algo).encrypt(true).doFinalTest().doFinalTest();
        new ResetByteBuffer(algo).encrypt(false).doFinalTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(true).updateTest().updateTest();
        new ResetByteBuffer(algo).decrypt(false).updateTest().updateTest();
        new ResetByteBuffer(algo).decrypt(true).updateTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(false).updateTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(true).doFinalTest().updateTest();
        new ResetByteBuffer(algo).decrypt(false).doFinalTest().updateTest();
        new ResetByteBuffer(algo).decrypt(true).doFinalTest().doFinalTest();
        new ResetByteBuffer(algo).decrypt(false).doFinalTest().doFinalTest();
    }

    public ResetByteBuffer(String algo) throws Exception {
        c = Cipher.getInstance(algo);
        String a[] = algo.split("/");
        KeyGenerator kg = KeyGenerator.getInstance(a[0]);
        key = kg.generateKey();
        // Setup encrypted data
        c.init(Cipher.ENCRYPT_MODE, key, c.getParameters());
        encrypted = new byte[c.getOutputSize(data.length)];
        c.doFinal(data, 0, data.length, encrypted, 0);
    }

    ResetByteBuffer decrypt(boolean direct) throws Exception {
        // allocate bytebuffers
        if (direct) {
            in = ByteBuffer.allocateDirect(encrypted.length);
            out = ByteBuffer.allocateDirect(encrypted.length);
        } else {
            in = ByteBuffer.allocate(encrypted.length);
            out = ByteBuffer.allocate(encrypted.length);
        }
        in.put(encrypted);
        in.flip();
        c.init(Cipher.DECRYPT_MODE, key, c.getParameters());
        return this;
    }

    ResetByteBuffer encrypt(boolean direct) throws Exception {
        // allocate bytebuffers
        if (direct) {
            in = ByteBuffer.allocateDirect(data.length);
            out = ByteBuffer.allocateDirect(c.getOutputSize(data.length));
        } else {
            in = ByteBuffer.allocate(data.length);
            out = ByteBuffer.allocate(c.getOutputSize(data.length));
        }
        c.init(Cipher.ENCRYPT_MODE, key, c.getParameters());
        return this;
    }

    ResetByteBuffer updateTest() throws Exception {
        int updateLen = data.length / 2;
        in.limit(updateLen);
        c.update(in, out);
        in.limit(in.capacity());
        c.doFinal(in, out);
        if (in.capacity() != in.position()) {
            System.out.println("There is data remaining in the input buffer");
        }
        if (out.limit() != out.position()) {
            System.out.println("There is data remaining in the output buffer");
        }
        in.flip();
        out.position(0);
        out.limit(out.capacity());
        return this;
    }

    ResetByteBuffer doFinalTest() throws Exception {
        c.doFinal(in, out);
        if (in.capacity() != in.position()) {
            System.out.println("There is data remaining in the input buffer");
        }
        if (out.limit() != out.position()) {
            System.out.println("There is data remaining in the output buffer");
        }
        in.flip();
        out.position(0);
        out.limit(out.capacity());
        return this;
    }
}
