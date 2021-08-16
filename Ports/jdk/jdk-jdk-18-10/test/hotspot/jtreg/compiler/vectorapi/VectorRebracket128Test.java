/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.incubator.vector.*;
import jdk.internal.vm.annotation.ForceInline;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;
import jdk.internal.vm.annotation.ForceInline;

/*
 * @test
 * @bug 8260473
 * @requires vm.gc.Z
 * @modules jdk.incubator.vector
 * @modules java.base/jdk.internal.vm.annotation
 * @run testng/othervm -XX:CompileCommand=compileonly,jdk/incubator/vector/ByteVector.fromByteBuffer
 *      -XX:-TieredCompilation -XX:CICompilerCount=1 -XX:+UseZGC -Xbatch -Xmx256m VectorRebracket128Test
 */

@Test
public class VectorRebracket128Test {
    static final int INVOC_COUNT = Integer.getInteger("jtreg.compiler.vectorapi.vectorrebracket128test.loop-iterations", 1000);
    static final int NUM_ITER = 200 * INVOC_COUNT;

    static final VectorSpecies<Integer> ispec128 = IntVector.SPECIES_128;
    static final VectorSpecies<Float> fspec128 = FloatVector.SPECIES_128;
    static final VectorSpecies<Long> lspec128 = LongVector.SPECIES_128;
    static final VectorSpecies<Double> dspec128 = DoubleVector.SPECIES_128;
    static final VectorSpecies<Byte> bspec128 = ByteVector.SPECIES_128;
    static final VectorSpecies<Short> sspec128 = ShortVector.SPECIES_128;

    static <T> IntFunction<T> withToString(String s, IntFunction<T> f) {
        return new IntFunction<T>() {
            @Override
            public T apply(int v) {
                return f.apply(v);
            }

            @Override
            public String toString() {
                return s;
            }
        };
    }

    interface ToByteF {
        byte apply(int i);
    }

    static byte[] fill_byte(int s , ToByteF f) {
        return fill_byte(new byte[s], f);
    }

    static byte[] fill_byte(byte[] a, ToByteF f) {
        for (int i = 0; i < a.length; i++) {
            a[i] = f.apply(i);
        }
        return a;
    }

    static final List<IntFunction<byte[]>> BYTE_GENERATORS = List.of(
            withToString("byte(i)", (int s) -> {
                return fill_byte(s, i -> (byte)(i+1));
            })
    );

    @DataProvider
    public Object[][] byteUnaryOpProvider() {
        return BYTE_GENERATORS.stream().
                map(f -> new Object[]{f}).
                toArray(Object[][]::new);
    }

    static
    void checkPartialResult(VectorSpecies<?> a, VectorSpecies<?> b,
                            byte[] input, byte[] output, byte[] expected,
                            int part, int origin) {
        if (Arrays.equals(expected, output)) {
            return;
        }
        int block;
        block = Math.min(a.vectorByteSize(), b.vectorByteSize());

        System.out.println("input:  "+Arrays.toString(input));
        System.out.println("Failing with "+a+"->"+b+
                           " (reinterpret)"+
                           ", block=" + block +
                           ", part=" + part +
                           ", origin=" + origin);
        System.out.println("expect: "+Arrays.toString(expected));
        System.out.println("output: "+Arrays.toString(output));
        Assert.assertEquals(expected, output);
    }

    @ForceInline
    static <E,F>
    void testVectorRebracket(VectorSpecies<E> a, VectorSpecies<F> b, byte[] input, byte[] output) {
        Vector<E> av = a.fromByteArray(input, 0, ByteOrder.nativeOrder());
        int block;
        assert(input.length == output.length);

        block = Math.min(a.vectorByteSize(), b.vectorByteSize());
        if (false)
            System.out.println("testing "+a+"->"+b+
                    (false?" (lanewise)":" (reinterpret)")+
                    ", block=" + block);
        byte[] expected;
        int origin;

        int part = 0;
        Vector<F> bv = av.reinterpretShape(b, part);
        bv.intoByteArray(output, 0, ByteOrder.nativeOrder());
        // in-place copy, no resize
        expected = input;
        origin = 0;
        checkPartialResult(a, b, input, output, expected,
                part, origin);

    }

    @Test(dataProvider = "byteUnaryOpProvider")
    static void testRebracket128(IntFunction<byte[]> fa) {
        byte[] barr = fa.apply(128/Byte.SIZE);
        byte[] bout = new byte[barr.length];
        for (int i = 0; i < NUM_ITER; i++) {
            testVectorRebracket(bspec128, bspec128, barr, bout);
            testVectorRebracket(bspec128, sspec128, barr, bout);
            testVectorRebracket(bspec128, ispec128, barr, bout);
        }
    }

}
