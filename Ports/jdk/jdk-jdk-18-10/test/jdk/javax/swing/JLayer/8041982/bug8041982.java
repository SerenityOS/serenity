/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8041982
 * @summary Use of animated icon in JLayer causes CPU spin
 * @author Alexander Potochkin
 */

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.beans.PropertyChangeEvent;

public class bug8041982 extends JFrame {

    public bug8041982() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new JLayer<>(new JPanel(), new BusyLayer()));
        setSize(200, 300);
        setVisible(true);
    }

    public static void main(String... args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new bug8041982().setVisible(true);
            }
        });
        Thread.sleep(5000);
    }

    private class BusyLayer extends LayerUI<JComponent> {
        private volatile boolean animated = true;
        private Icon icon = new ImageIcon(bug8041982.class.getResource("duke.gif"));
        private int imageUpdateCount;

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            if (isAnimated()) {
                icon.paintIcon(c, g, c.getWidth() / 2 - icon.getIconWidth() /
                        2,
                        c.getHeight() / 2 - icon.getIconHeight() / 2);
            }
        }

        public boolean isAnimated() {
            return animated;
        }

        public void setAnimated(boolean animated) {
            if (this.animated != animated) {
                this.animated = animated;
                firePropertyChange("animated", !animated, animated);
            }
        }

        @Override
        public void applyPropertyChange(PropertyChangeEvent evt, JLayer l) {
            // this will be called when the busy flag is changed
            l.repaint();
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h, JLayer<? extends JComponent> l) {
            System.out.println("imageUpdate " + imageUpdateCount);
            if (imageUpdateCount++ == 100) {
                setAnimated(false);
            } else if (imageUpdateCount > 100) {
                throw new RuntimeException("Test failed");
            }
            return isAnimated() && super.imageUpdate(img, infoflags, x, y, w, h, l);
        }
    }
}

