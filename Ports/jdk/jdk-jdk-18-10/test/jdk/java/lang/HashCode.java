/*
 * Copyright 2009 Google, Inc.  All Rights Reserved.
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
 * @bug 4245470 7088913
 * @summary Test the primitive wrappers hashCode()
 * @key randomness
 */

import java.util.Objects;
import java.util.Random;

public class HashCode {

    final Random rnd = new Random();

    void testOrdinals(String args[]) throws Exception {
        long[] longs = {
            Long.MIN_VALUE,
            Integer.MIN_VALUE,
            Short.MIN_VALUE,
            Character.MIN_VALUE,
            Byte.MIN_VALUE,
            -1, 0, 1,
            Byte.MAX_VALUE,
            Character.MAX_VALUE,
            Short.MAX_VALUE,
            Integer.MAX_VALUE,
            Long.MAX_VALUE,
            rnd.nextInt(),
        };

        for (long x : longs) {
            check(    new Long(x).hashCode() == (int)((long)x ^ (long)x>>>32));
            check(Long.valueOf(x).hashCode() == (int)((long)x ^ (long)x>>>32));
            check(  (new Long(x)).hashCode() == Long.hashCode(x));
            check(    new Integer((int)x).hashCode() == (int) x);
            check(Integer.valueOf((int)x).hashCode() == (int) x);
            check(  (new Integer((int)x)).hashCode() == Integer.hashCode((int)x));
            check(    new Short((short)x).hashCode() == (short) x);
            check(Short.valueOf((short)x).hashCode() == (short) x);
            check(         (new Short((short)x)).hashCode() == Short.hashCode((short)x));
            check(    new Character((char) x).hashCode() == (char) x);
            check(Character.valueOf((char) x).hashCode() == (char) x);
            check(         (new Character((char)x)).hashCode() == Character.hashCode((char)x));
            check(    new Byte((byte) x).hashCode() == (byte) x);
            check(Byte.valueOf((byte) x).hashCode() == (byte) x);
            check(         (new Byte((byte)x)).hashCode() == Byte.hashCode((byte)x));
        }
    }

    void testBoolean() {
        check( Boolean.FALSE.hashCode() == 1237);
        check( Boolean.TRUE.hashCode() == 1231);
        check( Boolean.valueOf(false).hashCode() == 1237);
        check( Boolean.valueOf(true).hashCode() == 1231);
        check( (new Boolean(false)).hashCode() == 1237);
        check( (new Boolean(true)).hashCode() == 1231);
        check( Boolean.hashCode(false) == 1237);
        check( Boolean.hashCode(true) == 1231);
    }

    void testFloat() {
        float[] floats = {
            Float.NaN,
            Float.NEGATIVE_INFINITY,
               -1f,
               0f,
               1f,
               Float.POSITIVE_INFINITY
        };

        for(float f : floats) {
            check( Float.hashCode(f) == Float.floatToIntBits(f));
            check( Float.valueOf(f).hashCode() == Float.floatToIntBits(f));
            check( (new Float(f)).hashCode() == Float.floatToIntBits(f));
        }
    }

    void testDouble() {
        double[] doubles = {
            Double.NaN,
            Double.NEGATIVE_INFINITY,
               -1f,
               0f,
               1f,
               Double.POSITIVE_INFINITY
        };

        for(double d : doubles) {
            long bits = Double.doubleToLongBits(d);
            int bitsHash = (int)(bits^(bits>>>32));
            check( Double.hashCode(d) == bitsHash);
            check( Double.valueOf(d).hashCode() == bitsHash);
            check( (new Double(d)).hashCode() == bitsHash);
        }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (Objects.equals(x,y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new HashCode().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try { testOrdinals(args);
              testBoolean();
                testFloat();
                testDouble();
        } catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
