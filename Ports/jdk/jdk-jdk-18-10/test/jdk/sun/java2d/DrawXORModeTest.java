/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     8036022
 * @summary Test verifies that drawing shapes with XOR composite
 *          does not trigger an InternalError in GDI surface data.
 * @run main/othervm -Dsun.java2d.d3d=True DrawXORModeTest
 */
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.util.concurrent.CountDownLatch;

public class DrawXORModeTest extends Component {

    public static void main(String[] args) {
        final DrawXORModeTest c = new DrawXORModeTest();

        final Frame f = new Frame("XOR mode test");
        f.add(c);
        f.pack();

        f.setVisible(true);

        try {
            c.checkResult();
        } finally {
            f.dispose();
        }
    }

    @Override
    public void paint(Graphics g) {
        if (g == null || !(g instanceof Graphics2D)) {
            return;
        }
        g.setColor(Color.white);
        g.setXORMode(Color.black);
        Graphics2D dg = (Graphics2D) g;
        Stroke stroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{1.0f, 1.0f},
                0.0f);
        dg.setStroke(stroke);
        try {
            dg.draw(new Line2D.Float(10, 10, 20, 20));
        } catch (Throwable e) {
            synchronized (this) {
                theError = e;
            }
        } finally {
            didDraw.countDown();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 100);
    }

    public void checkResult() {
        try {
            didDraw.await();
        } catch (InterruptedException e) {
        }

        synchronized (this) {
            if (theError != null) {
                System.out.println("Error: " + theError);

                throw new RuntimeException("Test FAILED.");
            }
        }
        System.out.println("Test PASSED.");

    }

    private Throwable theError = null;

    private final CountDownLatch didDraw = new CountDownLatch(1);
}
