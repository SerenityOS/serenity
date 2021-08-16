/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

/**
 * Class to efficiently store glyph information for laid out glyphs,
 * passed to native or java backend.
 *
 * @author Clemens Eisserer
 */
public class GrowableEltArray extends GrowableIntArray {
    private static final int ELT_SIZE = 4;
    GrowableIntArray glyphs;

    public GrowableEltArray(int initialSize)
    {
        super(ELT_SIZE, initialSize);
        glyphs = new GrowableIntArray(1, initialSize*8);
    }

    public final int getCharCnt(int index) {
        return array[getCellIndex(index) + 0];
    }

    public final void setCharCnt(int index, int cnt) {
        array[getCellIndex(index) + 0] = cnt;
    }

    public final int getXOff(int index) {
        return array[getCellIndex(index) + 1];
    }

    public final void setXOff(int index, int xOff) {
        array[getCellIndex(index) + 1] = xOff;
    }

    public final int getYOff(int index) {
        return array[getCellIndex(index) + 2];
    }

    public final void setYOff(int index, int yOff) {
        array[getCellIndex(index) + 2] = yOff;
    }

    public final int getGlyphSet(int index) {
        return array[getCellIndex(index) + 3];
    }

    public final void setGlyphSet(int index, int glyphSet) {
        array[getCellIndex(index) + 3] = glyphSet;
    }

    public GrowableIntArray getGlyphs() {
        return glyphs;
    }

    public void clear() {
        glyphs.clear();
        super.clear();
    }
}
