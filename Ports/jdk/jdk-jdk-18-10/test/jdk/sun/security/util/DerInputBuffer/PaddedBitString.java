/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4511556
 * @summary Verify BitString value containing padding bits is accepted.
 * @modules java.base/sun.security.util
 */

import java.io.*;
import java.util.Arrays;
import java.math.BigInteger;

import sun.security.util.DerInputStream;

public class PaddedBitString {

    // Relaxed the BitString parsing routine to accept bit strings
    // with padding bits, ex. treat DER_BITSTRING_PAD6 as the same
    // bit string as DER_BITSTRING_NOPAD.
    // Note:
    // 1. the number of padding bits has to be in [0...7]
    // 2. value of the padding bits is ignored

    // bit string (01011101 11000000)
    // With 6 padding bits (01011101 11001011)
    private final static byte[] DER_BITSTRING_PAD6 = { 3, 3, 6,
                                                   (byte)0x5d, (byte)0xcb };

    // With no padding bits
    private final static byte[] DER_BITSTRING_NOPAD = { 3, 3, 0,
                                                   (byte)0x5d, (byte)0xc0 };

    public static void main(String args[]) throws Exception {
        byte[] ba0, ba1;
        try {
            DerInputStream derin = new DerInputStream(DER_BITSTRING_PAD6);
            ba1 = derin.getBitString();
        } catch( IOException e ) {
            e.printStackTrace();
            throw new Exception("Unable to parse BitString with 6 padding bits");
        }

        try {
            DerInputStream derin = new DerInputStream(DER_BITSTRING_NOPAD);
            ba0 = derin.getBitString();
        } catch( IOException e ) {
            e.printStackTrace();
            throw new Exception("Unable to parse BitString with no padding");
        }

        if (Arrays.equals(ba1, ba0) == false ) {
            throw new Exception("BitString comparison check failed");
        }
    }
}
