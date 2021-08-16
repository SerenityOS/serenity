/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.beans.ConstructorProperties;

import java.lang.annotation.Native;

/**
 * The {@code BasicStroke} class defines a basic set of rendering
 * attributes for the outlines of graphics primitives, which are rendered
 * with a {@link Graphics2D} object that has its Stroke attribute set to
 * this {@code BasicStroke}.
 * The rendering attributes defined by {@code BasicStroke} describe
 * the shape of the mark made by a pen drawn along the outline of a
 * {@link Shape} and the decorations applied at the ends and joins of
 * path segments of the {@code Shape}.
 * These rendering attributes include:
 * <dl>
 * <dt><i>width</i>
 * <dd>The pen width, measured perpendicularly to the pen trajectory.
 * <dt><i>end caps</i>
 * <dd>The decoration applied to the ends of unclosed subpaths and
 * dash segments.  Subpaths that start and end on the same point are
 * still considered unclosed if they do not have a CLOSE segment.
 * See {@link java.awt.geom.PathIterator#SEG_CLOSE SEG_CLOSE}
 * for more information on the CLOSE segment.
 * The three different decorations are: {@link #CAP_BUTT},
 * {@link #CAP_ROUND}, and {@link #CAP_SQUARE}.
 * <dt><i>line joins</i>
 * <dd>The decoration applied at the intersection of two path segments
 * and at the intersection of the endpoints of a subpath that is closed
 * using {@link java.awt.geom.PathIterator#SEG_CLOSE SEG_CLOSE}.
 * The three different decorations are: {@link #JOIN_BEVEL},
 * {@link #JOIN_MITER}, and {@link #JOIN_ROUND}.
 * <dt><i>miter limit</i>
 * <dd>The limit to trim a line join that has a JOIN_MITER decoration.
 * A line join is trimmed when the ratio of miter length to stroke
 * width is greater than the miterlimit value.  The miter length is
 * the diagonal length of the miter, which is the distance between
 * the inside corner and the outside corner of the intersection.
 * The smaller the angle formed by two line segments, the longer
 * the miter length and the sharper the angle of intersection.  The
 * default miterlimit value of 10.0f causes all angles less than
 * 11 degrees to be trimmed.  Trimming miters converts
 * the decoration of the line join to bevel.
 * <dt><i>dash attributes</i>
 * <dd>The definition of how to make a dash pattern by alternating
 * between opaque and transparent sections.
 * </dl>
 * All attributes that specify measurements and distances controlling
 * the shape of the returned outline are measured in the same
 * coordinate system as the original unstroked {@code Shape}
 * argument.  When a {@code Graphics2D} object uses a
 * {@code Stroke} object to redefine a path during the execution
 * of one of its {@code draw} methods, the geometry is supplied
 * in its original form before the {@code Graphics2D} transform
 * attribute is applied.  Therefore, attributes such as the pen width
 * are interpreted in the user space coordinate system of the
 * {@code Graphics2D} object and are subject to the scaling and
 * shearing effects of the user-space-to-device-space transform in that
 * particular {@code Graphics2D}.
 * For example, the width of a rendered shape's outline is determined
 * not only by the width attribute of this {@code BasicStroke},
 * but also by the transform attribute of the
 * {@code Graphics2D} object.  Consider this code:
 * <blockquote><pre>{@code
 *      // sets the Graphics2D object's Transform attribute
 *      g2d.scale(10, 10);
 *      // sets the Graphics2D object's Stroke attribute
 *      g2d.setStroke(new BasicStroke(1.5f));
 * }</pre></blockquote>
 * Assuming there are no other scaling transforms added to the
 * {@code Graphics2D} object, the resulting line
 * will be approximately 15 pixels wide.
 * As the example code demonstrates, a floating-point line
 * offers better precision, especially when large transforms are
 * used with a {@code Graphics2D} object.
 * When a line is diagonal, the exact width depends on how the
 * rendering pipeline chooses which pixels to fill as it traces the
 * theoretical widened outline.  The choice of which pixels to turn
 * on is affected by the antialiasing attribute because the
 * antialiasing rendering pipeline can choose to color
 * partially-covered pixels.
 * <p>
 * For more information on the user space coordinate system and the
 * rendering process, see the {@code Graphics2D} class comments.
 * @see Graphics2D
 * @author Jim Graham
 */
public class BasicStroke implements Stroke {

    /**
     * Joins path segments by extending their outside edges until
     * they meet.
     */
    @Native public static final int JOIN_MITER = 0;

    /**
     * Joins path segments by rounding off the corner at a radius
     * of half the line width.
     */
    @Native public static final int JOIN_ROUND = 1;

