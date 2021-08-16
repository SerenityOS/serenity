/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, Red Hat Inc. All rights reserved.
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
 * @bug 8130150 8131779 8139907
 * @summary Verify that the Montgomery multiply and square intrinsic works and correctly checks their arguments.
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @modules java.base/jdk.internal.misc:open
 * @modules java.base/java.math:open
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      compiler.intrinsics.bigInteger.MontgomeryMultiplyTest
 */

package compiler.intrinsics.bigInteger;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

public class MontgomeryMultiplyTest {

    private static final WhiteBox wb = WhiteBox.getWhiteBox();

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    static final MethodHandle montgomeryMultiplyHandle, montgomerySquareHandle;
    static final MethodHandle bigIntegerConstructorHandle;
    static final Field bigIntegerMagField;

    static {
       // Use reflection to gain access to the methods we want to test.
        try {
            Method m = BigInteger.class.getDeclaredMethod("montgomeryMultiply",
                /*a*/int[].class, /*b*/int[].class, /*n*/int[].class, /*len*/int.class,
                /*inv*/long.class, /*product*/int[].class);
            m.setAccessible(true);
            montgomeryMultiplyHandle = lookup.unreflect(m);

            m = BigInteger.class.getDeclaredMethod("montgomerySquare",
                /*a*/int[].class, /*n*/int[].class, /*len*/int.class,
                /*inv*/long.class, /*product*/int[].class);
            m.setAccessible(true);
            montgomerySquareHandle = lookup.unreflect(m);

            Constructor c
                = BigInteger.class.getDeclaredConstructor(int.class, int[].class);
            c.setAccessible(true);
            bigIntegerConstructorHandle = lookup.unreflectConstructor(c);

            bigIntegerMagField = BigInteger.class.getDeclaredField("mag");
            bigIntegerMagField.setAccessible(true);

        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /* Obtain executable for the intrinsics tested. Depending on the
     * value of 'isMultiply', the executable corresponding to either
     * implMontgomerMultiply or implMontgomerySqure is returned. */
    static Executable getExecutable(boolean isMultiply) throws RuntimeException {
        try {
            Class aClass = Class.forName("java.math.BigInteger");
            Method aMethod;
            if (isMultiply) {
                aMethod = aClass.getDeclaredMethod("implMontgomeryMultiply",
                                                   int[].class,
                                                   int[].class,
                                                   int[].class,
                                                   int.class,
                                                   long.class,
                                                   int[].class);
            } else {
                aMethod = aClass.getDeclaredMethod("implMontgomerySquare",
                                                   int[].class,
                                                   int[].class,
                                                   int.class,
                                                   long.class,
                                                   int[].class);
            }
            return aMethod;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Test bug, method is unavailable. " + e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Test bug, class is unavailable. " + e);
        }
    }

    // Invoke either BigInteger.montgomeryMultiply or BigInteger.montgomerySquare.
    int[] montgomeryMultiply(int[] a, int[] b, int[] n, int len, long inv,
                             int[] product) throws Throwable {
        int[] result =
            (a == b) ? (int[]) montgomerySquareHandle.invokeExact(a, n, len, inv, product)
                     : (int[]) montgomeryMultiplyHandle.invokeExact(a, b, n, len, inv, product);
        return Arrays.copyOf(result, len);
    }

    // Invoke the private constructor BigInteger(int[]).
    BigInteger newBigInteger(int[] val) throws Throwable {
        return (BigInteger) bigIntegerConstructorHandle.invokeExact(1, val);
    }

    // Get the private field BigInteger.mag
    int[] mag(BigInteger n) {
        try {
            return (int[]) bigIntegerMagField.get(n);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // Montgomery multiplication
    // Calculate a * b * r^-1 mod n)
    //
    // R is a power of the word size
    // N' = R^-1 mod N
    //
    // T := ab
    // m := (T mod R)N' mod R [so 0 <= m < R]
    // t := (T + mN)/R
    // if t >= N then return t - N else return t
    //
    BigInteger montgomeryMultiply(BigInteger a, BigInteger b, BigInteger N,
            int len, BigInteger n_prime)
            throws Throwable {
        BigInteger T = a.multiply(b);
        BigInteger R = BigInteger.ONE.shiftLeft(len*32);
        BigInteger mask = R.subtract(BigInteger.ONE);
        BigInteger m = (T.and(mask)).multiply(n_prime);
        m = m.and(mask); // i.e. m.mod(R)
        T = T.add(m.multiply(N));
        T = T.shiftRight(len*32); // i.e. T.divide(R)
        if (T.compareTo(N) > 0) {
            T = T.subtract(N);
        }
        return T;
    }

    // Call the Montgomery multiply intrinsic.
    BigInteger montgomeryMultiply(int[] a_words, int[] b_words, int[] n_words,
            int len, BigInteger inv)
            throws Throwable {
        BigInteger t = montgomeryMultiply(
                newBigInteger(a_words),
                newBigInteger(b_words),
                newBigInteger(n_words),
                len, inv);
        return t;
    }

    // Check that the Montgomery multiply intrinsic returns the same
    // result as the longhand calculation.
    void check(int[] a_words, int[] b_words, int[] n_words, int len, BigInteger inv)
            throws Throwable {
        BigInteger n = newBigInteger(n_words);
        BigInteger slow = montgomeryMultiply(a_words, b_words, n_words, len, inv);
        BigInteger fast
            = newBigInteger(montgomeryMultiply
                            (a_words, b_words, n_words, len, inv.longValue(), null));
        // The intrinsic may not return the same value as the longhand
        // calculation but they must have the same residue mod N.
        if (!slow.mod(n).equals(fast.mod(n))) {
            throw new RuntimeException();
        }
    }

    Random rnd = new Random(42);

    // Return a random value of length <= bits in an array of even length
    int[] random_val(int bits) {
        int len = (bits+63)/64;  // i.e. length in longs
        int[] val = new int[len*2];
        for (int i = 0; i < val.length; i++)
            val[i] = rnd.nextInt();
        int leadingZeros = 64 - (bits & 64);
        if (leadingZeros >= 32) {
            val[0] = 0;
            val[1] &= ~(-1l << (leadingZeros & 31));
        } else {
            val[0] &= ~(-1l << leadingZeros);
        }
        return val;
    }

    void testOneLength(int lenInBits, int lenInInts) throws Throwable {
        BigInteger mod = new BigInteger(lenInBits, 2, rnd);
        BigInteger r = BigInteger.ONE.shiftLeft(lenInInts * 32);
        BigInteger n_prime = mod.modInverse(r).negate();

        // Make n.length even, padding with a zero if necessary
        int[] n = mag(mod);
        if (n.length < lenInInts) {
            int[] x = new int[lenInInts];
            System.arraycopy(n, 0, x, lenInInts-n.length, n.length);
            n = x;
        }

        for (int i = 0; i < 10000; i++) {
            // multiply
            check(random_val(lenInBits), random_val(lenInBits), n, lenInInts, n_prime);
            // square
            int[] tmp = random_val(lenInBits);
            check(tmp, tmp, n, lenInInts, n_prime);
        }
    }

    // Test the Montgomery multiply intrinsic with a bunch of random
    // values of varying lengths.  Do this for long enough that the
    // caller of the intrinsic is C2-compiled.
    void testResultValues() throws Throwable {
        // Test a couple of interesting edge cases.
        testOneLength(1024, 32);
        testOneLength(1025, 34);
        for (int j = 10; j > 0; j--) {
            // Construct a random prime whose length in words is even
            int lenInBits = rnd.nextInt(2048) + 64;
            int lenInInts = (lenInBits + 63)/64*2;
            testOneLength(lenInBits, lenInInts);
        }
    }

    // Range checks
    void testOneMontgomeryMultiplyCheck(int[] a, int[] b, int[] n, int len, long inv,
                                        int[] product, Class klass) {
        try {
            montgomeryMultiply(a, b, n, len, inv, product);
        } catch (Throwable ex) {
            if (klass.isAssignableFrom(ex.getClass()))
                return;
            throw new RuntimeException(klass + " expected, " + ex + " was thrown");
        }
        throw new RuntimeException(klass + " expected, was not thrown");
    }

    void testOneMontgomeryMultiplyCheck(int[] a, int[] b, BigInteger n, int len, BigInteger inv,
            Class klass) {
        testOneMontgomeryMultiplyCheck(a, b, mag(n), len, inv.longValue(), null, klass);
    }

    void testOneMontgomeryMultiplyCheck(int[] a, int[] b, BigInteger n, int len, BigInteger inv,
            int[] product, Class klass) {
        testOneMontgomeryMultiplyCheck(a, b, mag(n), len, inv.longValue(), product, klass);
    }

    void testMontgomeryMultiplyChecks() {
        int[] blah = random_val(40);
        int[] small = random_val(39);
        BigInteger mod = new BigInteger(40*32 , 2, rnd);
        BigInteger r = BigInteger.ONE.shiftLeft(40*32);
        BigInteger n_prime = mod.modInverse(r).negate();

        // Length out of range: square
        testOneMontgomeryMultiplyCheck(blah, blah, mod, 41, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah, mod, 0, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah, mod, -1, n_prime, IllegalArgumentException.class);
        // As above, but for multiply
        testOneMontgomeryMultiplyCheck(blah, blah.clone(), mod, 41, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah.clone(), mod, 0, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah.clone(), mod, 0, n_prime, IllegalArgumentException.class);

        // Length odd
        testOneMontgomeryMultiplyCheck(small, small, mod, 39, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, small, mod, 0, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, small, mod, -1, n_prime, IllegalArgumentException.class);
        // As above, but for multiply
        testOneMontgomeryMultiplyCheck(small, small.clone(), mod, 39, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, small.clone(), mod, 0, n_prime, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, small.clone(), mod, -1, n_prime, IllegalArgumentException.class);

        // array too small
        testOneMontgomeryMultiplyCheck(blah, blah, mod, 40, n_prime, small, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah.clone(), mod, 40, n_prime, small, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, blah, mod, 40, n_prime, blah, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, small, mod, 40, n_prime, blah, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(blah, blah, mod, 40, n_prime, small, IllegalArgumentException.class);
        testOneMontgomeryMultiplyCheck(small, small, mod, 40, n_prime, blah, IllegalArgumentException.class);
    }

    public static void main(String args[]) {
        if (!Platform.isServer() || Platform.isEmulatedClient()) {
            throw new Error("TESTBUG: Not server mode");
        }
        if (wb.isIntrinsicAvailable(getExecutable(true), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION) &&
                wb.isIntrinsicAvailable(getExecutable(false), CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION)) {
            try {
                new MontgomeryMultiplyTest().testMontgomeryMultiplyChecks();
                new MontgomeryMultiplyTest().testResultValues();
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
