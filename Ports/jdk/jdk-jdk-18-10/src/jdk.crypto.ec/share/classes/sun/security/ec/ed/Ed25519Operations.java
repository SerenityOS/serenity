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

/*
 * Elliptic curve point arithmetic, decoding, and other operations for the
 * family of curves including edwards25519 and its related group. Though the
 * operations in this class are optimized for edwards25519, they are correct
 * for any twisted Edwards curve ax^2 + y^2 = 1 + dx^2y^2 (mod p) with the
 * following properties:
 *   1) a = -1 (mod p)
 *   2) a is square (mod p)
 *   3) d is not square (mod p)
 */
public class Ed25519Operations extends EdECOperations {

    private final SmallValue two;
    private final ImmutableIntegerModuloP d;
    private final ExtendedHomogeneousPoint.Immutable basePoint;

    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger SEVEN = BigInteger.valueOf(7);
    private final BigInteger sizeMinus5;

    public Ed25519Operations(ImmutableIntegerModuloP d, BigInteger baseX,
                             BigInteger baseY) {

        this.two = d.getField().getSmallValue(2);
        this.d = d;
        this.basePoint = of(new AffinePoint(
            d.getField().getElement(baseX), d.getField().getElement(baseY)
        ));
        this.sizeMinus5 =
            d.getField().getSize().subtract(BigInteger.valueOf(5));
    }

    @Override
    public Point basePointMultiply(byte[] scalar) {
        return setProduct(basePoint.mutable(), scalar);
    }

    @Override
    protected ExtendedHomogeneousPoint.Immutable getNeutral() {
        IntegerFieldModuloP field = d.getField();
        return new ExtendedHomogeneousPoint.Immutable(field.get0(),
            field.get1(), field.get0(), field.get1());
    }

    @Override
    protected MutablePoint setSum(MutablePoint p1, MutablePoint p2,
                                  MutableIntegerModuloP t1,
                                  MutableIntegerModuloP t2,
                                  MutableIntegerModuloP t3) {

        ExtendedHomogeneousPoint.Mutable ehp1 =
            (ExtendedHomogeneousPoint.Mutable) p1;
        ExtendedHomogeneousPoint.Mutable ehp2 =
            (ExtendedHomogeneousPoint.Mutable) p2;
        return setSum(ehp1, ehp2, t1, t2, t3);
    }

    @Override
    protected MutablePoint setDouble(MutablePoint p, MutableIntegerModuloP t1,
        MutableIntegerModuloP t2) {

        ExtendedHomogeneousPoint.Mutable ehp =
            (ExtendedHomogeneousPoint.Mutable) p;
        return setDouble(ehp, t1, t2);
    }

    @Override
    public ExtendedHomogeneousPoint.Immutable of(AffinePoint p) {
        return new ExtendedHomogeneousPoint.Immutable(p.getX(), p.getY(),
            p.getX().multiply(p.getY()), p.getX().getField().get1());
    }

    @Override
    public <T extends Throwable>
    AffinePoint decodeAffinePoint(Function<String, T> exception,
                                  int xLSB, IntegerModuloP y) throws T {

        IntegerFieldModuloP field = d.getField();
        BigInteger p = field.getSize();
        ImmutableIntegerModuloP y2 = y.square();
        ImmutableIntegerModuloP u = y2.subtract(field.get1());
        MutableIntegerModuloP v = d.mutable().setProduct(y2)
            .setSum(field.get1());

        MutableIntegerModuloP x =
            u.mutable().setProduct(v.pow(BigInteger.valueOf(3)));
        ImmutableIntegerModuloP uv7pow =
            u.multiply(v.pow(SEVEN)).pow(sizeMinus5.shiftRight(3));
        x.setProduct(uv7pow);

        v.setProduct(x).setProduct(x);
        // v now holds vx^2
        BigInteger bigVX2 = v.asBigInteger();
        if (bigVX2.equals(u.asBigInteger())) {
            // do nothing---x is correct
        } else if (bigVX2.equals(u.additiveInverse().asBigInteger())) {
            BigInteger exp = p.subtract(BigInteger.ONE).shiftRight(2);
            IntegerModuloP twoPow = field.getElement(TWO.modPow(exp, p));
            x.setProduct(twoPow);
        } else {
            throw exception.apply("Invalid point");
        }

        if (x.asBigInteger().equals(BigInteger.ZERO) && xLSB == 1) {
            throw exception.apply("Invalid point");
        }

        if (xLSB != x.asBigInteger().mod(BigInteger.valueOf(2)).intValue()) {
            x.setAdditiveInverse();
        }

        return new AffinePoint(x.fixed(), y.fixed());
    }

    ExtendedHomogeneousPoint.Mutable setSum(
            ExtendedHomogeneousPoint.Mutable p1,
            ExtendedHomogeneousPoint.Mutable p2,
            MutableIntegerModuloP t1,
            MutableIntegerModuloP t2,
            MutableIntegerModuloP t3) {

        t1.setValue(p2.getY()).setDifference(p2.getX());
        // t1 holds y2 - x2
        t2.setValue(p1.getY()).setDifference(p1.getX()).setProduct(t1);
        // t2 holds A = (y1 - x1) * (y2 - x2)
        t1.setValue(p2.getY()).setSum(p2.getX());
        // t1 holds y2 + x2
        t3.setValue(p1.getY()).setSum(p1.getX()).setProduct(t1);
        // t3 holds B = (y1 + x1) * (y2 + x2)
        p1.getX().setValue(t3).setDifference(t2);
        // x holds E = B - A
        t3.setSum(t2);
        // t3 holds H = B + A, t2 is unused
        t2.setValue(d).setSum(d).setProduct(p1.getT()).setProduct(p2.getT());
        // t2 holds C
        t1.setValue(p1.getZ()).setProduct(p2.getZ()).setProduct(two);
        // t1 holds D
        p1.getY().setValue(t1).setSum(t2);
        // y holds G
        p1.getZ().setValue(t1).setDifference(t2);
        // z holds F

        p1.getT().setValue(p1.getX()).setProduct(t3);
        p1.getX().setProduct(p1.getZ());
        p1.getZ().setProduct(p1.getY());
        p1.getY().setProduct(t3);

        return p1;

    }

    protected ExtendedHomogeneousPoint.Mutable setDouble(
        ExtendedHomogeneousPoint.Mutable p,
        MutableIntegerModuloP t1, MutableIntegerModuloP t2) {

        t1.setValue(p.getX()).setSum(p.getY()).setSquare();
        // t1 holds (x + y)^2
        p.getX().setSquare();
        // x = A = x^2
        p.getY().setSquare();
        // y = B = y^2
        t2.setValue(p.getX()).setSum(p.getY()).setReduced();
        // t2 holds H
        p.getZ().setSquare().setProduct(two);
        // z holds C

        p.getT().setValue(t2).setDifference(t1);
        // t holds E
        t1.setValue(p.getX()).setDifference(p.getY()).setReduced();
        // t1 holds G

        p.getZ().setSum(t1);
        // z holds F

        p.getX().setValue(p.getT()).setProduct(p.getZ());
        p.getY().setValue(t1).setProduct(t2);
        p.getT().setProduct(t2);
        p.getZ().setProduct(t1);

        return p;
    }
}
