/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8227368
 * @summary Test deserialization of a stream containing EnumSet.class object
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EnumSetClassSerialization {

    public static void main(String[] args) throws Exception {
        // EnumSet.class object serialized with JDK 8
        int[] bytes = {
            0xac, 0xed, 0x00, 0x05, 0x76, 0x72, 0x00, 0x11, 0x6a, 0x61, 0x76, 0x61, 0x2e, 0x75, 0x74, 0x69,
            0x6c, 0x2e, 0x45, 0x6e, 0x75, 0x6d, 0x53, 0x65, 0x74, 0x0e, 0x03, 0x21, 0x6a, 0xcd, 0x8c, 0x29,
            0xdd, 0x02, 0x00, 0x02, 0x4c, 0x00, 0x0b, 0x65, 0x6c, 0x65, 0x6d, 0x65, 0x6e, 0x74, 0x54, 0x79,
            0x70, 0x65, 0x74, 0x00, 0x11, 0x4c, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e, 0x67, 0x2f,
            0x43, 0x6c, 0x61, 0x73, 0x73, 0x3b, 0x5b, 0x00, 0x08, 0x75, 0x6e, 0x69, 0x76, 0x65, 0x72, 0x73,
            0x65, 0x74, 0x00, 0x11, 0x5b, 0x4c, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c, 0x61, 0x6e, 0x67, 0x2f,
            0x45, 0x6e, 0x75, 0x6d, 0x3b, 0x78, 0x70
        };

        InputStream in = new InputStream() {
            int i = 0;

            @Override
            public int read() {
                return i < bytes.length ? bytes[i++] & 0xFF : -1;
            }
        };
        ObjectInputStream ois = new ObjectInputStream(in);

        Object res = ois.readObject();

        if (res != EnumSet.class) {
            throw new AssertionError(
                "Expected: " + EnumSet.class + ", got: " + res);
        }
    }

    /**
     * This class can be used to print out lines that constitute
     * the 'bytes' variable initializer in the test.
     */
    public static class Serializer {
        public static void main(String[] args) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(EnumSet.class);
            oos.close();
            byte[] bytes = baos.toByteArray();
            int bpl = 16;
            System.out.print(
                IntStream
                    .range(0, (bytes.length + bpl - 1) / bpl)
                    .mapToObj(i -> IntStream
                        .range(
                            i * bpl,
                            Math.min(i * bpl + bpl, bytes.length)
                        )
                        .mapToObj(ii -> {
                            String s = Integer.toHexString(bytes[ii] & 0xFF);
                            return s.length() == 1 ? "0x0" + s : "0x" + s;
                        })
                        .collect(Collectors.joining(", "))
                    )
                    .collect(Collectors.joining(",\n  ", "int[] bytes = {\n  ", "\n};"))
            );
        }
    }
}
