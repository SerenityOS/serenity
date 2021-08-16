/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6663621
 * @summary JVM crashes while trying to execute api/java_security/Signature/SignatureTests.html#initSign tests.
 *
 * @run main compiler.c2.IVTest
 */

package compiler.c2;

public class IVTest {
    static int paddedSize;

    static void padV15(byte[] padded) {
        int psSize = padded.length;
        int k = 0;
        while (psSize-- > 0) {
            padded[k++] = (byte)0xff;
        }
    }

    static void padV15_2(int paddedSize) {
        byte[] padded = new byte[paddedSize];
        int psSize = padded.length;
        int k = 0;
        while (psSize-- > 0) {
            padded[k++] = (byte)0xff;
        }
    }

    static void padV15_3() {
        byte[] padded = new byte[paddedSize];
        int psSize = padded.length;
        int k = 0;
        while (psSize-- > 0) {
            padded[k++] = (byte)0xff;
        }
    }

    static void padV15_4() {
        byte[] padded = new byte[paddedSize];
        int psSize = padded.length;
        for (int k = 0;psSize > 0; psSize--) {
            int i = padded.length - psSize;
            padded[i] = (byte)0xff;
        }
    }

    static void padV15_5() {
        byte[] padded = new byte[paddedSize];
        int psSize = padded.length;
        int k = psSize - 1;
        for (int i = 0; i < psSize; i++) {
            padded[k--] = (byte)0xff;
        }
    }

    public static void main(String argv[]) {
        int bounds = 1024;
        int lim = 500000;
        long start = System.currentTimeMillis();
        for (int j = 0; j < lim; j++) {
            paddedSize = j % bounds;
            padV15(new byte[paddedSize]);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
        start = System.currentTimeMillis();
        for (int j = 0; j < lim; j++) {
            paddedSize = j % bounds;
            padV15_2(paddedSize);
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
        start = System.currentTimeMillis();
        for (int j = 0; j < lim; j++) {
            paddedSize = j % bounds;
            padV15_3();
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
        start = System.currentTimeMillis();
        for (int j = 0; j < lim; j++) {
            paddedSize = j % bounds;
            padV15_4();
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
        start = System.currentTimeMillis();
        for (int j = 0; j < lim; j++) {
            paddedSize = j % bounds;
            padV15_5();
        }
        end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}
