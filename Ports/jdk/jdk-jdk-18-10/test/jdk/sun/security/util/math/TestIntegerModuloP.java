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

/*
 * @test
 * @bug 8181594 8208648
 * @summary Test proper operation of integer field arithmetic
 * @modules java.base/sun.security.util java.base/sun.security.util.math java.base/sun.security.util.math.intpoly
 * @build BigIntegerModuloP
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomial25519 32 0
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomial448 56 1
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomial1305 16 2
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomialP256 32 5
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomialP384 48 6
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.IntegerPolynomialP521 66 7
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.P256OrderField 32 8
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.P384OrderField 48 9
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.P521OrderField 66 10
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.Curve25519OrderField 32 11
 * @run main TestIntegerModuloP sun.security.util.math.intpoly.Curve448OrderField 56 12
 */

import sun.security.util.math.*;
import sun.security.util.math.intpoly.*;
import java.util.function.*;

import java.util.*;
import java.math.*;
import java.nio.*;

public class TestIntegerModuloP {

    static BigInteger TWO = BigInteger.valueOf(2);

    // The test has a list of functions, and it selects randomly from that list

    // The function types
    interface ElemFunction extends BiFunction
        <MutableIntegerModuloP, IntegerModuloP, IntegerModuloP> { }
    interface ElemArrayFunction extends BiFunction
        <MutableIntegerModuloP, IntegerModuloP, byte[]> { }
    interface TriConsumer <T, U, V> {
        void accept(T t, U u, V v);
    }
    interface ElemSetFunction extends TriConsumer
        <MutableIntegerModuloP, IntegerModuloP, byte[]> { }

    // The lists of functions. Multiple lists are needed because the test
    // respects the limitations of the arithmetic implementations.
    static final List<ElemFunction> ADD_FUNCTIONS = new ArrayList<>();
    static final List<ElemFunction> MULT_FUNCTIONS = new ArrayList<>();
    static final List<ElemArrayFunction> ARRAY_FUNCTIONS = new ArrayList<>();
    static final List<ElemSetFunction> SET_FUNCTIONS = new ArrayList<>();

    static void setUpFunctions(IntegerFieldModuloP field, int length) {

        ADD_FUNCTIONS.clear();
        MULT_FUNCTIONS.clear();
        SET_FUNCTIONS.clear();
        ARRAY_FUNCTIONS.clear();

        byte highByte = (byte)
            (field.getSize().bitLength() > length * 8 ? 1 : 0);

        // add functions are (im)mutable add/subtract
        ADD_FUNCTIONS.add(IntegerModuloP::add);
        ADD_FUNCTIONS.add(IntegerModuloP::subtract);
        ADD_FUNCTIONS.add(MutableIntegerModuloP::setSum);
        ADD_FUNCTIONS.add(MutableIntegerModuloP::setDifference);
        // also include functions that return the first/second argument
        ADD_FUNCTIONS.add((a, b) -> a);
        ADD_FUNCTIONS.add((a, b) -> b);

        // mult functions are (im)mutable multiply and square
        MULT_FUNCTIONS.add(IntegerModuloP::multiply);
        MULT_FUNCTIONS.add((a, b) -> a.square());
        MULT_FUNCTIONS.add((a, b) -> b.square());
        MULT_FUNCTIONS.add(MutableIntegerModuloP::setProduct);
        MULT_FUNCTIONS.add((a, b) -> a.setSquare());
        // also test multiplication by a small value
        MULT_FUNCTIONS.add((a, b) -> a.setProduct(b.getField().getSmallValue(
            b.asBigInteger().mod(BigInteger.valueOf(262144)).intValue())));

        // set functions are setValue with various argument types
        SET_FUNCTIONS.add((a, b, c) -> a.setValue(b));
        SET_FUNCTIONS.add((a, b, c) ->
            a.setValue(c, 0, c.length, (byte) 0));
        SET_FUNCTIONS.add((a, b, c) ->
            a.setValue(c, 0, c.length / 2, (byte) 0));
        SET_FUNCTIONS.add((a, b, c) ->
            a.setValue(ByteBuffer.wrap(c, 0, c.length / 2).order(ByteOrder.LITTLE_ENDIAN),
            c.length / 2, highByte));

        // array functions return the (possibly modified) value as byte array
        ARRAY_FUNCTIONS.add((a, b ) -> a.asByteArray(length));
        ARRAY_FUNCTIONS.add((a, b) -> a.addModPowerTwo(b, length));
    }

