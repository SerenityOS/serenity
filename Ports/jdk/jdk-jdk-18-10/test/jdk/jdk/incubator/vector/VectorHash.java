/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.incubator.vector
 */

import jdk.incubator.vector.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.SplittableRandom;

import static java.util.stream.Collectors.joining;

public class VectorHash {

    public static void main(String[] args) {
        Properties p = System.getProperties();
        p.forEach((k, v) -> {
            byte[] bk = k.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bv = v.toString().getBytes(StandardCharsets.UTF_8);
            assertHashCode(bk);
            assertHashCode(bv);
        });

        String letters = new SplittableRandom().ints(1024, 'A', 'Z' + 1).
                mapToObj(c -> Character.toString((char) c)).collect(joining());
        assertHashCode(letters.getBytes(StandardCharsets.UTF_8));
    }

    static void assertHashCode(byte[] b) {
        int expected = Arrays.hashCode(b);
        assertEquals(hashCodeUnrollExplicit(b), expected);
        assertEquals(hashCodeUnrollConstants(b), expected);
        assertEquals(hashCodeVector64(b), expected);
        assertEquals(hashCodeVector128(b), expected);
        assertEquals(hashCodeVector512Shift(b), expected);
    }

    static void assertEquals(Object a, Object b) {
        if (!a.equals(b)) throw new AssertionError();
    }

    static int hashCodeUnrollExplicit(byte[] a) {
        if (a == null)
            return 0;

        int h = 1;
        int i = 0;
        for (; i < (a.length & ~(8 - 1)); i += 8) {
            h = h * 31 * 31 * 31 * 31 * 31 * 31 * 31 * 31 +
                a[i + 0] * 31 * 31 * 31 * 31 * 31 * 31 * 31 +
                a[i + 1] * 31 * 31 * 31 * 31 * 31 * 31 +
                a[i + 2] * 31 * 31 * 31 * 31 * 31 +
                a[i + 3] * 31 * 31 * 31 * 31 +
                a[i + 4] * 31 * 31 * 31 +
                a[i + 5] * 31 * 31 +
                a[i + 6] * 31 +
                a[i + 7];
        }

        for (; i < a.length; i++) {
            h = 31 * h + a[i];
        }
        return h;
    }

    static int hashCodeUnrollConstants(byte[] a) {
        if (a == null)
            return 0;

        int h = 1;
        int i = 0;
        for (; i < (a.length & ~(8 - 1)); i += 8) {
            h = h * COEFF_31_TO_8 +
                a[i + 0] * H_COEFF_8.lane(0) +
                a[i + 1] * H_COEFF_8.lane(1) +
                a[i + 2] * H_COEFF_8.lane(2) +
                a[i + 3] * H_COEFF_8.lane(3) +
                a[i + 4] * H_COEFF_8.lane(4) +
                a[i + 5] * H_COEFF_8.lane(5) +
                a[i + 6] * H_COEFF_8.lane(6) +
                a[i + 7] * H_COEFF_8.lane(7);
        }

        for (; i < a.length; i++) {
            h = 31 * h + a[i];
        }
        return h;
    }

    static int hashCodeVector64(byte[] a) {
        int h = 1;
        int i = 0;
        for (; i < (a.length & ~(BYTE_64_SPECIES.length() - 1)); i += BYTE_64_SPECIES.length()) {
            ByteVector b = ByteVector.fromArray(BYTE_64_SPECIES, a, i);
            IntVector x = (IntVector) b.castShape(INT_256_SPECIES, 0);
            h = h * COEFF_31_TO_8 + x.mul(H_COEFF_8).reduceLanes(VectorOperators.ADD);
        }

        for (; i < a.length; i++) {
            h = 31 * h + a[i];
        }
        return h;
    }

    static int hashCodeVector128(byte[] a) {
        int h = 1;
        int i = 0;
        for (; i < (a.length & ~(BYTE_128_SPECIES.length() - 1)); i += BYTE_128_SPECIES.length()) {
            ByteVector b = ByteVector.fromArray(BYTE_128_SPECIES, a, i);
            IntVector x = (IntVector) b.castShape(INT_512_SPECIES, 0);
            h = h * COEFF_31_TO_16 + x.mul(H_COEFF_16).reduceLanes(VectorOperators.ADD);
        }

        for (; i < a.length; i++) {
            h = 31 * h + a[i];
        }
        return h;
    }

    static int hashCodeVector512Shift(byte[] a) {
        return hashCodeVectorGenericShift(a,
                                          BYTE_128_SPECIES,
                                          BYTE_512_SPECIES,
                                          INT_512_SPECIES,
                                          COEFF_31_TO_16,
                                          H_COEFF_16);
    }

    static int hashCodeVectorGenericShift(
            byte[] a,
            VectorSpecies<Byte> bytesForIntsSpecies,
            VectorSpecies<Byte> byteSpecies, VectorSpecies<Integer> intSpecies,
            int top_h_coeff,
            IntVector v_h_coeff) {
        assert bytesForIntsSpecies.length() == intSpecies.length();

        int h = 1;
        int i = 0;
        for (; i < (a.length & ~(byteSpecies.length() - 1)); i += byteSpecies.length()) {
            ByteVector b = ByteVector.fromArray(byteSpecies, a, i);

            for (int j = 0; j < byteSpecies.length() / intSpecies.length(); j++) {
                // Reduce the size of the byte vector and then cast to int
                IntVector x = (IntVector)(b.reinterpretShape(bytesForIntsSpecies, 0)).castShape(intSpecies, 0);

                h = h * top_h_coeff + x.mul(v_h_coeff).reduceLanes(VectorOperators.ADD);

                b = b.slice(intSpecies.length());
            }
        }

        for (; i < a.length; i++) {
            h = 31 * h + a[i];
        }
        return h;
    }

    static final VectorSpecies<Integer> INT_512_SPECIES = IntVector.SPECIES_512;
    static final VectorSpecies<Integer> INT_256_SPECIES = IntVector.SPECIES_256;
    static final int COEFF_31_TO_16;
    static final IntVector H_COEFF_16;

    static final VectorSpecies<Byte> BYTE_512_SPECIES = ByteVector.SPECIES_512;
    static final VectorSpecies<Byte> BYTE_128_SPECIES = ByteVector.SPECIES_128;
    static final VectorSpecies<Byte> BYTE_64_SPECIES = ByteVector.SPECIES_64;
    static final int COEFF_31_TO_8;
    static final IntVector H_COEFF_8;

    static {
        int[] a = new int[INT_256_SPECIES.length()];
        a[a.length - 1] = 1;
        for (int i = 1; i < a.length; i++) {
            a[a.length - 1 - i] = a[a.length - 1 - i + 1] * 31;
        }

        COEFF_31_TO_8 = a[0] * 31;
        H_COEFF_8 = IntVector.fromArray(INT_256_SPECIES, a, 0);


        a = new int[INT_512_SPECIES.length()];
        a[a.length - 1] = 1;
        for (int i = 1; i < a.length; i++) {
            a[a.length - 1 - i] = a[a.length - 1 - i + 1] * 31;
        }

        COEFF_31_TO_16 = a[0] * 31;
        H_COEFF_16 = IntVector.fromArray(INT_512_SPECIES, a, 0);
    }
}
