/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * @test
 * @bug 7160052
 * @run main TestStandardGlyphVectorBug
 * @summary GlyphVector.setGlyphPosition should not throw an exception on valid input
 */
public class TestStandardGlyphVectorBug
{
    public static void main(String[] args)
    {
        Font defaultFont = new Font(null);
        FontRenderContext defaultFrc = new FontRenderContext(new AffineTransform(),
                                                             true, true);
        GlyphVector gv = defaultFont.createGlyphVector(defaultFrc, "test");

        //this causes the bounds to be cached
        //which is necessary to trigger the bug
        gv.getGlyphLogicalBounds(0);

        //this correctly gets the position of the overall advance
        Point2D glyphPosition = gv.getGlyphPosition(gv.getNumGlyphs());

        // this sets the position of the overall advance,
        // but also incorrectly tries to clear the bounds cache
        // of a specific glyph indexed by the glyphIndex parameter
        // even if the glyphIndex represents the overall advance
        // (i.e. if glyphIndex == getNumGlyphs())
        gv.setGlyphPosition(gv.getNumGlyphs(), glyphPosition);
    }
}
