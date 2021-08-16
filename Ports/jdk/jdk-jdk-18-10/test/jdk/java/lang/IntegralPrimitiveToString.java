/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongFunction;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;

/**
 * @test
 * @run testng IntegralPrimitiveToString
 * @summary test string conversions for primitive integral types.
 * @author Mike Duigou
 */
public class IntegralPrimitiveToString {

    @Test(dataProvider="numbers")
    public <N extends Number> void testToString(String description,
        Function<N, BigInteger> converter,
        Function<N, BigInteger> unsignedConverter,
        N[] values,
        Stringifier<N>[] stringifiers) {
        System.out.printf("%s : conversions: %d values: %d\n", description, stringifiers.length, values.length);
        for( N value : values) {
            BigInteger asBigInt = converter.apply(value);
            BigInteger asUnsignedBigInt = unsignedConverter.apply(value);
            for(Stringifier<N> stringifier : stringifiers) {
                stringifier.assertMatchingToString(value, asBigInt, asUnsignedBigInt, description);
            }
        }
    }

    static class Stringifier<N extends Number> {
        final boolean signed;
        final int  radix;
        final Function<N,String> toString;
        Stringifier(boolean signed, int radix, Function<N,String> toString) {
            this.signed = signed;
            this.radix = radix;
            this.toString = toString;
        }

        public void assertMatchingToString(N value, BigInteger asSigned, BigInteger asUnsigned, String description) {
            String expected = signed
                ? asSigned.toString(radix)
                : asUnsigned.toString(radix);

            String actual = toString.apply(value);

            assertEquals(actual, expected, description + " conversion should be the same");
        }
    }

