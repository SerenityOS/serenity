/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6392804
   @summary Decoder fails to detect decoding error
*/
import java.nio.*;
import java.nio.charset.*;

public class Test6392804 {
    public static void main(String[] args) throws Throwable {
        test("ISO-2022-JP",
             new byte[] {0x1b,(byte)0x8e, 0x24, 0x40, 0x0, 0x0});
        test("ISO-2022-JP-2",
             new byte[] {0x1b,(byte)0x8e, 0x24, 0x40, 0x0, 0x0});
        test("x-windows-50220",
             new byte[] {0x1b,(byte)0x8e, 0x24, 0x40, 0x0, 0x0});
        test("x-windows-50221",
             new byte[] {0x1b,(byte)0x8e, 0x24, 0x40, 0x0, 0x0});
        test("x-windows-iso2022jp",
             new byte[] {0x1b,(byte)0x8e, 0x24, 0x40, 0x0, 0x0});
        test("EUC_TW",
             new byte[] {(byte)0x8e, (byte)0xa8, (byte)0xad, (byte)0xe5});
        //out of range second  byte
        test("EUC_TW",
             new byte[] {(byte)0x8e, (byte)0x92, (byte)0xa1, (byte)0xa1});
        test("EUC_TW",
             new byte[] {(byte)0x8e, (byte)0x98, (byte)0xa1, (byte)0xa1});
    }

    static void test(String csn, byte[] bytes) throws Throwable {
        CharsetDecoder dec = Charset.forName(csn).newDecoder();
        CharBuffer cb = CharBuffer.allocate(1024);
        CoderResult cr = dec.decode(ByteBuffer.wrap(bytes), cb, true);
        if (cr.isUnderflow())
            throw new RuntimeException(csn + " failed cr=" + cr);
    }
}
