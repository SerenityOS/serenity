/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4504839 4215269 6322074 8030814
 * @summary Basic tests for unsigned operations
 * @author Joseph D. Darcy
 */

import java.math.*;

public class Unsigned {
    public static void main(String... args) {
        int errors = 0;

        errors += testRoundtrip();
        errors += testByteToUnsignedLong();
        errors += testShortToUnsignedLong();
        errors += testUnsignedCompare();
        errors += testToStringUnsigned();
        errors += testParseUnsignedLong();
        errors += testDivideAndRemainder();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found in unsigned operations.");
        }
    }

    private static final BigInteger TWO = BigInteger.valueOf(2L);

    private static int testRoundtrip() {
        int errors = 0;

        long[] data = {-1L, 0L, 1L};

        for(long datum : data) {
            if (Long.parseUnsignedLong(Long.toBinaryString(datum), 2) != datum) {
                errors++;
                System.err.println("Bad binary roundtrip conversion of " + datum);
            }

            if (Long.parseUnsignedLong(Long.toOctalString(datum), 8) != datum) {
                errors++;
                System.err.println("Bad octal roundtrip conversion of " + datum);
            }

            if (Long.parseUnsignedLong(Long.toHexString(datum), 16) != datum) {
                errors++;
                System.err.println("Bad hex roundtrip conversion of " + datum);
            }
        }
        return errors;
    }

    private static int testByteToUnsignedLong() {
        int errors = 0;

        for(int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            byte datum = (byte) i;
            long ui = Byte.toUnsignedLong(datum);

            if ( (ui & (~0xffL)) != 0L ||
                 ((byte)ui != datum )) {
                errors++;
                System.err.printf("Bad conversion of byte %d to unsigned long %d%n",
                                  datum, ui);
            }
        }
        return errors;
    }

    private static int testShortToUnsignedLong() {
        int errors = 0;

        for(int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
            short datum = (short) i;
            long ui = Short.toUnsignedLong(datum);

            if ( (ui & (~0xffffL)) != 0L ||
                 ((short)ui != datum )) {
                errors++;
                System.err.printf("Bad conversion of short %d to unsigned long %d%n",
                                  datum, ui);
            }
        }
        return errors;
    }
    private static int testUnsignedCompare() {
        int errors = 0;

        long[] data = {
            0L,
            1L,
            2L,
            3L,
            0x00000000_80000000L,
            0x00000000_FFFFFFFFL,
            0x00000001_00000000L,
            0x80000000_00000000L,
            0x80000000_00000001L,
            0x80000000_00000002L,
            0x80000000_00000003L,
            0x80000000_80000000L,
            0xFFFFFFFF_FFFFFFFEL,
            0xFFFFFFFF_FFFFFFFFL,
        };

        for(long i : data) {
            for(long j : data) {
                long libraryResult    = Long.compareUnsigned(i, j);
                long libraryResultRev = Long.compareUnsigned(j, i);
                long localResult      = compUnsigned(i, j);

                if (i == j) {
                    if (libraryResult != 0) {
                        errors++;
                        System.err.printf("Value 0x%x did not compare as " +
                                          "an unsigned equal to itself; got %d%n",
                                          i, libraryResult);
                    }
                }

                   if (Long.signum(libraryResult) != Long.signum(localResult)) {
                       errors++;
                       System.err.printf("Unsigned compare of 0x%x to 0x%x%n:" +
                                         "\texpected sign of %d, got %d%n",
                                         i, j, localResult, libraryResult);
                   }

                if (Long.signum(libraryResult) !=
                    -Long.signum(libraryResultRev)) {
                    errors++;
                    System.err.printf("signum(compareUnsigned(x, y)) != -signum(compareUnsigned(y,x))" +
                                      " for \t0x%x and 0x%x, computed %d and %d%n",
                                      i, j, libraryResult, libraryResultRev);
                }
            }
        }

        return errors;
    }

    private static int compUnsigned(long x, long y) {
        BigInteger big_x = toUnsignedBigInt(x);
        BigInteger big_y = toUnsignedBigInt(y);

        return big_x.compareTo(big_y);
    }

    private static BigInteger toUnsignedBigInt(long x) {
        if (x >= 0)
            return BigInteger.valueOf(x);
        else {
            int upper = (int)(((long)x) >> 32);
            int lower = (int) x;

            BigInteger bi = // (upper << 32) + lower
                (BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
                add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));

            // System.out.printf("%n\t%d%n\t%s%n", x, bi.toString());
            return bi;
        }
    }

    private static int testToStringUnsigned() {
        int errors = 0;

        long[] data = {
            0L,
            1L,
            2L,
            3L,
            99999L,
            100000L,
            999999L,
            100000L,
            999999999L,
            1000000000L,
            0x1234_5678L,
            0x8000_0000L,
            0x8000_0001L,
            0x8000_0002L,
            0x8000_0003L,
            0x8765_4321L,
            0xFFFF_FFFEL,
            0xFFFF_FFFFL,

            // Long-range values
              999_999_999_999L,
            1_000_000_000_000L,

              999_999_999_999_999_999L,
            1_000_000_000_000_000_000L,

            0xFFFF_FFFF_FFFF_FFFEL,
            0xFFFF_FFFF_FFFF_FFFFL,
        };

        for(int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
            for(long datum : data) {
                String result1 = Long.toUnsignedString(datum, radix);
                String result2 = toUnsignedBigInt(datum).toString(radix);

                if (!result1.equals(result2)) {
                    errors++;
                    System.err.printf("Unexpected string difference converting 0x%x:" +
                                      "\t%s %s%n",
                                      datum, result1, result2);
                }

                if (radix == 10) {
                    String result3 = Long.toUnsignedString(datum);
                    if (!result2.equals(result3)) {
                        errors++;
                        System.err.printf("Unexpected string difference converting 0x%x:" +
                                          "\t%s %s%n",
                                          datum, result3, result2);
                    }
                }

                long parseResult = Long.parseUnsignedLong(result1, radix);

                if (parseResult != datum) {
                    errors++;
                        System.err.printf("Bad roundtrip conversion of %d in base %d" +
                                          "\tconverting back ''%s'' resulted in %d%n",
                                          datum, radix, result1,  parseResult);
                }
            }
        }

        return errors;
    }

    private static int testParseUnsignedLong() {
        int errors = 0;
        long maxUnsignedInt = Integer.toUnsignedLong(0xffff_ffff);

        // Values include those between signed Long.MAX_VALUE and
        // unsignted Long MAX_VALUE.
        BigInteger[] inRange = {
            BigInteger.valueOf(0L),
            BigInteger.valueOf(1L),
            BigInteger.valueOf(10L),
            BigInteger.valueOf(2147483646L),   // Integer.MAX_VALUE - 1
            BigInteger.valueOf(2147483647L),   // Integer.MAX_VALUE
            BigInteger.valueOf(2147483648L),   // Integer.MAX_VALUE + 1

            BigInteger.valueOf(maxUnsignedInt - 1L),
            BigInteger.valueOf(maxUnsignedInt),

            BigInteger.valueOf(Long.MAX_VALUE - 1L),
            BigInteger.valueOf(Long.MAX_VALUE),
            BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),

            TWO.pow(64).subtract(BigInteger.ONE)
        };

        for(BigInteger value : inRange) {
            for(int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
                String bigString = value.toString(radix);
                long longResult = Long.parseUnsignedLong(bigString, radix);

                if (!toUnsignedBigInt(longResult).equals(value)) {
                    errors++;
                    System.err.printf("Bad roundtrip conversion of %d in base %d" +
                                      "\tconverting back ''%s'' resulted in %d%n",
                                      value, radix, bigString,  longResult);
                }

                // test offset based parse method
                longResult = Long.parseUnsignedLong("prefix" + bigString + "suffix", "prefix".length(),
                        "prefix".length() + bigString.length(), radix);

                if (!toUnsignedBigInt(longResult).equals(value)) {
                    errors++;
                    System.err.printf("Bad roundtrip conversion of %d in base %d" +
                            "\tconverting back ''%s'' resulted in %d%n",
                            value, radix, bigString,  longResult);
                }
            }
        }

        String[] outOfRange = {
            null,
            "",
            "-1",
            TWO.pow(64).toString(),
        };

        for(String s : outOfRange) {
            try {
                long result = Long.parseUnsignedLong(s);
                errors++; // Should not reach here
                System.err.printf("Unexpected got %d from an unsigned conversion of %s",
                                  result, s);
            } catch(NumberFormatException nfe) {
                ; // Correct result
            }
        }

        // test case known at one time to fail
        errors += testUnsignedOverflow("1234567890abcdef1", 16, true);

        // largest value with guard = 91 = 13*7; radix = 13
        errors += testUnsignedOverflow("196a78a44c3bba320c", 13, false);

        // smallest value with guard = 92 = 23*2*2; radix = 23
        errors += testUnsignedOverflow("137060c6g1c1dg0", 23, false);

        // guard in [92,98]: no overflow

        // one less than smallest guard value to overflow: guard = 99 = 11*3*3, radix = 33
        errors += testUnsignedOverflow("b1w8p7j5q9r6f", 33, false);

        // smallest guard value to overflow: guard = 99 = 11*3*3, radix = 33
        errors += testUnsignedOverflow("b1w8p7j5q9r6g", 33, true);

        // test overflow of overflow
        BigInteger maxUnsignedLong =
                BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        for (int radix = Character.MIN_RADIX; radix <= Character.MAX_RADIX; radix++) {
            BigInteger quotient = maxUnsignedLong.divide(BigInteger.valueOf(radix));
            for (int addend = 2; addend <= radix; addend++) {
                BigInteger b = quotient.multiply(BigInteger.valueOf(radix + addend));
                errors += testUnsignedOverflow(b.toString(radix), radix, b.compareTo(maxUnsignedLong) > 0);
            }
        }

        return errors;
    }

    // test for missing or unexpected unsigned overflow exception
    private static int testUnsignedOverflow(String s, int radix, boolean exception) {
        int errors = 0;
        long result;
        try {
            result = Long.parseUnsignedLong(s, radix);
            if (exception) {
                System.err.printf("Unexpected result %d for Long.parseUnsignedLong(%s,%d)\n",
                        result, s, radix);
                errors++;
            }
        } catch (NumberFormatException nfe) {
            if (!exception) {
                System.err.printf("Unexpected exception %s for Long.parseUnsignedLong(%s,%d)\n",
                        nfe.toString(), s, radix);
                errors++;
            }
        }
        return errors;
    }

    private static int testDivideAndRemainder() {
        int errors = 0;
        long TWO_31 = 1L << Integer.SIZE - 1;
        long TWO_32 = 1L << Integer.SIZE;
        long TWO_33 = 1L << Integer.SIZE + 1;
        BigInteger NINETEEN = BigInteger.valueOf(19L);
        BigInteger TWO_63 = BigInteger.ONE.shiftLeft(Long.SIZE - 1);
        BigInteger TWO_64 = BigInteger.ONE.shiftLeft(Long.SIZE);

        BigInteger[] inRange = {
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TEN,
            NINETEEN,

            BigInteger.valueOf(TWO_31 - 19L),
            BigInteger.valueOf(TWO_31 - 10L),
            BigInteger.valueOf(TWO_31 - 1L),
            BigInteger.valueOf(TWO_31),
            BigInteger.valueOf(TWO_31 + 1L),
            BigInteger.valueOf(TWO_31 + 10L),
            BigInteger.valueOf(TWO_31 + 19L),

            BigInteger.valueOf(TWO_32 - 19L),
            BigInteger.valueOf(TWO_32 - 10L),
            BigInteger.valueOf(TWO_32 - 1L),
            BigInteger.valueOf(TWO_32),
            BigInteger.valueOf(TWO_32 + 1L),
            BigInteger.valueOf(TWO_32 + 10L),
            BigInteger.valueOf(TWO_32 - 19L),

            BigInteger.valueOf(TWO_33 - 19L),
            BigInteger.valueOf(TWO_33 - 10L),
            BigInteger.valueOf(TWO_33 - 1L),
            BigInteger.valueOf(TWO_33),
            BigInteger.valueOf(TWO_33 + 1L),
            BigInteger.valueOf(TWO_33 + 10L),
            BigInteger.valueOf(TWO_33 + 19L),

            TWO_63.subtract(NINETEEN),
            TWO_63.subtract(BigInteger.TEN),
            TWO_63.subtract(BigInteger.ONE),
            TWO_63,
            TWO_63.add(BigInteger.ONE),
            TWO_63.add(BigInteger.TEN),
            TWO_63.add(NINETEEN),

            TWO_64.subtract(NINETEEN),
            TWO_64.subtract(BigInteger.TEN),
            TWO_64.subtract(BigInteger.ONE),
        };

        for(BigInteger dividend : inRange) {
            for(BigInteger divisor : inRange) {
                long quotient;
                BigInteger longQuotient;

                long remainder;
                BigInteger longRemainder;

                if (divisor.equals(BigInteger.ZERO)) {
                    try {
                        quotient = Long.divideUnsigned(dividend.longValue(), divisor.longValue());
                        errors++;
                    } catch(ArithmeticException ea) {
                        ; // Expected
                    }

                    try {
                        remainder = Long.remainderUnsigned(dividend.longValue(), divisor.longValue());
                        errors++;
                    } catch(ArithmeticException ea) {
                        ; // Expected
                    }
                } else {
                    quotient = Long.divideUnsigned(dividend.longValue(), divisor.longValue());
                    longQuotient = dividend.divide(divisor);

                    if (quotient != longQuotient.longValue()) {
                        errors++;
                        System.err.printf("Unexpected unsigned divide result %s on %s/%s%n",
                                          Long.toUnsignedString(quotient),
                                          Long.toUnsignedString(dividend.longValue()),
                                          Long.toUnsignedString(divisor.longValue()));
                    }

                    remainder = Long.remainderUnsigned(dividend.longValue(), divisor.longValue());
                    longRemainder = dividend.remainder(divisor);

                    if (remainder != longRemainder.longValue()) {
                        errors++;
                        System.err.printf("Unexpected unsigned remainder result %s on %s%%%s%n",
                                          Long.toUnsignedString(remainder),
                                          Long.toUnsignedString(dividend.longValue()),
                                          Long.toUnsignedString(divisor.longValue()));
                    }
                }
            }
        }

        return errors;
    }
}
