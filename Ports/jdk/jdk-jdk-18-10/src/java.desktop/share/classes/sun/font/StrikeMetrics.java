/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/* These are font metrics: they are in user space, not device space.
 * Hence they are not truly "strike" metrics. However it is convenient to
 * treat them as such since we need to have a scaler context to obtain them
 * and also to cache them. The old implementation obtained a C++ strike object
 * that matched the Font TX + pt size only. It was wasteful of strike objects.
 * This new implementation still has separate StrikeMetrics for 2 fonts that
 * are really the same but are used in different device transforms, but at
 * least it doesn't create a whole new strike just to get the metrics for
 * a strike in a transformed graphics.
 * So these metrics do not take into account the device transform. They
 * are considered inherent properties of the font. Hence it may be that we
 * should use the device transform to obtain the most accurate metrics, but
 * typically 1.1 APIs do not provide for this. So some APIs may want to
 * ignore the dev. tx and others may want to use it, and then apply an
 * inverse transform. For now we ignore the dev. tx.
 * "Font" metrics are representative of a typical glyph in the font.
 * Generally speaking these values are the choice of the font designer and
 * are stored in the font, from which we retrieve the values. They do
 * not necessarily equate to the maximum bounds of all glyphs in the font.
 * Note that the ascent fields are typically a -ve value as we use a top-left
 * origin user space, and text is positioned relative to its baseline.
 */
public final class StrikeMetrics {

    public float ascentX;
    public float ascentY;
    public float descentX;
    public float descentY;
    public float baselineX;
    public float baselineY;
    public float leadingX;
    public float leadingY;
    public float maxAdvanceX;
    public float maxAdvanceY;


    /* The no-args constructor is used by CompositeStrike, which then
     * merges in the metrics of physical fonts.
     * The approach here is the same as earlier releases but it is flawed
     * take for example the following which ignores leading for simplicity.
     * Say we have a composite with an element asc=-9, dsc=2, and another with
     * asc=-7, dsc=3.  The merged font is (-9,3) for height of -(-9)+3=12.
     * Suppose this same font has been derived with a 180% rotation
     * Now its signs for ascent/descent are reversed. Its (9,-2) and (7,-3)
     * Its merged values are (using the code in this class) (7,-2) for
     * a height of -(7)+-2 = =-9!
     * We need to have a more intelligent merging algorithm,
     * which so far as I can see needs to apply an inverse of the font
     * tx, do its merging, and then reapply the font tx.
     * This wouldn't often be a problem as there rarely is a font TX, and
     * the tricky part is getting the information. Probably the no-args
     * constructor needs to pass a TX in to be applied to all merges.
     * CompositeStrike would be left with the problem of figuring out what
     * tx to use.
     * But at least for now we are probably no worse than 1.4 ...
     * REMIND: FIX THIS.
     */
    StrikeMetrics() {
        ascentX = ascentY = Integer.MAX_VALUE;
        descentX = descentY = leadingX = leadingY = Integer.MIN_VALUE;
        baselineX = baselineY = maxAdvanceX = maxAdvanceY = Integer.MIN_VALUE;
    }

    StrikeMetrics(float ax, float ay, float dx, float dy, float bx, float by,
                  float lx, float ly, float mx, float my) {
       ascentX = ax;
       ascentY = ay;
       descentX = dx;
       descentY = dy;
       baselineX = bx;
       baselineY = by;
       leadingX = lx;
       leadingY = ly;
       maxAdvanceX = mx;
       maxAdvanceY = my;
    }

    public float getAscent() {
        return -ascentY;
    }

    public float getDescent() {
        return descentY;
    }

    public float getLeading() {
        return leadingY;
    }

    public float getMaxAdvance() {
        return maxAdvanceX;
    }

    /*
     * Currently only used to merge together slot metrics to create
     * the metrics for a composite font.
     */
     void merge(StrikeMetrics other) {
         if (other == null) {
             return;
         }
         if (other.ascentX < ascentX) {
             ascentX = other.ascentX;
         }
         if (other.ascentY < ascentY) {
             ascentY = other.ascentY;
         }
         if (other.descentX > descentX) {
             descentX = other.descentX;
         }
         if (other.descentY > descentY) {
             descentY = other.descentY;
         }
         if (other.baselineX > baselineX) {
             baselineX = other.baselineX;
         }
         if (other.baselineY > baselineY) {
             baselineY = other.baselineY;
         }
         if (other.leadingX > leadingX) {
             leadingX = other.leadingX;
         }
         if (other.leadingY > leadingY) {
             leadingY = other.leadingY;
         }
         if (other.maxAdvanceX > maxAdvanceX) {
             maxAdvanceX = other.maxAdvanceX;
         }
         if (other.maxAdvanceY > maxAdvanceY) {
             maxAdvanceY = other.maxAdvanceY;
         }
    }

    /* Used to transform the values back into user space.
     * This is done ONCE by the strike so clients should not need
     * to worry about this
     */
    void convertToUserSpace(AffineTransform invTx) {
        Point2D.Float pt2D = new Point2D.Float();

        pt2D.x = ascentX; pt2D.y = ascentY;
        invTx.deltaTransform(pt2D, pt2D);
        ascentX = pt2D.x; ascentY = pt2D.y;

        pt2D.x = descentX; pt2D.y = descentY;
        invTx.deltaTransform(pt2D, pt2D);
        descentX = pt2D.x; descentY = pt2D.y;

        pt2D.x = baselineX; pt2D.y = baselineY;
        invTx.deltaTransform(pt2D, pt2D);
        baselineX = pt2D.x; baselineY = pt2D.y;

        pt2D.x = leadingX; pt2D.y = leadingY;
        invTx.deltaTransform(pt2D, pt2D);
        leadingX = pt2D.x; leadingY = pt2D.y;

        pt2D.x = maxAdvanceX; pt2D.y = maxAdvanceY;
        invTx.deltaTransform(pt2D, pt2D);
        maxAdvanceX = pt2D.x; maxAdvanceY = pt2D.y;
    }

    public String toString() {
        return "ascent:x="      + ascentX +     " y=" + ascentY +
               " descent:x="    + descentX +    " y=" + descentY +
               " baseline:x="   + baselineX +   " y=" + baselineY +
               " leading:x="    + leadingX +    " y=" + leadingY +
               " maxAdvance:x=" + maxAdvanceX + " y=" + maxAdvanceY;
    }
}
