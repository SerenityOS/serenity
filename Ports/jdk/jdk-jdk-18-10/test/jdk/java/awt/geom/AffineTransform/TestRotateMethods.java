/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4980035
 * @summary Unit test for new methods:
 *
 *          AffineTransform.getRotateInstance(double x, double y);
 *          AffineTransform.setToRotation(double x, double y);
 *          AffineTransform.rotate(double x, double y);
 *
 *          AffineTransform.getQuadrantRotateInstance(int numquads);
 *          AffineTransform.setToQuadrantRotation(int numquads);
 *          AffineTransform.quadrantRotate(int numquads);
 *
 * @author flar
 * @run main TestRotateMethods
 */

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class TestRotateMethods {
    /* The maximum errors allowed, measured in double precision "ulps"
     * Note that for most fields, the tests are extremely accurate - to
     * within 3 ulps of the smaller value in the comparison
     * For the translation components, the tests are still very accurate,
     * but the absolute number of ulps can be noticeably higher when we
     * use one of the rotate methods that takes an anchor point.
     * Since a double precision value has 56 bits of precision, even
     * 1024 ulps is extremely small as a ratio of the value.
     */
    public static final double MAX_ULPS = 3.0;
    public static final double MAX_ANCHOR_TX_ULPS = 1024.0;
    public static double MAX_TX_ULPS = MAX_ULPS;

    // Vectors for quadrant rotations
    public static final double quadxvec[] = {  1.0,  0.0, -1.0,  0.0 };
    public static final double quadyvec[] = {  0.0,  1.0,  0.0, -1.0 };

    // Run tests once for each type of method:
    //     tx = AffineTransform.get<Rotate>Instance()
    //     tx.set<Rotate>()
    //     tx.<rotate>()
    public static enum Mode { GET, SET, MOD };

    // Used to accumulate and report largest differences encountered by tests
    public static double maxulps = 0.0;
    public static double maxtxulps = 0.0;

    // Sample anchor points for testing.
    public static Point2D zeropt = new Point2D.Double(0, 0);
    public static Point2D testtxpts[] = {
        new Point2D.Double(       5,      5),
        new Point2D.Double(      20,    -10),
        new Point2D.Double(-Math.PI, Math.E),
    };

    public static void main(String argv[]) {
        test(Mode.GET);
        test(Mode.SET);
        test(Mode.MOD);

        System.out.println("Max scale and shear difference: "+maxulps+" ulps");
        System.out.println("Max translate difference: "+maxtxulps+" ulps");
    }

    public static void test(Mode mode) {
        MAX_TX_ULPS = MAX_ULPS; // Stricter tx testing with no anchor point
        test(mode, 0.5, null);
        test(mode, 1.0, null);
        test(mode, 3.0, null);

        // Anchor points make the tx values less reliable
        MAX_TX_ULPS = MAX_ANCHOR_TX_ULPS;
        for (int i = 0; i < testtxpts.length; i++) {
            test(mode, 1.0, testtxpts[i]);
        }
        MAX_TX_ULPS = MAX_ULPS; // Restore to default
    }

    public static void verify(AffineTransform at1, AffineTransform at2,
                              Mode mode, double vectorscale, Point2D txpt,
                              String message, double num, String units)
    {
        if (!compare(at1, at2)) {
            System.out.println("mode == "+mode);
            System.out.println("vectorscale == "+vectorscale);
            System.out.println("txpt == "+txpt);
            System.out.println(at1+", type = "+at1.getType());
            System.out.println(at2+", type = "+at2.getType());
            System.out.println("ScaleX values differ by "+
                               ulps(at1.getScaleX(), at2.getScaleX())+" ulps");
            System.out.println("ScaleY values differ by "+
                               ulps(at1.getScaleY(), at2.getScaleY())+" ulps");
            System.out.println("ShearX values differ by "+
                               ulps(at1.getShearX(), at2.getShearX())+" ulps");
            System.out.println("ShearY values differ by "+
                               ulps(at1.getShearY(), at2.getShearY())+" ulps");
            System.out.println("TranslateX values differ by "+
                               ulps(at1.getTranslateX(),
                                    at2.getTranslateX())+" ulps");
            System.out.println("TranslateY values differ by "+
                               ulps(at1.getTranslateY(),
                                    at2.getTranslateY())+" ulps");
            throw new RuntimeException(message + num + units);
        }
    }

    public static void test(Mode mode, double vectorscale, Point2D txpt) {
        AffineTransform at1, at2, at3;

        for (int deg = -720; deg <= 720; deg++) {
            if ((deg % 90) == 0) continue;
            double radians = Math.toRadians(deg);
            double vecy = Math.sin(radians) * vectorscale;
            double vecx = Math.cos(radians) * vectorscale;

            at1 = makeAT(mode, txpt, radians);
            at2 = makeAT(mode, txpt, vecx, vecy);
            verify(at1, at2, mode, vectorscale, txpt,
                   "vector and radians do not match for ", deg, " degrees");

            if (txpt == null) {
                // Make sure output was same as a with a 0,0 anchor point
                if (vectorscale == 1.0) {
                    // Only need to test radians method for one scale factor
                    at3 = makeAT(mode, zeropt, radians);
                    verify(at1, at3, mode, vectorscale, zeropt,
                           "radians not invariant with 0,0 translate at ",
                           deg, " degrees");
                }
                // But test vector methods with all scale factors
                at3 = makeAT(mode, zeropt, vecx, vecy);
                verify(at2, at3, mode, vectorscale, zeropt,
                       "vector not invariant with 0,0 translate at ",
                       deg, " degrees");
            }
        }

        for (int quad = -8; quad <= 8; quad++) {
            double degrees = quad * 90.0;
            double radians = Math.toRadians(degrees);
            double vecx = quadxvec[quad & 3] * vectorscale;
            double vecy = quadyvec[quad & 3] * vectorscale;

            at1 = makeAT(mode, txpt, radians);
            at2 = makeAT(mode, txpt, vecx, vecy);
            verify(at1, at2, mode, vectorscale, txpt,
                   "quadrant vector and radians do not match for ",
                   degrees, " degrees");
            at2 = makeQuadAT(mode, txpt, quad);
            verify(at1, at2, mode, vectorscale, txpt,
                   "quadrant and radians do not match for ",
                   quad, " quadrants");
            if (txpt == null) {
                at3 = makeQuadAT(mode, zeropt, quad);
                verify(at2, at3, mode, vectorscale, zeropt,
                       "quadrant not invariant with 0,0 translate at ",
                       quad, " quadrants");
            }
        }
    }

    public static AffineTransform makeRandomAT() {
        AffineTransform at = new AffineTransform();
        at.scale(Math.random() * -10.0, Math.random() * 100.0);
        at.rotate(Math.random() * Math.PI);
        at.shear(Math.random(), Math.random());
        at.translate(Math.random() * 300.0, Math.random() * -20.0);
        return at;
    }

    public static AffineTransform makeAT(Mode mode, Point2D txpt,
                                         double radians)
    {
        AffineTransform at;
        double tx = (txpt == null) ? 0.0 : txpt.getX();
        double ty = (txpt == null) ? 0.0 : txpt.getY();
        switch (mode) {
        case GET:
            if (txpt != null) {
                at = AffineTransform.getRotateInstance(radians, tx, ty);
            } else {
                at = AffineTransform.getRotateInstance(radians);
            }
            break;
        case SET:
            at = makeRandomAT();
            if (txpt != null) {
                at.setToRotation(radians, tx, ty);
            } else {
                at.setToRotation(radians);
            }
            break;
        case MOD:
            at = makeRandomAT();
            at.setToIdentity();
            if (txpt != null) {
                at.rotate(radians, tx, ty);
            } else {
                at.rotate(radians);
            }
            break;
        default:
            throw new InternalError("unrecognized mode: "+mode);
        }

        return at;
    }

    public static AffineTransform makeAT(Mode mode, Point2D txpt,
                                         double vx, double vy)
    {
        AffineTransform at;
        double tx = (txpt == null) ? 0.0 : txpt.getX();
        double ty = (txpt == null) ? 0.0 : txpt.getY();
        switch (mode) {
        case GET:
            if (txpt != null) {
                at = AffineTransform.getRotateInstance(vx, vy, tx, ty);
            } else {
                at = AffineTransform.getRotateInstance(vx, vy);
            }
            break;
        case SET:
            at = makeRandomAT();
            if (txpt != null) {
                at.setToRotation(vx, vy, tx, ty);
            } else {
                at.setToRotation(vx, vy);
            }
            break;
        case MOD:
            at = makeRandomAT();
            at.setToIdentity();
            if (txpt != null) {
                at.rotate(vx, vy, tx, ty);
            } else {
                at.rotate(vx, vy);
            }
            break;
        default:
            throw new InternalError("unrecognized mode: "+mode);
        }

        return at;
    }

    public static AffineTransform makeQuadAT(Mode mode, Point2D txpt,
                                             int quads)
    {
        AffineTransform at;
        double tx = (txpt == null) ? 0.0 : txpt.getX();
        double ty = (txpt == null) ? 0.0 : txpt.getY();
        switch (mode) {
        case GET:
            if (txpt != null) {
                at = AffineTransform.getQuadrantRotateInstance(quads, tx, ty);
            } else {
                at = AffineTransform.getQuadrantRotateInstance(quads);
            }
            break;
        case SET:
            at = makeRandomAT();
            if (txpt != null) {
                at.setToQuadrantRotation(quads, tx, ty);
            } else {
                at.setToQuadrantRotation(quads);
            }
            break;
        case MOD:
            at = makeRandomAT();
            at.setToIdentity();
            if (txpt != null) {
                at.quadrantRotate(quads, tx, ty);
            } else {
                at.quadrantRotate(quads);
            }
            break;
        default:
            throw new InternalError("unrecognized mode: "+mode);
        }

        return at;
    }

    public static boolean compare(AffineTransform at1, AffineTransform at2) {
        maxulps = Math.max(maxulps, ulps(at1.getScaleX(), at2.getScaleX()));
        maxulps = Math.max(maxulps, ulps(at1.getScaleY(), at2.getScaleY()));
        maxulps = Math.max(maxulps, ulps(at1.getShearX(), at2.getShearX()));
        maxulps = Math.max(maxulps, ulps(at1.getShearY(), at2.getShearY()));
        maxtxulps = Math.max(maxtxulps,
                             ulps(at1.getTranslateX(), at2.getTranslateX()));
        maxtxulps = Math.max(maxtxulps,
                             ulps(at1.getTranslateY(), at2.getTranslateY()));
        return (getModifiedType(at1) == getModifiedType(at2) &&
                (compare(at1.getScaleX(), at2.getScaleX(), MAX_ULPS)) &&
                (compare(at1.getScaleY(), at2.getScaleY(), MAX_ULPS)) &&
                (compare(at1.getShearX(), at2.getShearX(), MAX_ULPS)) &&
                (compare(at1.getShearY(), at2.getShearY(), MAX_ULPS)) &&
                (compare(at1.getTranslateX(),
                         at2.getTranslateX(), MAX_TX_ULPS)) &&
                (compare(at1.getTranslateY(),
                         at2.getTranslateY(), MAX_TX_ULPS)));
    }

    public static int getModifiedType(AffineTransform at) {
        int type = at.getType();
        // Some of the vector methods can introduce a tiny uniform scale
        // at some angles...
        if ((type & AffineTransform.TYPE_UNIFORM_SCALE) != 0) {
            maxulps = Math.max(maxulps, ulps(at.getDeterminant(), 1.0));
            if (ulps(at.getDeterminant(), 1.0) <= MAX_ULPS) {
                // Really tiny - we will ignore it
                type &= (~AffineTransform.TYPE_UNIFORM_SCALE);
            }
        }
        return type;
    }

    public static boolean compare(double val1, double val2, double maxulps) {
        return (ulps(val1, val2) <= maxulps);
    }

    public static double ulps(double val1, double val2) {
        double diff = Math.abs(val1 - val2);
        double ulpmax = Math.min(Math.ulp(val1), Math.ulp(val2));
        return (diff / ulpmax);
    }
}
