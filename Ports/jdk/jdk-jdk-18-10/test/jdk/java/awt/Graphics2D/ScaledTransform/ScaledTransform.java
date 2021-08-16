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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Panel;
import java.awt.geom.AffineTransform;

/*
 * @test
 * @bug 8069361
 * @key headful
 * @summary SunGraphics2D.getDefaultTransform() does not include scale factor
 * @author Alexander Scherbatiy
 * @run main ScaledTransform
 */
public class ScaledTransform {

    private static volatile boolean passed = false;

    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.
                getLocalGraphicsEnvironment();

        if (ge.isHeadlessInstance()) {
            return;
        }

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            for (GraphicsConfiguration gc : gd.getConfigurations()) {
                testScaleFactor(gc);
            }
        }
    }

    private static void testScaleFactor(final GraphicsConfiguration gc) {
        final Dialog dialog = new Dialog((Frame) null, "Test", true, gc);

        try {
            dialog.setSize(100, 100);
            Panel panel = new Panel() {

                @Override
                public void paint(Graphics g) {
                    if (g instanceof Graphics2D) {
                        AffineTransform gcTx = gc.getDefaultTransform();
                        AffineTransform gTx
                                = ((Graphics2D) g).getTransform();
                        passed = gcTx.getScaleX() == gTx.getScaleX()
                                && gcTx.getScaleY() == gTx.getScaleY();
                    } else {
                        passed = true;
                    }
                    dialog.setVisible(false);
                }
            };
            dialog.add(panel);
            dialog.setVisible(true);

            if (!passed) {
                throw new RuntimeException("Transform is not scaled!");
            }
        } finally {
            dialog.dispose();
        }
    }
}
