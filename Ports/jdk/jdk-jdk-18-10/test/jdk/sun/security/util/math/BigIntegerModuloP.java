/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import sun.security.util.math.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

/**
 * Arithmetic in the field of integers modulo a prime value implemented using
 * BigInteger. This implementation is very versatile, but it is slow and none
 * of the operations are value-independent. This class is intended for use in
 * testing and prototyping, and production code should probably use a more
 * specialized arithmetic implementation.
 */

public class BigIntegerModuloP implements IntegerFieldModuloP {

    private final BigInteger p;

    public BigIntegerModuloP(BigInteger p) {
        this.p = p;
    }

    @Override
    public BigInteger getSize() {
        return p;
    }

    @Override
    public ImmutableElement get0() {
        return new ImmutableElement(BigInteger.ZERO);
    }
    @Override
    public ImmutableElement get1() {
        return new ImmutableElement(BigInteger.ONE);
    }
    @Override
    public ImmutableElement getElement(BigInteger v) {
        return new ImmutableElement(v);
    }
    @Override
    public ImmutableElement getElement(byte[] v, int offset, int length,
                                       byte highByte) {
        byte[] bigIntIn = new byte[length + 1];
        System.arraycopy(v, offset, bigIntIn, 0, length);
        bigIntIn[length] = highByte;
        reverse(bigIntIn);
        return new ImmutableElement(new BigInteger(1, bigIntIn).mod(getSize()));
    }
    @Override
    public SmallValue getSmallValue(int i) {
        return new SmallElement(i);
    }

    private abstract class Element implements IntegerModuloP {

        protected BigInteger v;

        protected Element(BigInteger v) {
            this.v = v;
        }

        protected Element(boolean v) {
            this.v = BigInteger.valueOf(v ? 1 : 0);
        }

        private BigInteger getModulus() {
            return getField().getSize();
        }

        @Override
        public IntegerFieldModuloP getField() {
            return BigIntegerModuloP.this;
        }

        @Override
        public BigInteger asBigInteger() {
            return v;
        }

        @Override
        public MutableElement mutable() {
            return new MutableElement(v);
        }

        @Override
        public ImmutableElement fixed() {
            return new ImmutableElement(v);
        }

        @Override
        public ImmutableElement add(IntegerModuloP b) {
            return new ImmutableElement(
                v.add(b.asBigInteger()).mod(getModulus()));
        }

        @Override
        public ImmutableElement additiveInverse() {
            return new ImmutableElement(v.negate().mod(getModulus()));
        }

        @Override
        public ImmutableElement multiply(IntegerModuloP b) {
            return new ImmutableElement(
                v.multiply(b.asBigInteger()).mod(getModulus()));
        }

        @Override
        public void addModPowerTwo(IntegerModuloP arg, byte[] result) {
            BigInteger biThis = asBigInteger();
            BigInteger biArg = arg.asBigInteger();
            bigIntAsByteArray(biThis.add(biArg), result);
        }

        private void bigIntAsByteArray(BigInteger arg, byte[] result) {
            byte[] bytes = arg.toByteArray();
            // bytes is backwards and possibly too big
            // Copy the low-order bytes into result in reverse
            int sourceIndex = bytes.length - 1;
            for (int i = 0; i < result.length; i++) {
                if (sourceIndex >= 0) {
                    result[i] = bytes[sourceIndex--];
                } else {
                    result[i] = 0;
                }
            }
        }

        @Override
        public void asByteArray(byte[] result) {
            bigIntAsByteArray(v, result);
        }
    }

    private class ImmutableElement extends Element
        implements ImmutableIntegerModuloP {

        private ImmutableElement(BigInteger v) {
            super(v);
        }
    }

    private class MutableElement extends Element
        implements MutableIntegerModuloP {

        private MutableElement(BigInteger v) {
            super(v);
        }

        @Override
        public void conditionalSet(IntegerModuloP b, int set) {
            if (set == 1) {
                v = b.asBigInteger();
            }
        }

        @Override
        public void conditionalSwapWith(MutableIntegerModuloP b, int swap) {
            if (swap == 1) {
                BigInteger temp = v;
                v = b.asBigInteger();
                ((Element) b).v = temp;
            }
        }

        @Override
        public MutableElement setValue(IntegerModuloP v) {
            this.v = ((Element) v).v;

            return this;
        }

        @Override
        public MutableElement setValue(byte[] arr, int offset, int length,
                                       byte highByte) {
            byte[] bigIntIn = new byte[length + 1];
            System.arraycopy(arr, offset, bigIntIn, 0, length);
            bigIntIn[length] = highByte;
            reverse(bigIntIn);
            v = new BigInteger(bigIntIn).mod(getSize());

            return this;
        }

        @Override
        public MutableElement setValue(ByteBuffer buf, int length,
                                       byte highByte) {
            byte[] bigIntIn = new byte[length + 1];
            buf.get(bigIntIn, 0, length);
            bigIntIn[length] = highByte;
            reverse(bigIntIn);
            v = new BigInteger(bigIntIn).mod(getSize());

            return this;
        }

        @Override
        public MutableElement setSquare() {
            v = v.multiply(v).mod(getSize());
            return this;
        }

        @Override
        public MutableElement setProduct(IntegerModuloP b) {
            Element other = (Element) b;
            v = v.multiply(other.v).mod(getSize());
            return this;
        }

        @Override
        public MutableElement setProduct(SmallValue value) {
            BigInteger bigIntValue = ((SmallElement) value).asBigInteger();
            v = v.multiply(bigIntValue).mod(getSize());
            return this;
        }

        @Override
        public MutableElement setSum(IntegerModuloP b) {
            Element other = (Element) b;
            v = v.add(other.v).mod(getSize());
            return this;
        }

        @Override
        public MutableElement setDifference(IntegerModuloP b) {
            Element other = (Element) b;
            v = v.subtract(other.v).mod(getSize());
            return this;
        }

        @Override
        public MutableElement setAdditiveInverse() {
            v = BigInteger.ZERO.subtract(v);
            return this;
        }

        @Override
        public MutableElement setReduced() {
            // do nothing
            return this;
        }

    }

    private class SmallElement extends ImmutableElement implements SmallValue {

        public SmallElement(int v) {
            super(BigInteger.valueOf(v).mod(getSize()));
        }
    }

    private static void swap(byte[] arr, int i, int j) {
        byte tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    private static void reverse(byte [] arr) {
        int i = 0;
        int j = arr.length - 1;

        while (i < j) {
            swap(arr, i, j);
            i++;
            j--;
        }
    }

}
