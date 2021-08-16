/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4418285
 * @summary Tests that transforms modified with degenerate operations
 *          continue to return their more optimal type from getType().
 *          This test also confirms that isIdentity() returns the
 *          optimal value under all histories of modification.
 * @run main GetTypeOptimization
 */

import java.awt.geom.AffineTransform;
import java.util.Random;

public class GetTypeOptimization {
    static int TYPE_IDENTITY          = AffineTransform.TYPE_IDENTITY;
    static int TYPE_TRANSLATION       = AffineTransform.TYPE_TRANSLATION;
    static int TYPE_UNIFORM_SCALE     = AffineTransform.TYPE_UNIFORM_SCALE;
    static int TYPE_GENERAL_SCALE     = AffineTransform.TYPE_GENERAL_SCALE;
    static int TYPE_FLIP              = AffineTransform.TYPE_FLIP;
    static int TYPE_QUADRANT_ROTATION = AffineTransform.TYPE_QUADRANT_ROTATION;
    static int TYPE_GENERAL_ROTATION  = AffineTransform.TYPE_GENERAL_ROTATION;
    static int TYPE_GENERAL_TRANSFORM = AffineTransform.TYPE_GENERAL_TRANSFORM;

    public static Random rand = new Random();

    public static boolean verbose;
    public static int numerrors;

    public static void main(String argv[]) {
        verbose = (argv.length != 0);

        checkBug4418285();

        checkAtType(new AffineTransform());
        checkAtType(AffineTransform.getTranslateInstance(0, 0));
        checkAtType(AffineTransform.getScaleInstance(1, 1));
        checkAtType(AffineTransform.getShearInstance(0, 0));
        checkAtType(AffineTransform.getRotateInstance(0));
        checkAtType(AffineTransform.getRotateInstance(0, 0, 0));
        for (int i = 90; i <= 360; i += 90) {
            double angle = Math.toRadians(i);
            checkAtType(AffineTransform.getRotateInstance(angle));
            checkAtType(AffineTransform.getRotateInstance(angle, 0, 0));
        }

        AffineTransform at = new AffineTransform();
        checkAtType(at);

        at.setToIdentity(); checkAtType(at);
        at.setToTranslation(0.0, 0.0); checkAtType(at);
        at.setToScale(1.0, 1.0); checkAtType(at);
        at.setToShear(0.0, 0.0); checkAtType(at);
        at.setToRotation(0); checkAtType(at);
        at.setToRotation(0, 0, 0); checkAtType(at);
        for (int i = 90; i <= 360; i += 90) {
            double angle = Math.toRadians(i);
            at.setToRotation(angle); checkAtType(at);
            at.setToRotation(angle, 0, 0); checkAtType(at);
        }

        at.setToIdentity(); at.scale(1, 1); checkAtType(at);
        at.setToIdentity(); at.translate(0, 0); checkAtType(at);
        at.setToIdentity(); at.shear(0, 0); checkAtType(at);
        at.setToIdentity(); at.rotate(0); checkAtType(at);
        for (int i = 90; i <= 360; i += 90) {
            double angle = Math.toRadians(i);
            at.setToIdentity(); at.rotate(angle); checkAtType(at);
            at.setToIdentity(); at.rotate(angle, 0, 0); checkAtType(at);
        }

        at.setToIdentity();
        for (int i = 0; i < 4; i++) {
            at.rotate(Math.toRadians(90)); checkAtType(at);
        }

        at.setToIdentity();
        at.scale(2, 2); checkAtType(at);
        at.scale(.5, .5); checkAtType(at);

        for (int n = 1; n <= 3; n++) {
            for (int i = 0; i < 500; i++) {
                checkAtType(makeRandomTransform(n));
            }
        }
        if (numerrors != 0) {
            if (!verbose) {
                System.err.println("Rerun test with an argument for details");
            }
            throw new RuntimeException(numerrors+" tests failed!");
        }
    }

    public static void checkBug4418285() {
        AffineTransform id =
            new AffineTransform ();
        AffineTransform translate0 =
            AffineTransform.getTranslateInstance (0, 0);
        if (id.isIdentity() != translate0.isIdentity() ||
            id.getType() != translate0.getType())
        {
            numerrors++;
            if (verbose) {
                System.err.println("id=        " + id         +
                                   ", isIdentity()=" +
                                   id.isIdentity());
                System.err.println("translate0=" + translate0 +
                                   ", isIdentity()=" +
                                   translate0.isIdentity());
                System.err.println("equals="     + id.equals (translate0));
                System.err.println();
            }
        }
    }

    public static AffineTransform makeRandomTransform(int numops) {
        AffineTransform at = new AffineTransform();
        while (--numops >= 0) {
            switch (rand.nextInt(4)) {
            case 0:
                at.scale(rand.nextDouble() * 5 - 2.5,
                         rand.nextDouble() * 5 - 2.5);
                break;
            case 1:
                at.shear(rand.nextDouble() * 5 - 2.5,
                         rand.nextDouble() * 5 - 2.5);
                break;
            case 2:
                at.rotate(rand.nextDouble() * Math.PI * 2);
                break;
            case 3:
                at.translate(rand.nextDouble() * 50 - 25,
                             rand.nextDouble() * 50 - 25);
                break;
            default:
                throw new InternalError("bad case!");
            }
        }
        return at;
    }

