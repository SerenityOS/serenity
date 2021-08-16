/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4374642
 * @summary Verifies that the Rectangle2D.createIntersection method produces
 *          an empty result rectangle if the two source rects did not intersect
 */

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

public class CreateIntersectBug extends Canvas {
    public static boolean showresults = false;

    public static RuntimeException t = null;

    public static void main(String[] args) {
        showresults = (args.length > 0);
        test(50, 50, 20, 20, 100, 100, 100, 100);
        test(-100, -100, 50, 50, 100, 100, 50, 50);
        if (t != null) {
            throw t;
        }
    }

    public static void test(int x1, int y1, int w1, int h1,
                            int x2, int y2, int w2, int h2)
    {
        Rectangle2D r1 = new Rectangle2D.Double(x1, y1, w1, h1);
        Rectangle2D r2 = new Rectangle2D.Double(x2, y2, w2, h2);

        write(r1, "R1");
        write(r2, "R2");
        if (r1.intersects(r2)) {
            System.out.println("r1 and r2 intersect");
        } else {
            System.out.println("r1 and r2 don't intersect");
        }
        Rectangle2D r3 = r1.createIntersection(r2);
        write(r3, "intersect");

        Rectangle2D r4 = r1.createUnion(r2);
        write(r4, "union");

        if (showresults) {
            new CreateIntersectBug(r1, r2, r3, r4);
        }
        if (!r1.intersects(r2) && !r3.isEmpty()) {
            String s = ("Intersection of non-intersecting "+
                        "rectangles is not empty!");
            t = new RuntimeException(s);
            System.out.println(s);
        }
    }

    private Rectangle2D r1, r2, r3, r4;

    public CreateIntersectBug(Rectangle2D r1, Rectangle2D r2,
                              Rectangle2D r3, Rectangle2D r4)
    {
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;

        Frame f = new Frame();
        f.add(this);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        f.pack();
        f.show();
    }

    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }


    public void paint(Graphics g){
        Graphics2D g2 = (Graphics2D) g;

        // graphic aids to understanding - these have no
        // effect on the problem.
        if (!r1.intersects(r2) && !r3.isEmpty()) {
            g2.setColor(Color.red);
        } else {
            g2.setColor(Color.green);
        }
        g2.draw(r3);
        g2.setColor(Color.yellow);
        g2.draw(r4);
        g2.setColor(Color.blue);
        g2.draw(r1);
        g2.draw(r2);
    }

    public static void write(Rectangle2D r, String s) {
        System.out.println(s + ": (" +
                           (int)r.getX() + "," + (int)r.getY() + ","
                           + (int)r.getWidth() + "," + (int)r.getHeight() +
                           ")");
    }
}
