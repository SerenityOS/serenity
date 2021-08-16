/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6989617
 * @summary Enable JComponent to control repaintings of its children
 * @author Alexander Potochkin
 * @run main bug6989617
 */

import javax.swing.*;
import java.awt.*;

public class bug6989617 {
    private static MyPanel panel;
    private static JButton button;
    private static JFrame frame;

    public static void main(String... args) throws Exception {
        try {
            Robot robot = new Robot();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame = new JFrame();
                    frame. setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    panel = new MyPanel();

                    button = new JButton("Hello");
                    panel.add(button);
                    frame.add(panel);

                    frame.setSize(200, 300);
                    frame.setVisible(true);
                }
            });
            // Testing the panel as a painting origin,
            // the panel.paintImmediately() must be triggered
            // when button.repaint() is called
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    panel.resetPaintRectangle();
                    button.repaint();
                }
            });
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    Rectangle pr = panel.getPaintRectangle();
                    if (!pr.getSize().equals(button.getSize())) {
                        throw new RuntimeException("wrong size of the dirty area");
                    }
                    if (!pr.getLocation().equals(button.getLocation())) {
                        throw new RuntimeException("wrong location of the dirty area");
                    }
                }
            });
            // Testing the panel as NOT a painting origin
            // the panel.paintImmediately() must NOT be triggered
            // when button.repaint() is called
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    panel.resetPaintRectangle();
                    panel.setPaintingOrigin(false);
                    if (panel.getPaintRectangle() != null) {
                        throw new RuntimeException("paint rectangle is not null");
                    }
                    button.repaint();
                }
            });
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if(panel.getPaintRectangle() != null) {
                        throw new RuntimeException("paint rectangle is not null");
                    }
                    System.out.println("Test passed...");
                }
            });
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static class MyPanel extends JPanel {
        private boolean isPaintingOrigin = true;
        private Rectangle paintRectangle;

        {
            setLayout(new GridBagLayout());
        }

        public boolean isPaintingOrigin() {
            return isPaintingOrigin;
        }

        public void setPaintingOrigin(boolean paintingOrigin) {
            isPaintingOrigin = paintingOrigin;
        }

        public void paintImmediately(int x, int y, int w, int h) {
            super.paintImmediately(x, y, w, h);
            paintRectangle = new Rectangle(x, y, w, h);
        }

        public Rectangle getPaintRectangle() {
            return paintRectangle == null? null: new Rectangle(paintRectangle);
        }

        public void resetPaintRectangle() {
            this.paintRectangle = null;
        }
    }
}
