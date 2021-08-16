/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import jdk.test.lib.RandomFactory;

/* @test
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main ReadAllReadNTransferTo
 * @bug 6766844 8180451
 * @summary Verify ByteArrayInputStream readAllBytes, readNBytes, and transferTo
 * @key randomness
 */
public class ReadAllReadNTransferTo {
    private static final int SIZE = 0x4d4d;

    private static Random random = RandomFactory.getRandom();

    public static void main(String... args) throws IOException {
        byte[] buf = new byte[SIZE];
        random.nextBytes(buf);
        int position = random.nextInt(SIZE/2);
        int size = random.nextInt(SIZE - position);

        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        bais.readAllBytes();
        if (bais.read(new byte[0]) != -1) {
            throw new RuntimeException("read(byte[]) did not return -1");
        }
        if (bais.read(new byte[1], 0, 0) != -1) {
            throw new RuntimeException("read(byte[],int,int) did not return -1");
        }

        bais = new ByteArrayInputStream(buf, position, size);
        int off = size < 2 ? 0 : random.nextInt(size / 2);
        int len = size - off < 1 ? 0 : random.nextInt(size - off);

        byte[] bN = new byte[off + len];
        if (bais.readNBytes(bN, off, len) != len) {
            throw new RuntimeException("readNBytes return value");
        }
        if (!Arrays.equals(bN, off, off + len,
            buf, position, position + len)) {
            throw new RuntimeException("readNBytes content");
        }

        byte[] bAll = bais.readAllBytes();
        Objects.requireNonNull(bAll, "readAllBytes return value");
        if (bAll.length != size - len) {
            throw new RuntimeException("readAllBytes return value length");
        }
        if (!Arrays.equals(bAll, 0, bAll.length,
            buf, position + len, position + len + bAll.length)) {
            throw new RuntimeException("readAllBytes content");
        }

        bais = new ByteArrayInputStream(buf);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
        if (bais.transferTo(baos) != buf.length) {
            throw new RuntimeException("transferTo return value length");
        }
        if (!Arrays.equals(buf, baos.toByteArray())) {
            throw new RuntimeException("transferTo content");
        }
    }
}