    /**
     * Joins path segments by connecting the outer corners of their
     * wide outlines with a straight segment.
     */
    @Native public static final int JOIN_BEVEL = 2;

    /**
     * Ends unclosed subpaths and dash segments with no added
     * decoration.
     */
    @Native public static final int CAP_BUTT = 0;

    /**
     * Ends unclosed subpaths and dash segments with a round
     * decoration that has a radius equal to half of the width
     * of the pen.
     */
    @Native public static final int CAP_ROUND = 1;

    /**
     * Ends unclosed subpaths and dash segments with a square
     * projection that extends beyond the end of the segment
     * to a distance equal to half of the line width.
     */
    @Native public static final int CAP_SQUARE = 2;

    float width;

    int join;
    int cap;
    float miterlimit;

    float[] dash;
    float dash_phase;

    /**
     * Constructs a new {@code BasicStroke} with the specified
     * attributes.
     * @param width the width of this {@code BasicStroke}.  The
     *         width must be greater than or equal to 0.0f.  If width is
     *         set to 0.0f, the stroke is rendered as the thinnest
     *         possible line for the target device and the antialias
     *         hint setting.
     * @param cap the decoration of the ends of a {@code BasicStroke}
     * @param join the decoration applied where path segments meet
     * @param miterlimit the limit to trim the miter join.  The miterlimit
     *        must be greater than or equal to 1.0f.
     * @param dash the array representing the dashing pattern
     * @param dash_phase the offset to start the dashing pattern
     * @throws IllegalArgumentException if {@code width} is negative
     * @throws IllegalArgumentException if {@code cap} is not either
     *         CAP_BUTT, CAP_ROUND or CAP_SQUARE
     * @throws IllegalArgumentException if {@code miterlimit} is less
     *         than 1 and {@code join} is JOIN_MITER
     * @throws IllegalArgumentException if {@code join} is not
     *         either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER
     * @throws IllegalArgumentException if {@code dash_phase}
     *         is negative and {@code dash} is not {@code null}
     * @throws IllegalArgumentException if the length of
     *         {@code dash} is zero
     * @throws IllegalArgumentException if dash lengths are all zero.
     */
    @ConstructorProperties({ "lineWidth", "endCap", "lineJoin", "miterLimit", "dashArray", "dashPhase" })
    public BasicStroke(float width, int cap, int join, float miterlimit,
                       float[] dash, float dash_phase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            if (miterlimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (dash != null) {
            if (dash_phase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean allzero = true;
            for (int i = 0; i < dash.length; i++) {
                float d = dash[i];
                if (d > 0.0) {
                    allzero = false;
                } else if (d < 0.0) {
                    throw new IllegalArgumentException("negative dash length");
                }
            }
            if (allzero) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width      = width;
        this.cap        = cap;
        this.join       = join;
        this.miterlimit = miterlimit;
        if (dash != null) {
            this.dash = dash.clone();
        }
        this.dash_phase = dash_phase;
    }

    /**
     * Constructs a solid {@code BasicStroke} with the specified
     * attributes.
     * @param width the width of the {@code BasicStroke}
     * @param cap the decoration of the ends of a {@code BasicStroke}
     * @param join the decoration applied where path segments meet
     * @param miterlimit the limit to trim the miter join
     * @throws IllegalArgumentException if {@code width} is negative
     * @throws IllegalArgumentException if {@code cap} is not either
     *         CAP_BUTT, CAP_ROUND or CAP_SQUARE
     * @throws IllegalArgumentException if {@code miterlimit} is less
     *         than 1 and {@code join} is JOIN_MITER
     * @throws IllegalArgumentException if {@code join} is not
     *         either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER
     */
    public BasicStroke(float width, int cap, int join, float miterlimit) {
        this(width, cap, join, miterlimit, null, 0.0f);
    }

    /**
     * Constructs a solid {@code BasicStroke} with the specified
     * attributes.  The {@code miterlimit} parameter is
     * unnecessary in cases where the default is allowable or the
     * line joins are not specified as JOIN_MITER.
     * @param width the width of the {@code BasicStroke}
     * @param cap the decoration of the ends of a {@code BasicStroke}
     * @param join the decoration applied where path segments meet
     * @throws IllegalArgumentException if {@code width} is negative
     * @throws IllegalArgumentException if {@code cap} is not either
     *         CAP_BUTT, CAP_ROUND or CAP_SQUARE
     * @throws IllegalArgumentException if {@code join} is not
     *         either JOIN_ROUND, JOIN_BEVEL, or JOIN_MITER
     */
    public BasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }

    /**
     * Constructs a solid {@code BasicStroke} with the specified
     * line width and with default values for the cap and join
     * styles.
     * @param width the width of the {@code BasicStroke}
     * @throws IllegalArgumentException if {@code width} is negative
     */
    public BasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    /**
     * Constructs a new {@code BasicStroke} with defaults for all
     * attributes.
     * The default attributes are a solid line of width 1.0, CAP_SQUARE,
     * JOIN_MITER, a miter limit of 10.0.
     */
    public BasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }


    /**
     * Returns a {@code Shape} whose interior defines the
     * stroked outline of a specified {@code Shape}.
     * @param s the {@code Shape} boundary be stroked
     * @return the {@code Shape} of the stroked outline.
     * @throws NullPointerException if {@code s} is {@code null}
     */
    public Shape createStrokedShape(Shape s) {
        sun.java2d.pipe.RenderingEngine re =
            sun.java2d.pipe.RenderingEngine.getInstance();
        return re.createStrokedShape(s, width, cap, join, miterlimit,
                                     dash, dash_phase);
    }

    /**
     * Returns the line width.  Line width is represented in user space,
     * which is the default-coordinate space used by Java 2D.  See the
     * {@code Graphics2D} class comments for more information on
     * the user space coordinate system.
     * @return the line width of this {@code BasicStroke}.
     * @see Graphics2D
     */
    public float getLineWidth() {
        return width;
    }

    /**
     * Returns the end cap style.
     * @return the end cap style of this {@code BasicStroke} as one
     * of the static {@code int} values that define possible end cap
     * styles.
     */
    public int getEndCap() {
        return cap;
    }

    /**
     * Returns the line join style.
     * @return the line join style of the {@code BasicStroke} as one
     * of the static {@code int} values that define possible line
     * join styles.
     */
    public int getLineJoin() {
        return join;
    }

    /**
     * Returns the limit of miter joins.
     * @return the limit of miter joins of the {@code BasicStroke}.
     */
    public float getMiterLimit() {
        return miterlimit;
    }

    /**
     * Returns the array representing the lengths of the dash segments.
     * Alternate entries in the array represent the user space lengths
     * of the opaque and transparent segments of the dashes.
     * As the pen moves along the outline of the {@code Shape}
     * to be stroked, the user space
     * distance that the pen travels is accumulated.  The distance
     * value is used to index into the dash array.
     * The pen is opaque when its current cumulative distance maps
     * to an even element of the dash array and transparent otherwise.
     * @return the dash array.
     */
    public float[] getDashArray() {
        if (dash == null) {
            return null;
        }

        return dash.clone();
    }

    /**
     * Returns the current dash phase.
     * The dash phase is a distance specified in user coordinates that
     * represents an offset into the dashing pattern. In other words, the dash
     * phase defines the point in the dashing pattern that will correspond to
     * the beginning of the stroke.
     * @return the dash phase as a {@code float} value.
     */
    public float getDashPhase() {
        return dash_phase;
    }

    /**
     * Returns the hashcode for this stroke.
     * @return      a hash code for this stroke.
     */
    public int hashCode() {
        int hash = Float.floatToIntBits(width);
        hash = hash * 31 + join;
        hash = hash * 31 + cap;
        hash = hash * 31 + Float.floatToIntBits(miterlimit);
        if (dash != null) {
            hash = hash * 31 + Float.floatToIntBits(dash_phase);
            for (int i = 0; i < dash.length; i++) {
                hash = hash * 31 + Float.floatToIntBits(dash[i]);
            }
        }
        return hash;
    }

    /**
     * Returns true if this BasicStroke represents the same
     * stroking operation as the given argument.
     */
   /**
    * Tests if a specified object is equal to this {@code BasicStroke}
    * by first testing if it is a {@code BasicStroke} and then comparing
    * its width, join, cap, miter limit, dash, and dash phase attributes with
    * those of this {@code BasicStroke}.
    * @param  obj the specified object to compare to this
    *              {@code BasicStroke}
    * @return {@code true} if the width, join, cap, miter limit, dash, and
    *            dash phase are the same for both objects;
    *            {@code false} otherwise.
    */
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicStroke)) {
            return false;
        }

        BasicStroke bs = (BasicStroke) obj;
        if (width != bs.width) {
            return false;
        }

        if (join != bs.join) {
            return false;
        }

        if (cap != bs.cap) {
            return false;
        }

        if (miterlimit != bs.miterlimit) {
            return false;
        }

        if (dash != null) {
            if (dash_phase != bs.dash_phase) {
                return false;
            }

            if (!java.util.Arrays.equals(dash, bs.dash)) {
                return false;
            }
        }
        else if (bs.dash != null) {
            return false;
        }

        return true;
    }
}
