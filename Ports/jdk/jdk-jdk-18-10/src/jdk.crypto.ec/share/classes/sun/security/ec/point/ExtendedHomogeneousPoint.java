/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.ec.point;

import sun.security.util.math.*;

import java.math.BigInteger;

/**
 * Elliptic curve point in extended homogeneous coordinates (X, Y, T, Z) where
 * an affine point (x, y) is represented using any (X, Y, T, Z) s.t.
 * x = X/Z, y = Y/Z, and x*y = T/Z.
 */
public abstract class ExtendedHomogeneousPoint
    <T extends IntegerModuloP> implements Point {

    protected final T x;
    protected final T y;
    protected final T t;
    protected final T z;

    protected ExtendedHomogeneousPoint(T x, T y, T t, T z) {

        this.x = x;
        this.y = y;
        this.t = t;
        this.z = z;
    }

    @Override
    public IntegerFieldModuloP getField() {
        return this.x.getField();
    }
    @Override
    public Immutable fixed() {
        return new Immutable(x.fixed(), y.fixed(), t.fixed(), z.fixed());
    }

    @Override
    public Mutable mutable() {
        return new Mutable(x.mutable(), y.mutable(), t.mutable(), z.mutable());
    }

    public T getX() {
        return x;
    }

    public T getY() {
        return y;
    }

    public T getT() {
        return t;
    }

    public T getZ() {
        return z;
    }

    public AffinePoint asAffine() {
        IntegerModuloP zInv = z.multiplicativeInverse();
        return new AffinePoint(x.multiply(zInv), y.multiply(zInv));
    }

    private static
    <T1 extends IntegerModuloP, T2 extends IntegerModuloP>
    boolean affineEquals(ExtendedHomogeneousPoint<T1> p1,
                         ExtendedHomogeneousPoint<T2> p2) {
       MutableIntegerModuloP x1 = p1.getX().mutable().setProduct(p2.getZ());
       MutableIntegerModuloP x2 = p2.getX().mutable().setProduct(p1.getZ());
       if (!x1.asBigInteger().equals(x2.asBigInteger())) {
           return false;
       }

        MutableIntegerModuloP y1 = p1.getY().mutable().setProduct(p2.getZ());
        MutableIntegerModuloP y2 = p2.getY().mutable().setProduct(p1.getZ());
        if (!y1.asBigInteger().equals(y2.asBigInteger())) {
            return false;
        }

        return true;
    }

    public boolean affineEquals(Point p) {
        if (p instanceof ExtendedHomogeneousPoint) {
            @SuppressWarnings("unchecked")
            ExtendedHomogeneousPoint<IntegerModuloP> ehp =
                (ExtendedHomogeneousPoint<IntegerModuloP>) p;
            return affineEquals(this, ehp);
        }

        return asAffine().equals(p.asAffine());
    }

    public static class Immutable
        extends ExtendedHomogeneousPoint<ImmutableIntegerModuloP>
        implements ImmutablePoint {

        public Immutable(ImmutableIntegerModuloP x,
                         ImmutableIntegerModuloP y,
                         ImmutableIntegerModuloP t,
                         ImmutableIntegerModuloP z) {
            super(x, y, t, z);
        }
    }

    public static class Mutable
        extends ExtendedHomogeneousPoint<MutableIntegerModuloP>
        implements MutablePoint {

        public Mutable(MutableIntegerModuloP x,
                       MutableIntegerModuloP y,
                       MutableIntegerModuloP t,
                       MutableIntegerModuloP z) {
            super(x, y, t, z);
        }

        @Override
        public Mutable conditionalSet(Point p, int set) {
            if (!(p instanceof ExtendedHomogeneousPoint)) {
                throw new RuntimeException("Incompatible point");
            }
            @SuppressWarnings("unchecked")
            ExtendedHomogeneousPoint<IntegerModuloP> ehp =
                (ExtendedHomogeneousPoint<IntegerModuloP>) p;
            return conditionalSet(ehp, set);
        }

        private <T extends IntegerModuloP>
        Mutable conditionalSet(ExtendedHomogeneousPoint<T> ehp, int set) {

            x.conditionalSet(ehp.x, set);
            y.conditionalSet(ehp.y, set);
            t.conditionalSet(ehp.t, set);
            z.conditionalSet(ehp.z, set);

            return this;
        }

        @Override
        public Mutable setValue(AffinePoint p) {
            x.setValue(p.getX());
            y.setValue(p.getY());
            t.setValue(p.getX()).setProduct(p.getY());
            z.setValue(p.getX().getField().get1());

            return this;
        }

        @Override
        public Mutable setValue(Point p) {

            @SuppressWarnings("unchecked")
            ExtendedHomogeneousPoint<IntegerModuloP> ehp =
                (ExtendedHomogeneousPoint<IntegerModuloP>) p;

            return setValue(ehp);
        }

        private <T extends IntegerModuloP>
        Mutable setValue(ExtendedHomogeneousPoint<T> ehp) {

            x.setValue(ehp.x);
            y.setValue(ehp.y);
            t.setValue(ehp.t);
            z.setValue(ehp.z);

            return this;
        }
    }

}
