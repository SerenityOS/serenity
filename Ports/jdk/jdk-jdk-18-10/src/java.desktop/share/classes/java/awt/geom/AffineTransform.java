/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.geom;

import java.awt.Shape;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.Serial;

/**
 * The {@code AffineTransform} class represents a 2D affine transform
 * that performs a linear mapping from 2D coordinates to other 2D
 * coordinates that preserves the "straightness" and
 * "parallelness" of lines.  Affine transformations can be constructed
 * using sequences of translations, scales, flips, rotations, and shears.
 * <p>
 * Such a coordinate transformation can be represented by a 3 row by
 * 3 column matrix with an implied last row of [ 0 0 1 ].  This matrix
 * transforms source coordinates {@code (x,y)} into
 * destination coordinates {@code (x',y')} by considering
 * them to be a column vector and multiplying the coordinate vector
 * by the matrix according to the following process:
 * <pre>
 *      [ x']   [  m00  m01  m02  ] [ x ]   [ m00x + m01y + m02 ]
 *      [ y'] = [  m10  m11  m12  ] [ y ] = [ m10x + m11y + m12 ]
 *      [ 1 ]   [   0    0    1   ] [ 1 ]   [         1         ]
 * </pre>
 * <h2><a id="quadrantapproximation">Handling 90-Degree Rotations</a></h2>
 * <p>
 * In some variations of the {@code rotate} methods in the
 * {@code AffineTransform} class, a double-precision argument
 * specifies the angle of rotation in radians.
 * These methods have special handling for rotations of approximately
 * 90 degrees (including multiples such as 180, 270, and 360 degrees),
 * so that the common case of quadrant rotation is handled more
 * efficiently.
 * This special handling can cause angles very close to multiples of
 * 90 degrees to be treated as if they were exact multiples of
 * 90 degrees.
 * For small multiples of 90 degrees the range of angles treated
 * as a quadrant rotation is approximately 0.00000121 degrees wide.
 * This section explains why such special care is needed and how
 * it is implemented.
 * <p>
 * Since 90 degrees is represented as {@code PI/2} in radians,
 * and since PI is a transcendental (and therefore irrational) number,
 * it is not possible to exactly represent a multiple of 90 degrees as
 * an exact double precision value measured in radians.
 * As a result it is theoretically impossible to describe quadrant
 * rotations (90, 180, 270 or 360 degrees) using these values.
 * Double precision floating point values can get very close to
 * non-zero multiples of {@code PI/2} but never close enough
 * for the sine or cosine to be exactly 0.0, 1.0 or -1.0.
 * The implementations of {@code Math.sin()} and
 * {@code Math.cos()} correspondingly never return 0.0
 * for any case other than {@code Math.sin(0.0)}.
 * These same implementations do, however, return exactly 1.0 and
 * -1.0 for some range of numbers around each multiple of 90
 * degrees since the correct answer is so close to 1.0 or -1.0 that
 * the double precision significand cannot represent the difference
 * as accurately as it can for numbers that are near 0.0.
 * <p>
 * The net result of these issues is that if the
 * {@code Math.sin()} and {@code Math.cos()} methods
 * are used to directly generate the values for the matrix modifications
 * during these radian-based rotation operations then the resulting
 * transform is never strictly classifiable as a quadrant rotation
 * even for a simple case like {@code rotate(Math.PI/2.0)},
 * due to minor variations in the matrix caused by the non-0.0 values
 * obtained for the sine and cosine.
 * If these transforms are not classified as quadrant rotations then
 * subsequent code which attempts to optimize further operations based
 * upon the type of the transform will be relegated to its most general
 * implementation.
 * <p>
 * Because quadrant rotations are fairly common,
 * this class should handle these cases reasonably quickly, both in
 * applying the rotations to the transform and in applying the resulting
 * transform to the coordinates.
 * To facilitate this optimal handling, the methods which take an angle
 * of rotation measured in radians attempt to detect angles that are
 * intended to be quadrant rotations and treat them as such.
 * These methods therefore treat an angle <em>theta</em> as a quadrant
 * rotation if either <code>Math.sin(<em>theta</em>)</code> or
 * <code>Math.cos(<em>theta</em>)</code> returns exactly 1.0 or -1.0.
 * As a rule of thumb, this property holds true for a range of
 * approximately 0.0000000211 radians (or 0.00000121 degrees) around
 * small multiples of {@code Math.PI/2.0}.
 *
 * @author Jim Graham
 * @since 1.2
 */
public class AffineTransform implements Cloneable, java.io.Serializable {

    /*
     * This constant is only useful for the cached type field.
     * It indicates that the type has been decached and must be recalculated.
     */
    private static final int TYPE_UNKNOWN = -1;

    /**
     * This constant indicates that the transform defined by this object
     * is an identity transform.
     * An identity transform is one in which the output coordinates are
     * always the same as the input coordinates.
     * If this transform is anything other than the identity transform,
     * the type will either be the constant GENERAL_TRANSFORM or a
     * combination of the appropriate flag bits for the various coordinate
     * conversions that this transform performs.
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_IDENTITY = 0;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a translation in addition to the conversions indicated
     * by other flag bits.
     * A translation moves the coordinates by a constant amount in x
     * and y without changing the length or angle of vectors.
     * @see #TYPE_IDENTITY
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_TRANSLATION = 1;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a uniform scale in addition to the conversions indicated
     * by other flag bits.
     * A uniform scale multiplies the length of vectors by the same amount
     * in both the x and y directions without changing the angle between
     * vectors.
     * This flag bit is mutually exclusive with the TYPE_GENERAL_SCALE flag.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_UNIFORM_SCALE = 2;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a general scale in addition to the conversions indicated
     * by other flag bits.
     * A general scale multiplies the length of vectors by different
     * amounts in the x and y directions without changing the angle
     * between perpendicular vectors.
     * This flag bit is mutually exclusive with the TYPE_UNIFORM_SCALE flag.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_GENERAL_SCALE = 4;

    /**
     * This constant is a bit mask for any of the scale flag bits.
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @since 1.2
     */
    public static final int TYPE_MASK_SCALE = (TYPE_UNIFORM_SCALE |
                                               TYPE_GENERAL_SCALE);

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a mirror image flip about some axis which changes the
     * normally right handed coordinate system into a left handed
     * system in addition to the conversions indicated by other flag bits.
     * A right handed coordinate system is one where the positive X
     * axis rotates counterclockwise to overlay the positive Y axis
     * similar to the direction that the fingers on your right hand
     * curl when you stare end on at your thumb.
     * A left handed coordinate system is one where the positive X
     * axis rotates clockwise to overlay the positive Y axis similar
     * to the direction that the fingers on your left hand curl.
     * There is no mathematical way to determine the angle of the
     * original flipping or mirroring transformation since all angles
     * of flip are identical given an appropriate adjusting rotation.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_FLIP = 64;
    /* NOTE: TYPE_FLIP was added after GENERAL_TRANSFORM was in public
     * circulation and the flag bits could no longer be conveniently
     * renumbered without introducing binary incompatibility in outside
     * code.
     */

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a quadrant rotation by some multiple of 90 degrees in
     * addition to the conversions indicated by other flag bits.
     * A rotation changes the angles of vectors by the same amount
     * regardless of the original direction of the vector and without
     * changing the length of the vector.
     * This flag bit is mutually exclusive with the TYPE_GENERAL_ROTATION flag.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_QUADRANT_ROTATION = 8;

    /**
     * This flag bit indicates that the transform defined by this object
     * performs a rotation by an arbitrary angle in addition to the
     * conversions indicated by other flag bits.
     * A rotation changes the angles of vectors by the same amount
     * regardless of the original direction of the vector and without
     * changing the length of the vector.
     * This flag bit is mutually exclusive with the
     * TYPE_QUADRANT_ROTATION flag.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_GENERAL_ROTATION = 16;

    /**
     * This constant is a bit mask for any of the rotation flag bits.
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @since 1.2
     */
    public static final int TYPE_MASK_ROTATION = (TYPE_QUADRANT_ROTATION |
                                                  TYPE_GENERAL_ROTATION);

    /**
     * This constant indicates that the transform defined by this object
     * performs an arbitrary conversion of the input coordinates.
     * If this transform can be classified by any of the above constants,
     * the type will either be the constant TYPE_IDENTITY or a
     * combination of the appropriate flag bits for the various coordinate
     * conversions that this transform performs.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #getType
     * @since 1.2
     */
    public static final int TYPE_GENERAL_TRANSFORM = 32;

    /**
     * This constant is used for the internal state variable to indicate
     * that no calculations need to be performed and that the source
     * coordinates only need to be copied to their destinations to
     * complete the transformation equation of this transform.
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #state
     */
    static final int APPLY_IDENTITY = 0;

    /**
     * This constant is used for the internal state variable to indicate
     * that the translation components of the matrix (m02 and m12) need
     * to be added to complete the transformation equation of this transform.
     * @see #APPLY_IDENTITY
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     * @see #state
     */
    static final int APPLY_TRANSLATE = 1;

    /**
     * This constant is used for the internal state variable to indicate
     * that the scaling components of the matrix (m00 and m11) need
     * to be factored in to complete the transformation equation of
     * this transform.  If the APPLY_SHEAR bit is also set then it
     * indicates that the scaling components are not both 0.0.  If the
     * APPLY_SHEAR bit is not also set then it indicates that the
     * scaling components are not both 1.0.  If neither the APPLY_SHEAR
     * nor the APPLY_SCALE bits are set then the scaling components
     * are both 1.0, which means that the x and y components contribute
     * to the transformed coordinate, but they are not multiplied by
     * any scaling factor.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SHEAR
     * @see #state
     */
    static final int APPLY_SCALE = 2;

    /**
     * This constant is used for the internal state variable to indicate
     * that the shearing components of the matrix (m01 and m10) need
     * to be factored in to complete the transformation equation of this
     * transform.  The presence of this bit in the state variable changes
     * the interpretation of the APPLY_SCALE bit as indicated in its
     * documentation.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #state
     */
    static final int APPLY_SHEAR = 4;

    /*
     * For methods which combine together the state of two separate
     * transforms and dispatch based upon the combination, these constants
     * specify how far to shift one of the states so that the two states
     * are mutually non-interfering and provide constants for testing the
     * bits of the shifted (HI) state.  The methods in this class use
     * the convention that the state of "this" transform is unshifted and
     * the state of the "other" or "argument" transform is shifted (HI).
     */
    private static final int HI_SHIFT = 3;
    private static final int HI_IDENTITY = APPLY_IDENTITY << HI_SHIFT;
    private static final int HI_TRANSLATE = APPLY_TRANSLATE << HI_SHIFT;
    private static final int HI_SCALE = APPLY_SCALE << HI_SHIFT;
    private static final int HI_SHEAR = APPLY_SHEAR << HI_SHIFT;

    /**
     * The X coordinate scaling element of the 3x3
     * affine transformation matrix.
     *
     * @serial
     */
    double m00;

    /**
     * The Y coordinate shearing element of the 3x3
     * affine transformation matrix.
     *
     * @serial
     */
     double m10;

    /**
     * The X coordinate shearing element of the 3x3
     * affine transformation matrix.
     *
     * @serial
     */
     double m01;

    /**
     * The Y coordinate scaling element of the 3x3
     * affine transformation matrix.
     *
     * @serial
     */
     double m11;

    /**
     * The X coordinate of the translation element of the
     * 3x3 affine transformation matrix.
     *
     * @serial
     */
     double m02;

    /**
     * The Y coordinate of the translation element of the
     * 3x3 affine transformation matrix.
     *
     * @serial
     */
     double m12;

    /**
     * This field keeps track of which components of the matrix need to
     * be applied when performing a transformation.
     * @see #APPLY_IDENTITY
     * @see #APPLY_TRANSLATE
     * @see #APPLY_SCALE
     * @see #APPLY_SHEAR
     */
    transient int state;

    /**
     * This field caches the current transformation type of the matrix.
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_FLIP
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @see #TYPE_UNKNOWN
     * @see #getType
     */
    private transient int type;

