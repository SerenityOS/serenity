/*
 *
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package java2d.demos.Clipping;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import javax.swing.*;
import java2d.ControlsSurface;
import java2d.CustomControls;
import static java.awt.Color.*;


/**
 * The Areas class demonstrates the CAG (Constructive Area Geometry)
 * operations: Add(union), Subtract, Intersect, and ExclusiveOR.
 */
@SuppressWarnings("serial")
public class Areas extends ControlsSurface {

    protected String areaType = "nop";

    public Areas() {
        setBackground(WHITE);
        setControls(new Component[] { new DemoControls(this) });
    }

    @Override
    public void render(int w, int h, Graphics2D g2) {
        GeneralPath p1 = new GeneralPath();
        p1.moveTo(w * .25f, 0.0f);
        p1.lineTo(w * .75f, h * .5f);
        p1.lineTo(w * .25f, h);
        p1.lineTo(0.0f, h * .5f);
        p1.closePath();

        GeneralPath p2 = new GeneralPath();
        p2.moveTo(w * .75f, 0.0f);
        p2.lineTo(w, h * .5f);
        p2.lineTo(w * .75f, h);
        p2.lineTo(w * .25f, h * .5f);
        p2.closePath();


        Area area = new Area(p1);
        g2.setColor(YELLOW);
        if (areaType.equals("nop")) {
            g2.fill(p1);
            g2.fill(p2);
            g2.setColor(RED);
            g2.draw(p1);
            g2.draw(p2);
            return;
        } else if (areaType.equals("add")) {
            area.add(new Area(p2));
        } else if (areaType.equals("sub")) {
            area.subtract(new Area(p2));
        } else if (areaType.equals("xor")) {
            area.exclusiveOr(new Area(p2));
        } else if (areaType.equals("int")) {
            area.intersect(new Area(p2));
        } else if (areaType.equals("pear")) {

            double sx = w / 100;
            double sy = h / 140;
            g2.scale(sx, sy);
            double x = w / sx / 2;
            double y = h / sy / 2;

            // Creates the first leaf by filling the intersection of two Area
            // objects created from an ellipse.
            Ellipse2D leaf = new Ellipse2D.Double(x - 16, y - 29, 15.0, 15.0);
            Area leaf1 = new Area(leaf);
            leaf.setFrame(x - 14, y - 47, 30.0, 30.0);
            Area leaf2 = new Area(leaf);
            leaf1.intersect(leaf2);
            g2.setColor(GREEN);
            g2.fill(leaf1);

            // Creates the second leaf.
            leaf.setFrame(x + 1, y - 29, 15.0, 15.0);
            leaf1 = new Area(leaf);
            leaf2.intersect(leaf1);
            g2.fill(leaf2);

            // Creates the stem by filling the Area resulting from the
            // subtraction of two Area objects created from an ellipse.
            Ellipse2D stem = new Ellipse2D.Double(x, y - 42, 40.0, 40.0);
            Area st1 = new Area(stem);
            stem.setFrame(x + 3, y - 47, 50.0, 50.0);
            st1.subtract(new Area(stem));
            g2.setColor(BLACK);
            g2.fill(st1);

            // Creates the pear itself by filling the Area resulting from the
            // union of two Area objects created by two different ellipses.
            Ellipse2D circle = new Ellipse2D.Double(x - 25, y, 50.0, 50.0);
            Ellipse2D oval = new Ellipse2D.Double(x - 19, y - 20, 40.0, 70.0);
            Area circ = new Area(circle);
            circ.add(new Area(oval));

            g2.setColor(YELLOW);
            g2.fill(circ);
            return;
        }

        g2.fill(area);
        g2.setColor(RED);
        g2.draw(area);
    }

    public static void main(String[] argv) {
        createDemoFrame(new Areas());
    }


    static final class DemoControls extends CustomControls implements
            ActionListener {

        Areas demo;
        JToolBar toolbar;

        public DemoControls(Areas demo) {
            super(demo.name);
            this.demo = demo;
            add(toolbar = new JToolBar());
            toolbar.setFloatable(false);
            addTool("nop", "no area operation", true);
            addTool("add", "add", false);
            addTool("sub", "subtract", false);
            addTool("xor", "exclusiveOr", false);
            addTool("int", "intersection", false);
            addTool("pear", "pear", false);
        }

        public void addTool(String str, String tooltip, boolean state) {
            JToggleButton b =
                    (JToggleButton) toolbar.add(new JToggleButton(str));
            b.setFocusPainted(false);
            b.setToolTipText(tooltip);
            b.setSelected(state);
            b.addActionListener(this);
            int width = b.getPreferredSize().width;
            Dimension prefSize = new Dimension(width, 21);
            b.setPreferredSize(prefSize);
            b.setMaximumSize(prefSize);
            b.setMinimumSize(prefSize);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (Component comp : toolbar.getComponents()) {
                ((JToggleButton) comp).setSelected(false);
            }
            JToggleButton b = (JToggleButton) e.getSource();
            b.setSelected(true);
            demo.areaType = b.getText();
            demo.repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 40);
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run() {
            try {
                Thread.sleep(1111);
            } catch (Exception e) {
                return;
            }
            Thread me = Thread.currentThread();
            while (thread == me) {
                for (Component comp : toolbar.getComponents()) {
                    ((AbstractButton) comp).doClick();
                    try {
                        Thread.sleep(4444);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            thread = null;
        }
    } // End DemoControls
} // End Areas

