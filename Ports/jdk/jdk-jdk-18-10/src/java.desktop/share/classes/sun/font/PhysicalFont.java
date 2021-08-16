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

import java.awt.FontFormatException;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public abstract class PhysicalFont extends Font2D {

    protected String platName;
    // nativeNames is a String or a (possibly null) String[].
    protected Object nativeNames;

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        PhysicalFont other = (PhysicalFont)o;
        return
           (this.fullName.equals(other.fullName)) &&
            ((this.platName == null && other.platName == null) ||
             (this.platName != null && this.platName.equals(other.platName)));
    }

    public int hashCode() {
        return fullName.hashCode() +
               (platName != null ? platName.hashCode() : 0);
    }

    /**
     * Opens the file (temporarily) and does basic verification.
     * Initializes the CMAP
     * @throws FontFormatException if the font can't be opened
     * or fails verification,  or there's no usable cmap
     */
    PhysicalFont(String platname, Object nativeNames)
        throws FontFormatException {

        handle = new Font2DHandle(this);
        this.platName = platname;
        this.nativeNames = nativeNames;
    }

    protected PhysicalFont() {
        handle = new Font2DHandle(this);
    }

    /* The following methods are delegated to the font by the strike
     * for physical fonts as the PhysicalFont holds a shared reference
     * to the native resource, so all invocations need to be directed
     * through a synchronization point. Implementations of these methods
     * will typically be "synchronized native"
     */

    Point2D.Float getGlyphPoint(long pScalerContext,
                             int glyphCode, int ptNumber) {
        return new Point2D.Float();
    }

    /* These 3 metrics methods should be implemented to return
     * values in user space.
     */
    abstract StrikeMetrics getFontMetrics(long pScalerContext);

    abstract float getGlyphAdvance(long pScalerContext, int glyphCode);

    abstract void getGlyphMetrics(long pScalerContext, int glyphCode,
                                  Point2D.Float metrics);

    abstract long getGlyphImage(long pScalerContext, int glyphCode);

    /* These 3 outline methods should be implemented to return
     * values in device space. Callers need to be aware of this
     * as typically Java client code will need to have them in user space.
     */
    abstract Rectangle2D.Float getGlyphOutlineBounds(long pScalerContext,
                                                     int glyphCode);

    abstract GeneralPath getGlyphOutline(long pScalerContext, int glyphCode,
                                         float x, float y);

    abstract GeneralPath getGlyphVectorOutline(long pScalerContext,
                                               int[] glyphs, int numGlyphs,
                                               float x, float y);
}
