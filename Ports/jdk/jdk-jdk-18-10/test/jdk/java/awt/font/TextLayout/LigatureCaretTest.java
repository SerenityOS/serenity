/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 *  @test
 *  @bug 4178145 8144015
*/

/*
 * Copyright 1998 IBM Corp.  All Rights Reserved.
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TextHitInfo;
import java.awt.font.FontRenderContext;
import java.util.Hashtable;

/**
 * This test ensures that TextLayout will not place a caret within
 * an Arabic lam-alef ligature, and will correctly caret through
 * bidirectional text with numbers.
 */

public class LigatureCaretTest {

    public static void main(String[] args) {

        testBidiWithNumbers();
        testLamAlef();
        System.out.println("LigatureCaretTest PASSED");
    }

    private static final FontRenderContext frc =
                                new FontRenderContext(null, false, false);

    private static Font getFontForText(String s) {
        GraphicsEnvironment ge =
           GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();

        for (Font f : fonts) {
           if (f.canDisplayUpTo(s) == -1) {
               return f.deriveFont(Font.PLAIN, 24);
           }
        }
        return null;
    }

    /**
     * Caret through text mixed-direction text and check the results.
     * If the test fails an Error is thrown.
     * @exception an Error is thrown if the test fails
     */
    public static void testBidiWithNumbers() {

        String bidiWithNumbers = "abc\u05D0\u05D1\u05D2123abc";
        Font font = getFontForText(bidiWithNumbers);
        if (font == null) {
            return;
        }
        Hashtable map = new Hashtable();
        map.put(TextAttribute.FONT, font);

        // visual order for the text:
        // abc123<gimel><bet><aleph>abc

        int[] carets = { 0, 1, 2, 3, 7, 8, 6, 5, 4, 9, 10, 11, 12 };
        TextLayout layout = new TextLayout(bidiWithNumbers, map, frc);

        // Caret through TextLayout in both directions and check results.
        for (int i=0; i < carets.length-1; i++) {

            TextHitInfo hit = layout.getNextRightHit(carets[i]);
            if (hit.getInsertionIndex() != carets[i+1]) {
                throw new Error("right hit failed within layout");
            }
        }

        if (layout.getNextRightHit(carets[carets.length-1]) != null) {
            throw new Error("right hit failed at end of layout");
        }

        for (int i=carets.length-1; i > 0; i--) {

            TextHitInfo hit = layout.getNextLeftHit(carets[i]);
            if (hit.getInsertionIndex() != carets[i-1]) {
                throw new Error("left hit failed within layout");
            }
        }

        if (layout.getNextLeftHit(carets[0]) != null) {
            throw new Error("left hit failed at end of layout");
        }
    }

    /**
     * Ensure proper careting and hit-testing behavior with
     * a lam-alef ligature.
     * If the test fails, an Error is thrown.
     * @exception an Error is thrown if the test fails
     */
    public static void testLamAlef() {

        // lam-alef form a mandantory ligature.
        final String lamAlef = "\u0644\u0627";
        final String ltrText = "abcd";

        Font font = getFontForText(lamAlef+ltrText);
        if (font == null) {
            return;
        }
        Hashtable map = new Hashtable();
        map.put(TextAttribute.FONT, font);

        // Create a TextLayout with just a lam-alef sequence.  There
        // should only be two valid caret positions:  one at
        // insertion offset 0 and the other at insertion offset 2.
        TextLayout layout = new TextLayout(lamAlef, map, frc);

        TextHitInfo hit;

        hit = layout.getNextLeftHit(0);
        if (hit.getInsertionIndex() != 2) {
            throw new Error("Left hit failed.  Hit:" + hit);
        }

        hit = layout.getNextRightHit(2);
        if (hit.getInsertionIndex() != 0) {
            throw new Error("Right hit failed.  Hit:" + hit);
        }

        hit = layout.hitTestChar(layout.getAdvance()/2, 0);
        if (hit.getInsertionIndex() != 0 && hit.getInsertionIndex() != 2) {
            throw new Error("Hit-test allowed incorrect caret.  Hit:" + hit);
        }


        // Create a TextLayout with some left-to-right text
        // before the lam-alef sequence.  There should not be
        // a caret position between the lam and alef.
        layout = new TextLayout(ltrText+lamAlef, map, frc);

        final int ltrLen = ltrText.length();
        final int layoutLen = layout.getCharacterCount();

        for (int i=0; i < ltrLen; i++) {
            hit = layout.getNextRightHit(i);
            if (hit.getInsertionIndex() != i+1) {
                throw new Error("Right hit failed in ltr text.");
            }
        }

        hit = layout.getNextRightHit(ltrLen);
        if (layoutLen != hit.getInsertionIndex()) {
            throw new Error("Right hit failed at direction boundary.");
        }

        hit = layout.getNextLeftHit(layoutLen);
        if (hit.getInsertionIndex() != ltrLen) {
            throw new Error("Left hit failed at end of text.");
        }
    }
}
