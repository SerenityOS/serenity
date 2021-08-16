/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

import java.awt.Font;

import sun.font.Font2D;
import sun.font.FontStrike;

/*
 * A FontInfo object holds all calculated or derived data needed
 * to handle rendering operations based on a particular set of
 * Graphics2D rendering attributes.
 * Note that this does not use a Font2DHandle, and also has a reference
 * to the strike which also references the Font2D.
 * So presently, until SG2D objects no longer reference this FontInfo,
 * there is still some potential for a bad Font2D to be used for a short
 * time. I am reluctant to add the overhead of that machinery here without
 * a proven benefit.
 */
public class FontInfo implements Cloneable {
    public Font font;
    public Font2D font2D;
    public FontStrike fontStrike;
    public double[] devTx;
    public double[] glyphTx;
    public boolean nonInvertibleTx;
    public int pixelHeight;
    public float originX;
    public float originY;
    public int aaHint;
    public boolean lcdRGBOrder;
    /* lcdSubPixPos is used if FM is ON for HRGB/HBGR LCD text mode */
    public boolean lcdSubPixPos;

    public String mtx(double[] matrix) {
        return ("["+
                matrix[0]+", "+
                matrix[1]+", "+
                matrix[2]+", "+
                matrix[3]+
                "]");
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public String toString() {
        return ("FontInfo["+
                "font="+font+", "+
                "devTx="+mtx(devTx)+", "+
                "glyphTx="+mtx(glyphTx)+", "+
                "pixelHeight="+pixelHeight+", "+
                "origin=("+originX+","+originY+"), "+
                "aaHint="+aaHint+", "+
                "lcdRGBOrder="+(lcdRGBOrder ? "RGB" : "BGR")+
                "lcdSubPixPos="+lcdSubPixPos+
                "]");
    }
}
