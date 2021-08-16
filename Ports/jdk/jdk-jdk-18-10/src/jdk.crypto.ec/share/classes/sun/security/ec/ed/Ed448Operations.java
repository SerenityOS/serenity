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
package sun.security.ec.ed;

import sun.security.ec.point.*;
import sun.security.util.math.*;

import java.math.BigInteger;
import java.util.function.Function;

// Arithmetic works for a=1 and non-square d
/*
 * Elliptic curve point arithmetic, decoding, and other operations for the
 * family of curves including edwards448 and its related group. Though the
 * operations in this class are optimized for edwards448, they are correct
 * for any untwisted Edwards curve x^2 + y^2 = 1 + dx^2y^2 (mod p) where
 * d is not square (mod p).
 */
public class Ed448Operations extends EdECOperations {

    private final SmallValue two;
    private final ImmutableIntegerModuloP d;
    private final ProjectivePoint.Immutable basePoint;

    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private static final BigInteger FIVE = BigInteger.valueOf(5);
    private final BigInteger sizeMinus3;

    public Ed448Operations(ImmutableIntegerModuloP d, BigInteger baseX,
                           BigInteger baseY) {

        this.two = d.getField().getSmallValue(2);
        this.d = d;
        this.basePoint = of(new AffinePoint(
            d.getField().getElement(baseX),
            d.getField().getElement(baseY)
        ));

        this.sizeMinus3 = d.getField().getSize().subtract(THREE);
    }

    @Override
    public Point basePointMultiply(byte[] scalar) {
        return setProduct(basePoint.mutable(), scalar);
    }

    @Override
    protected ProjectivePoint.Immutable getNeutral() {
        IntegerFieldModuloP field = d.getField();
        return new ProjectivePoint.Immutable(field.get0(), field.get1(),
            field.get1());
    }

    @Override
    protected MutablePoint setSum(MutablePoint p1, MutablePoint p2,
                                  MutableIntegerModuloP t1,
                                  MutableIntegerModuloP t2,
                                  MutableIntegerModuloP t3) {

        ProjectivePoint.Mutable ehp1 = (ProjectivePoint.Mutable) p1;
        ProjectivePoint.Mutable ehp2 = (ProjectivePoint.Mutable) p2;
        return setSum(ehp1, ehp2, t1, t2, t3);
    }

    @Override
    protected MutablePoint setDouble(MutablePoint p, MutableIntegerModuloP t1,
                                     MutableIntegerModuloP t2) {

        ProjectivePoint.Mutable ehp = (ProjectivePoint.Mutable) p;
        return setDouble(ehp, t1, t2);
    }

    @Override
    public ProjectivePoint.Immutable of(AffinePoint p) {
        return new ProjectivePoint.Immutable(p.getX(), p.getY(),
            p.getX().getField().get1());
    }

    @Override
    public <T extends Throwable>
    AffinePoint decodeAffinePoint(Function<String, T> exception, int xLSB,
                                  IntegerModuloP y) throws T {

        ImmutableIntegerModuloP y2 = y.square();
        ImmutableIntegerModuloP u = y2.subtract(d.getField().get1());
        MutableIntegerModuloP v = d.mutable().setProduct(y2)
            .setDifference(d.getField().get1());

        IntegerModuloP u5v3pow = u.pow(FIVE).multiply(v.pow(THREE))
            .pow(sizeMinus3.shiftRight(2));

        MutableIntegerModuloP x = v.mutable().setProduct(u.pow(THREE))
            .setProduct(u5v3pow);

        v.setProduct(x).setProduct(x);
        // v now holds vx^2
        if (v.asBigInteger().equals(u.asBigInteger())) {
            // x is correct
        } else {
            throw exception.apply("Invalid point");
        }

        if (x.asBigInteger().equals(BigInteger.ZERO) && xLSB == 1) {
            throw exception.apply("Invalid point");
        }

        if (xLSB != x.asBigInteger().mod(TWO).intValue()) {
            x.setAdditiveInverse();
        }

        return new AffinePoint(x.fixed(), y.fixed());
    }

    ProjectivePoint.Mutable setSum(
            ProjectivePoint.Mutable p1,
            ProjectivePoint.Mutable p2,
            MutableIntegerModuloP t1,
            MutableIntegerModuloP t2,
            MutableIntegerModuloP t3) {

        t1.setValue(p1.getX()).setProduct(p2.getX());
        // t1 holds C
        t2.setValue(p2.getX()).setSum(p2.getY());
        p1.getX().setSum(p1.getY()).setProduct(t2);
        // x holds H
        p1.getZ().setProduct(p2.getZ());
        // z holds A
        p1.getY().setProduct(p2.getY());
        // y holds D

        t3.setValue(d).setProduct(t1).setProduct(p1.getY());
        // t3 holds E
        // do part of the final calculation of x and y to free up t1
        p1.getX().setDifference(t1).setReduced().setDifference(p1.getY());
        p1.getY().setDifference(t1);
        t1.setValue(p1.getZ()).setSquare();
        // t2 holds B

        t2.setValue(t1).setDifference(t3);
        // t2 holds F
        t1.setSum(t3);
        // t1 holds G

        p1.getX().setProduct(t2).setProduct(p1.getZ());
        p1.getY().setProduct(t1).setProduct(p1.getZ());
        p1.getZ().setValue(t2.multiply(t1));

        return p1;

    }

    protected ProjectivePoint.Mutable setDouble(ProjectivePoint.Mutable p,
                                                MutableIntegerModuloP t1,
                                                MutableIntegerModuloP t2) {

        t2.setValue(p.getX()).setSquare();
        // t2 holds C
        p.getX().setSum(p.getY()).setSquare();
        // x holds B
        p.getY().setSquare();
        // y holds D
        p.getZ().setSquare();
        // z holds H

        t1.setValue(t2).setSum(p.getY()).setReduced();
        // t1 holds E
        t2.setDifference(p.getY());
        p.getY().setValue(t1).setProduct(t2);

        p.getZ().setProduct(two);
        p.getZ().setAdditiveInverse().setSum(t1);
        // z holds J
        p.getX().setDifference(t1).setProduct(p.getZ());
        p.getZ().setProduct(t1);

        return p;
    }
}
