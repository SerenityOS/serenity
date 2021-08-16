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

/*
 * @test
 * @bug 8237581
 * @summary Testing allocation expansion when there is no use of the allocation
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+PrintCompilation -XX:+PrintEliminateAllocations -XX:CompileCommand=compileonly,compiler.allocation.TestAllocation::test*
 *                   compiler.allocation.TestAllocation
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+PrintCompilation -XX:+PrintEliminateAllocations -XX:-DoEscapeAnalysis -XX:CompileCommand=compileonly,compiler.allocation.TestAllocation::test*
 *                   compiler.allocation.TestAllocation
 */

package compiler.allocation;

public class TestAllocation {

    public static volatile int size = 128;
    public static volatile int neg_size = -128;

    public int testUnknownPositiveArrayLength() {
        Payload w = new Payload(17, size);
        return w.i + w.ba.length;
    }

    public int testStoreCapture() {
        byte[] bs = new byte[1];
        bs[0] = 1;
        return bs.length;
    }

    public int testUnknownNegativeArrayLength() {
        boolean success = false;
        try {
            Payload w = new Payload(17, neg_size);
            return w.i + w.ba.length;
        } catch (NegativeArraySizeException e) {
            success = true;
        }
        if (!success) {
            throw new RuntimeException("Should have thrown NegativeArraySizeException");
        }
        return 0;

    }

    public int testConstantPositiveArrayLength() {
        Payload w = new Payload(173, 10);
        return w.i + w.ba.length;
    }

    public int testConstantPositiveArrayLength2() {
        Payload w = new Payload(173, 10);
        w.i = 17;
        w.ba = new byte[10];
        return w.i + w.ba.length;
    }

    public int testConstantNegativeArrayLength() {
        boolean success = false;
        try {
            Payload w = new Payload(173, -10);
            return w.i + w.ba.length;
        } catch (NegativeArraySizeException e) {
            success = true;
        }
        if (!success) {
            throw new RuntimeException("Should have thrown NegativeArraySizeException");
        }
        return 0;
    }

    public static class Payload {
        public int i;
        public byte ba[];

        public Payload(int i, int size) {
            this.i = i;
            this.ba = new byte[size];
        }
    }

    public static void main(String[] strArr) {
        TestAllocation test = new TestAllocation();
        for (int i = 0; i < 10_000; i++ ) {
            test.testUnknownPositiveArrayLength();
            test.testUnknownNegativeArrayLength();
            test.testConstantPositiveArrayLength();
            test.testConstantPositiveArrayLength2();
            test.testConstantNegativeArrayLength();
            test.testStoreCapture();
        }
    }
}

class Wrapper {
    int[] arr;
    void setArr(int... many) { arr = many; }
}
