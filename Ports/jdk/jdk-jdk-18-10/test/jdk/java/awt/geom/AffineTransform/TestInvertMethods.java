/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4987374 8062163
 * @summary Unit test for inversion methods:
 *
 *          AffineTransform.createInverse();
 *          AffineTransform.invert();
 *
 * @author flar
 * @run main TestInvertMethods
 */

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/*
 * Instances of the inner class Tester are "nodes" which take an input
 * AffineTransform (AT), modify it in some way and pass the modified
 * AT onto another Tester node.
 *
 * There is one particular Tester node of note called theVerifier.
 * This is a leaf node which takes the input AT and tests the various
 * inversion methods on that matrix.
 *
 * Most of the other Tester nodes will perform a single affine operation
 * on their input, such as a rotate by various angles, or a scale by
 * various predefined scale  values, and then pass the modified AT on
 * to the next node in the chain which may be a verifier or another
 * modifier.
 *
 * The Tester instances can also be chained together using the chain
 * method so that we can test not only matrices modified by some single
 * affine operation (scale, rotate, etc.) but also composite matrices
 * that represent multiple operations concatenated together.
 */
public class TestInvertMethods {
    public static boolean verbose;

    public static final double MAX_ULPS = 2.0;
    public static double MAX_TX_ULPS = MAX_ULPS;
    public static double maxulps = 0.0;
    public static double maxtxulps = 0.0;
    public static int numtests = 0;

    public static void main(String argv[]) {
        Tester rotate = new Tester.Rotate();
        Tester scale = new Tester.Scale();
        Tester shear = new Tester.Shear();
        Tester translate = new Tester.Translate();

        if (argv.length > 1) {
            // This next line verifies that chaining works correctly...
            scale.chain(translate.chain(new Tester.Debug())).test(false);
            return;
        }

        verbose = (argv.length > 0);

        new Tester.Identity().test(true);
        translate.test(true);
        scale.test(true);
        rotate.test(true);
        shear.test(true);
        scale.chain(translate).test(true);
        rotate.chain(translate).test(true);
        shear.chain(translate).test(true);
        translate.chain(scale).test(true);
        translate.chain(rotate).test(true);
        translate.chain(shear).test(true);
        translate.chain(scale.chain(rotate.chain(shear))).test(false);
        shear.chain(rotate.chain(scale.chain(translate))).test(false);

        System.out.println(numtests+" tests performed");
        System.out.println("Max scale and shear difference: "+maxulps+" ulps");
        System.out.println("Max translate difference: "+maxtxulps+" ulps");
    }

    public abstract static class Tester {
        public static AffineTransform IdentityTx = new AffineTransform();

        /*
         * This is the leaf node that performs inversion testing
         * on the incoming AffineTransform.
         */
        public static final Tester theVerifier = new Tester() {
            public void test(AffineTransform at, boolean full) {
                numtests++;
                AffineTransform inv1, inv2;
                boolean isinvertible =
                    (Math.abs(at.getDeterminant()) >= Double.MIN_VALUE);
                try {
                    inv1 = at.createInverse();
                    if (!isinvertible) missingNTE("createInverse", at);
                } catch (NoninvertibleTransformException e) {
                    inv1 = null;
                    if (isinvertible) extraNTE("createInverse", at);
                }
                inv2 = new AffineTransform(at);
                try {
                    inv2.invert();
                    if (!isinvertible) missingNTE("invert", at);
                } catch (NoninvertibleTransformException e) {
                    if (isinvertible) extraNTE("invert", at);
                }
                if (verbose) System.out.println("at = "+at);
                if (isinvertible) {
                    if (verbose) System.out.println(" inv1 = "+inv1);
                    if (verbose) System.out.println(" inv2 = "+inv2);
                    if (!inv1.equals(inv2)) {
                        report(at, inv1, inv2,
                               "invert methods do not agree");
                    }
                    inv1.concatenate(at);
                    inv2.concatenate(at);
                    // "Fix" some values that don't always behave
                    // well with all the math that we've done up
                    // to this point.
                    // See the note on the concatfix method below.
                    concatfix(inv1);
                    concatfix(inv2);
                    if (verbose) System.out.println("  at*inv1 = "+inv1);
                    if (verbose) System.out.println("  at*inv2 = "+inv2);
                    if (!compare(inv1, IdentityTx)) {
                        report(at, inv1, IdentityTx,
                               "createInverse() check failed");
                    }
                    if (!compare(inv2, IdentityTx)) {
                        report(at, inv2, IdentityTx,
                               "invert() check failed");
                    }
                } else {
                    if (verbose) System.out.println(" is not invertible");
                }
                if (verbose) System.out.println();
            }

            void missingNTE(String methodname, AffineTransform at) {
                throw new RuntimeException("Noninvertible was not "+
                                           "thrown from "+methodname+
                                           " for: "+at);
            }

            void extraNTE(String methodname, AffineTransform at) {
                throw new RuntimeException("Unexpected Noninvertible "+
                                           "thrown from "+methodname+
                                           " for: "+at);
            }
        };

