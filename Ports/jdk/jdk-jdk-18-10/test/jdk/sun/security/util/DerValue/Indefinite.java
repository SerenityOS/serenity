/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6731685 8249783
 * @summary CertificateFactory.generateCertificates throws IOException on PKCS7 cert chain
 * @modules java.base/sun.security.util:+open
 * @library /test/lib
 */

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import sun.security.util.*;

public class Indefinite {

    public static void main(String[] args) throws Exception {

        // Indefinite length with trailing bytes
        test(true, new byte[] {
                // An OCTET-STRING in 2 parts
                0x24, (byte) 0x80, 4, 2, 'a', 'b', 4, 2, 'c', 'd', 0, 0,
                // Garbage follows, may be falsely recognized as EOC
                0, 0, 0, 0,
                // and more
                7, 8, 9, 10});

        // Definite length with trailing bytes
        test(false, new byte[] {
                4, 4, 'a', 'b', 'c', 'd',
                0, 0, 0, 0, 7, 8, 9, 10 });
    }

    static void test(boolean indefinite, byte[] input) throws Exception {

        // 1. parse stream
        InputStream ins = new ByteArrayInputStream(input);
        DerValue v = new DerValue(ins);
        Asserts.assertEQ(new String(v.getOctetString()), "abcd");

        if (indefinite) {
            // Trailing bytes might be consumed by the conversion but can
            // be found in DerValue "after end".
            Field buffer = DerValue.class.getDeclaredField("buffer");
            Field end = DerValue.class.getDeclaredField("end");
            buffer.setAccessible(true);
            end.setAccessible(true);
            int bufferLen = ((byte[])buffer.get(v)).length;
            int endPos = end.getInt(v);
            // Data "after end": bufferLen - endPos
            // Data remained in stream: ins.available()x`
            Asserts.assertEQ(bufferLen - endPos + ins.available(), 8);
        } else {
            // Trailing bytes remain in the stream for definite length
            Asserts.assertEQ(ins.available(), 8);
        }

        // 2. parse DerInputStream
        DerInputStream ds = new DerInputStream(input);
        Asserts.assertEQ(new String(ds.getDerValue().getOctetString()), "abcd");
        Asserts.assertEQ(ds.available(), 8);
        Asserts.assertTrue(Arrays.equals(ds.toByteArray(),
                new byte[]{0,0,0,0,7,8,9,10}));

        // 3. Parse full byte array
        Utils.runAndCheckException(() -> new DerValue(input),
                e -> Asserts.assertTrue(e instanceof IOException
                        && e.getMessage().equals("extra data at the end")));

        // 4. Parse exact byte array
        Asserts.assertEQ(new String(new DerValue(Arrays.copyOf(input, input.length - 8))
                .getOctetString()), "abcd");
    }
}
