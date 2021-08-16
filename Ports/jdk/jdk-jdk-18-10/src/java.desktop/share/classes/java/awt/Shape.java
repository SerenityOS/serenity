/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The {@code Shape} interface provides definitions for objects
 * that represent some form of geometric shape.  The {@code Shape}
 * is described by a {@link PathIterator} object, which can express the
 * outline of the {@code Shape} as well as a rule for determining
 * how the outline divides the 2D plane into interior and exterior
 * points.  Each {@code Shape} object provides callbacks to get the
 * bounding box of the geometry, determine whether points or
 * rectangles lie partly or entirely within the interior
 * of the {@code Shape}, and retrieve a {@code PathIterator}
 * object that describes the trajectory path of the {@code Shape}
 * outline.
 * <p>
 * <a id="def_insideness"><b>Definition of insideness:</b></a>
 * A point is considered to lie inside a
 * {@code Shape} if and only if:
 * <ul>
 * <li> it lies completely
 * inside the {@code Shape} boundary <i>or</i>
 * <li>
 * it lies exactly on the {@code Shape} boundary <i>and</i> the
 * space immediately adjacent to the
 * point in the increasing {@code X} direction is
 * entirely inside the boundary <i>or</i>
 * <li>
 * it lies exactly on a horizontal boundary segment <b>and</b> the
 * space immediately adjacent to the point in the
 * increasing {@code Y} direction is inside the boundary.
 * </ul>
 * <p>The {@code contains} and {@code intersects} methods
 * consider the interior of a {@code Shape} to be the area it
 * encloses as if it were filled.  This means that these methods
 * consider
 * unclosed shapes to be implicitly closed for the purpose of
 * determining if a shape contains or intersects a rectangle or if a
 * shape contains a point.
 *
 * @see java.awt.geom.PathIterator
 * @see java.awt.geom.AffineTransform
 * @see java.awt.geom.FlatteningPathIterator
 * @see java.awt.geom.GeneralPath
 *
 * @author Jim Graham
 * @since 1.2
 */
public interface Shape {
    /**
     * Returns an integer {@link Rectangle} that completely encloses the
     * {@code Shape}.  Note that there is no guarantee that the
     * returned {@code Rectangle} is the smallest bounding box that
     * encloses the {@code Shape}, only that the {@code Shape}
     * lies entirely within the indicated  {@code Rectangle}.  The
     * returned {@code Rectangle} might also fail to completely
     * enclose the {@code Shape} if the {@code Shape} overflows
     * the limited range of the integer data type.  The
     * {@code getBounds2D} method generally returns a
     * tighter bounding box due to its greater flexibility in
     * representation.
     *
     * <p>
     * Note that the
     * <a href="{@docRoot}/java.desktop/java/awt/Shape.html#def_insideness">
     * definition of insideness</a> can lead to situations where points
     * on the defining outline of the {@code shape} may not be considered
     * contained in the returned {@code bounds} object, but only in cases
     * where those points are also not considered contained in the original
     * {@code shape}.
     * </p>
     * <p>
     * If a {@code point} is inside the {@code shape} according to the
     * {@link #contains(double x, double y) contains(point)} method, then
     * it must be inside the returned {@code Rectangle} bounds object
     * according to the {@link #contains(double x, double y) contains(point)}
     * method of the {@code bounds}. Specifically:
     * </p>
     * <p>
     *  {@code shape.contains(x,y)} requires {@code bounds.contains(x,y)}
     * </p>
     * <p>
     * If a {@code point} is not inside the {@code shape}, then it might
     * still be contained in the {@code bounds} object:
     * </p>
     * <p>
     *  {@code bounds.contains(x,y)} does not imply {@code shape.contains(x,y)}
     * </p>
     * @return an integer {@code Rectangle} that completely encloses
     *                 the {@code Shape}.
     * @see #getBounds2D
     * @since 1.2
     */
    public Rectangle getBounds();