        /*
         * The inversion math may work out fairly exactly, but when
         * we concatenate the inversions back with the original matrix
         * in an attempt to restore them to the identity matrix,
         * then we can end up compounding errors to a fairly high
         * level, particularly if the component values had mantissas
         * that were repeating fractions.  This function therefore
         * "fixes" the results of concatenating the inversions back
         * with their original matrices to get rid of small variations
         * in the values that should have ended up being 0.0.
         */
        public void concatfix(AffineTransform at) {
            double m00 = at.getScaleX();
            double m10 = at.getShearY();
            double m01 = at.getShearX();
            double m11 = at.getScaleY();
            double m02 = at.getTranslateX();
            double m12 = at.getTranslateY();
            if (Math.abs(m00-1.0) < 1E-10) m00 = 1.0;
            if (Math.abs(m11-1.0) < 1E-10) m11 = 1.0;
            if (Math.abs(m02) < 1E-10) m02 = 0.0;
            if (Math.abs(m12) < 1E-10) m12 = 0.0;
            if (Math.abs(m01) < 1E-15) m01 = 0.0;
            if (Math.abs(m10) < 1E-15) m10 = 0.0;
            at.setTransform(m00, m10,
                            m01, m11,
                            m02, m12);
        }

        public void test(boolean full) {
            test(IdentityTx, full);
        }

        public void test(AffineTransform init, boolean full) {
            test(init, theVerifier, full);
        }

        public void test(AffineTransform init, Tester next, boolean full) {
            next.test(init, full);
        }

        public Tester chain(Tester next) {
            return new Chain(this, next);
        }

        /*
         * Utility node used to chain together two other nodes for
         * implementing the "chain" method.
         */
        public static class Chain extends Tester {
            Tester prev;
            Tester next;

            public Chain(Tester prev, Tester next) {
                this.prev = prev;
                this.next = next;
            }

            public void test(AffineTransform init, boolean full) {
                prev.test(init, next, full);
            }

            public Tester chain(Tester next) {
                this.next = this.next.chain(next);
                return this;
            }
        }

        /*
         * Utility node for testing.
         */
        public static class Fail extends Tester {
            public void test(AffineTransform init, Tester next, boolean full) {
                throw new RuntimeException("Debug: Forcing failure");
            }
        }

        /*
         * Utility node for testing that chaining works.
         */
        public static class Debug extends Tester {
            public void test(AffineTransform init, Tester next, boolean full) {
                new Throwable().printStackTrace();
                next.test(init, full);
            }
        }

        /*
         * NOP node.
         */
        public static class Identity extends Tester {
            public void test(AffineTransform init, Tester next, boolean full) {
                if (verbose) System.out.println("*Identity = "+init);
                next.test(init, full);
            }
        }

        /*
         * Affine rotation node.
         */
        public static class Rotate extends Tester {
            public void test(AffineTransform init, Tester next, boolean full) {
                int inc = full ? 10 : 45;
                for (int i = -720; i <= 720; i += inc) {
                    AffineTransform at2 = new AffineTransform(init);
                    at2.rotate(i / 180.0 * Math.PI);
                    if (verbose) System.out.println("*Rotate("+i+") = "+at2);
                    next.test(at2, full);
                }
            }
        }

        public static final double SMALL_VALUE = .0001;
        public static final double LARGE_VALUE = 10000;

        /*
         * Affine scale node.
         */
        public static class Scale extends Tester {
            public double fullvals[] = {
                // Noninvertibles
                0.0, 0.0,
                0.0, 1.0,
                1.0, 0.0,

                // Invertibles
                SMALL_VALUE, SMALL_VALUE,
                SMALL_VALUE, 1.0,
                1.0, SMALL_VALUE,

                SMALL_VALUE, LARGE_VALUE,
                LARGE_VALUE, SMALL_VALUE,

                LARGE_VALUE, LARGE_VALUE,
                LARGE_VALUE, 1.0,
                1.0, LARGE_VALUE,

                0.5, 0.5,
                1.0, 1.0,
                2.0, 2.0,
                Math.PI, Math.E,
            };
            public double abbrevvals[] = {
                0.0, 0.0,
                1.0, 1.0,
                2.0, 3.0,
            };

