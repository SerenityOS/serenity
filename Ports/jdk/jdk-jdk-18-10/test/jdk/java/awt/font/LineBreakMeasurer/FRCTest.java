/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6448405 6519513 6745225
 * @summary static HashMap cache in LineBreakMeasurer can grow wihout bounds
 * @run main/othervm/timeout=600 -client -Xms16m -Xmx16m FRCTest
 */
import java.awt.*;
import java.awt.image.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.text.*;
import java.util.Hashtable;

public class FRCTest {

    static AttributedString vanGogh = new AttributedString(
        "Many people believe that Vincent van Gogh painted his best works " +
        "during the two-year period he spent in Provence. Here is where he " +
        "painted The Starry Night--which some consider to be his greatest " +
        "work of all. However, as his artistic brilliance reached new " +
        "heights in Provence, his physical and mental health plummeted. ",
        new Hashtable());

    public static void main(String[] args) {

        // First test the behaviour of Graphics2D.getFontRenderContext();
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        AffineTransform g2dTx = new AffineTransform(2,0,2,0,1,1);
        g2d.setTransform(g2dTx);
        AffineTransform frcTx = g2d.getFontRenderContext().getTransform();
        AffineTransform frcExpected = new AffineTransform(2,0,2,0,0,0);
        if (!frcTx.equals(frcExpected)) {
            throw new RuntimeException("FRC Tx may have translate?");
        }

        // Now test that using different translates with LBM is OK
        // This test doesn't prove a lot since showing a leak really
        // requires a basher test that can run for a long time.
        for (int x=0;x<100;x++) {
            for (int y=0;y<100;y++) {
                AttributedCharacterIterator aci = vanGogh.getIterator();
                AffineTransform tx = AffineTransform.getTranslateInstance(x, y);
                FontRenderContext frc = new FontRenderContext(tx, false, false);
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                lbm.setPosition(aci.getBeginIndex());
                while (lbm.getPosition() < aci.getEndIndex()) {
                    lbm.nextLayout(100f);
                }
            }
        }

        for (int x=0;x<25;x++) {
            for (int y=0;y<25;y++) {
                AttributedCharacterIterator aci = vanGogh.getIterator();
                double rot = Math.random()*.4*Math.PI - .2*Math.PI;
                AffineTransform tx = AffineTransform.getRotateInstance(rot);
                FontRenderContext frc = new FontRenderContext(tx, false, false);
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                lbm.setPosition(aci.getBeginIndex());
                while (lbm.getPosition() < aci.getEndIndex()) {
                    lbm.nextLayout(100f);
                }
            }
        }
    }
}
