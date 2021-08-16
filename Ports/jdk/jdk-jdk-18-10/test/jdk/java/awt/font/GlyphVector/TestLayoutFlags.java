/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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


/* @test
   @bug 4328745 5090704 8166111 8176510 8202767
   @summary exercise getLayoutFlags, getGlyphCharIndex, getGlyphCharIndices
 */

import java.awt.Font;
import static java.awt.Font.*;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import static java.awt.font.GlyphVector.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class TestLayoutFlags {

    static public void main(String[] args) {
        new TestLayoutFlags().runTest();
    }

    void runTest() {

        Font font = findFont("abcde");
        if (font == null) {
           return; // this system is no use for this test.
        }

        String latin1 = "This is a latin1 string"; // none
        String hebrew = "\u05d0\u05d1\u05d2\u05d3"; // rtl
        String arabic = "\u0646\u0644\u0622\u0646"; // rtl + mc/g
        String hindi = "\u0939\u093f\u0923\u094d\u0921\u0940"; // ltr + reorder
        String tamil = "\u0b9c\u0bcb"; // ltr + mg/c + split

        FontRenderContext frc = new FontRenderContext(null, true, true);

        // get glyph char indices needs to initializes layoutFlags before
        // use (5090704)
        {
          GlyphVector gv = font.createGlyphVector(frc, "abcde");
          int ix = gv.getGlyphCharIndex(0);
          if (ix != 0) {
            throw new Error("glyph 0 incorrectly mapped to char " + ix);
          }
          int[] ixs = gv.getGlyphCharIndices(0, gv.getNumGlyphs(), null);
          for (int i = 0; i < ixs.length; ++i) {
            if (ixs[i] != i) {
              throw new Error("glyph " + i + " incorrectly mapped to char " + ixs[i]);
            }
          }
        }

        GlyphVector latinGV = makeGlyphVector("latin", latin1, false, frc);
        GlyphVector hebrewGV = makeGlyphVector("hebrew", hebrew, true, frc);
        GlyphVector arabicGV = makeGlyphVector("arabic", arabic, true, frc);
        GlyphVector hindiGV = makeGlyphVector("devanagari", hindi, false, frc);
        GlyphVector tamilGV = makeGlyphVector("tamil", tamil, false, frc);

        GlyphVector latinPos = font.createGlyphVector(frc, latin1);
        Point2D pt = latinPos.getGlyphPosition(0);
        pt.setLocation(pt.getX(), pt.getY() + 1.0);
        latinPos.setGlyphPosition(0, pt);

        GlyphVector latinTrans = font.createGlyphVector(frc, latin1);
        latinTrans.setGlyphTransform(0, AffineTransform.getRotateInstance(.15));

        test("latin", latinGV, true, 0);
        test("hebrew", hebrewGV, true,  FLAG_RUN_RTL);
        test("arabic", arabicGV, true, FLAG_COMPLEX_GLYPHS | FLAG_RUN_RTL);
        test("hindi", hindiGV, true, FLAG_COMPLEX_GLYPHS);
        test("tamil", tamilGV, true, FLAG_COMPLEX_GLYPHS);
        test("pos", latinPos, true, 0);
        test("trans", latinTrans, false, 0);
    }

    static boolean isLogicalFont(Font f) {
        String family = f.getFamily().toLowerCase();
        switch (family) {
          case "dialog":
          case "dialoginput":
          case "serif":
          case "sansserif":
          case "monospaced": return true;
          default: return false;
        }
    }

    Font[] allFonts = null;

    Font findFont(String text) {
        if (allFonts == null) {
            allFonts =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        }
        for (Font f : allFonts) {
            if (isLogicalFont(f)) {
                continue;
            }
            if (f.canDisplayUpTo(text) == -1) {
                 return f.deriveFont(Font.PLAIN, 24);
            }
        }
        return null;
    }

    GlyphVector makeGlyphVector(String script, String text,
                                boolean rtl, FontRenderContext frc) {

        Font font = findFont(text);
        if (font == null) {
            System.out.println("No font found for script " + script);
            return null;
        } else {
            System.out.println("Using " + font.getFontName() +
                               " for script " + script);
        }
        int flags = rtl ? LAYOUT_RIGHT_TO_LEFT : LAYOUT_LEFT_TO_RIGHT;
        return font.layoutGlyphVector(frc, text.toCharArray(),
                                      0, text.length(), flags);
    }

    void test(String name, GlyphVector gv, boolean layout, int allowedFlags) {

        if (gv == null) {
            return;
        }
        int iv = (layout) ? FLAG_HAS_POSITION_ADJUSTMENTS : 0;

        int computedFlags = computeFlags(gv, iv) & gv.FLAG_MASK;
        int actualFlags = gv.getLayoutFlags() & gv.FLAG_MASK;

        System.out.println("\n*** " + name + " ***");
        System.out.println(" test flags");
        System.out.print("computed ");
        printFlags(computedFlags);
        System.out.print("  actual ");
        printFlags(actualFlags);
        System.out.print("allowed layout ");
        printFlags(allowedFlags);

        if (computedFlags != actualFlags) {
            // Depending on the font, if layout is run for a RTL script
            // you might get that flag set, or COMPLEX_GLYPHS instead.
            boolean error = false;
            int COMPLEX_RTL = FLAG_COMPLEX_GLYPHS | FLAG_RUN_RTL;
            if (allowedFlags == 0) {
                error = (allowedFlags & COMPLEX_RTL) != 0;
            }
            if (allowedFlags == FLAG_RUN_RTL) {
               error = (actualFlags & FLAG_COMPLEX_GLYPHS) != 0;
            }
            if (allowedFlags == FLAG_COMPLEX_GLYPHS) {
               error = (actualFlags & FLAG_RUN_RTL) != 0;
            }
            if (allowedFlags == COMPLEX_RTL) {
               error = (actualFlags & COMPLEX_RTL) == 0;
            }
            if (error) {
                throw new Error("layout flags in test: " + name +
                     " expected: " + Integer.toHexString(computedFlags) +
                     " but got: " + Integer.toHexString(actualFlags));
            }
        }
    }

    static public void printFlags(int flags) {
        System.out.print("flags:");
        if ((flags & FLAG_HAS_POSITION_ADJUSTMENTS) != 0) {
            System.out.print(" pos");
        }
        if ((flags & FLAG_HAS_TRANSFORMS) != 0) {
            System.out.print(" trans");
        }
        if ((flags & FLAG_RUN_RTL) != 0) {
            System.out.print(" rtl");
        }
        if ((flags & FLAG_COMPLEX_GLYPHS) != 0) {
            System.out.print(" complex");
        }
        if ((flags & FLAG_MASK) == 0) {
            System.out.print(" none");
        }
        System.out.println();
    }

    int computeFlags(GlyphVector gv, int initValue) {
        validateCharIndexMethods(gv);

        int result = initValue;
        if (glyphsAreRTL(gv)) {
            result |= FLAG_RUN_RTL;
        }
        if (hasComplexGlyphs(gv)) {
            result |= FLAG_COMPLEX_GLYPHS;
        }

        if (gv.getFont().isTransformed()) {
            result |= FLAG_HAS_TRANSFORMS;
        }
        return result;
    }

    /**
     * throw an exception if getGlyphCharIndices returns a different result than
     * you get from iterating through getGlyphCharIndex one at a time.
     */
    void validateCharIndexMethods(GlyphVector gv) {
        int[] indices = gv.getGlyphCharIndices(0, gv.getNumGlyphs(), null);
        for (int i = 0; i < gv.getNumGlyphs(); ++i) {
            if (gv.getGlyphCharIndex(i) != indices[i]) {
                throw new Error("glyph index mismatch at " + i);
            }
        }
    }

    /**
     * Return true if the glyph indices are pure ltr
     */
    boolean glyphsAreLTR(GlyphVector gv) {
        int[] indices = gv.getGlyphCharIndices(0, gv.getNumGlyphs(), null);
        for (int i = 0; i < indices.length; ++i) {
            if (indices[i] != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if the glyph indices are pure rtl
     */
    boolean glyphsAreRTL(GlyphVector gv) {
        int[] indices = gv.getGlyphCharIndices(0, gv.getNumGlyphs(), null);
        for (int i = 0; i < indices.length; ++i) {
            if (indices[i] != indices.length - i - 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if there is a local reordering (the run is not ltr or rtl).
     * !!! We can't have mixed bidi runs in the glyphs.
     */
    boolean hasComplexGlyphs(GlyphVector gv) {
        return !glyphsAreLTR(gv) && !glyphsAreRTL(gv);
    }
}