    /**
     * Returns a high precision and more accurate bounding box of
     * the {@code Shape} than the {@code getBounds} method.
     * Note that there is no guarantee that the returned
     * {@link Rectangle2D} is the smallest bounding box that encloses
     * the {@code Shape}, only that the {@code Shape} lies
     * entirely within the indicated {@code Rectangle2D}.  The
     * bounding box returned by this method is usually tighter than that
     * returned by the {@code getBounds} method and never fails due
     * to overflow problems since the return value can be an instance of
     * the {@code Rectangle2D} that uses double precision values to
     * store the dimensions.
     *
     * <p>
     * Note that the
     * <a href="{@docRoot}/java.desktop/java/awt/Shape.html#def_insideness">
     * definition of insideness</a> can lead to situations where points
     * on the defining outline of the {@code shape} may not be considered
     * contained in the returned {@code bounds} object, but only in cases
     * where those points are also not considered contained in the original
     * {@code shape}.
     * </p>
     * <p>
     * If a {@code point} is inside the {@code shape} according to the
     * {@link #contains(Point2D p) contains(point)} method, then it must
     * be inside the returned {@code Rectangle2D} bounds object according
     * to the {@link #contains(Point2D p) contains(point)} method of the
     * {@code bounds}. Specifically:
     * </p>
     * <p>
     *  {@code shape.contains(p)} requires {@code bounds.contains(p)}
     * </p>
     * <p>
     * If a {@code point} is not inside the {@code shape}, then it might
     * still be contained in the {@code bounds} object:
     * </p>
     * <p>
     *  {@code bounds.contains(p)} does not imply {@code shape.contains(p)}
     * </p>
     * @return an instance of {@code Rectangle2D} that is a
     *                 high-precision bounding box of the {@code Shape}.
     * @see #getBounds
     * @since 1.2
     */
    public Rectangle2D getBounds2D();

    /**
     * Tests if the specified coordinates are inside the boundary of the
     * {@code Shape}, as described by the
     * <a href="{@docRoot}/java.desktop/java/awt/Shape.html#def_insideness">
     * definition of insideness</a>.
     * @param x the specified X coordinate to be tested
     * @param y the specified Y coordinate to be tested
     * @return {@code true} if the specified coordinates are inside
     *         the {@code Shape} boundary; {@code false}
     *         otherwise.
     * @since 1.2
     */
    public boolean contains(double x, double y);

    /**
     * Tests if a specified {@link Point2D} is inside the boundary
     * of the {@code Shape}, as described by the
     * <a href="{@docRoot}/java.desktop/java/awt/Shape.html#def_insideness">
     * definition of insideness</a>.
     * @param p the specified {@code Point2D} to be tested
     * @return {@code true} if the specified {@code Point2D} is
     *          inside the boundary of the {@code Shape};
     *          {@code false} otherwise.
     * @since 1.2
     */
    public boolean contains(Point2D p);

    /**
     * Tests if the interior of the {@code Shape} intersects the
     * interior of a specified rectangular area.
     * The rectangular area is considered to intersect the {@code Shape}
     * if any point is contained in both the interior of the
     * {@code Shape} and the specified rectangular area.
     * <p>
     * The {@code Shape.intersects()} method allows a {@code Shape}
     * implementation to conservatively return {@code true} when:
     * <ul>
     * <li>
     * there is a high probability that the rectangular area and the
     * {@code Shape} intersect, but
     * <li>
     * the calculations to accurately determine this intersection
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code true} even though the rectangular area does not
     * intersect the {@code Shape}.
     * The {@link java.awt.geom.Area Area} class performs
     * more accurate computations of geometric intersection than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param y the Y coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return {@code true} if the interior of the {@code Shape} and
     *          the interior of the rectangular area intersect, or are
     *          both highly likely to intersect and intersection calculations
     *          would be too expensive to perform; {@code false} otherwise.
     * @see java.awt.geom.Area
     * @since 1.2
     */
    public boolean intersects(double x, double y, double w, double h);

    /**
     * Tests if the interior of the {@code Shape} intersects the
     * interior of a specified {@code Rectangle2D}.
     * The {@code Shape.intersects()} method allows a {@code Shape}
     * implementation to conservatively return {@code true} when:
     * <ul>
     * <li>
     * there is a high probability that the {@code Rectangle2D} and the
     * {@code Shape} intersect, but
     * <li>
     * the calculations to accurately determine this intersection
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code true} even though the {@code Rectangle2D} does not
     * intersect the {@code Shape}.
     * The {@link java.awt.geom.Area Area} class performs
     * more accurate computations of geometric intersection than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param r the specified {@code Rectangle2D}
     * @return {@code true} if the interior of the {@code Shape} and
     *          the interior of the specified {@code Rectangle2D}
     *          intersect, or are both highly likely to intersect and intersection
     *          calculations would be too expensive to perform; {@code false}
     *          otherwise.
     * @see #intersects(double, double, double, double)
     * @since 1.2
     */
    public boolean intersects(Rectangle2D r);

