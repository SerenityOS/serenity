/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/*
 * @test
 * @summary This tests overlapping buffers using ByteBuffer.slice() with
 *  array-backed ByteBuffer, read only array-backed, ByteBuffer, and direct
 *  ByteBuffer.
 */

/*
 * This tests overlapping buffers created with ByteBuffer.slice().  That is
 * when the input and output ByteBuffers have shared memory (use the same
 * underlying buffer space, commonly used for in-place crypto).  The
 * complication is the Cipher object specifies that it must be copy-safe.  That
 * means the output buffer will not overwrite any input data that has not been
 * processed.  If the output buffer's position or offset is greater than the
 * input's overwriting will occur.
 */

public class OverlapByteBuffer {

    public static void main(String[] args) throws Exception {
        byte[] baseBuf = new byte[8192];
        ByteBuffer output, input, in;
        // Output offset from the baseBuf
        int outOfs;

        for (int i = 0; i < 3; i++) {
            for (outOfs = -1; outOfs <= 1; outOfs++) {

                SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
                GCMParameterSpec params =
                    new GCMParameterSpec(128, new byte[12]);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key, params);

                // Offset on the particular ByteBuffer (aka position())
                int inOfsInBuf = 1;
                int outOfsInBuf = inOfsInBuf + outOfs;
                int sliceLen = cipher.getOutputSize(baseBuf.length);
                int bufferSize = sliceLen + Math.max(inOfsInBuf, outOfsInBuf);
                byte[] buffer;
                // Create overlapping input and output buffers
                switch (i) {
                    case 0 -> {
                        buffer = new byte[bufferSize];
                        output = ByteBuffer.wrap(buffer, outOfsInBuf, sliceLen).
                            slice();
                        input = ByteBuffer.wrap(buffer, inOfsInBuf, sliceLen).
                            slice();
                        System.out.println("Using array-backed ByteBuffer");
                        in = input.duplicate();
                    }
                    case 1 -> {
                        buffer = new byte[bufferSize];
                        output = ByteBuffer.wrap(buffer, outOfsInBuf, sliceLen).
                            slice();
                        input = ByteBuffer.wrap(buffer, inOfsInBuf, sliceLen).
                            slice();

                        System.out.println("Using read-only array-backed " + "ByteBuffer");
                        in = input.asReadOnlyBuffer();
                    }
                    case 2 -> {
                        System.out.println("Using direct ByteBuffer");
                        ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
                        output = buf.duplicate();
                        output.position(outOfsInBuf);
                        output.limit(sliceLen + outOfsInBuf);
                        output = output.slice();

                        input = buf.duplicate();
                        input.position(inOfsInBuf);
                        input.limit(sliceLen + inOfsInBuf);
                        input = input.slice();

                        in = input.duplicate();
                    }
                    default -> {
                        throw new Exception("Unknown index " + i);
                    }
                }

                System.out.println("inOfsInBuf  = " + inOfsInBuf);
                System.out.println("outOfsInBuf = " + outOfsInBuf);

                // Copy data into shared buffer
                input.put(baseBuf);
                input.flip();
                in.limit(input.limit());

                try {
                    int ctSize = cipher.doFinal(in, output);

                    // Get ready to decrypt
                    byte[] tmp = new byte[ctSize];
                    output.flip();
                    output.get(tmp);
                    output.clear();

                    input.clear();
                    input.put(tmp);
                    input.flip();

                    in.clear();
                    in.limit(input.limit());

                    cipher.init(Cipher.DECRYPT_MODE, key, params);
                    cipher.doFinal(in, output);

                    output.flip();
                    ByteBuffer b = ByteBuffer.wrap(baseBuf);
                    if (b.compareTo(output) != 0) {
                        System.err.println(
                            "\nresult   (" + output + "):\n" +
                            byteToHex(output) +
                            "\nexpected (" + b + "):\n" +
                            byteToHex(b));
                        throw new Exception("Mismatch");
                    }
                } catch (Exception e) {
                    throw new Exception("Error with base offset " + outOfs, e);
                }
            }
        }
    }
        private static String byteToHex(ByteBuffer bb) {
        StringBuilder s = new StringBuilder();
        while (bb.remaining() > 0) {
            s.append(String.format("%02x", bb.get()));
        }
        return s.toString();
    }
}
