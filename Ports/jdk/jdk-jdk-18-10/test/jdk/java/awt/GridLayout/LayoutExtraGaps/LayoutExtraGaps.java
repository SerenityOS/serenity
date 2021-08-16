/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
   @test
  @key headful
   @bug 4370316
   @summary GridLayout does not centre its component properly
    (summary was GridLayout does not fill its Container)
   @library ../../regtesthelpers
   @build Util
   @author Andrei Dmitriev : area=awt.layout
   @run main LayoutExtraGaps
*/

import java.awt.*;
import java.awt.event.*;
import test.java.awt.regtesthelpers.Util;

public class LayoutExtraGaps extends Frame {
    final static int compCount = 30;

    public LayoutExtraGaps() {
        super("GridLayoutTest");
        Panel yellowPanel = new Panel(new GridLayout(compCount, 1, 3, 3));
        yellowPanel.setBackground(Color.yellow);

        for(int i = 0; i < compCount ; i++) {
            Label redLabel = new Label(""+i);
            redLabel.setBackground(Color.red);
            yellowPanel.add(redLabel);
        }

        Panel bluePanel = new Panel(new GridLayout(1, compCount, 3, 3));
        bluePanel.setBackground(Color.blue);

        for(int i = 0; i < compCount; i++) {
            Label greenLabel = new Label(""+i);
            greenLabel.setBackground(Color.green);
            bluePanel.add(greenLabel);
        }

        //RTL orientation
        Panel blackPanel = new Panel(new GridLayout(compCount, 1, 3, 3));
        blackPanel.setBackground(Color.black);
        blackPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        for(int i = 0; i < compCount ; i++) {
            Label redLabel = new Label(""+i);
            redLabel.setBackground(Color.red);
            blackPanel.add(redLabel);
        }

        Panel redPanel = new Panel(new GridLayout(1, compCount, 3, 3));
        redPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        redPanel.setBackground(Color.red);

        for(int i = 0; i < compCount; i++) {
            Label greenLabel = new Label(""+i);
            greenLabel.setBackground(Color.green);
            redPanel.add(greenLabel);
        }

        setLayout(new GridLayout(2, 2, 20, 20));

        add(yellowPanel);
        add(bluePanel);
        add(redPanel);
        add(blackPanel);
        pack();
        setSize((int)getPreferredSize().getWidth() + 50, (int)getPreferredSize().getHeight() + 50);
        setVisible(true);

        Util.waitForIdle(Util.createRobot());

        if (isComponentCentredLTR(yellowPanel) && isComponentCentredLTR(bluePanel)
                && isComponentCentredLTR(blackPanel) && isComponentCentredRTL(redPanel))
        {
            System.out.println("Test passed.");
        } else {
            throw new RuntimeException("Test failed. GridLayout doesn't center component.");
        }
    }

    /**
     * Checks if the components under Panel p are properly centred (i.e.
     * opposite borders between the Panel and component are equal). Panel p
     * must not be affect by RTL orientation (RTL only affects horizontal
     * positioning and components lay out from top right corner).
     *
     * @param      p the panel where the components exist and is not affected
     *             by right to left orientation
     * @return     true if components of panel p are properly centre, false
     *             otherwise
     */
    public static boolean isComponentCentredLTR(Panel p) {
        double borderLeft;
        double borderRight;
        double borderTop;
        double borderBottom;

        //The first component(rectangle) in panel p.
        Rectangle firstRec = p.getComponent(0).getBounds();

        //The last component(rectangle) in panel p.
        Rectangle lastRec = p.getComponent(compCount - 1).getBounds();

        System.out.println("bounds of the first rectangle in "+ p.getName() + " = " + firstRec);
        System.out.println("bounds of the last rectangle in "+ p.getName() + " = " + lastRec);

        borderLeft = firstRec.getX();
        borderRight = p.getWidth() - lastRec.getWidth() - lastRec.getX();
        borderTop = firstRec.getY();
        borderBottom = p.getHeight() - lastRec.getHeight() - lastRec.getY();

        return areBordersEqual(borderLeft, borderRight) &&
                areBordersEqual(borderTop, borderBottom);
    }

    /**
     * Checks if the components under Panel p are properly centred (i.e.
     * opposite borders between the Panel and component are equal). Panel p
     * must be affect by RTL orientation (RTL only affects horizontal positioning
     * and components lay out from top right corner).
     *
     * @param      p the panel where the components exist and is affected by
     *             right to left orientation
     * @return     true if components of panel p are properly centre, false
     *             otherwise
     */
    public static boolean isComponentCentredRTL(Panel p) {
        double borderLeft;
        double borderRight;
        double borderTop;
        double borderBottom;

        //The first component(rectangle) in panel p.
        Rectangle firstRec = p.getComponent(0).getBounds();

        //The last component(rectangle) in panel p.
        Rectangle lastRec = p.getComponent(compCount - 1).getBounds();

        System.out.println("bounds of the first rectangle in "+ p.getName() + " = " + firstRec);
        System.out.println("bounds of the last rectangle in "+ p.getName() + " = " + lastRec);

        borderLeft = lastRec.getX();
        borderRight = p.getWidth() - firstRec.getWidth() - firstRec.getX();
        borderTop = lastRec.getY();
        borderBottom = p.getHeight() - firstRec.getHeight() - firstRec.getY();

        return areBordersEqual(borderLeft, borderRight) &&
                areBordersEqual(borderTop, borderBottom);
    }

    /**
     * Given two borders border1 and border2 check if they are equal.
     *
     * @param      border1 one of the borders being compared
     * @param      border2 the other border being compared
     * @return     true if border1 and border2 are equal to each other (i.e.
     *             their width/height difference is at most 1, assuming the
     *             smallest pixel is of size 1), false otherwise
     */
    public static boolean areBordersEqual(double border1, double border2) {
        return Math.abs(border1 - border2) <= 1;
    }

    public static void main(String[] args) {
        new LayoutExtraGaps();
    }
}
