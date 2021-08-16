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

/* @test
 * @bug 6196991
 * @summary Roundtrip Encoding/Decoding of just one ASCII char
 * @author Martin Buchholz
 */

import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public class FindASCIICodingBugs {
    private static int failures = 0;

    private static void check(boolean condition) {
        if (! condition) {
            new Error("test failed").printStackTrace();
            failures++;
        }
    }

    private static boolean equals(byte[] ba, ByteBuffer bb) {
        if (ba.length != bb.limit())
            return false;
        for (int i = 0; i < ba.length; i++)
            if (ba[i] != bb.get(i))
                return false;
        return true;
    }

    public static void main(String[] args) throws Exception {
        for (Map.Entry<String,Charset> e
                 : Charset.availableCharsets().entrySet()) {
            String csn = e.getKey();
            Charset cs = e.getValue();

            // Delete the following lines when these charsets are fixed!
            if (csn.equals("x-JIS0208"))      continue; // MalformedInput
            if (csn.equals("JIS_X0212-1990")) continue; // MalformedInput

            if (! cs.canEncode()) continue;

            CharsetEncoder enc = cs.newEncoder();
            CharsetDecoder dec = cs.newDecoder();

            if (! enc.canEncode('A')) continue;

            System.out.println(csn);

            try {
                byte[] bytes1 = "A".getBytes(csn);
                ByteBuffer bb = enc.encode(CharBuffer.wrap(new char[]{'A'}));

                check(equals(bytes1, bb));
                check(new String(bytes1, csn).equals("A"));

                CharBuffer cb = dec.decode(bb);
                check(cb.toString().equals("A"));
            } catch (Throwable t) {
                t.printStackTrace();
                failures++;
            }
        }

        if (failures > 0)
            throw new Exception(failures + "tests failed");
    }
}