    /**
     * Tests if the interior of the {@code Shape} entirely contains
     * the specified rectangular area.  All coordinates that lie inside
     * the rectangular area must lie within the {@code Shape} for the
     * entire rectangular area to be considered contained within the
     * {@code Shape}.
     * <p>
     * The {@code Shape.contains()} method allows a {@code Shape}
     * implementation to conservatively return {@code false} when:
     * <ul>
     * <li>
     * the {@code intersect} method returns {@code true} and
     * <li>
     * the calculations to determine whether or not the
     * {@code Shape} entirely contains the rectangular area are
     * prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code false} even though the {@code Shape} contains
     * the rectangular area.
     * The {@link java.awt.geom.Area Area} class performs
     * more accurate geometric computations than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param x the X coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param y the Y coordinate of the upper-left corner
     *          of the specified rectangular area
     * @param w the width of the specified rectangular area
     * @param h the height of the specified rectangular area
     * @return {@code true} if the interior of the {@code Shape}
     *          entirely contains the specified rectangular area;
     *          {@code false} otherwise or, if the {@code Shape}
     *          contains the rectangular area and the
     *          {@code intersects} method returns {@code true}
     *          and the containment calculations would be too expensive to
     *          perform.
     * @see java.awt.geom.Area
     * @see #intersects
     * @since 1.2
     */
    public boolean contains(double x, double y, double w, double h);

    /**
     * Tests if the interior of the {@code Shape} entirely contains the
     * specified {@code Rectangle2D}.
     * The {@code Shape.contains()} method allows a {@code Shape}
     * implementation to conservatively return {@code false} when:
     * <ul>
     * <li>
     * the {@code intersect} method returns {@code true} and
     * <li>
     * the calculations to determine whether or not the
     * {@code Shape} entirely contains the {@code Rectangle2D}
     * are prohibitively expensive.
     * </ul>
     * This means that for some {@code Shapes} this method might
     * return {@code false} even though the {@code Shape} contains
     * the {@code Rectangle2D}.
     * The {@link java.awt.geom.Area Area} class performs
     * more accurate geometric computations than most
     * {@code Shape} objects and therefore can be used if a more precise
     * answer is required.
     *
     * @param r The specified {@code Rectangle2D}
     * @return {@code true} if the interior of the {@code Shape}
     *          entirely contains the {@code Rectangle2D};
     *          {@code false} otherwise or, if the {@code Shape}
     *          contains the {@code Rectangle2D} and the
     *          {@code intersects} method returns {@code true}
     *          and the containment calculations would be too expensive to
     *          perform.
     * @see #contains(double, double, double, double)
     * @since 1.2
     */
    public boolean contains(Rectangle2D r);

    /**
     * Returns an iterator object that iterates along the
     * {@code Shape} boundary and provides access to the geometry of the
     * {@code Shape} outline.  If an optional {@link AffineTransform}
     * is specified, the coordinates returned in the iteration are
     * transformed accordingly.
     * <p>
     * Each call to this method returns a fresh {@code PathIterator}
     * object that traverses the geometry of the {@code Shape} object
     * independently from any other {@code PathIterator} objects in use
     * at the same time.
     * <p>
     * It is recommended, but not guaranteed, that objects
     * implementing the {@code Shape} interface isolate iterations
     * that are in process from any changes that might occur to the original
     * object's geometry during such iterations.
     *
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration, or
     *          {@code null} if untransformed coordinates are desired
     * @return a new {@code PathIterator} object, which independently
     *          traverses the geometry of the {@code Shape}.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at);

    /**
     * Returns an iterator object that iterates along the {@code Shape}
     * boundary and provides access to a flattened view of the
     * {@code Shape} outline geometry.
     * <p>
     * Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point types are
     * returned by the iterator.
     * <p>
     * If an optional {@code AffineTransform} is specified,
     * the coordinates returned in the iteration are transformed
     * accordingly.
     * <p>
     * The amount of subdivision of the curved segments is controlled
     * by the {@code flatness} parameter, which specifies the
     * maximum distance that any point on the unflattened transformed
     * curve can deviate from the returned flattened path segments.
     * Note that a limit on the accuracy of the flattened path might be
     * silently imposed, causing very small flattening parameters to be
     * treated as larger values.  This limit, if there is one, is
     * defined by the particular implementation that is used.
     * <p>
     * Each call to this method returns a fresh {@code PathIterator}
     * object that traverses the {@code Shape} object geometry
     * independently from any other {@code PathIterator} objects in use at
     * the same time.
     * <p>
     * It is recommended, but not guaranteed, that objects
     * implementing the {@code Shape} interface isolate iterations
     * that are in process from any changes that might occur to the original
     * object's geometry during such iterations.
     *
     * @param at an optional {@code AffineTransform} to be applied to the
     *          coordinates as they are returned in the iteration, or
     *          {@code null} if untransformed coordinates are desired
     * @param flatness the maximum distance that the line segments used to
     *          approximate the curved segments are allowed to deviate
     *          from any point on the original curve
     * @return a new {@code PathIterator} that independently traverses
     *         a flattened view of the geometry of the  {@code Shape}.
     * @since 1.2
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness);
}