    public static void checkAtType(AffineTransform at) {
        int reftype = getRefType(at);
        boolean isident = isIdentity(at);
        for (int i = 0; i < 5; i++) {
            boolean atisident = at.isIdentity();
            int attype = at.getType();
            if (isident != atisident || reftype != attype) {
                numerrors++;
                if (verbose) {
                    System.err.println(at+".isIdentity() == "+atisident);
                    System.err.println(at+".getType() == "+attype);
                    System.err.println("should be "+isident+", "+reftype);
                    new Throwable().printStackTrace();
                    System.err.println();
                }
                break;
            }
        }
    }

    public static boolean isIdentity(AffineTransform at) {
        return (at.getScaleX() == 1 &&
                at.getScaleY() == 1 &&
                at.getShearX() == 0 &&
                at.getShearY() == 0 &&
                at.getTranslateX() == 0 &&
                at.getTranslateY() == 0);

    }

    public static int getRefType(AffineTransform at) {
        double m00 = at.getScaleX();
        double m11 = at.getScaleY();
        double m01 = at.getShearX();
        double m10 = at.getShearY();
        if (m00 * m01 + m10 * m11 != 0) {
            // Transformed unit vectors are not perpendicular...
            return TYPE_GENERAL_TRANSFORM;
        }
        int type = ((at.getTranslateX() != 0 || at.getTranslateY() != 0)
                    ? TYPE_TRANSLATION : TYPE_IDENTITY);
        boolean sgn0, sgn1;
        if (m01 == 0 && m10 == 0) {
            sgn0 = (m00 >= 0.0);
            sgn1 = (m11 >= 0.0);
            if (sgn0 == sgn1) {
                if (sgn0) {
                    // Both scaling factors non-negative - simple scale
                    if (m00 != m11) {
                        type |= TYPE_GENERAL_SCALE;
                    } else if (m00 != 1.0) {
                        type |= TYPE_UNIFORM_SCALE;
                    }
                } else {
                    // Both scaling factors negative - 180 degree rotation
                    type |= TYPE_QUADRANT_ROTATION;
                    if (m00 != m11) {
                        type |= TYPE_GENERAL_SCALE;
                    } else if (m00 != -1.0) {
                        type |= TYPE_UNIFORM_SCALE;
                    }
                }
            } else {
                // Scaling factor signs different - flip about some axis
                type |= TYPE_FLIP;
                if (m00 != -m11) {
                    type |= TYPE_GENERAL_SCALE;
                } else if (m00 != 1.0 && m00 != -1.0) {
                    type |= TYPE_UNIFORM_SCALE;
                }
            }
        } else if (m00 == 0 && m11 == 0) {
            sgn0 = (m01 >= 0.0);
            sgn1 = (m10 >= 0.0);
            if (sgn0 != sgn1) {
                // Different signs - simple 90 degree rotation
                if (m01 != -m10) {
                    type |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                } else if (m01 != 1.0 && m01 != -1.0) {
                    type |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    type |= TYPE_QUADRANT_ROTATION;
                }
            } else {
                // Same signs - 90 degree rotation plus an axis flip too
                if (m01 == m10) {
                    if (m01 == 0) {
                        // All four m[01][01] elements are 0
                        type |= TYPE_UNIFORM_SCALE;
                    } else {
                        // Note - shouldn't (1,1) be no scale at all?
                        type |= (TYPE_QUADRANT_ROTATION |
                                 TYPE_FLIP |
                                 TYPE_UNIFORM_SCALE);
                    }
                } else {
                    type |= (TYPE_QUADRANT_ROTATION |
                             TYPE_FLIP |
                             TYPE_GENERAL_SCALE);
                }
            }
        } else {
            if (m00 * m11 >= 0.0) {
                // sgn(m00) == sgn(m11) therefore sgn(m01) == -sgn(m10)
                // This is the "unflipped" (right-handed) state
                if (m00 != m11 || m01 != -m10) {
                    type |= (TYPE_GENERAL_ROTATION | TYPE_GENERAL_SCALE);
                } else if (m00 == 0) {
                    // then m11 == 0 also
                    if (m01 == -m10) {
                        type |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                    } else {
                        type |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                    }
                } else if (m00 * m11 - m01 * m10 != 1.0) {
                    type |= (TYPE_GENERAL_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    type |= TYPE_GENERAL_ROTATION;
                }
            } else {
                // sgn(m00) == -sgn(m11) therefore sgn(m01) == sgn(m10)
                // This is the "flipped" (left-handed) state
                if (m00 != -m11 || m01 != m10) {
                    type |= (TYPE_GENERAL_ROTATION |
                             TYPE_FLIP |
                             TYPE_GENERAL_SCALE);
                } else if (m01 == 0) {
                    if (m00 == 1.0 || m00 == -1.0) {
                        type |= TYPE_FLIP;
                    } else {
                        type |= (TYPE_FLIP | TYPE_UNIFORM_SCALE);
                    }
                } else if (m00 * m11 - m01 * m10 != 1.0) {
                    type |= (TYPE_GENERAL_ROTATION |
                             TYPE_FLIP |
                             TYPE_UNIFORM_SCALE);
                } else {
                    type |= (TYPE_GENERAL_ROTATION | TYPE_FLIP);
                }
            }
        }
        return type;
    }
}