    private AffineTransform(double m00, double m10,
                            double m01, double m11,
                            double m02, double m12,
                            int state) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        this.state = state;
        this.type = TYPE_UNKNOWN;
    }

    /**
     * Constructs a new {@code AffineTransform} representing the
     * Identity transformation.
     * @since 1.2
     */
    public AffineTransform() {
        m00 = m11 = 1.0;
        // m01 = m10 = m02 = m12 = 0.0;         /* Not needed. */
        // state = APPLY_IDENTITY;              /* Not needed. */
        // type = TYPE_IDENTITY;                /* Not needed. */
    }

    /**
     * Constructs a new {@code AffineTransform} that is a copy of
     * the specified {@code AffineTransform} object.
     * @param Tx the {@code AffineTransform} object to copy
     * @since 1.2
     */
    public AffineTransform(AffineTransform Tx) {
        this.m00 = Tx.m00;
        this.m10 = Tx.m10;
        this.m01 = Tx.m01;
        this.m11 = Tx.m11;
        this.m02 = Tx.m02;
        this.m12 = Tx.m12;
        this.state = Tx.state;
        this.type = Tx.type;
    }

    /**
     * Constructs a new {@code AffineTransform} from 6 floating point
     * values representing the 6 specifiable entries of the 3x3
     * transformation matrix.
     *
     * @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     * @since 1.2
     */
    @ConstructorProperties({ "scaleX", "shearY", "shearX", "scaleY", "translateX", "translateY" })
    public AffineTransform(float m00, float m10,
                           float m01, float m11,
                           float m02, float m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        updateState();
    }

    /**
     * Constructs a new {@code AffineTransform} from an array of
     * floating point values representing either the 4 non-translation
     * entries or the 6 specifiable entries of the 3x3 transformation
     * matrix.  The values are retrieved from the array as
     * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;[m02&nbsp;m12]}.
     * @param flatmatrix the float array containing the values to be set
     * in the new {@code AffineTransform} object. The length of the
     * array is assumed to be at least 4. If the length of the array is
     * less than 6, only the first 4 values are taken. If the length of
     * the array is greater than 6, the first 6 values are taken.
     * @since 1.2
     */
    public AffineTransform(float[] flatmatrix) {
        m00 = flatmatrix[0];
        m10 = flatmatrix[1];
        m01 = flatmatrix[2];
        m11 = flatmatrix[3];
        if (flatmatrix.length > 5) {
            m02 = flatmatrix[4];
            m12 = flatmatrix[5];
        }
        updateState();
    }

    /**
     * Constructs a new {@code AffineTransform} from 6 double
     * precision values representing the 6 specifiable entries of the 3x3
     * transformation matrix.
     *
     * @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     * @since 1.2
     */
    public AffineTransform(double m00, double m10,
                           double m01, double m11,
                           double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        updateState();
    }

    /**
     * Constructs a new {@code AffineTransform} from an array of
     * double precision values representing either the 4 non-translation
     * entries or the 6 specifiable entries of the 3x3 transformation
     * matrix. The values are retrieved from the array as
     * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;[m02&nbsp;m12]}.
     * @param flatmatrix the double array containing the values to be set
     * in the new {@code AffineTransform} object. The length of the
     * array is assumed to be at least 4. If the length of the array is
     * less than 6, only the first 4 values are taken. If the length of
     * the array is greater than 6, the first 6 values are taken.
     * @since 1.2
     */
    public AffineTransform(double[] flatmatrix) {
        m00 = flatmatrix[0];
        m10 = flatmatrix[1];
        m01 = flatmatrix[2];
        m11 = flatmatrix[3];
        if (flatmatrix.length > 5) {
            m02 = flatmatrix[4];
            m12 = flatmatrix[5];
        }
        updateState();
    }

    /**
     * Returns a transform representing a translation transformation.
     * The matrix representing the returned transform is:
     * <pre>
     *          [   1    0    tx  ]
     *          [   0    1    ty  ]
     *          [   0    0    1   ]
     * </pre>
     * @param tx the distance by which coordinates are translated in the
     * X axis direction
     * @param ty the distance by which coordinates are translated in the
     * Y axis direction
     * @return an {@code AffineTransform} object that represents a
     *  translation transformation, created with the specified vector.
     * @since 1.2
     */
    public static AffineTransform getTranslateInstance(double tx, double ty) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToTranslation(tx, ty);
        return Tx;
    }

    /**
     * Returns a transform representing a rotation transformation.
     * The matrix representing the returned transform is:
     * <pre>
     *          [   cos(theta)    -sin(theta)    0   ]
     *          [   sin(theta)     cos(theta)    0   ]
     *          [       0              0         1   ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     * @param theta the angle of rotation measured in radians
     * @return an {@code AffineTransform} object that is a rotation
     *  transformation, created with the specified angle of rotation.
     * @since 1.2
     */
    public static AffineTransform getRotateInstance(double theta) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta);
        return Tx;
    }

    /**
     * Returns a transform that rotates coordinates around an anchor point.
     * This operation is equivalent to translating the coordinates so
     * that the anchor point is at the origin (S1), then rotating them
     * about the new origin (S2), and finally translating so that the
     * intermediate origin is restored to the coordinates of the original
     * anchor point (S3).
     * <p>
     * This operation is equivalent to the following sequence of calls:
     * <pre>
     *     AffineTransform Tx = new AffineTransform();
     *     Tx.translate(anchorx, anchory);    // S3: final translation
     *     Tx.rotate(theta);                  // S2: rotate around anchor
     *     Tx.translate(-anchorx, -anchory);  // S1: translate anchor to origin
     * </pre>
     * The matrix representing the returned transform is:
     * <pre>
     *          [   cos(theta)    -sin(theta)    x-x*cos+y*sin  ]
     *          [   sin(theta)     cos(theta)    y-x*sin-y*cos  ]
     *          [       0              0               1        ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     *
     * @param theta the angle of rotation measured in radians
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @return an {@code AffineTransform} object that rotates
     *  coordinates around the specified point by the specified angle of
     *  rotation.
     * @since 1.2
     */
    public static AffineTransform getRotateInstance(double theta,
                                                    double anchorx,
                                                    double anchory)
    {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta, anchorx, anchory);
        return Tx;
    }

    /**
     * Returns a transform that rotates coordinates according to
     * a rotation vector.
     * All coordinates rotate about the origin by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * an identity transform is returned.
     * This operation is equivalent to calling:
     * <pre>
     *     AffineTransform.getRotateInstance(Math.atan2(vecy, vecx));
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @return an {@code AffineTransform} object that rotates
     *  coordinates according to the specified rotation vector.
     * @since 1.6
     */
    public static AffineTransform getRotateInstance(double vecx, double vecy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(vecx, vecy);
        return Tx;
    }

    /**
     * Returns a transform that rotates coordinates around an anchor
     * point according to a rotation vector.
     * All coordinates rotate about the specified anchor coordinates
     * by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * an identity transform is returned.
     * This operation is equivalent to calling:
     * <pre>
     *     AffineTransform.getRotateInstance(Math.atan2(vecy, vecx),
     *                                       anchorx, anchory);
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @return an {@code AffineTransform} object that rotates
     *  coordinates around the specified point according to the
     *  specified rotation vector.
     * @since 1.6
     */
    public static AffineTransform getRotateInstance(double vecx,
                                                    double vecy,
                                                    double anchorx,
                                                    double anchory)
    {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(vecx, vecy, anchorx, anchory);
        return Tx;
    }

    /**
     * Returns a transform that rotates coordinates by the specified
     * number of quadrants.
     * This operation is equivalent to calling:
     * <pre>
     *     AffineTransform.getRotateInstance(numquadrants * Math.PI / 2.0);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @return an {@code AffineTransform} object that rotates
     *  coordinates by the specified number of quadrants.
     * @since 1.6
     */
    public static AffineTransform getQuadrantRotateInstance(int numquadrants) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToQuadrantRotation(numquadrants);
        return Tx;
    }

    /**
     * Returns a transform that rotates coordinates by the specified
     * number of quadrants around the specified anchor point.
     * This operation is equivalent to calling:
     * <pre>
     *     AffineTransform.getRotateInstance(numquadrants * Math.PI / 2.0,
     *                                       anchorx, anchory);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @return an {@code AffineTransform} object that rotates
     *  coordinates by the specified number of quadrants around the
     *  specified anchor point.
     * @since 1.6
     */
    public static AffineTransform getQuadrantRotateInstance(int numquadrants,
                                                            double anchorx,
                                                            double anchory)
    {
        AffineTransform Tx = new AffineTransform();
        Tx.setToQuadrantRotation(numquadrants, anchorx, anchory);
        return Tx;
    }

    /**
     * Returns a transform representing a scaling transformation.
     * The matrix representing the returned transform is:
     * <pre>
     *          [   sx   0    0   ]
     *          [   0    sy   0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param sx the factor by which coordinates are scaled along the
     * X axis direction
     * @param sy the factor by which coordinates are scaled along the
     * Y axis direction
     * @return an {@code AffineTransform} object that scales
     *  coordinates by the specified factors.
     * @since 1.2
     */
    public static AffineTransform getScaleInstance(double sx, double sy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToScale(sx, sy);
        return Tx;
    }

    /**
     * Returns a transform representing a shearing transformation.
     * The matrix representing the returned transform is:
     * <pre>
     *          [   1   shx   0   ]
     *          [  shy   1    0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate
     * @return an {@code AffineTransform} object that shears
     *  coordinates by the specified multipliers.
     * @since 1.2
     */
    public static AffineTransform getShearInstance(double shx, double shy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToShear(shx, shy);
        return Tx;
    }

    /**
     * Retrieves the flag bits describing the conversion properties of
     * this transform.
     * The return value is either one of the constants TYPE_IDENTITY
     * or TYPE_GENERAL_TRANSFORM, or a combination of the
     * appropriate flag bits.
     * A valid combination of flag bits is an exclusive OR operation
     * that can combine
     * the TYPE_TRANSLATION flag bit
     * in addition to either of the
     * TYPE_UNIFORM_SCALE or TYPE_GENERAL_SCALE flag bits
     * as well as either of the
     * TYPE_QUADRANT_ROTATION or TYPE_GENERAL_ROTATION flag bits.
     * @return the OR combination of any of the indicated flags that
     * apply to this transform
     * @see #TYPE_IDENTITY
     * @see #TYPE_TRANSLATION
     * @see #TYPE_UNIFORM_SCALE
     * @see #TYPE_GENERAL_SCALE
     * @see #TYPE_QUADRANT_ROTATION
     * @see #TYPE_GENERAL_ROTATION
     * @see #TYPE_GENERAL_TRANSFORM
     * @since 1.2
     */
    public int getType() {
        if (type == TYPE_UNKNOWN) {
            calculateType();
        }
        return type;
    }

    /**
     * This is the utility function to calculate the flag bits when
     * they have not been cached.
     * @see #getType
     */
    @SuppressWarnings("fallthrough")
    private void calculateType() {
        int ret = TYPE_IDENTITY;
        boolean sgn0, sgn1;
        double M0, M1, M2, M3;
        updateState();
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            ret = TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            if ((M0 = m00) * (M2 = m01) + (M3 = m10) * (M1 = m11) != 0) {
                // Transformed unit vectors are not perpendicular...
                this.type = TYPE_GENERAL_TRANSFORM;
                return;
            }
            sgn0 = (M0 >= 0.0);
            sgn1 = (M1 >= 0.0);
            if (sgn0 == sgn1) {
                // sgn(M0) == sgn(M1) therefore sgn(M2) == -sgn(M3)
                // This is the "unflipped" (right-handed) state
                if (M0 != M1 || M2 != -M3) {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_GENERAL_SCALE);
                } else if (M0 * M1 - M2 * M3 != 1.0) {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret |= TYPE_GENERAL_ROTATION;
                }
            } else {
                // sgn(M0) == -sgn(M1) therefore sgn(M2) == sgn(M3)
                // This is the "flipped" (left-handed) state
                if (M0 != -M1 || M2 != M3) {
                    ret |= (TYPE_GENERAL_ROTATION |
                            TYPE_FLIP |
                            TYPE_GENERAL_SCALE);
                } else if (M0 * M1 - M2 * M3 != 1.0) {
                    ret |= (TYPE_GENERAL_ROTATION |
                            TYPE_FLIP |
                            TYPE_UNIFORM_SCALE);
                } else {
                    ret |= (TYPE_GENERAL_ROTATION | TYPE_FLIP);
                }
            }
            break;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            ret = TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SHEAR):
            sgn0 = ((M0 = m01) >= 0.0);
            sgn1 = ((M1 = m10) >= 0.0);
            if (sgn0 != sgn1) {
                // Different signs - simple 90 degree rotation
                if (M0 != -M1) {
                    ret |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                } else if (M0 != 1.0 && M0 != -1.0) {
                    ret |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret |= TYPE_QUADRANT_ROTATION;
                }
            } else {
                // Same signs - 90 degree rotation plus an axis flip too
                if (M0 == M1) {
                    ret |= (TYPE_QUADRANT_ROTATION |
                            TYPE_FLIP |
                            TYPE_UNIFORM_SCALE);
                } else {
                    ret |= (TYPE_QUADRANT_ROTATION |
                            TYPE_FLIP |
                            TYPE_GENERAL_SCALE);
                }
            }
            break;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            ret = TYPE_TRANSLATION;
            /* NOBREAK */
        case (APPLY_SCALE):
            sgn0 = ((M0 = m00) >= 0.0);
            sgn1 = ((M1 = m11) >= 0.0);
            if (sgn0 == sgn1) {
                if (sgn0) {
                    // Both scaling factors non-negative - simple scale
                    // Note: APPLY_SCALE implies M0, M1 are not both 1
                    if (M0 == M1) {
                        ret |= TYPE_UNIFORM_SCALE;
                    } else {
                        ret |= TYPE_GENERAL_SCALE;
                    }
                } else {
                    // Both scaling factors negative - 180 degree rotation
                    if (M0 != M1) {
                        ret |= (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                    } else if (M0 != -1.0) {
                        ret |= (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                    } else {
                        ret |= TYPE_QUADRANT_ROTATION;
                    }
                }
            } else {
                // Scaling factor signs different - flip about some axis
                if (M0 == -M1) {
                    if (M0 == 1.0 || M0 == -1.0) {
                        ret |= TYPE_FLIP;
                    } else {
                        ret |= (TYPE_FLIP | TYPE_UNIFORM_SCALE);
                    }
                } else {
                    ret |= (TYPE_FLIP | TYPE_GENERAL_SCALE);
                }
            }
            break;
        case (APPLY_TRANSLATE):
            ret = TYPE_TRANSLATION;
            break;
        case (APPLY_IDENTITY):
            break;
        }
        this.type = ret;
    }

    /**
     * Returns the determinant of the matrix representation of the transform.
     * The determinant is useful both to determine if the transform can
     * be inverted and to get a single value representing the
     * combined X and Y scaling of the transform.
     * <p>
     * If the determinant is non-zero, then this transform is
     * invertible and the various methods that depend on the inverse
     * transform do not need to throw a
     * {@link NoninvertibleTransformException}.
     * If the determinant is zero then this transform can not be
     * inverted since the transform maps all input coordinates onto
     * a line or a point.
     * If the determinant is near enough to zero then inverse transform
     * operations might not carry enough precision to produce meaningful
     * results.
     * <p>
     * If this transform represents a uniform scale, as indicated by
     * the {@code getType} method then the determinant also
     * represents the square of the uniform scale factor by which all of
     * the points are expanded from or contracted towards the origin.
     * If this transform represents a non-uniform scale or more general
     * transform then the determinant is not likely to represent a
     * value useful for any purpose other than determining if inverse
     * transforms are possible.
     * <p>
     * Mathematically, the determinant is calculated using the formula:
     * <pre>
     *          |  m00  m01  m02  |
     *          |  m10  m11  m12  |  =  m00 * m11 - m01 * m10
     *          |   0    0    1   |
     * </pre>
     *
     * @return the determinant of the matrix used to transform the
     * coordinates.
     * @see #getType
     * @see #createInverse
     * @see #inverseTransform
     * @see #TYPE_UNIFORM_SCALE
     * @since 1.2
     */
    @SuppressWarnings("fallthrough")
    public double getDeterminant() {
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            return m00 * m11 - m01 * m10;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            return -(m01 * m10);
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            return m00 * m11;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            return 1.0;
        }
    }

    /**
     * Manually recalculates the state of the transform when the matrix
     * changes too much to predict the effects on the state.
     * The following table specifies what the various settings of the
     * state field say about the values of the corresponding matrix
     * element fields.
     * Note that the rules governing the SCALE fields are slightly
     * different depending on whether the SHEAR flag is also set.
     * <pre>
     *                     SCALE            SHEAR          TRANSLATE
     *                    m00/m11          m01/m10          m02/m12
     *
     * IDENTITY             1.0              0.0              0.0
     * TRANSLATE (TR)       1.0              0.0          not both 0.0
     * SCALE (SC)       not both 1.0         0.0              0.0
     * TR | SC          not both 1.0         0.0          not both 0.0
     * SHEAR (SH)           0.0          not both 0.0         0.0
     * TR | SH              0.0          not both 0.0     not both 0.0
     * SC | SH          not both 0.0     not both 0.0         0.0
     * TR | SC | SH     not both 0.0     not both 0.0     not both 0.0
     * </pre>
     */
    void updateState() {
        if (m01 == 0.0 && m10 == 0.0) {
            if (m00 == 1.0 && m11 == 1.0) {
                if (m02 == 0.0 && m12 == 0.0) {
                    state = APPLY_IDENTITY;
                    type = TYPE_IDENTITY;
                } else {
                    state = APPLY_TRANSLATE;
                    type = TYPE_TRANSLATION;
                }
            } else {
                if (m02 == 0.0 && m12 == 0.0) {
                    state = APPLY_SCALE;
                    type = TYPE_UNKNOWN;
                } else {
                    state = (APPLY_SCALE | APPLY_TRANSLATE);
                    type = TYPE_UNKNOWN;
                }
            }
        } else {
            if (m00 == 0.0 && m11 == 0.0) {
                if (m02 == 0.0 && m12 == 0.0) {
                    state = APPLY_SHEAR;
                    type = TYPE_UNKNOWN;
                } else {
                    state = (APPLY_SHEAR | APPLY_TRANSLATE);
                    type = TYPE_UNKNOWN;
                }
            } else {
                if (m02 == 0.0 && m12 == 0.0) {
                    state = (APPLY_SHEAR | APPLY_SCALE);
                    type = TYPE_UNKNOWN;
                } else {
                    state = (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE);
                    type = TYPE_UNKNOWN;
                }
            }
        }
    }

    /*
     * Convenience method used internally to throw exceptions when
     * a case was forgotten in a switch statement.
     */
    private void stateError() {
        throw new InternalError("missing case in transform state switch");
    }

    /**
     * Retrieves the 6 specifiable values in the 3x3 affine transformation
     * matrix and places them into an array of double precisions values.
     * The values are stored in the array as
     * {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;m02&nbsp;m12&nbsp;}.
     * An array of 4 doubles can also be specified, in which case only the
     * first four elements representing the non-transform
     * parts of the array are retrieved and the values are stored into
     * the array as {&nbsp;m00&nbsp;m10&nbsp;m01&nbsp;m11&nbsp;}
     * @param flatmatrix the double array used to store the returned
     * values.
     * @see #getScaleX
     * @see #getScaleY
     * @see #getShearX
     * @see #getShearY
     * @see #getTranslateX
     * @see #getTranslateY
     * @since 1.2
     */
    public void getMatrix(double[] flatmatrix) {
        flatmatrix[0] = m00;
        flatmatrix[1] = m10;
        flatmatrix[2] = m01;
        flatmatrix[3] = m11;
        if (flatmatrix.length > 5) {
            flatmatrix[4] = m02;
            flatmatrix[5] = m12;
        }
    }

    /**
     * Returns the {@code m00} element of the 3x3 affine transformation matrix.
     * This matrix factor determines how input X coordinates will affect output
     * X coordinates and is one element of the scale of the transform.
     * To measure the full amount by which X coordinates are stretched or
     * contracted by this transform, use the following code:
     * <pre>
     *     Point2D p = new Point2D.Double(1, 0);
     *     p = tx.deltaTransform(p, p);
     *     double scaleX = p.distance(0, 0);
     * </pre>
     * @return a double value that is {@code m00} element of the
     *         3x3 affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getScaleX() {
        return m00;
    }

    /**
     * Returns the {@code m11} element of the 3x3 affine transformation matrix.
     * This matrix factor determines how input Y coordinates will affect output
     * Y coordinates and is one element of the scale of the transform.
     * To measure the full amount by which Y coordinates are stretched or
     * contracted by this transform, use the following code:
     * <pre>
     *     Point2D p = new Point2D.Double(0, 1);
     *     p = tx.deltaTransform(p, p);
     *     double scaleY = p.distance(0, 0);
     * </pre>
     * @return a double value that is {@code m11} element of the
     *         3x3 affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getScaleY() {
        return m11;
    }

    /**
     * Returns the X coordinate shearing element (m01) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the X coordinate of the shearing
     *  element of the affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getShearX() {
        return m01;
    }

    /**
     * Returns the Y coordinate shearing element (m10) of the 3x3
     * affine transformation matrix.
     * @return a double value that is the Y coordinate of the shearing
     *  element of the affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getShearY() {
        return m10;
    }

    /**
     * Returns the X coordinate of the translation element (m02) of the
     * 3x3 affine transformation matrix.
     * @return a double value that is the X coordinate of the translation
     *  element of the affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getTranslateX() {
        return m02;
    }

    /**
     * Returns the Y coordinate of the translation element (m12) of the
     * 3x3 affine transformation matrix.
     * @return a double value that is the Y coordinate of the translation
     *  element of the affine transformation matrix.
     * @see #getMatrix
     * @since 1.2
     */
    public double getTranslateY() {
        return m12;
    }

    /**
     * Concatenates this transform with a translation transformation.
     * This is equivalent to calling concatenate(T), where T is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   1    0    tx  ]
     *          [   0    1    ty  ]
     *          [   0    0    1   ]
     * </pre>
     * @param tx the distance by which coordinates are translated in the
     * X axis direction
     * @param ty the distance by which coordinates are translated in the
     * Y axis direction
     * @since 1.2
     */
    public void translate(double tx, double ty) {
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            m02 = tx * m00 + ty * m01 + m02;
            m12 = tx * m10 + ty * m11 + m12;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SHEAR | APPLY_SCALE;
                if (type != TYPE_UNKNOWN) {
                    type -= TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            m02 = tx * m00 + ty * m01;
            m12 = tx * m10 + ty * m11;
            if (m02 != 0.0 || m12 != 0.0) {
                state = APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            m02 = ty * m01 + m02;
            m12 = tx * m10 + m12;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SHEAR;
                if (type != TYPE_UNKNOWN) {
                    type -= TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SHEAR):
            m02 = ty * m01;
            m12 = tx * m10;
            if (m02 != 0.0 || m12 != 0.0) {
                state = APPLY_SHEAR | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            m02 = tx * m00 + m02;
            m12 = ty * m11 + m12;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SCALE;
                if (type != TYPE_UNKNOWN) {
                    type -= TYPE_TRANSLATION;
                }
            }
            return;
        case (APPLY_SCALE):
            m02 = tx * m00;
            m12 = ty * m11;
            if (m02 != 0.0 || m12 != 0.0) {
                state = APPLY_SCALE | APPLY_TRANSLATE;
                type |= TYPE_TRANSLATION;
            }
            return;
        case (APPLY_TRANSLATE):
            m02 = tx + m02;
            m12 = ty + m12;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            }
            return;
        case (APPLY_IDENTITY):
            m02 = tx;
            m12 = ty;
            if (tx != 0.0 || ty != 0.0) {
                state = APPLY_TRANSLATE;
                type = TYPE_TRANSLATION;
            }
            return;
        }
    }

    // Utility methods to optimize rotate methods.
    // These tables translate the flags during predictable quadrant
    // rotations where the shear and scale values are swapped and negated.
    private static final int[] rot90conversion = {
        /* IDENTITY => */        APPLY_SHEAR,
        /* TRANSLATE (TR) => */  APPLY_SHEAR | APPLY_TRANSLATE,
        /* SCALE (SC) => */      APPLY_SHEAR,
        /* SC | TR => */         APPLY_SHEAR | APPLY_TRANSLATE,
        /* SHEAR (SH) => */      APPLY_SCALE,
        /* SH | TR => */         APPLY_SCALE | APPLY_TRANSLATE,
        /* SH | SC => */         APPLY_SHEAR | APPLY_SCALE,
        /* SH | SC | TR => */    APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE,
    };
    private void rotate90() {
        double M0 = m00;
        m00 = m01;
        m01 = -M0;
        M0 = m10;
        m10 = m11;
        m11 = -M0;
        int state = rot90conversion[this.state];
        if ((state & (APPLY_SHEAR | APPLY_SCALE)) == APPLY_SCALE &&
            m00 == 1.0 && m11 == 1.0)
        {
            state -= APPLY_SCALE;
        }
        this.state = state;
        type = TYPE_UNKNOWN;
    }
    private void rotate180() {
        m00 = -m00;
        m11 = -m11;
        int state = this.state;
        if ((state & (APPLY_SHEAR)) != 0) {
            // If there was a shear, then this rotation has no
            // effect on the state.
            m01 = -m01;
            m10 = -m10;
        } else {
            // No shear means the SCALE state may toggle when
            // m00 and m11 are negated.
            if (m00 == 1.0 && m11 == 1.0) {
                this.state = state & ~APPLY_SCALE;
            } else {
                this.state = state | APPLY_SCALE;
            }
        }
        type = TYPE_UNKNOWN;
    }
    private void rotate270() {
        double M0 = m00;
        m00 = -m01;
        m01 = M0;
        M0 = m10;
        m10 = -m11;
        m11 = M0;
        int state = rot90conversion[this.state];
        if ((state & (APPLY_SHEAR | APPLY_SCALE)) == APPLY_SCALE &&
            m00 == 1.0 && m11 == 1.0)
        {
            state -= APPLY_SCALE;
        }
        this.state = state;
        type = TYPE_UNKNOWN;
    }

    /**
     * Concatenates this transform with a rotation transformation.
     * This is equivalent to calling concatenate(R), where R is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   cos(theta)    -sin(theta)    0   ]
     *          [   sin(theta)     cos(theta)    0   ]
     *          [       0              0         1   ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     * @param theta the angle of rotation measured in radians
     * @since 1.2
     */
    public void rotate(double theta) {
        double sin = Math.sin(theta);
        if (sin == 1.0) {
            rotate90();
        } else if (sin == -1.0) {
            rotate270();
        } else {
            double cos = Math.cos(theta);
            if (cos == -1.0) {
                rotate180();
            } else if (cos != 1.0) {
                double M0, M1;
                M0 = m00;
                M1 = m01;
                m00 =  cos * M0 + sin * M1;
                m01 = -sin * M0 + cos * M1;
                M0 = m10;
                M1 = m11;
                m10 =  cos * M0 + sin * M1;
                m11 = -sin * M0 + cos * M1;
                updateState();
            }
        }
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates around an anchor point.
     * This operation is equivalent to translating the coordinates so
     * that the anchor point is at the origin (S1), then rotating them
     * about the new origin (S2), and finally translating so that the
     * intermediate origin is restored to the coordinates of the original
     * anchor point (S3).
     * <p>
     * This operation is equivalent to the following sequence of calls:
     * <pre>
     *     translate(anchorx, anchory);      // S3: final translation
     *     rotate(theta);                    // S2: rotate around anchor
     *     translate(-anchorx, -anchory);    // S1: translate anchor to origin
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     *
     * @param theta the angle of rotation measured in radians
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.2
     */
    public void rotate(double theta, double anchorx, double anchory) {
        // REMIND: Simple for now - optimize later
        translate(anchorx, anchory);
        rotate(theta);
        translate(-anchorx, -anchory);
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates according to a rotation vector.
     * All coordinates rotate about the origin by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * no additional rotation is added to this transform.
     * This operation is equivalent to calling:
     * <pre>
     *          rotate(Math.atan2(vecy, vecx));
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @since 1.6
     */
    public void rotate(double vecx, double vecy) {
        if (vecy == 0.0) {
            if (vecx < 0.0) {
                rotate180();
            }
            // If vecx > 0.0 - no rotation
            // If vecx == 0.0 - undefined rotation - treat as no rotation
        } else if (vecx == 0.0) {
            if (vecy > 0.0) {
                rotate90();
            } else {  // vecy must be < 0.0
                rotate270();
            }
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            double sin = vecy / len;
            double cos = vecx / len;
            double M0, M1;
            M0 = m00;
            M1 = m01;
            m00 =  cos * M0 + sin * M1;
            m01 = -sin * M0 + cos * M1;
            M0 = m10;
            M1 = m11;
            m10 =  cos * M0 + sin * M1;
            m11 = -sin * M0 + cos * M1;
            updateState();
        }
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates around an anchor point according to a rotation
     * vector.
     * All coordinates rotate about the specified anchor coordinates
     * by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * the transform is not modified in any way.
     * This method is equivalent to calling:
     * <pre>
     *     rotate(Math.atan2(vecy, vecx), anchorx, anchory);
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.6
     */
    public void rotate(double vecx, double vecy,
                       double anchorx, double anchory)
    {
        // REMIND: Simple for now - optimize later
        translate(anchorx, anchory);
        rotate(vecx, vecy);
        translate(-anchorx, -anchory);
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates by the specified number of quadrants.
     * This is equivalent to calling:
     * <pre>
     *     rotate(numquadrants * Math.PI / 2.0);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @since 1.6
     */
    public void quadrantRotate(int numquadrants) {
        switch (numquadrants & 3) {
        case 0:
            break;
        case 1:
            rotate90();
            break;
        case 2:
            rotate180();
            break;
        case 3:
            rotate270();
            break;
        }
    }

    /**
     * Concatenates this transform with a transform that rotates
     * coordinates by the specified number of quadrants around
     * the specified anchor point.
     * This method is equivalent to calling:
     * <pre>
     *     rotate(numquadrants * Math.PI / 2.0, anchorx, anchory);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.6
     */
    public void quadrantRotate(int numquadrants,
                               double anchorx, double anchory)
    {
        switch (numquadrants & 3) {
        case 0:
            return;
        case 1:
            m02 += anchorx * (m00 - m01) + anchory * (m01 + m00);
            m12 += anchorx * (m10 - m11) + anchory * (m11 + m10);
            rotate90();
            break;
        case 2:
            m02 += anchorx * (m00 + m00) + anchory * (m01 + m01);
            m12 += anchorx * (m10 + m10) + anchory * (m11 + m11);
            rotate180();
            break;
        case 3:
            m02 += anchorx * (m00 + m01) + anchory * (m01 - m00);
            m12 += anchorx * (m10 + m11) + anchory * (m11 - m10);
            rotate270();
            break;
        }
        if (m02 == 0.0 && m12 == 0.0) {
            state &= ~APPLY_TRANSLATE;
        } else {
            state |= APPLY_TRANSLATE;
        }
    }

    /**
     * Concatenates this transform with a scaling transformation.
     * This is equivalent to calling concatenate(S), where S is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   sx   0    0   ]
     *          [   0    sy   0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param sx the factor by which coordinates are scaled along the
     * X axis direction
     * @param sy the factor by which coordinates are scaled along the
     * Y axis direction
     * @since 1.2
     */
    @SuppressWarnings("fallthrough")
    public void scale(double sx, double sy) {
        int state = this.state;
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            m00 *= sx;
            m11 *= sy;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            m01 *= sy;
            m10 *= sx;
            if (m01 == 0 && m10 == 0) {
                state &= APPLY_TRANSLATE;
                if (m00 == 1.0 && m11 == 1.0) {
                    this.type = (state == APPLY_IDENTITY
                                 ? TYPE_IDENTITY
                                 : TYPE_TRANSLATION);
                } else {
                    state |= APPLY_SCALE;
                    this.type = TYPE_UNKNOWN;
                }
                this.state = state;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            m00 *= sx;
            m11 *= sy;
            if (m00 == 1.0 && m11 == 1.0) {
                this.state = (state &= APPLY_TRANSLATE);
                this.type = (state == APPLY_IDENTITY
                             ? TYPE_IDENTITY
                             : TYPE_TRANSLATION);
            } else {
                this.type = TYPE_UNKNOWN;
            }
            return;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            m00 = sx;
            m11 = sy;
            if (sx != 1.0 || sy != 1.0) {
                this.state = state | APPLY_SCALE;
                this.type = TYPE_UNKNOWN;
            }
            return;
        }
    }

    /**
     * Concatenates this transform with a shearing transformation.
     * This is equivalent to calling concatenate(SH), where SH is an
     * {@code AffineTransform} represented by the following matrix:
     * <pre>
     *          [   1   shx   0   ]
     *          [  shy   1    0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate
     * @since 1.2
     */
    public void shear(double shx, double shy) {
        int state = this.state;
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            double M0, M1;
            M0 = m00;
            M1 = m01;
            m00 = M0 + M1 * shy;
            m01 = M0 * shx + M1;

            M0 = m10;
            M1 = m11;
            m10 = M0 + M1 * shy;
            m11 = M0 * shx + M1;
            updateState();
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            m00 = m01 * shy;
            m11 = m10 * shx;
            if (m00 != 0.0 || m11 != 0.0) {
                this.state = state | APPLY_SCALE;
            }
            this.type = TYPE_UNKNOWN;
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            m01 = m00 * shx;
            m10 = m11 * shy;
            if (m01 != 0.0 || m10 != 0.0) {
                this.state = state | APPLY_SHEAR;
            }
            this.type = TYPE_UNKNOWN;
            return;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            m01 = shx;
            m10 = shy;
            if (m01 != 0.0 || m10 != 0.0) {
                this.state = state | APPLY_SCALE | APPLY_SHEAR;
                this.type = TYPE_UNKNOWN;
            }
            return;
        }
    }

    /**
     * Resets this transform to the Identity transform.
     * @since 1.2
     */
    public void setToIdentity() {
        m00 = m11 = 1.0;
        m10 = m01 = m02 = m12 = 0.0;
        state = APPLY_IDENTITY;
        type = TYPE_IDENTITY;
    }

    /**
     * Sets this transform to a translation transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *          [   1    0    tx  ]
     *          [   0    1    ty  ]
     *          [   0    0    1   ]
     * </pre>
     * @param tx the distance by which coordinates are translated in the
     * X axis direction
     * @param ty the distance by which coordinates are translated in the
     * Y axis direction
     * @since 1.2
     */
    public void setToTranslation(double tx, double ty) {
        m00 = 1.0;
        m10 = 0.0;
        m01 = 0.0;
        m11 = 1.0;
        m02 = tx;
        m12 = ty;
        if (tx != 0.0 || ty != 0.0) {
            state = APPLY_TRANSLATE;
            type = TYPE_TRANSLATION;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    /**
     * Sets this transform to a rotation transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *          [   cos(theta)    -sin(theta)    0   ]
     *          [   sin(theta)     cos(theta)    0   ]
     *          [       0              0         1   ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     * @param theta the angle of rotation measured in radians
     * @since 1.2
     */
    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos;
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
        } else {
            cos = Math.cos(theta);
            if (cos == -1.0) {
                sin = 0.0;
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
            } else if (cos == 1.0) {
                sin = 0.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            } else {
                state = APPLY_SHEAR | APPLY_SCALE;
                type = TYPE_GENERAL_ROTATION;
            }
        }
        m00 =  cos;
        m10 =  sin;
        m01 = -sin;
        m11 =  cos;
        m02 =  0.0;
        m12 =  0.0;
    }

    /**
     * Sets this transform to a translated rotation transformation.
     * This operation is equivalent to translating the coordinates so
     * that the anchor point is at the origin (S1), then rotating them
     * about the new origin (S2), and finally translating so that the
     * intermediate origin is restored to the coordinates of the original
     * anchor point (S3).
     * <p>
     * This operation is equivalent to the following sequence of calls:
     * <pre>
     *     setToTranslation(anchorx, anchory); // S3: final translation
     *     rotate(theta);                      // S2: rotate around anchor
     *     translate(-anchorx, -anchory);      // S1: translate anchor to origin
     * </pre>
     * The matrix representing this transform becomes:
     * <pre>
     *          [   cos(theta)    -sin(theta)    x-x*cos+y*sin  ]
     *          [   sin(theta)     cos(theta)    y-x*sin-y*cos  ]
     *          [       0              0               1        ]
     * </pre>
     * Rotating by a positive angle theta rotates points on the positive
     * X axis toward the positive Y axis.
     * Note also the discussion of
     * <a href="#quadrantapproximation">Handling 90-Degree Rotations</a>
     * above.
     *
     * @param theta the angle of rotation measured in radians
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.2
     */
    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
        if (m02 != 0.0 || m12 != 0.0) {
            state |= APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
        }
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates according to a rotation vector.
     * All coordinates rotate about the origin by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * the transform is set to an identity transform.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(Math.atan2(vecy, vecx));
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @since 1.6
     */
    public void setToRotation(double vecx, double vecy) {
        double sin, cos;
        if (vecy == 0) {
            sin = 0.0;
            if (vecx < 0.0) {
                cos = -1.0;
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
            } else {
                cos = 1.0;
                state = APPLY_IDENTITY;
                type = TYPE_IDENTITY;
            }
        } else if (vecx == 0) {
            cos = 0.0;
            sin = (vecy > 0.0) ? 1.0 : -1.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            cos = vecx / len;
            sin = vecy / len;
            state = APPLY_SHEAR | APPLY_SCALE;
            type = TYPE_GENERAL_ROTATION;
        }
        m00 =  cos;
        m10 =  sin;
        m01 = -sin;
        m11 =  cos;
        m02 =  0.0;
        m12 =  0.0;
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates around an anchor point according to a rotation
     * vector.
     * All coordinates rotate about the specified anchor coordinates
     * by the same amount.
     * The amount of rotation is such that coordinates along the former
     * positive X axis will subsequently align with the vector pointing
     * from the origin to the specified vector coordinates.
     * If both {@code vecx} and {@code vecy} are 0.0,
     * the transform is set to an identity transform.
     * This operation is equivalent to calling:
     * <pre>
     *     setToTranslation(Math.atan2(vecy, vecx), anchorx, anchory);
     * </pre>
     *
     * @param vecx the X coordinate of the rotation vector
     * @param vecy the Y coordinate of the rotation vector
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.6
     */
    public void setToRotation(double vecx, double vecy,
                              double anchorx, double anchory)
    {
        setToRotation(vecx, vecy);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
        if (m02 != 0.0 || m12 != 0.0) {
            state |= APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
        }
    }

    /**
     * Sets this transform to a rotation transformation that rotates
     * coordinates by the specified number of quadrants.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(numquadrants * Math.PI / 2.0);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @since 1.6
     */
    public void setToQuadrantRotation(int numquadrants) {
        switch (numquadrants & 3) {
        case 0:
            m00 =  1.0;
            m10 =  0.0;
            m01 =  0.0;
            m11 =  1.0;
            m02 =  0.0;
            m12 =  0.0;
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
            break;
        case 1:
            m00 =  0.0;
            m10 =  1.0;
            m01 = -1.0;
            m11 =  0.0;
            m02 =  0.0;
            m12 =  0.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
            break;
        case 2:
            m00 = -1.0;
            m10 =  0.0;
            m01 =  0.0;
            m11 = -1.0;
            m02 =  0.0;
            m12 =  0.0;
            state = APPLY_SCALE;
            type = TYPE_QUADRANT_ROTATION;
            break;
        case 3:
            m00 =  0.0;
            m10 = -1.0;
            m01 =  1.0;
            m11 =  0.0;
            m02 =  0.0;
            m12 =  0.0;
            state = APPLY_SHEAR;
            type = TYPE_QUADRANT_ROTATION;
            break;
        }
    }

    /**
     * Sets this transform to a translated rotation transformation
     * that rotates coordinates by the specified number of quadrants
     * around the specified anchor point.
     * This operation is equivalent to calling:
     * <pre>
     *     setToRotation(numquadrants * Math.PI / 2.0, anchorx, anchory);
     * </pre>
     * Rotating by a positive number of quadrants rotates points on
     * the positive X axis toward the positive Y axis.
     *
     * @param numquadrants the number of 90 degree arcs to rotate by
     * @param anchorx the X coordinate of the rotation anchor point
     * @param anchory the Y coordinate of the rotation anchor point
     * @since 1.6
     */
    public void setToQuadrantRotation(int numquadrants,
                                      double anchorx, double anchory)
    {
        switch (numquadrants & 3) {
        case 0:
            m00 =  1.0;
            m10 =  0.0;
            m01 =  0.0;
            m11 =  1.0;
            m02 =  0.0;
            m12 =  0.0;
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
            break;
        case 1:
            m00 =  0.0;
            m10 =  1.0;
            m01 = -1.0;
            m11 =  0.0;
            m02 =  anchorx + anchory;
            m12 =  anchory - anchorx;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SHEAR;
                type = TYPE_QUADRANT_ROTATION;
            } else {
                state = APPLY_SHEAR | APPLY_TRANSLATE;
                type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
            }
            break;
        case 2:
            m00 = -1.0;
            m10 =  0.0;
            m01 =  0.0;
            m11 = -1.0;
            m02 =  anchorx + anchorx;
            m12 =  anchory + anchory;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SCALE;
                type = TYPE_QUADRANT_ROTATION;
            } else {
                state = APPLY_SCALE | APPLY_TRANSLATE;
                type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
            }
            break;
        case 3:
            m00 =  0.0;
            m10 = -1.0;
            m01 =  1.0;
            m11 =  0.0;
            m02 =  anchorx - anchory;
            m12 =  anchory + anchorx;
            if (m02 == 0.0 && m12 == 0.0) {
                state = APPLY_SHEAR;
                type = TYPE_QUADRANT_ROTATION;
            } else {
                state = APPLY_SHEAR | APPLY_TRANSLATE;
                type = TYPE_QUADRANT_ROTATION | TYPE_TRANSLATION;
            }
            break;
        }
    }

    /**
     * Sets this transform to a scaling transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *          [   sx   0    0   ]
     *          [   0    sy   0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param sx the factor by which coordinates are scaled along the
     * X axis direction
     * @param sy the factor by which coordinates are scaled along the
     * Y axis direction
     * @since 1.2
     */
    public void setToScale(double sx, double sy) {
        m00 = sx;
        m10 = 0.0;
        m01 = 0.0;
        m11 = sy;
        m02 = 0.0;
        m12 = 0.0;
        if (sx != 1.0 || sy != 1.0) {
            state = APPLY_SCALE;
            type = TYPE_UNKNOWN;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    /**
     * Sets this transform to a shearing transformation.
     * The matrix representing this transform becomes:
     * <pre>
     *          [   1   shx   0   ]
     *          [  shy   1    0   ]
     *          [   0    0    1   ]
     * </pre>
     * @param shx the multiplier by which coordinates are shifted in the
     * direction of the positive X axis as a factor of their Y coordinate
     * @param shy the multiplier by which coordinates are shifted in the
     * direction of the positive Y axis as a factor of their X coordinate
     * @since 1.2
     */
    public void setToShear(double shx, double shy) {
        m00 = 1.0;
        m01 = shx;
        m10 = shy;
        m11 = 1.0;
        m02 = 0.0;
        m12 = 0.0;
        if (shx != 0.0 || shy != 0.0) {
            state = (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
        } else {
            state = APPLY_IDENTITY;
            type = TYPE_IDENTITY;
        }
    }

    /**
     * Sets this transform to a copy of the transform in the specified
     * {@code AffineTransform} object.
     * @param Tx the {@code AffineTransform} object from which to
     * copy the transform
     * @since 1.2
     */
    public void setTransform(AffineTransform Tx) {
        this.m00 = Tx.m00;
        this.m10 = Tx.m10;
        this.m01 = Tx.m01;
        this.m11 = Tx.m11;
        this.m02 = Tx.m02;
        this.m12 = Tx.m12;
        this.state = Tx.state;
        this.type = Tx.type;
    }

    /**
     * Sets this transform to the matrix specified by the 6
     * double precision values.
     *
     * @param m00 the X coordinate scaling element of the 3x3 matrix
     * @param m10 the Y coordinate shearing element of the 3x3 matrix
     * @param m01 the X coordinate shearing element of the 3x3 matrix
     * @param m11 the Y coordinate scaling element of the 3x3 matrix
     * @param m02 the X coordinate translation element of the 3x3 matrix
     * @param m12 the Y coordinate translation element of the 3x3 matrix
     * @since 1.2
     */
    public void setTransform(double m00, double m10,
                             double m01, double m11,
                             double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        updateState();
    }

    /**
     * Concatenates an {@code AffineTransform Tx} to
     * this {@code AffineTransform} Cx in the most commonly useful
     * way to provide a new user space
     * that is mapped to the former user space by {@code Tx}.
     * Cx is updated to perform the combined transformation.
     * Transforming a point p by the updated transform Cx' is
     * equivalent to first transforming p by {@code Tx} and then
     * transforming the result by the original transform Cx like this:
     * Cx'(p) = Cx(Tx(p))
     * In matrix notation, if this transform Cx is
     * represented by the matrix [this] and {@code Tx} is represented
     * by the matrix [Tx] then this method does the following:
     * <pre>
     *          [this] = [this] x [Tx]
     * </pre>
     * @param Tx the {@code AffineTransform} object to be
     * concatenated with this {@code AffineTransform} object.
     * @see #preConcatenate
     * @since 1.2
     */
    @SuppressWarnings("fallthrough")
    public void concatenate(AffineTransform Tx) {
        double M0, M1;
        double T00, T01, T10, T11;
        double T02, T12;
        int mystate = state;
        int txstate = Tx.state;
        switch ((txstate << HI_SHIFT) | mystate) {

            /* ---------- Tx == IDENTITY cases ---------- */
        case (HI_IDENTITY | APPLY_IDENTITY):
        case (HI_IDENTITY | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            return;

            /* ---------- this == IDENTITY cases ---------- */
        case (HI_SHEAR | HI_SCALE | HI_TRANSLATE | APPLY_IDENTITY):
            m01 = Tx.m01;
            m10 = Tx.m10;
            /* NOBREAK */
        case (HI_SCALE | HI_TRANSLATE | APPLY_IDENTITY):
            m00 = Tx.m00;
            m11 = Tx.m11;
            /* NOBREAK */
        case (HI_TRANSLATE | APPLY_IDENTITY):
            m02 = Tx.m02;
            m12 = Tx.m12;
            state = txstate;
            type = Tx.type;
            return;
        case (HI_SHEAR | HI_SCALE | APPLY_IDENTITY):
            m01 = Tx.m01;
            m10 = Tx.m10;
            /* NOBREAK */
        case (HI_SCALE | APPLY_IDENTITY):
            m00 = Tx.m00;
            m11 = Tx.m11;
            state = txstate;
            type = Tx.type;
            return;
        case (HI_SHEAR | HI_TRANSLATE | APPLY_IDENTITY):
            m02 = Tx.m02;
            m12 = Tx.m12;
            /* NOBREAK */
        case (HI_SHEAR | APPLY_IDENTITY):
            m01 = Tx.m01;
            m10 = Tx.m10;
            m00 = m11 = 0.0;
            state = txstate;
            type = Tx.type;
            return;

            /* ---------- Tx == TRANSLATE cases ---------- */
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR):
        case (HI_TRANSLATE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SCALE):
        case (HI_TRANSLATE | APPLY_TRANSLATE):
            translate(Tx.m02, Tx.m12);
            return;

            /* ---------- Tx == SCALE cases ---------- */
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR):
        case (HI_SCALE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SCALE):
        case (HI_SCALE | APPLY_TRANSLATE):
            scale(Tx.m00, Tx.m11);
            return;

            /* ---------- Tx == SHEAR cases ---------- */
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE):
            T01 = Tx.m01; T10 = Tx.m10;
            M0 = m00;
            m00 = m01 * T10;
            m01 = M0 * T01;
            M0 = m10;
            m10 = m11 * T10;
            m11 = M0 * T01;
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR):
            m00 = m01 * Tx.m10;
            m01 = 0.0;
            m11 = m10 * Tx.m01;
            m10 = 0.0;
            state = mystate ^ (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SCALE):
            m01 = m00 * Tx.m01;
            m00 = 0.0;
            m10 = m11 * Tx.m10;
            m11 = 0.0;
            state = mystate ^ (APPLY_SHEAR | APPLY_SCALE);
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_TRANSLATE):
            m00 = 0.0;
            m01 = Tx.m01;
            m10 = Tx.m10;
            m11 = 0.0;
            state = APPLY_TRANSLATE | APPLY_SHEAR;
            type = TYPE_UNKNOWN;
            return;
        }
        // If Tx has more than one attribute, it is not worth optimizing
        // all of those cases...
        T00 = Tx.m00; T01 = Tx.m01; T02 = Tx.m02;
        T10 = Tx.m10; T11 = Tx.m11; T12 = Tx.m12;
        switch (mystate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE):
            state = mystate | txstate;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M0 = m00;
            M1 = m01;
            m00  = T00 * M0 + T10 * M1;
            m01  = T01 * M0 + T11 * M1;
            m02 += T02 * M0 + T12 * M1;

            M0 = m10;
            M1 = m11;
            m10  = T00 * M0 + T10 * M1;
            m11  = T01 * M0 + T11 * M1;
            m12 += T02 * M0 + T12 * M1;
            type = TYPE_UNKNOWN;
            return;

        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            M0 = m01;
            m00  = T10 * M0;
            m01  = T11 * M0;
            m02 += T12 * M0;

            M0 = m10;
            m10  = T00 * M0;
            m11  = T01 * M0;
            m12 += T02 * M0;
            break;

        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            M0 = m00;
            m00  = T00 * M0;
            m01  = T01 * M0;
            m02 += T02 * M0;

            M0 = m11;
            m10  = T10 * M0;
            m11  = T11 * M0;
            m12 += T12 * M0;
            break;

        case (APPLY_TRANSLATE):
            m00  = T00;
            m01  = T01;
            m02 += T02;

            m10  = T10;
            m11  = T11;
            m12 += T12;
            state = txstate | APPLY_TRANSLATE;
            type = TYPE_UNKNOWN;
            return;
        }
        updateState();
    }

    /**
     * Concatenates an {@code AffineTransform Tx} to
     * this {@code AffineTransform} Cx
     * in a less commonly used way such that {@code Tx} modifies the
     * coordinate transformation relative to the absolute pixel
     * space rather than relative to the existing user space.
     * Cx is updated to perform the combined transformation.
     * Transforming a point p by the updated transform Cx' is
     * equivalent to first transforming p by the original transform
     * Cx and then transforming the result by
     * {@code Tx} like this:
     * Cx'(p) = Tx(Cx(p))
     * In matrix notation, if this transform Cx
     * is represented by the matrix [this] and {@code Tx} is
     * represented by the matrix [Tx] then this method does the
     * following:
     * <pre>
     *          [this] = [Tx] x [this]
     * </pre>
     * @param Tx the {@code AffineTransform} object to be
     * concatenated with this {@code AffineTransform} object.
     * @see #concatenate
     * @since 1.2
     */
    @SuppressWarnings("fallthrough")
    public void preConcatenate(AffineTransform Tx) {
        double M0, M1;
        double T00, T01, T10, T11;
        double T02, T12;
        int mystate = state;
        int txstate = Tx.state;
        switch ((txstate << HI_SHIFT) | mystate) {
        case (HI_IDENTITY | APPLY_IDENTITY):
        case (HI_IDENTITY | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE):
        case (HI_IDENTITY | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            // Tx is IDENTITY...
            return;

        case (HI_TRANSLATE | APPLY_IDENTITY):
        case (HI_TRANSLATE | APPLY_SCALE):
        case (HI_TRANSLATE | APPLY_SHEAR):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE):
            // Tx is TRANSLATE, this has no TRANSLATE
            m02 = Tx.m02;
            m12 = Tx.m12;
            state = mystate | APPLY_TRANSLATE;
            type |= TYPE_TRANSLATION;
            return;

        case (HI_TRANSLATE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_TRANSLATE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            // Tx is TRANSLATE, this has one too
            m02 = m02 + Tx.m02;
            m12 = m12 + Tx.m12;
            return;

        case (HI_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_IDENTITY):
            // Only these two existing states need a new state
            state = mystate | APPLY_SCALE;
            /* NOBREAK */
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_SCALE):
        case (HI_SCALE | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SHEAR):
        case (HI_SCALE | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SCALE | APPLY_SCALE):
            // Tx is SCALE, this is anything
            T00 = Tx.m00;
            T11 = Tx.m11;
            if ((mystate & APPLY_SHEAR) != 0) {
                m01 = m01 * T00;
                m10 = m10 * T11;
                if ((mystate & APPLY_SCALE) != 0) {
                    m00 = m00 * T00;
                    m11 = m11 * T11;
                }
            } else {
                m00 = m00 * T00;
                m11 = m11 * T11;
            }
            if ((mystate & APPLY_TRANSLATE) != 0) {
                m02 = m02 * T00;
                m12 = m12 * T11;
            }
            type = TYPE_UNKNOWN;
            return;
        case (HI_SHEAR | APPLY_SHEAR | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR):
            mystate = mystate | APPLY_SCALE;
            /* NOBREAK */
        case (HI_SHEAR | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_IDENTITY):
        case (HI_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SCALE):
            state = mystate ^ APPLY_SHEAR;
            /* NOBREAK */
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (HI_SHEAR | APPLY_SHEAR | APPLY_SCALE):
            // Tx is SHEAR, this is anything
            T01 = Tx.m01;
            T10 = Tx.m10;

            M0 = m00;
            m00 = m10 * T01;
            m10 = M0 * T10;

            M0 = m01;
            m01 = m11 * T01;
            m11 = M0 * T10;

            M0 = m02;
            m02 = m12 * T01;
            m12 = M0 * T10;
            type = TYPE_UNKNOWN;
            return;
        }
        // If Tx has more than one attribute, it is not worth optimizing
        // all of those cases...
        T00 = Tx.m00; T01 = Tx.m01; T02 = Tx.m02;
        T10 = Tx.m10; T11 = Tx.m11; T12 = Tx.m12;
        switch (mystate) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M0 = m02;
            M1 = m12;
            T02 += M0 * T00 + M1 * T01;
            T12 += M0 * T10 + M1 * T11;

            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            m02 = T02;
            m12 = T12;

            M0 = m00;
            M1 = m10;
            m00 = M0 * T00 + M1 * T01;
            m10 = M0 * T10 + M1 * T11;

            M0 = m01;
            M1 = m11;
            m01 = M0 * T00 + M1 * T01;
            m11 = M0 * T10 + M1 * T11;
            break;

        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M0 = m02;
            M1 = m12;
            T02 += M0 * T00 + M1 * T01;
            T12 += M0 * T10 + M1 * T11;

            /* NOBREAK */
        case (APPLY_SHEAR):
            m02 = T02;
            m12 = T12;

            M0 = m10;
            m00 = M0 * T01;
            m10 = M0 * T11;

            M0 = m01;
            m01 = M0 * T00;
            m11 = M0 * T10;
            break;

        case (APPLY_SCALE | APPLY_TRANSLATE):
            M0 = m02;
            M1 = m12;
            T02 += M0 * T00 + M1 * T01;
            T12 += M0 * T10 + M1 * T11;

            /* NOBREAK */
        case (APPLY_SCALE):
            m02 = T02;
            m12 = T12;

            M0 = m00;
            m00 = M0 * T00;
            m10 = M0 * T10;

            M0 = m11;
            m01 = M0 * T01;
            m11 = M0 * T11;
            break;

        case (APPLY_TRANSLATE):
            M0 = m02;
            M1 = m12;
            T02 += M0 * T00 + M1 * T01;
            T12 += M0 * T10 + M1 * T11;

            /* NOBREAK */
        case (APPLY_IDENTITY):
            m02 = T02;
            m12 = T12;

            m00 = T00;
            m10 = T10;

            m01 = T01;
            m11 = T11;

            state = mystate | txstate;
            type = TYPE_UNKNOWN;
            return;
        }
        updateState();
    }

    /**
     * Returns an {@code AffineTransform} object representing the
     * inverse transformation.
     * The inverse transform Tx' of this transform Tx
     * maps coordinates transformed by Tx back
     * to their original coordinates.
     * In other words, Tx'(Tx(p)) = p = Tx(Tx'(p)).
     * <p>
     * If this transform maps all coordinates onto a point or a line
     * then it will not have an inverse, since coordinates that do
     * not lie on the destination point or line will not have an inverse
     * mapping.
     * The {@code getDeterminant} method can be used to determine if this
     * transform has no inverse, in which case an exception will be
     * thrown if the {@code createInverse} method is called.
     * @return a new {@code AffineTransform} object representing the
     * inverse transformation.
     * @see #getDeterminant
     * @exception NoninvertibleTransformException
     * if the matrix cannot be inverted.
     * @since 1.2
     */
    public AffineTransform createInverse()
        throws NoninvertibleTransformException
    {
        double det;
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return null;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            det = m00 * m11 - m01 * m10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            return new AffineTransform( m11 / det, -m10 / det,
                                       -m01 / det,  m00 / det,
                                       (m01 * m12 - m11 * m02) / det,
                                       (m10 * m02 - m00 * m12) / det,
                                       (APPLY_SHEAR |
                                        APPLY_SCALE |
                                        APPLY_TRANSLATE));
        case (APPLY_SHEAR | APPLY_SCALE):
            det = m00 * m11 - m01 * m10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            return new AffineTransform( m11 / det, -m10 / det,
                                       -m01 / det,  m00 / det,
                                        0.0,        0.0,
                                       (APPLY_SHEAR | APPLY_SCALE));
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            if (m01 == 0.0 || m10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            return new AffineTransform( 0.0,        1.0 / m01,
                                        1.0 / m10,  0.0,
                                       -m12 / m10, -m02 / m01,
                                       (APPLY_SHEAR | APPLY_TRANSLATE));
        case (APPLY_SHEAR):
            if (m01 == 0.0 || m10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            return new AffineTransform(0.0,       1.0 / m01,
                                       1.0 / m10, 0.0,
                                       0.0,       0.0,
                                       (APPLY_SHEAR));
        case (APPLY_SCALE | APPLY_TRANSLATE):
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            return new AffineTransform( 1.0 / m00,  0.0,
                                        0.0,        1.0 / m11,
                                       -m02 / m00, -m12 / m11,
                                       (APPLY_SCALE | APPLY_TRANSLATE));
        case (APPLY_SCALE):
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            return new AffineTransform(1.0 / m00, 0.0,
                                       0.0,       1.0 / m11,
                                       0.0,       0.0,
                                       (APPLY_SCALE));
        case (APPLY_TRANSLATE):
            return new AffineTransform( 1.0,  0.0,
                                        0.0,  1.0,
                                       -m02, -m12,
                                       (APPLY_TRANSLATE));
        case (APPLY_IDENTITY):
            return new AffineTransform();
        }

        /* NOTREACHED */
    }

    /**
     * Sets this transform to the inverse of itself.
     * The inverse transform Tx' of this transform Tx
     * maps coordinates transformed by Tx back
     * to their original coordinates.
     * In other words, Tx'(Tx(p)) = p = Tx(Tx'(p)).
     * <p>
     * If this transform maps all coordinates onto a point or a line
     * then it will not have an inverse, since coordinates that do
     * not lie on the destination point or line will not have an inverse
     * mapping.
     * The {@code getDeterminant} method can be used to determine if this
     * transform has no inverse, in which case an exception will be
     * thrown if the {@code invert} method is called.
     * @see #getDeterminant
     * @exception NoninvertibleTransformException
     * if the matrix cannot be inverted.
     * @since 1.6
     */
    public void invert()
        throws NoninvertibleTransformException
    {
        double M00, M01, M02;
        double M10, M11, M12;
        double det;
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            det = M00 * M11 - M01 * M10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            m00 =  M11 / det;
            m10 = -M10 / det;
            m01 = -M01 / det;
            m11 =  M00 / det;
            m02 = (M01 * M12 - M11 * M02) / det;
            m12 = (M10 * M02 - M00 * M12) / det;
            break;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            det = M00 * M11 - M01 * M10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            m00 =  M11 / det;
            m10 = -M10 / det;
            m01 = -M01 / det;
            m11 =  M00 / det;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            if (M01 == 0.0 || M10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            // m00 = 0.0;
            m10 = 1.0 / M01;
            m01 = 1.0 / M10;
            // m11 = 0.0;
            m02 = -M12 / M10;
            m12 = -M02 / M01;
            break;
        case (APPLY_SHEAR):
            M01 = m01;
            M10 = m10;
            if (M01 == 0.0 || M10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            // m00 = 0.0;
            m10 = 1.0 / M01;
            m01 = 1.0 / M10;
            // m11 = 0.0;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            if (M00 == 0.0 || M11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            m00 = 1.0 / M00;
            // m10 = 0.0;
            // m01 = 0.0;
            m11 = 1.0 / M11;
            m02 = -M02 / M00;
            m12 = -M12 / M11;
            break;
        case (APPLY_SCALE):
            M00 = m00;
            M11 = m11;
            if (M00 == 0.0 || M11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            m00 = 1.0 / M00;
            // m10 = 0.0;
            // m01 = 0.0;
            m11 = 1.0 / M11;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        case (APPLY_TRANSLATE):
            // m00 = 1.0;
            // m10 = 0.0;
            // m01 = 0.0;
            // m11 = 1.0;
            m02 = -m02;
            m12 = -m12;
            break;
        case (APPLY_IDENTITY):
            // m00 = 1.0;
            // m10 = 0.0;
            // m01 = 0.0;
            // m11 = 1.0;
            // m02 = 0.0;
            // m12 = 0.0;
            break;
        }
    }

    /**
     * Transforms the specified {@code ptSrc} and stores the result
     * in {@code ptDst}.
     * If {@code ptDst} is {@code null}, a new {@link Point2D}
     * object is allocated and then the result of the transformation is
     * stored in this object.
     * In either case, {@code ptDst}, which contains the
     * transformed point, is returned for convenience.
     * If {@code ptSrc} and {@code ptDst} are the same
     * object, the input point is correctly overwritten with
     * the transformed point.
     * @param ptSrc the specified {@code Point2D} to be transformed
     * @param ptDst the specified {@code Point2D} that stores the
     * result of transforming {@code ptSrc}
     * @return the {@code ptDst} after transforming
     * {@code ptSrc} and storing the result in {@code ptDst}.
     * @since 1.2
     */
    public Point2D transform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            if (ptSrc instanceof Point2D.Double) {
                ptDst = new Point2D.Double();
            } else {
                ptDst = new Point2D.Float();
            }
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return null;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            ptDst.setLocation(x * m00 + y * m01 + m02,
                              x * m10 + y * m11 + m12);
            return ptDst;
        case (APPLY_SHEAR | APPLY_SCALE):
            ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11);
            return ptDst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            ptDst.setLocation(y * m01 + m02, x * m10 + m12);
            return ptDst;
        case (APPLY_SHEAR):
            ptDst.setLocation(y * m01, x * m10);
            return ptDst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            ptDst.setLocation(x * m00 + m02, y * m11 + m12);
            return ptDst;
        case (APPLY_SCALE):
            ptDst.setLocation(x * m00, y * m11);
            return ptDst;
        case (APPLY_TRANSLATE):
            ptDst.setLocation(x + m02, y + m12);
            return ptDst;
        case (APPLY_IDENTITY):
            ptDst.setLocation(x, y);
            return ptDst;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of point objects by this transform.
     * If any element of the {@code ptDst} array is
     * {@code null}, a new {@code Point2D} object is allocated
     * and stored into that element before storing the results of the
     * transformation.
     * <p>
     * Note that this method does not take any precautions to
     * avoid problems caused by storing results into {@code Point2D}
     * objects that will be used as the source for calculations
     * further down the source array.
     * This method does guarantee that if a specified {@code Point2D}
     * object is both the source and destination for the same single point
     * transform operation then the results will not be stored until
     * the calculations are complete to avoid storing the results on
     * top of the operands.
     * If, however, the destination {@code Point2D} object for one
     * operation is the same object as the source {@code Point2D}
     * object for another operation further down the source array then
     * the original coordinates in that point are overwritten before
     * they can be converted.
     * @param ptSrc the array containing the source point objects
     * @param ptDst the array into which the transform point objects are
     * returned
     * @param srcOff the offset to the first point object to be
     * transformed in the source array
     * @param dstOff the offset to the location of the first
     * transformed point object that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @since 1.2
     */
    public void transform(Point2D[] ptSrc, int srcOff,
                          Point2D[] ptDst, int dstOff,
                          int numPts) {
        int state = this.state;
        while (--numPts >= 0) {
            // Copy source coords into local variables in case src == dst
            Point2D src = ptSrc[srcOff++];
            double x = src.getX();
            double y = src.getY();
            Point2D dst = ptDst[dstOff++];
            if (dst == null) {
                if (src instanceof Point2D.Double) {
                    dst = new Point2D.Double();
                } else {
                    dst = new Point2D.Float();
                }
                ptDst[dstOff - 1] = dst;
            }
            switch (state) {
            default:
                stateError();
                /* NOTREACHED */
                return;
            case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
                dst.setLocation(x * m00 + y * m01 + m02,
                                x * m10 + y * m11 + m12);
                break;
            case (APPLY_SHEAR | APPLY_SCALE):
                dst.setLocation(x * m00 + y * m01, x * m10 + y * m11);
                break;
            case (APPLY_SHEAR | APPLY_TRANSLATE):
                dst.setLocation(y * m01 + m02, x * m10 + m12);
                break;
            case (APPLY_SHEAR):
                dst.setLocation(y * m01, x * m10);
                break;
            case (APPLY_SCALE | APPLY_TRANSLATE):
                dst.setLocation(x * m00 + m02, y * m11 + m12);
                break;
            case (APPLY_SCALE):
                dst.setLocation(x * m00, y * m11);
                break;
            case (APPLY_TRANSLATE):
                dst.setLocation(x + m02, y + m12);
                break;
            case (APPLY_IDENTITY):
                dst.setLocation(x, y);
                break;
            }
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of floating point coordinates by this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are overwritten by a
     * previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @since 1.2
     */
    public void transform(float[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        double M00, M01, M02, M10, M11, M12;    // For caching
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
            // If the arrays overlap partially with the destination higher
            // than the source and we transform the coordinates normally
            // we would overwrite some of the later source coordinates
            // with results of previous transformations.
            // To get around this we use arraycopy to copy the points
            // to their final destination with correct overwrite
            // handling and then transform them in place in the new
            // safer location.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            // srcPts = dstPts;         // They are known to be equal.
            srcOff = dstOff;
        }
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M00 * x + M01 * y + M02);
                dstPts[dstOff++] = (float) (M10 * x + M11 * y + M12);
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M00 * x + M01 * y);
                dstPts[dstOff++] = (float) (M10 * x + M11 * y);
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M01 * srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (M10 * x + M12);
            }
            return;
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M01 * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (M10 * x);
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (M00 * srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (M11 * srcPts[srcOff++] + M12);
            }
            return;
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (M00 * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (M11 * srcPts[srcOff++]);
            }
            return;
        case (APPLY_TRANSLATE):
            M02 = m02; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + M12);
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of double precision coordinates by this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the indicated
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @since 1.2
     */
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        double M00, M01, M02, M10, M11, M12;    // For caching
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
            // If the arrays overlap partially with the destination higher
            // than the source and we transform the coordinates normally
            // we would overwrite some of the later source coordinates
            // with results of previous transformations.
            // To get around this we use arraycopy to copy the points
            // to their final destination with correct overwrite
            // handling and then transform them in place in the new
            // safer location.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            // srcPts = dstPts;         // They are known to be equal.
            srcOff = dstOff;
        }
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = M00 * x + M01 * y + M02;
                dstPts[dstOff++] = M10 * x + M11 * y + M12;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = M00 * x + M01 * y;
                dstPts[dstOff++] = M10 * x + M11 * y;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = M01 * srcPts[srcOff++] + M02;
                dstPts[dstOff++] = M10 * x + M12;
            }
            return;
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = M01 * srcPts[srcOff++];
                dstPts[dstOff++] = M10 * x;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = M00 * srcPts[srcOff++] + M02;
                dstPts[dstOff++] = M11 * srcPts[srcOff++] + M12;
            }
            return;
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            while (--numPts >= 0) {
                dstPts[dstOff++] = M00 * srcPts[srcOff++];
                dstPts[dstOff++] = M11 * srcPts[srcOff++];
            }
            return;
        case (APPLY_TRANSLATE):
            M02 = m02; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] + M02;
                dstPts[dstOff++] = srcPts[srcOff++] + M12;
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of floating point coordinates by this transform
     * and stores the results into an array of doubles.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point coordinates
     * are returned.  Each point is stored as a pair of x,&nbsp;y
     * coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of points to be transformed
     * @since 1.2
     */
    public void transform(float[] srcPts, int srcOff,
                          double[] dstPts, int dstOff,
                          int numPts) {
        double M00, M01, M02, M10, M11, M12;    // For caching
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = M00 * x + M01 * y + M02;
                dstPts[dstOff++] = M10 * x + M11 * y + M12;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = M00 * x + M01 * y;
                dstPts[dstOff++] = M10 * x + M11 * y;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = M01 * srcPts[srcOff++] + M02;
                dstPts[dstOff++] = M10 * x + M12;
            }
            return;
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = M01 * srcPts[srcOff++];
                dstPts[dstOff++] = M10 * x;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = M00 * srcPts[srcOff++] + M02;
                dstPts[dstOff++] = M11 * srcPts[srcOff++] + M12;
            }
            return;
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            while (--numPts >= 0) {
                dstPts[dstOff++] = M00 * srcPts[srcOff++];
                dstPts[dstOff++] = M11 * srcPts[srcOff++];
            }
            return;
        case (APPLY_TRANSLATE):
            M02 = m02; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] + M02;
                dstPts[dstOff++] = srcPts[srcOff++] + M12;
            }
            return;
        case (APPLY_IDENTITY):
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++];
                dstPts[dstOff++] = srcPts[srcOff++];
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of double precision coordinates by this transform
     * and stores the results into an array of floats.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @since 1.2
     */
    public void transform(double[] srcPts, int srcOff,
                          float[] dstPts, int dstOff,
                          int numPts) {
        double M00, M01, M02, M10, M11, M12;    // For caching
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M00 * x + M01 * y + M02);
                dstPts[dstOff++] = (float) (M10 * x + M11 * y + M12);
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M00 * x + M01 * y);
                dstPts[dstOff++] = (float) (M10 * x + M11 * y);
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M01 * srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (M10 * x + M12);
            }
            return;
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = (float) (M01 * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (M10 * x);
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (M00 * srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (M11 * srcPts[srcOff++] + M12);
            }
            return;
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (M00 * srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (M11 * srcPts[srcOff++]);
            }
            return;
        case (APPLY_TRANSLATE):
            M02 = m02; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + M02);
                dstPts[dstOff++] = (float) (srcPts[srcOff++] + M12);
            }
            return;
        case (APPLY_IDENTITY):
            while (--numPts >= 0) {
                dstPts[dstOff++] = (float) (srcPts[srcOff++]);
                dstPts[dstOff++] = (float) (srcPts[srcOff++]);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms the specified {@code ptSrc} and stores the
     * result in {@code ptDst}.
     * If {@code ptDst} is {@code null}, a new
     * {@code Point2D} object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, {@code ptDst}, which contains the transformed
     * point, is returned for convenience.
     * If {@code ptSrc} and {@code ptDst} are the same
     * object, the input point is correctly overwritten with the
     * transformed point.
     * @param ptSrc the point to be inverse transformed
     * @param ptDst the resulting transformed point
     * @return {@code ptDst}, which contains the result of the
     * inverse transform.
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     * @since 1.2
     */
    @SuppressWarnings("fallthrough")
    public Point2D inverseTransform(Point2D ptSrc, Point2D ptDst)
        throws NoninvertibleTransformException
    {
        if (ptDst == null) {
            if (ptSrc instanceof Point2D.Double) {
                ptDst = new Point2D.Double();
            } else {
                ptDst = new Point2D.Float();
            }
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            x -= m02;
            y -= m12;
            /* NOBREAK */
        case (APPLY_SHEAR | APPLY_SCALE):
            double det = m00 * m11 - m01 * m10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            ptDst.setLocation((x * m11 - y * m01) / det,
                              (y * m00 - x * m10) / det);
            return ptDst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            x -= m02;
            y -= m12;
            /* NOBREAK */
        case (APPLY_SHEAR):
            if (m01 == 0.0 || m10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation(y / m10, x / m01);
            return ptDst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            x -= m02;
            y -= m12;
            /* NOBREAK */
        case (APPLY_SCALE):
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation(x / m00, y / m11);
            return ptDst;
        case (APPLY_TRANSLATE):
            ptDst.setLocation(x - m02, y - m12);
            return ptDst;
        case (APPLY_IDENTITY):
            ptDst.setLocation(x, y);
            return ptDst;
        }

        /* NOTREACHED */
    }

    /**
     * Inverse transforms an array of double precision coordinates by
     * this transform.
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the specified
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source point coordinates.
     * Each point is stored as a pair of x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed point
     * coordinates are returned.  Each point is stored as a pair of
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first point to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed point that is stored in the destination array
     * @param numPts the number of point objects to be transformed
     * @exception NoninvertibleTransformException  if the matrix cannot be
     *                                         inverted.
     * @since 1.2
     */
    public void inverseTransform(double[] srcPts, int srcOff,
                                 double[] dstPts, int dstOff,
                                 int numPts)
        throws NoninvertibleTransformException
    {
        double M00, M01, M02, M10, M11, M12;    // For caching
        double det;
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
            // If the arrays overlap partially with the destination higher
            // than the source and we transform the coordinates normally
            // we would overwrite some of the later source coordinates
            // with results of previous transformations.
            // To get around this we use arraycopy to copy the points
            // to their final destination with correct overwrite
            // handling and then transform them in place in the new
            // safer location.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            // srcPts = dstPts;         // They are known to be equal.
            srcOff = dstOff;
        }
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M01 = m01; M02 = m02;
            M10 = m10; M11 = m11; M12 = m12;
            det = M00 * M11 - M01 * M10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - M02;
                double y = srcPts[srcOff++] - M12;
                dstPts[dstOff++] = (x * M11 - y * M01) / det;
                dstPts[dstOff++] = (y * M00 - x * M10) / det;
            }
            return;
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            det = M00 * M11 - M01 * M10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is "+
                                                          det);
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = (x * M11 - y * M01) / det;
                dstPts[dstOff++] = (y * M00 - x * M10) / det;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
            M01 = m01; M02 = m02;
            M10 = m10; M12 = m12;
            if (M01 == 0.0 || M10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++] - M02;
                dstPts[dstOff++] = (srcPts[srcOff++] - M12) / M10;
                dstPts[dstOff++] = x / M01;
            }
            return;
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            if (M01 == 0.0 || M10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = srcPts[srcOff++] / M10;
                dstPts[dstOff++] = x / M01;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
            M00 = m00; M02 = m02;
            M11 = m11; M12 = m12;
            if (M00 == 0.0 || M11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = (srcPts[srcOff++] - M02) / M00;
                dstPts[dstOff++] = (srcPts[srcOff++] - M12) / M11;
            }
            return;
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            if (M00 == 0.0 || M11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] / M00;
                dstPts[dstOff++] = srcPts[srcOff++] / M11;
            }
            return;
        case (APPLY_TRANSLATE):
            M02 = m02; M12 = m12;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] - M02;
                dstPts[dstOff++] = srcPts[srcOff++] - M12;
            }
            return;
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms the relative distance vector specified by
     * {@code ptSrc} and stores the result in {@code ptDst}.
     * A relative distance vector is transformed without applying the
     * translation components of the affine transformation matrix
     * using the following equations:
     * <pre>
     *  [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
     *  [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
     *  [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
     * </pre>
     * If {@code ptDst} is {@code null}, a new
     * {@code Point2D} object is allocated and then the result of the
     * transform is stored in this object.
     * In either case, {@code ptDst}, which contains the
     * transformed point, is returned for convenience.
     * If {@code ptSrc} and {@code ptDst} are the same object,
     * the input point is correctly overwritten with the transformed
     * point.
     * @param ptSrc the distance vector to be delta transformed
     * @param ptDst the resulting transformed distance vector
     * @return {@code ptDst}, which contains the result of the
     * transformation.
     * @since 1.2
     */
    public Point2D deltaTransform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            if (ptSrc instanceof Point2D.Double) {
                ptDst = new Point2D.Double();
            } else {
                ptDst = new Point2D.Float();
            }
        }
        // Copy source coords into local variables in case src == dst
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return null;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            ptDst.setLocation(x * m00 + y * m01, x * m10 + y * m11);
            return ptDst;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            ptDst.setLocation(y * m01, x * m10);
            return ptDst;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            ptDst.setLocation(x * m00, y * m11);
            return ptDst;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            ptDst.setLocation(x, y);
            return ptDst;
        }

        /* NOTREACHED */
    }

    /**
     * Transforms an array of relative distance vectors by this
     * transform.
     * A relative distance vector is transformed without applying the
     * translation components of the affine transformation matrix
     * using the following equations:
     * <pre>
     *  [  x' ]   [  m00  m01 (m02) ] [  x  ]   [ m00x + m01y ]
     *  [  y' ] = [  m10  m11 (m12) ] [  y  ] = [ m10x + m11y ]
     *  [ (1) ]   [  (0)  (0) ( 1 ) ] [ (1) ]   [     (1)     ]
     * </pre>
     * The two coordinate array sections can be exactly the same or
     * can be overlapping sections of the same array without affecting the
     * validity of the results.
     * This method ensures that no source coordinates are
     * overwritten by a previous operation before they can be transformed.
     * The coordinates are stored in the arrays starting at the indicated
     * offset in the order {@code [x0, y0, x1, y1, ..., xn, yn]}.
     * @param srcPts the array containing the source distance vectors.
     * Each vector is stored as a pair of relative x,&nbsp;y coordinates.
     * @param dstPts the array into which the transformed distance vectors
     * are returned.  Each vector is stored as a pair of relative
     * x,&nbsp;y coordinates.
     * @param srcOff the offset to the first vector to be transformed
     * in the source array
     * @param dstOff the offset to the location of the first
     * transformed vector that is stored in the destination array
     * @param numPts the number of vector coordinate pairs to be
     * transformed
     * @since 1.2
     */
    public void deltaTransform(double[] srcPts, int srcOff,
                               double[] dstPts, int dstOff,
                               int numPts) {
        double M00, M01, M10, M11;      // For caching
        if (dstPts == srcPts &&
            dstOff > srcOff && dstOff < srcOff + numPts * 2)
        {
            // If the arrays overlap partially with the destination higher
            // than the source and we transform the coordinates normally
            // we would overwrite some of the later source coordinates
            // with results of previous transformations.
            // To get around this we use arraycopy to copy the points
            // to their final destination with correct overwrite
            // handling and then transform them in place in the new
            // safer location.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            // srcPts = dstPts;         // They are known to be equal.
            srcOff = dstOff;
        }
        switch (state) {
        default:
            stateError();
            /* NOTREACHED */
            return;
        case (APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SHEAR | APPLY_SCALE):
            M00 = m00; M01 = m01;
            M10 = m10; M11 = m11;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                double y = srcPts[srcOff++];
                dstPts[dstOff++] = x * M00 + y * M01;
                dstPts[dstOff++] = x * M10 + y * M11;
            }
            return;
        case (APPLY_SHEAR | APPLY_TRANSLATE):
        case (APPLY_SHEAR):
            M01 = m01; M10 = m10;
            while (--numPts >= 0) {
                double x = srcPts[srcOff++];
                dstPts[dstOff++] = srcPts[srcOff++] * M01;
                dstPts[dstOff++] = x * M10;
            }
            return;
        case (APPLY_SCALE | APPLY_TRANSLATE):
        case (APPLY_SCALE):
            M00 = m00; M11 = m11;
            while (--numPts >= 0) {
                dstPts[dstOff++] = srcPts[srcOff++] * M00;
                dstPts[dstOff++] = srcPts[srcOff++] * M11;
            }
            return;
        case (APPLY_TRANSLATE):
        case (APPLY_IDENTITY):
            if (srcPts != dstPts || srcOff != dstOff) {
                System.arraycopy(srcPts, srcOff, dstPts, dstOff,
                                 numPts * 2);
            }
            return;
        }

        /* NOTREACHED */
    }

    /**
     * Returns a new {@link Shape} object defined by the geometry of the
     * specified {@code Shape} after it has been transformed by
     * this transform.
     * @param pSrc the specified {@code Shape} object to be
     * transformed by this transform.
     * @return a new {@code Shape} object that defines the geometry
     * of the transformed {@code Shape}, or null if {@code pSrc} is null.
     * @since 1.2
     */
    public Shape createTransformedShape(Shape pSrc) {
        if (pSrc == null) {
            return null;
        }
        return new Path2D.Double(pSrc, this);
    }

    // Round values to sane precision for printing
    // Note that Math.sin(Math.PI) has an error of about 10^-16
    private static double _matround(double matval) {
        return Math.rint(matval * 1E15) / 1E15;
    }

    /**
     * Returns a {@code String} that represents the value of this
     * {@link Object}.
     * @return a {@code String} representing the value of this
     * {@code Object}.
     * @since 1.2
     */
    public String toString() {
        return ("AffineTransform[["
                + _matround(m00) + ", "
                + _matround(m01) + ", "
                + _matround(m02) + "], ["
                + _matround(m10) + ", "
                + _matround(m11) + ", "
                + _matround(m12) + "]]");
    }

    /**
     * Returns {@code true} if this {@code AffineTransform} is
     * an identity transform.
     * @return {@code true} if this {@code AffineTransform} is
     * an identity transform; {@code false} otherwise.
     * @since 1.2
     */
    public boolean isIdentity() {
        return (state == APPLY_IDENTITY || (getType() == TYPE_IDENTITY));
    }

    /**
     * Returns a copy of this {@code AffineTransform} object.
     * @return an {@code Object} that is a copy of this
     * {@code AffineTransform} object.
     * @since 1.2
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    /**
     * Returns the hashcode for this transform.
     * @return      a hash code for this transform.
     * @since 1.2
     */
    public int hashCode() {
        long bits = Double.doubleToLongBits(m00);
        bits = bits * 31 + Double.doubleToLongBits(m01);
        bits = bits * 31 + Double.doubleToLongBits(m02);
        bits = bits * 31 + Double.doubleToLongBits(m10);
        bits = bits * 31 + Double.doubleToLongBits(m11);
        bits = bits * 31 + Double.doubleToLongBits(m12);
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Returns {@code true} if this {@code AffineTransform}
     * represents the same affine coordinate transform as the specified
     * argument.
     * @param obj the {@code Object} to test for equality with this
     * {@code AffineTransform}
     * @return {@code true} if {@code obj} equals this
     * {@code AffineTransform} object; {@code false} otherwise.
     * @since 1.2
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof AffineTransform)) {
            return false;
        }

        AffineTransform a = (AffineTransform)obj;

        return ((m00 == a.m00) && (m01 == a.m01) && (m02 == a.m02) &&
                (m10 == a.m10) && (m11 == a.m11) && (m12 == a.m12));
    }

    /* Serialization support.  A readObject method is neccessary because
     * the state field is part of the implementation of this particular
     * AffineTransform and not part of the public specification.  The
     * state variable's value needs to be recalculated on the fly by the
     * readObject method as it is in the 6-argument matrix constructor.
     */

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 1330973210523860834L;

    /**
     * Writes default serializable fields to stream.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException
    {
        s.defaultWriteObject();
    }

    /**
     * Reads the {@code ObjectInputStream}.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private void readObject(java.io.ObjectInputStream s)
        throws java.lang.ClassNotFoundException, java.io.IOException
    {
        s.defaultReadObject();
        updateState();
    }
}
