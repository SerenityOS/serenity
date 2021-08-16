/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;


import java.awt.*;
import java.io.Serializable;

/**
 * For the convenience of layout managers,
 * calculates information about the size and position of components.
 * All size and position calculation methods are class methods
 * that take arrays of SizeRequirements as arguments.
 * The SizeRequirements class supports two types of layout:
 *
 * <blockquote>
 * <dl>
 * <dt> tiled
 * <dd> The components are placed end-to-end,
 *      starting either at coordinate 0 (the leftmost or topmost position)
 *      or at the coordinate representing the end of the allocated span
 *      (the rightmost or bottommost position).
 *
 * <dt> aligned
 * <dd> The components are aligned as specified
 *      by each component's X or Y alignment value.
 * </dl>
 * </blockquote>
 *
 * <p>
 *
 * Each SizeRequirements object contains information
 * about either the width (and X alignment)
 * or height (and Y alignment)
 * of a single component or a group of components:
 *
 * <blockquote>
 * <dl>
 * <dt> <code>minimum</code>
 * <dd> The smallest reasonable width/height of the component
 *      or component group, in pixels.
 *
 * <dt> <code>preferred</code>
 * <dd> The natural width/height of the component
 *      or component group, in pixels.
 *
 * <dt> <code>maximum</code>
 * <dd> The largest reasonable width/height of the component
 *      or component group, in pixels.
 *
 * <dt> <code>alignment</code>
 * <dd> The X/Y alignment of the component
 *      or component group.
 * </dl>
 * </blockquote>
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see Component#getMinimumSize
 * @see Component#getPreferredSize
 * @see Component#getMaximumSize
 * @see Component#getAlignmentX
 * @see Component#getAlignmentY
 *
 * @author Timothy Prinzing
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SizeRequirements implements Serializable {

    /**
     * The minimum size required.
     * For a component <code>comp</code>, this should be equal to either
     * <code>comp.getMinimumSize().width</code> or
     * <code>comp.getMinimumSize().height</code>.
     */
    public int minimum;

    /**
     * The preferred (natural) size.
     * For a component <code>comp</code>, this should be equal to either
     * <code>comp.getPreferredSize().width</code> or
     * <code>comp.getPreferredSize().height</code>.
     */
    public int preferred;

    /**
     * The maximum size allowed.
     * For a component <code>comp</code>, this should be equal to either
     * <code>comp.getMaximumSize().width</code> or
     * <code>comp.getMaximumSize().height</code>.
     */
    public int maximum;

    /**
     * The alignment, specified as a value between 0.0 and 1.0,
     * inclusive.
     * To specify centering, the alignment should be 0.5.
     */
    public float alignment;

    /**
     * Creates a SizeRequirements object with the minimum, preferred,
     * and maximum sizes set to zero and an alignment value of 0.5
     * (centered).
     */
    public SizeRequirements() {
        minimum = 0;
        preferred = 0;
        maximum = 0;
        alignment = 0.5f;
    }

    /**
     * Creates a SizeRequirements object with the specified minimum, preferred,
     * and maximum sizes and the specified alignment.
     *
     * @param min the minimum size &gt;= 0
     * @param pref the preferred size &gt;= 0
     * @param max the maximum size &gt;= 0
     * @param a the alignment &gt;= 0.0f &amp;&amp; &lt;= 1.0f
     */
    public SizeRequirements(int min, int pref, int max, float a) {
        minimum = min;
        preferred = pref;
        maximum = max;
        alignment = a > 1.0f ? 1.0f : a < 0.0f ? 0.0f : a;
    }

    /**
     * Returns a string describing the minimum, preferred, and maximum
     * size requirements, along with the alignment.
     *
     * @return the string
     */
    public String toString() {
        return "[" + minimum + "," + preferred + "," + maximum + "]@" + alignment;
    }

    /**
     * Determines the total space necessary to
     * place a set of components end-to-end.  The needs
     * of each component in the set are represented by an entry in the
     * passed-in SizeRequirements array.
     * The returned SizeRequirements object has an alignment of 0.5
     * (centered).  The space requirement is never more than
     * Integer.MAX_VALUE.
     *
     * @param children  the space requirements for a set of components.
     *   The vector may be of zero length, which will result in a
     *   default SizeRequirements object instance being passed back.
     * @return  the total space requirements.
     */
    public static SizeRequirements getTiledSizeRequirements(SizeRequirements[]
                                                            children) {
        SizeRequirements total = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            total.minimum = (int) Math.min((long) total.minimum + (long) req.minimum, Integer.MAX_VALUE);
            total.preferred = (int) Math.min((long) total.preferred + (long) req.preferred, Integer.MAX_VALUE);
            total.maximum = (int) Math.min((long) total.maximum + (long) req.maximum, Integer.MAX_VALUE);
        }
        return total;
    }

    /**
     * Determines the total space necessary to
     * align a set of components.  The needs
     * of each component in the set are represented by an entry in the
     * passed-in SizeRequirements array.  The total space required will
     * never be more than Integer.MAX_VALUE.
     *
     * @param children  the set of child requirements.  If of zero length,
     *  the returns result will be a default instance of SizeRequirements.
     * @return  the total space requirements.
     */
    public static SizeRequirements getAlignedSizeRequirements(SizeRequirements[]
                                                              children) {
        SizeRequirements totalAscent = new SizeRequirements();
        SizeRequirements totalDescent = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];

            int ascent = (int) (req.alignment * req.minimum);
            int descent = req.minimum - ascent;
            totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
            totalDescent.minimum = Math.max(descent, totalDescent.minimum);

            ascent = (int) (req.alignment * req.preferred);
            descent = req.preferred - ascent;
            totalAscent.preferred = Math.max(ascent, totalAscent.preferred);
            totalDescent.preferred = Math.max(descent, totalDescent.preferred);

            ascent = (int) (req.alignment * req.maximum);
            descent = req.maximum - ascent;
            totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
            totalDescent.maximum = Math.max(descent, totalDescent.maximum);
        }
        int min = (int) Math.min((long) totalAscent.minimum + (long) totalDescent.minimum, Integer.MAX_VALUE);
        int pref = (int) Math.min((long) totalAscent.preferred + (long) totalDescent.preferred, Integer.MAX_VALUE);
        int max = (int) Math.min((long) totalAscent.maximum + (long) totalDescent.maximum, Integer.MAX_VALUE);
        float alignment = 0.0f;
        if (min > 0) {
            alignment = (float) totalAscent.minimum / min;
            alignment = alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
        }
        return new SizeRequirements(min, pref, max, alignment);
    }

    /**
     * Creates a set of offset/span pairs representing how to
     * lay out a set of components end-to-end.
     * This method requires that you specify
     * the total amount of space to be allocated,
     * the size requirements for each component to be placed
     * (specified as an array of SizeRequirements), and
     * the total size requirement of the set of components.
     * You can get the total size requirement
     * by invoking the getTiledSizeRequirements method.  The components
     * will be tiled in the forward direction with offsets increasing from 0.
     *
     * @param allocated the total span to be allocated &gt;= 0.
     * @param total     the total of the children requests.  This argument
     *  is optional and may be null.
     * @param children  the size requirements for each component.
     * @param offsets   the offset from 0 for each child where
     *   the spans were allocated (determines placement of the span).
     * @param spans     the span allocated for each child to make the
     *   total target span.
     */
    public static void calculateTiledPositions(int allocated,
                                               SizeRequirements total,
                                               SizeRequirements[] children,
                                               int[] offsets,
                                               int[] spans) {
        calculateTiledPositions(allocated, total, children, offsets, spans, true);
    }

    /**
     * Creates a set of offset/span pairs representing how to
     * lay out a set of components end-to-end.
     * This method requires that you specify
     * the total amount of space to be allocated,
     * the size requirements for each component to be placed
     * (specified as an array of SizeRequirements), and
     * the total size requirement of the set of components.
     * You can get the total size requirement
     * by invoking the getTiledSizeRequirements method.
     *
     * This method also requires a flag indicating whether components
     * should be tiled in the forward direction (offsets increasing
     * from 0) or reverse direction (offsets decreasing from the end
     * of the allocated space).  The forward direction represents
     * components tiled from left to right or top to bottom.  The
     * reverse direction represents components tiled from right to left
     * or bottom to top.
     *
     * @param allocated the total span to be allocated &gt;= 0.
     * @param total     the total of the children requests.  This argument
     *  is optional and may be null.
     * @param children  the size requirements for each component.
     * @param offsets   the offset from 0 for each child where
     *   the spans were allocated (determines placement of the span).
     * @param spans     the span allocated for each child to make the
     *   total target span.
     * @param forward   tile with offsets increasing from 0 if true
     *   and with offsets decreasing from the end of the allocated space
     *   if false.
     * @since 1.4
     */
    public static void calculateTiledPositions(int allocated,
                                               SizeRequirements total,
                                               SizeRequirements[] children,
                                               int[] offsets,
                                               int[] spans,
                                               boolean forward) {
        // The total argument turns out to be a bad idea since the
        // total of all the children can overflow the integer used to
        // hold the total.  The total must therefore be calculated and
        // stored in long variables.
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < children.length; i++) {
            min += children[i].minimum;
            pref += children[i].preferred;
            max += children[i].maximum;
        }
        if (allocated >= pref) {
            expandedTile(allocated, min, pref, max, children, offsets, spans, forward);
        } else {
            compressedTile(allocated, min, pref, max, children, offsets, spans, forward);
        }
    }

    private static void compressedTile(int allocated, long min, long pref, long max,
                                       SizeRequirements[] request,
                                       int[] offsets, int[] spans,
                                       boolean forward) {

        // ---- determine what we have to work with ----
        float totalPlay = Math.min(pref - allocated, pref - min);
        float factor = (pref - min == 0) ? 0.0f : totalPlay / (pref - min);

        // ---- make the adjustments ----
        int totalOffset;
        if( forward ) {
            // lay out with offsets increasing from 0
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int)(req.preferred - play);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i], Integer.MAX_VALUE);
            }
        } else {
            // lay out with offsets decreasing from the end of the allocation
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int)(req.preferred - play);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    private static void expandedTile(int allocated, long min, long pref, long max,
                                     SizeRequirements[] request,
                                     int[] offsets, int[] spans,
                                     boolean forward) {

        // ---- determine what we have to work with ----
        float totalPlay = Math.min(allocated - pref, max - pref);
        float factor = (max - pref == 0) ? 0.0f : totalPlay / (max - pref);

        // ---- make the adjustments ----
        int totalOffset;
        if( forward ) {
            // lay out with offsets increasing from 0
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                int play = (int)(factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i], Integer.MAX_VALUE);
            }
        } else {
            // lay out with offsets decreasing from the end of the allocation
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                int play = (int)(factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    /**
     * Creates a bunch of offset/span pairs specifying how to
     * lay out a set of components with the specified alignments.
     * The resulting span allocations will overlap, with each one
     * fitting as well as possible into the given total allocation.
     * This method requires that you specify
     * the total amount of space to be allocated,
     * the size requirements for each component to be placed
     * (specified as an array of SizeRequirements), and
     * the total size requirements of the set of components
     * (only the alignment field of which is actually used).
     * You can get the total size requirement by invoking
     * getAlignedSizeRequirements.
     *
     * Normal alignment will be done with an alignment value of 0.0f
     * representing the left/top edge of a component.
     *
     * @param allocated the total span to be allocated &gt;= 0.
     * @param total     the total of the children requests.
     * @param children  the size requirements for each component.
     * @param offsets   the offset from 0 for each child where
     *   the spans were allocated (determines placement of the span).
     * @param spans     the span allocated for each child to make the
     *   total target span.
     */
    public static void calculateAlignedPositions(int allocated,
                                                 SizeRequirements total,
                                                 SizeRequirements[] children,
                                                 int[] offsets,
                                                 int[] spans) {
        calculateAlignedPositions( allocated, total, children, offsets, spans, true );
    }

    /**
     * Creates a set of offset/span pairs specifying how to
     * lay out a set of components with the specified alignments.
     * The resulting span allocations will overlap, with each one
     * fitting as well as possible into the given total allocation.
     * This method requires that you specify
     * the total amount of space to be allocated,
     * the size requirements for each component to be placed
     * (specified as an array of SizeRequirements), and
     * the total size requirements of the set of components
     * (only the alignment field of which is actually used)
     * You can get the total size requirement by invoking
     * getAlignedSizeRequirements.
     *
     * This method also requires a flag indicating whether normal or
     * reverse alignment should be performed.  With normal alignment
     * the value 0.0f represents the left/top edge of the component
     * to be aligned.  With reverse alignment, 0.0f represents the
     * right/bottom edge.
     *
     * @param allocated the total span to be allocated &gt;= 0.
     * @param total     the total of the children requests.
     * @param children  the size requirements for each component.
     * @param offsets   the offset from 0 for each child where
     *   the spans were allocated (determines placement of the span).
     * @param spans     the span allocated for each child to make the
     *   total target span.
     * @param normal    when true, the alignment value 0.0f means
     *   left/top; when false, it means right/bottom.
     * @since 1.4
     */
    public static void calculateAlignedPositions(int allocated,
                                                 SizeRequirements total,
                                                 SizeRequirements[] children,
                                                 int[] offsets,
                                                 int[] spans,
                                                 boolean normal) {
        float totalAlignment = normal ? total.alignment : 1.0f - total.alignment;
        int totalAscent = (int)(allocated * totalAlignment);
        int totalDescent = allocated - totalAscent;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            float alignment = normal ? req.alignment : 1.0f - req.alignment;
            int maxAscent = (int)(req.maximum * alignment);
            int maxDescent = req.maximum - maxAscent;
            int ascent = Math.min(totalAscent, maxAscent);
            int descent = Math.min(totalDescent, maxDescent);

            offsets[i] = totalAscent - ascent;
            spans[i] = (int) Math.min((long) ascent + (long) descent, Integer.MAX_VALUE);
        }
    }

    // This method was used by the JTable - which now uses a different technique.
    /**
     * Adjust a specified array of sizes by a given amount.
     *
     * @param delta     an int specifying the size difference
     * @param children  an array of SizeRequirements objects
     * @return an array of ints containing the final size for each item
     */
    public static int[] adjustSizes(int delta, SizeRequirements[] children) {
      return new int[0];
    }
}
