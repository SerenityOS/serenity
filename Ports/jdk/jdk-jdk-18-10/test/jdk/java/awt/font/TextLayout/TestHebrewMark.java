/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @summary verify TextLayout handles Hebrew marks correctly
 * @bug 6529141
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;

public class TestHebrewMark {

    public static void main(String args[]) {
       FontRenderContext frc = new FontRenderContext(null,false,false);
       final String fonts[] = { "Arial", "Arial Hebrew", "Arial Unicode" };
       final char ALEF = '\u05D0';  // a letter
       final char QAMATS = '\u05B8';  // a combining mark, should show up UNDER the alef (no advance)
       final String string1 = "\u05DE\u05B8\u05E9\u05C1\u05B0\u05DB\u05B5\u05E0\u05B4\u05D9\u05D0\u05B7\u05D7\u05B2\u05E8\u05B6\u05D9\u05DA\u05B8\u05E0\u05BC\u05B8\u05E8\u05D5\u05BC\u05E6\u05B8\u05D4\u05D4\u05B1\u05D1\u05B4\u05D9\u05D0\u05B7\u05E0\u05B4\u05D9\u05D4\u05B7\u05DE\u05BC\u05B6\u05DC\u05B6\u05DA\u05B0\u05D7\u05B2\u05D3\u05B8\u05E8\u05B8\u05D9\u05D5\u05E0\u05B8\u05D2\u05B4\u05D9\u05DC\u05B8\u05D4\u05D5\u05B0\u05E0\u05B4\u05E9\u05C2\u05B0\u05DE\u05B0\u05D7\u05B8\u05D4\u0020\u05D1\u05BC\u05B8\u05DA\u05B0\u05E0\u05B7\u05D6\u05B0\u05DB\u05BC\u05B4\u05D9\u05E8\u05B8\u05D4\u05D3\u05B9\u05D3\u05B6\u05D9\u05DA\u05B8\u05DE\u05B4\u05D9\u05BC\u05B7\u05D9\u05B4\u05DF\u05DE\u05B5\u05D9\u05E9\u05C1\u05B8\u05E8\u05B4\u05D9\u05DD\u05D0\u05B2\u05D4\u05B5\u05D1\u05D5\u05BC\u05DA\u05B8";
       final String string2 = string1.replaceAll("\u05B8", ""); // remove qamats
       int string1len = string1.length();
       int string2len = string2.length();
       System.out.println("String1 has " + string1len+" chars, and string2 (without the QAMATS) has " + string2.length());
       if(string1len == string2len) {
           throw new RuntimeException("Hey, string1 and string2 are both " + string1len + " chars long - shouldn't happen.");
       }
       Font f = null;
       // try to find a font that will work
       for(String fontname : fonts ) {
          System.err.println("trying: " +fontname);
           Font afont = new Font(fontname,Font.PLAIN,18);
           if(!afont.getFontName().equals(fontname)) {
             System.out.println(fontname + ": is actually  " + afont.getFontName() + " - skipping this font.");
             continue;
           }
           if(!afont.canDisplay(ALEF) || !afont.canDisplay(QAMATS)) {
             System.out.println(fontname + ": can't display ALEF or QAMATS - skipping this font");
             continue;
           }
           f = afont;
        System.err.println("Might be OK: " + fontname);
        System.out.println("Using font " + f.getFontName());
        TextLayout tl = new TextLayout(string1, f, frc);
        TextLayout tl2 = new TextLayout(string2, f, frc);
        Rectangle2D tlBounds = tl.getBounds();
        Rectangle2D tlBounds2 = tl2.getBounds();
        System.out.println("tlbounds="+tlBounds);
        System.out.println("tl.getAdvance()="+tl.getAdvance());
        System.out.println("tl2bounds="+tlBounds2);
        System.out.println("tl2.getAdvance()="+tl2.getAdvance());

        if(tl.getAdvance() != tl2.getAdvance()) {
          throw new RuntimeException("Advance of string with and without QAMATS differs: " + tl.getAdvance() + " vs. " + tl2.getAdvance());
        } else {
          System.out.println("6529141 OK, widths are same.");
        }
       }
       // print a notice if none of them worked.
       if(f == null) {
           System.out.println("Could not find a suitable font - skipping this test.");
           return;
       }
   }
}
