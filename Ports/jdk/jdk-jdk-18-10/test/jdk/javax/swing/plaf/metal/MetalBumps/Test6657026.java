/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6657026
 * @summary Tests shared MetalBumps in different application contexts
 * @author Sergey Malenkov
 * @modules java.desktop/sun.awt
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

import javax.swing.JToolBar;
import javax.swing.plaf.metal.MetalBorders.ToolBarBorder;

import sun.awt.SunToolkit;

public class Test6657026 extends ToolBarBorder implements Runnable {

    public static void main(String[] args) throws Exception {
        new Test6657026().test();

        ThreadGroup group = new ThreadGroup("$$$");
        Thread thread = new Thread(group, new Test6657026());
        thread.start();
        thread.join();
    }

    public void run() {
        SunToolkit.createNewAppContext();
        test();
    }

    private void test() {
        MyGraphics mg = new MyGraphics();
        ToolBarBorder border = new ToolBarBorder();
        border.paintBorder(mg.component, mg, 0, 0, 10, 10);
        if (mg.image != null) {
            boolean failed = true;
            int value = mg.image.getRGB(0, 0);
            for (int x = 0; x < mg.image.getWidth(); x++) {
                for (int y = 0; y < mg.image.getHeight(); y++) {
                    int current = mg.image.getRGB(x, y);
                    if (current != value) {
                        mg.image.setRGB(x, y, value);
                        failed = false;
                    }

                }
            }
            if (failed) {
                throw new Error("shared metal bumps");
            }
        }
    }

    private static class MyGraphics extends Graphics {

        private final Component component = new JToolBar() {};
        private BufferedImage image;

        public Graphics create() {
            return null;  // TODO: check
        }

        public void translate(int x, int y) {
            // TODO: check
        }

        public Color getColor() {
            return null;  // TODO: check
        }

        public void setColor(Color color) {
            // TODO: check
        }

        public void setPaintMode() {
            // TODO: check
        }

        public void setXORMode(Color c1) {
            // TODO: check
        }

        public Font getFont() {
            return null;  // TODO: check
        }

        public void setFont(Font font) {
            // TODO: check
        }

        public FontMetrics getFontMetrics(Font font) {
            return null;  // TODO: check
        }

        public Rectangle getClipBounds() {
            return null;  // TODO: check
        }

        public void clipRect(int x, int y, int width, int height) {
            // TODO: check
        }

        public void setClip(int x, int y, int width, int height) {
            // TODO: check
        }

        public Shape getClip() {
            return null;  // TODO: check
        }

        public void setClip(Shape clip) {
            // TODO: check
        }

        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
            // TODO: check
        }

        public void drawLine(int x1, int y1, int x2, int y2) {
            // TODO: check
        }

        public void fillRect(int x, int y, int width, int height) {
            // TODO: check
        }

        public void clearRect(int x, int y, int width, int height) {
            // TODO: check
        }

        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            // TODO: check
        }

        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
            // TODO: check
        }

        public void drawOval(int x, int y, int width, int height) {
            // TODO: check
        }

        public void fillOval(int x, int y, int width, int height) {
            // TODO: check
        }

        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            // TODO: check
        }

        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
            // TODO: check
        }

        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
            // TODO: check
        }

        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            // TODO: check
        }

        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
            // TODO: check
        }

        public void drawString(String str, int x, int y) {
            // TODO: check
        }

        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
            // TODO: check
        }

        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
            return false;  // TODO: check
        }

        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
            return false;  // TODO: check
        }

        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
            return false;  // TODO: check
        }

        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
            return false;  // TODO: check
        }

        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
            if (img instanceof BufferedImage) {
                this.image = (BufferedImage) img;
            }
            return false;  // TODO: check
        }

        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
            return false;  // TODO: check
        }

        public void dispose() {
            // TODO: check
        }
    }
}
