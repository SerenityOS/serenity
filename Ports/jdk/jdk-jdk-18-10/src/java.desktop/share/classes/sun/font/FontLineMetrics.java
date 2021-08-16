/*
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
 *
 */

/*
 *
 * (C) Copyright IBM Corp. 2003, All Rights Reserved
 *
 */

package sun.font;

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/**
 * Metrics from a font for layout of characters along a line
 * and layout of set of lines.
 * This and CoreMetrics replace what was previously a private internal class of Font
 */
public final class FontLineMetrics extends LineMetrics implements Cloneable {
    public int numchars; // mutated by Font
    public final CoreMetrics cm;
    public final FontRenderContext frc;

    public FontLineMetrics(int numchars, CoreMetrics cm, FontRenderContext frc) {
        this.numchars = numchars;
        this.cm = cm;
        this.frc = frc;
    }

    public int getNumChars() {
        return numchars;
    }

    public float getAscent() {
        return cm.ascent;
    }

    public float getDescent() {
        return cm.descent;
    }

    public float getLeading() {
        return cm.leading;
    }

    public float getHeight() {
        return cm.height;
    }

    public int getBaselineIndex() {
        return cm.baselineIndex;
    }

    public float[] getBaselineOffsets() {
        return cm.baselineOffsets.clone();
    }

    public float getStrikethroughOffset() {
        return cm.strikethroughOffset;
    }

    public float getStrikethroughThickness() {
        return cm.strikethroughThickness;
    }

    public float getUnderlineOffset() {
        return cm.underlineOffset;
    }

    public float getUnderlineThickness() {
        return cm.underlineThickness;
    }

    public int hashCode() {
        return cm.hashCode();
    }

    public boolean equals(Object rhs) {
        try {
            return cm.equals(((FontLineMetrics)rhs).cm);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    public Object clone() {
        // frc, cm do not need deep clone
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