            public void test(AffineTransform init, Tester next, boolean full) {
                double scales[] = (full ? fullvals : abbrevvals);
                for (int i = 0; i < scales.length; i += 2) {
                    AffineTransform at2 = new AffineTransform(init);
                    at2.scale(scales[i], scales[i+1]);
                    if (verbose) System.out.println("*Scale("+scales[i]+", "+
                                                    scales[i+1]+") = "+at2);
                    next.test(at2, full);
                }
            }
        }

        /*
         * Affine shear node.
         */
        public static class Shear extends Tester {
            public double fullvals[] = {
                0.0, 0.0,
                0.0, 1.0,
                1.0, 0.0,

                // Noninvertible
                1.0, 1.0,

                SMALL_VALUE, SMALL_VALUE,
                SMALL_VALUE, LARGE_VALUE,
                LARGE_VALUE, SMALL_VALUE,
                LARGE_VALUE, LARGE_VALUE,

                Math.PI, Math.E,
            };
            public double abbrevvals[] = {
                0.0, 0.0,
                0.0, 1.0,
                1.0, 0.0,

                // Noninvertible
                1.0, 1.0,
            };

            public void test(AffineTransform init, Tester next, boolean full) {
                double shears[] = (full ? fullvals : abbrevvals);
                for (int i = 0; i < shears.length; i += 2) {
                    AffineTransform at2 = new AffineTransform(init);
                    at2.shear(shears[i], shears[i+1]);
                    if (verbose) System.out.println("*Shear("+shears[i]+", "+
                                                    shears[i+1]+") = "+at2);
                    next.test(at2, full);
                }
            }
        }

        /*
         * Affine translate node.
         */
        public static class Translate extends Tester {
            public double fullvals[] = {
                0.0, 0.0,
                0.0, 1.0,
                1.0, 0.0,

                SMALL_VALUE, SMALL_VALUE,
                SMALL_VALUE, LARGE_VALUE,
                LARGE_VALUE, SMALL_VALUE,
                LARGE_VALUE, LARGE_VALUE,

                Math.PI, Math.E,
            };
            public double abbrevvals[] = {
                0.0, 0.0,
                0.0, 1.0,
                1.0, 0.0,
                Math.PI, Math.E,
            };

            public void test(AffineTransform init, Tester next, boolean full) {
                double translates[] = (full ? fullvals : abbrevvals);
                for (int i = 0; i < translates.length; i += 2) {
                    AffineTransform at2 = new AffineTransform(init);
                    at2.translate(translates[i], translates[i+1]);
                    if (verbose) System.out.println("*Translate("+
                                                    translates[i]+", "+
                                                    translates[i+1]+") = "+at2);
                    next.test(at2, full);
                }
            }
        }
    }

    public static void report(AffineTransform orig,
                              AffineTransform at1, AffineTransform at2,
                              String message)
    {
        System.out.println(orig+", type = "+orig.getType());
        System.out.println(at1+", type = "+at1.getType());
        System.out.println(at2+", type = "+at2.getType());
        System.out.println("ScaleX values differ by "+
                           ulps(at1.getScaleX(),
                                at2.getScaleX())+" ulps");
        System.out.println("ScaleY values differ by "+
                           ulps(at1.getScaleY(),
                                at2.getScaleY())+" ulps");
        System.out.println("ShearX values differ by "+
                           ulps(at1.getShearX(),
                                at2.getShearX())+" ulps");
        System.out.println("ShearY values differ by "+
                           ulps(at1.getShearY(),
                                at2.getShearY())+" ulps");
        System.out.println("TranslateX values differ by "+
                           ulps(at1.getTranslateX(),
                                at2.getTranslateX())+" ulps");
        System.out.println("TranslateY values differ by "+
                           ulps(at1.getTranslateY(),
                                at2.getTranslateY())+" ulps");
        throw new RuntimeException(message);
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

    public static final int ANY_SCALE_MASK =
        (AffineTransform.TYPE_UNIFORM_SCALE |
         AffineTransform.TYPE_GENERAL_SCALE);
    public static int getModifiedType(AffineTransform at) {
        int type = at.getType();
        // Some of the vector methods can introduce a tiny uniform scale
        // at some angles...
        if ((type & ANY_SCALE_MASK) != 0) {
            maxulps = Math.max(maxulps, ulps(at.getDeterminant(), 1.0));
            if (ulps(at.getDeterminant(), 1.0) <= MAX_ULPS) {
                // Really tiny - we will ignore it
                type &= ~ ANY_SCALE_MASK;
            }
        }
        return type;
    }

    public static boolean compare(double val1, double val2, double maxulps) {
        if (Math.abs(val1 - val2) < 1E-15) return true;
        return (ulps(val1, val2) <= maxulps);
    }

    public static double ulps(double val1, double val2) {
        double diff = Math.abs(val1 - val2);
        double ulpmax = Math.min(Math.ulp(val1), Math.ulp(val2));
        return (diff / ulpmax);
    }
}
