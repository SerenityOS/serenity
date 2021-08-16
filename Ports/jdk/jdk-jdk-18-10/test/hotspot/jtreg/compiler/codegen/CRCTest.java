/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7088419
 * @summary Use x86 Hardware CRC32 Instruction with java.util.zip.CRC32 and java.util.zip.Adler32
 *
 * @run main compiler.codegen.CRCTest
 */

package compiler.codegen;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CRCTest {

    public static void main(String[] args) throws Exception {

        byte[] b = initializedBytes(4096 * 4096);

        {
            CRC32 crc1 = new CRC32();
            CRC32 crc2 = new CRC32();
            CRC32 crc3 = new CRC32();
            CRC32 crc4 = new CRC32();

            crc1.update(b, 0, b.length);
            updateSerial(crc2, b, 0, b.length);
            updateDirect(crc3, b, 0, b.length);
            updateSerialSlow(crc4, b, 0, b.length);

            check(crc1, crc2);
            check(crc3, crc4);
            check(crc1, crc3);

            crc1.update(17);
            crc2.update(17);
            crc3.update(17);
            crc4.update(17);

            crc1.update(b, 1, b.length-2);
            updateSerial(crc2, b, 1, b.length-2);
            updateDirect(crc3, b, 1, b.length-2);
            updateSerialSlow(crc4, b, 1, b.length-2);

            check(crc1, crc2);
            check(crc3, crc4);
            check(crc1, crc3);

            report("finished huge crc", crc1, crc2, crc3, crc4);

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j += 1) {
                    crc1.update(b, i, j);
                    updateSerial(crc2, b, i, j);
                    updateDirect(crc3, b, i, j);
                    updateSerialSlow(crc4, b, i, j);

                    check(crc1, crc2);
                    check(crc3, crc4);
                    check(crc1, crc3);

                }
            }

            report("finished small survey crc", crc1, crc2, crc3, crc4);
        }

    }

    private static void report(String s, Checksum crc1, Checksum crc2,
            Checksum crc3, Checksum crc4) {
        System.out.println(s + ", crc1 = " + crc1.getValue() +
                ", crc2 = " + crc2.getValue()+
                ", crc3 = " + crc3.getValue()+
                ", crc4 = " + crc4.getValue());
    }

    private static void check(Checksum crc1, Checksum crc2) throws Exception {
        if (crc1.getValue() != crc2.getValue()) {
            String s = "value 1 = " + crc1.getValue() + ", value 2 = " + crc2.getValue();
            System.err.println(s);
            throw new Exception(s);
        }
    }

    private static byte[] initializedBytes(int M) {
        byte[] bytes = new byte[M];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private static void updateSerial(Checksum crc, byte[] b, int start, int length) {
        for (int i = 0; i < length; i++)
            crc.update(b[i+start]);
    }

    private static void updateSerialSlow(Checksum crc, byte[] b, int start, int length) {
        for (int i = 0; i < length; i++)
            crc.update(b[i+start]);
        crc.getValue();
    }

    private static void updateDirect(CRC32 crc3, byte[] b, int start, int length) {
        ByteBuffer buf = ByteBuffer.allocateDirect(length);
        buf.put(b, start, length);
        buf.flip();
        crc3.update(buf);
    }
}
