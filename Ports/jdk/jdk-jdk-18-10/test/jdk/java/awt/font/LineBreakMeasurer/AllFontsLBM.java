/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8012617
 * @summary ArrayIndexOutOfBoundsException in LineBreakMeasurer
 */

import java.awt.*;
import java.awt.image.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;
import java.util.Hashtable;

public class AllFontsLBM {

    public static void main(String[] args) {
        Font[] allFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        for (int i=0;i<allFonts.length; i++) {
           try {
           Font f = allFonts[i].deriveFont(Font.PLAIN, 20);

           if ( f.getFontName().startsWith("HiraKaku") ) {
               continue;
           }

           System.out.println("Try : " + f.getFontName());
           System.out.flush();
           breakLines(f);
           } catch (Exception e) {
              System.out.println(allFonts[i]);
           }
        }
    }

     static void breakLines(Font font) {
        AttributedString vanGogh = new AttributedString(
        "Many people believe that Vincent van Gogh painted his best works " +
        "during the two-year period he spent in Provence. Here is where he " +
        "painted The Starry Night--which some consider to be his greatest " +
        "work of all. However, as his artistic brilliance reached new " +
        "heights in Provence, his physical and mental health plummeted. ",
        new Hashtable());
        vanGogh.addAttribute(TextAttribute.FONT, font);
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        AttributedCharacterIterator aci = vanGogh.getIterator();
        FontRenderContext frc = new FontRenderContext(null, false, false);
        LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
        lbm.setPosition(aci.getBeginIndex());
        while (lbm.getPosition() < aci.getEndIndex()) {
             lbm.nextLayout(100f);
        }

    }
}
