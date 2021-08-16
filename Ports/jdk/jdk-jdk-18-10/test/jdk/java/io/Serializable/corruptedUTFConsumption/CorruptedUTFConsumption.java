/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4450867
 * @summary Although technically the behavior of ObjectInputStream following a
 *          UTFDataFormatException is unspecified, verify that
 *          ObjectInputStream consumes at most the expected number of utf
 *          bytes, even if the last byte(s) of the utf string indicate that the
 *          string overflows its expected length.
 * @key randomness
 */

import java.io.*;
import java.util.Random;

public class CorruptedUTFConsumption {

    static Random rand = new Random(System.currentTimeMillis());

    public static void main(String[] args) throws Exception {
        StringBuffer sbuf = new StringBuffer();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);

        for (int i = 0; i < 1200; i++) {
            sbuf.append(i % 10);
            bout.reset();
            dout.writeUTF(sbuf.toString());
            byte[] utf = bout.toByteArray();

            // set last byte to first byte of 2-char sequence
            utf[utf.length - 1] = (byte) (0xC0 | rand.nextInt() & 0x1F);
            checkConsume(utf);

            // set last byte to first byte of 3-char sequence
            utf[utf.length - 1] = (byte) (0xE0 | rand.nextInt() & 0x0F);
            checkConsume(utf);

            if (utf.length >= 4) {      // don't touch utf length bytes
                // set last 2 bytes to first, second byte of 3-char sequence
                utf[utf.length - 2] = (byte) (0xE0 | rand.nextInt() & 0x0F);
                utf[utf.length - 1] = (byte) (0x80 | rand.nextInt() & 0x3F);
                checkConsume(utf);
            }
        }
    }

    static void checkConsume(byte[] utf) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.write(utf);
        oout.writeByte(0);      // leave one byte of padding
        oout.close();
        ObjectInputStream oin = new ObjectInputStream(
            new ByteArrayInputStream(bout.toByteArray()));
        try {
            oin.readUTF();
            throw new Error();
        } catch (UTFDataFormatException ex) {
        }
        // if readUTF consumed padding byte, readByte will throw EOFException
        oin.readByte();
    }
}