    public static void main(String[] args) {

        String className = args[0];
        final int length = Integer.parseInt(args[1]);
        int seed = Integer.parseInt(args[2]);

        Class<IntegerFieldModuloP> fieldBaseClass = IntegerFieldModuloP.class;
        try {
            Class<? extends IntegerFieldModuloP> clazz =
                Class.forName(className).asSubclass(fieldBaseClass);
            IntegerFieldModuloP field =
                clazz.getDeclaredConstructor().newInstance();

            setUpFunctions(field, length);

            runFieldTest(field, length, seed);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        System.out.println("All tests passed");
    }

    static void assertEqual(IntegerModuloP e1, IntegerModuloP e2) {

        if (!e1.asBigInteger().equals(e2.asBigInteger())) {
            throw new RuntimeException("values not equal: "
                + e1.asBigInteger() + " != " + e2.asBigInteger());
        }
    }

    // A class that holds pairs of actual/expected values, and allows
    // computation on these pairs.
    static class TestPair<T extends IntegerModuloP> {
        private final T test;
        private final T baseline;

        public TestPair(T test, T baseline) {
            this.test = test;
            this.baseline = baseline;
        }

        public T getTest() {
            return test;
        }
        public T getBaseline() {
            return baseline;
        }

        private void assertEqual() {
            TestIntegerModuloP.assertEqual(test, baseline);
        }

        public TestPair<MutableIntegerModuloP> mutable() {
            return new TestPair<>(test.mutable(), baseline.mutable());
        }

        public
        <R extends IntegerModuloP, X extends IntegerModuloP>
        TestPair<X> apply(BiFunction<T, R, X> func, TestPair<R> right) {
            X testResult = func.apply(test, right.test);
            X baselineResult = func.apply(baseline, right.baseline);
            return new TestPair(testResult, baselineResult);
        }

        public
        <U extends IntegerModuloP, V>
        void apply(TriConsumer<T, U, V> func, TestPair<U> right, V argV) {
            func.accept(test, right.test, argV);
            func.accept(baseline, right.baseline, argV);
        }

        public
        <R extends IntegerModuloP>
        void applyAndCheckArray(BiFunction<T, R, byte[]> func,
                                TestPair<R> right) {
            byte[] testResult = func.apply(test, right.test);
            byte[] baselineResult = func.apply(baseline, right.baseline);
            if (!Arrays.equals(testResult, baselineResult)) {
                throw new RuntimeException("Array values do not match: "
                    + HexFormat.of().withUpperCase().formatHex(testResult) + " != "
                    + HexFormat.of().withUpperCase().formatHex(baselineResult));
            }
        }

    }

    static TestPair<IntegerModuloP>
    applyAndCheck(ElemFunction func, TestPair<MutableIntegerModuloP> left,
                  TestPair<IntegerModuloP> right) {

        TestPair<IntegerModuloP> result = left.apply(func, right);
        result.assertEqual();
        left.assertEqual();
        right.assertEqual();

        return result;
    }

    static void
    setAndCheck(ElemSetFunction func, TestPair<MutableIntegerModuloP> left,
                TestPair<IntegerModuloP> right, byte[] argV) {

        left.apply(func, right, argV);
        left.assertEqual();
        right.assertEqual();
    }

    static TestPair<MutableIntegerModuloP>
    applyAndCheckMutable(ElemFunction func,
                         TestPair<MutableIntegerModuloP> left,
                         TestPair<IntegerModuloP> right) {

        TestPair<IntegerModuloP> result = applyAndCheck(func, left, right);

        TestPair<MutableIntegerModuloP> mutableResult = result.mutable();
        mutableResult.assertEqual();
        result.assertEqual();
        left.assertEqual();
        right.assertEqual();

        return mutableResult;
    }

    static void
    cswapAndCheck(int swap, TestPair<MutableIntegerModuloP> left,
                  TestPair<MutableIntegerModuloP> right) {

        left.getTest().conditionalSwapWith(right.getTest(), swap);
        left.getBaseline().conditionalSwapWith(right.getBaseline(), swap);

        left.assertEqual();
        right.assertEqual();

    }

    // Request arithmetic that should overflow, and ensure that overflow is
    // detected.
    static void runOverflowTest(TestPair<IntegerModuloP> elem) {

        TestPair<MutableIntegerModuloP> mutableElem = elem.mutable();

        try {
            for (int i = 0; i < 1000; i++) {
                applyAndCheck(MutableIntegerModuloP::setSum, mutableElem, elem);
            }
            applyAndCheck(MutableIntegerModuloP::setProduct, mutableElem, elem);
        } catch (ArithmeticException ex) {
            // this is expected
        }

        mutableElem = elem.mutable();
        try {
            for (int i = 0; i < 1000; i++) {
                elem = applyAndCheck(IntegerModuloP::add,
                    mutableElem, elem);
            }
            applyAndCheck(IntegerModuloP::multiply, mutableElem, elem);
        } catch (ArithmeticException ex) {
            // this is expected
        }
    }

    // Run a large number of random operations and ensure that
    // results are correct
    static void runOperationsTest(Random random, int length,
                                  TestPair<IntegerModuloP> elem,
                                  TestPair<IntegerModuloP> right) {

        TestPair<MutableIntegerModuloP> left = elem.mutable();

        for (int i = 0; i < 10000; i++) {

            ElemFunction addFunc1 =
                ADD_FUNCTIONS.get(random.nextInt(ADD_FUNCTIONS.size()));
            TestPair<MutableIntegerModuloP> result1 =
                applyAndCheckMutable(addFunc1, left, right);

            // left could have been modified, so turn it back into a summand
            applyAndCheckMutable((a, b) -> a.setSquare(), left, right);

            ElemFunction addFunc2 =
                ADD_FUNCTIONS.get(random.nextInt(ADD_FUNCTIONS.size()));
            TestPair<IntegerModuloP> result2 =
                applyAndCheck(addFunc2, left, right);

            if (elem.test.getField() instanceof IntegerPolynomial) {
                IntegerPolynomial field =
                    (IntegerPolynomial) elem.test.getField();
                int numAdds = field.getMaxAdds();
                for (int j = 1; j < numAdds; j++) {
                    ElemFunction addFunc3 = ADD_FUNCTIONS.
                        get(random.nextInt(ADD_FUNCTIONS.size()));
                    result2 = applyAndCheck(addFunc3, left, right);
                }
            }

            ElemFunction multFunc2 =
                MULT_FUNCTIONS.get(random.nextInt(MULT_FUNCTIONS.size()));
            TestPair<MutableIntegerModuloP> multResult =
                applyAndCheckMutable(multFunc2, result1, result2);

            int swap = random.nextInt(2);
            cswapAndCheck(swap, left, multResult);

            ElemSetFunction setFunc =
                SET_FUNCTIONS.get(random.nextInt(SET_FUNCTIONS.size()));
            byte[] valueArr = new byte[2 * length];
            random.nextBytes(valueArr);
            setAndCheck(setFunc, result1, result2, valueArr);

            // left could have been modified, so to turn it back into a summand
            applyAndCheckMutable((a, b) -> a.setSquare(), left, right);

            ElemArrayFunction arrayFunc =
                ARRAY_FUNCTIONS.get(random.nextInt(ARRAY_FUNCTIONS.size()));
            left.applyAndCheckArray(arrayFunc, right);
        }
    }

    // Run all the tests for a given field
    static void runFieldTest(IntegerFieldModuloP testField,
                             int length, int seed) {
        System.out.println("Testing: " + testField.getClass().getSimpleName());

        Random random = new Random(seed);

        IntegerFieldModuloP baselineField =
            new BigIntegerModuloP(testField.getSize());

        int numBits = testField.getSize().bitLength();
        BigInteger r =
            new BigInteger(numBits, random).mod(testField.getSize());
        TestPair<IntegerModuloP> rand =
            new TestPair(testField.getElement(r), baselineField.getElement(r));

        runOverflowTest(rand);

        // check combinations of operations for different kinds of elements
        List<TestPair<IntegerModuloP>> testElements = new ArrayList<>();
        testElements.add(rand);
        testElements.add(new TestPair(testField.get0(), baselineField.get0()));
        testElements.add(new TestPair(testField.get1(), baselineField.get1()));
        byte[] testArr = {121, 37, -100, -5, 76, 33};
        testElements.add(new TestPair(testField.getElement(testArr),
            baselineField.getElement(testArr)));

        testArr = new byte[length];
        random.nextBytes(testArr);
        testElements.add(new TestPair(testField.getElement(testArr),
            baselineField.getElement(testArr)));

        random.nextBytes(testArr);
        byte highByte = (byte) (numBits > length * 8 ? 1 : 0);
        testElements.add(
            new TestPair(
                testField.getElement(testArr, 0, testArr.length, highByte),
                baselineField.getElement(testArr, 0, testArr.length, highByte)
            )
        );

        for (int i = 0; i < testElements.size(); i++) {
            for (int j = 0; j < testElements.size(); j++) {
                runOperationsTest(random, length, testElements.get(i),
                    testElements.get(j));
            }
        }
    }
}