    @DataProvider(name="numbers", parallel=true)
    public Iterator<Object[]> testSetProvider() {

    return Arrays.asList(
        new Object[] { "Byte",
            (Function<Byte,BigInteger>) b -> BigInteger.valueOf((long) b),
            (Function<Byte,BigInteger>) b -> BigInteger.valueOf(Integer.toUnsignedLong((byte) b)),
            numberProvider((LongFunction<Byte>) l -> Byte.valueOf((byte) l), Byte.SIZE),
            new Stringifier[] {
                new Stringifier<Byte>(true, 10, b -> b.toString()),
                new Stringifier<Byte>(true, 10, b -> Byte.toString(b))
            }
        },
        new Object[] { "Short",
            (Function<Short,BigInteger>) s -> BigInteger.valueOf((long) s),
            (Function<Short,BigInteger>) s -> BigInteger.valueOf(Integer.toUnsignedLong((short) s)),
            numberProvider((LongFunction<Short>) l -> Short.valueOf((short) l), Short.SIZE),
            new Stringifier[] {
                new Stringifier<Short>(true, 10, s -> s.toString()),
                new Stringifier<Short>(true, 10, s -> Short.toString( s))
            }
        },
        new Object[] { "Integer",
            (Function<Integer,BigInteger>) i -> BigInteger.valueOf((long) i),
            (Function<Integer,BigInteger>) i -> BigInteger.valueOf(Integer.toUnsignedLong(i)),
            numberProvider((LongFunction<Integer>) l -> Integer.valueOf((int) l), Integer.SIZE),
            new Stringifier[] {
                new Stringifier<Integer>(true, 10, i -> i.toString()),
                new Stringifier<Integer>(true, 10, i -> Integer.toString(i)),
                new Stringifier<Integer>(false, 2, Integer::toBinaryString),
                new Stringifier<Integer>(false, 16, Integer::toHexString),
                new Stringifier<Integer>(false, 8, Integer::toOctalString),
                new Stringifier<Integer>(true, 2, i -> Integer.toString(i, 2)),
                new Stringifier<Integer>(true, 8, i -> Integer.toString(i, 8)),
                new Stringifier<Integer>(true, 10, i -> Integer.toString(i, 10)),
                new Stringifier<Integer>(true, 16, i -> Integer.toString(i, 16)),
                new Stringifier<Integer>(true, Character.MAX_RADIX, i -> Integer.toString(i, Character.MAX_RADIX)),
                new Stringifier<Integer>(false, 10, i -> Integer.toUnsignedString(i)),
                new Stringifier<Integer>(false, 2, i -> Integer.toUnsignedString(i, 2)),
                new Stringifier<Integer>(false, 8, i -> Integer.toUnsignedString(i, 8)),
                new Stringifier<Integer>(false, 10, i -> Integer.toUnsignedString(i, 10)),
                new Stringifier<Integer>(false, 16, i -> Integer.toUnsignedString(i, 16)),
                new Stringifier<Integer>(false, Character.MAX_RADIX, i -> Integer.toUnsignedString(i, Character.MAX_RADIX))
            }
        },
        new Object[] { "Long",
            (Function<Long, BigInteger>) BigInteger::valueOf,
            (Function<Long, BigInteger>) l -> {
                if (l >= 0) {
                    return BigInteger.valueOf((long) l);
                } else {
                    int upper = (int)(l >>> 32);
                    int lower = (int) (long) l;

                    // return (upper << 32) + lower
                    return (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                    add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
                }
            },
            numberProvider((LongFunction<Long>) Long::valueOf, Long.SIZE),
            new Stringifier[] {
                new Stringifier<Long>(true, 10, l -> l.toString()),
                new Stringifier<Long>(true, 10, l -> Long.toString(l)),
                new Stringifier<Long>(false, 2, Long::toBinaryString),
                new Stringifier<Long>(false, 16, Long::toHexString),
                new Stringifier<Long>(false, 8, Long::toOctalString),
                new Stringifier<Long>(true, 2, l -> Long.toString(l, 2)),
                new Stringifier<Long>(true, 8, l -> Long.toString(l, 8)),
                new Stringifier<Long>(true, 10, l -> Long.toString(l, 10)),
                new Stringifier<Long>(true, 16, l -> Long.toString(l, 16)),
                new Stringifier<Long>(true, Character.MAX_RADIX, l -> Long.toString(l, Character.MAX_RADIX)),
                new Stringifier<Long>(false, 10, Long::toUnsignedString),
                new Stringifier<Long>(false, 2, l -> Long.toUnsignedString(l, 2)),
                new Stringifier<Long>(false, 8, l-> Long.toUnsignedString(l, 8)),
                new Stringifier<Long>(false, 10, l -> Long.toUnsignedString(l, 10)),
                new Stringifier<Long>(false, 16, l -> Long.toUnsignedString(l, 16)),
                new Stringifier<Long>(false, Character.MAX_RADIX, l -> Long.toUnsignedString(l, Character.MAX_RADIX))
            }
        }
        ).iterator();
    }
    private static final long[] SOME_PRIMES = {
        3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L, 31L, 37L, 41L, 43L, 47L, 53L,
        59L, 61L, 71L, 73L, 79L, 83L, 89L, 97L, 101L, 103L, 107L, 109L, 113L,
        5953L, 5981L, 5987L, 6007L, 6011L, 6029L, 6037L, 6043L, 6047L, 6053L,
        16369L, 16381L, 16411L, 32749L, 32771L, 65521L, 65537L,
        (long) Integer.MAX_VALUE };

    public <N extends Number> N[] numberProvider(LongFunction<N> boxer, int bits, N... extras) {
        List<N> numbers = new ArrayList<>();

        for(int bitmag = 0; bitmag < bits; bitmag++) {
            long value = 1L << bitmag;
            numbers.add(boxer.apply(value));
            numbers.add(boxer.apply(value - 1));
            numbers.add(boxer.apply(value + 1));
            numbers.add(boxer.apply(-value));
            for(int divisor = 0; divisor < SOME_PRIMES.length && value < SOME_PRIMES[divisor]; divisor++) {
                numbers.add(boxer.apply(value - SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value + SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value * SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value / SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value | SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value & SOME_PRIMES[divisor]));
                numbers.add(boxer.apply(value ^ SOME_PRIMES[divisor]));
            }
        }

        numbers.addAll(Arrays.asList(extras));

        return (N[]) numbers.toArray(new Number[numbers.size()]);
    }
}
