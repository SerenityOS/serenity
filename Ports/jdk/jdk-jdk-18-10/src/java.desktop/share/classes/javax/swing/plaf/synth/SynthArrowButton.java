/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package javax.swing.plaf.synth;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.UIResource;

/**
 * JButton object that draws a scaled Arrow in one of the cardinal directions.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class SynthArrowButton extends JButton implements SwingConstants, UIResource {
    private int direction;

    public SynthArrowButton(int direction) {
        super();
        super.setFocusable(false);
        setDirection(direction);
        setDefaultCapable(false);
    }

    public String getUIClassID() {
        return "ArrowButtonUI";
    }

    public void updateUI() {
        setUI(new SynthArrowButtonUI());
    }

    public void setDirection(int dir) {
        direction = dir;
        putClientProperty("__arrow_direction__", Integer.valueOf(dir));
        repaint();
    }

    public int getDirection() {
        return direction;
    }

    public void setFocusable(boolean focusable) {}

    private static class SynthArrowButtonUI extends SynthButtonUI {
        protected void installDefaults(AbstractButton b) {
            super.installDefaults(b);
            updateStyle(b);
        }

        protected void paint(SynthContext context, Graphics g) {
            SynthArrowButton button = (SynthArrowButton)context.
                                      getComponent();
            context.getPainter().paintArrowButtonForeground(
                context, g, 0, 0, button.getWidth(), button.getHeight(),
                button.getDirection());
        }

        void paintBackground(SynthContext context, Graphics g, JComponent c) {
            context.getPainter().paintArrowButtonBackground(context, g, 0, 0,
                                                c.getWidth(), c.getHeight());
        }

        public void paintBorder(SynthContext context, Graphics g, int x,
                                int y, int w, int h) {
            context.getPainter().paintArrowButtonBorder(context, g, x, y, w,h);
        }

        public Dimension getMinimumSize() {
            return new Dimension(5, 5);
        }

        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public Dimension getPreferredSize(JComponent c) {
            SynthContext context = getContext(c);
            Dimension dim = null;
            if (context.getComponent().getName() == "ScrollBar.button") {
                // ScrollBar arrow buttons can be non-square when
                // the ScrollBar.squareButtons property is set to FALSE
                // and the ScrollBar.buttonSize property is non-null
                dim = (Dimension)
                    context.getStyle().get(context, "ScrollBar.buttonSize");
            }
            if (dim == null) {
                // For all other cases (including Spinner, ComboBox), we will
                // fall back on the single ArrowButton.size value to create
                // a square return value
                int size =
                    context.getStyle().getInt(context, "ArrowButton.size", 16);
                dim = new Dimension(size, size);
            }

            // handle scaling for sizeVarients for special case components. The
            // key "JComponent.sizeVariant" scales for large/small/mini
            // components are based on Apples LAF
            Container parent = context.getComponent().getParent();
            if (parent instanceof JComponent && !(parent instanceof JComboBox)) {
                Object scaleKey = ((JComponent)parent).
                                    getClientProperty("JComponent.sizeVariant");
                if (scaleKey != null){
                    if ("large".equals(scaleKey)){
                        dim = new Dimension(
                                (int)(dim.width * 1.15),
                                (int)(dim.height * 1.15));
                    } else if ("small".equals(scaleKey)){
                        dim = new Dimension(
                                (int)(dim.width * 0.857),
                                (int)(dim.height * 0.857));
                    } else if ("mini".equals(scaleKey)){
                        dim = new Dimension(
                                (int)(dim.width * 0.714),
                                (int)(dim.height * 0.714));
                    }
                }
            }

            return dim;
        }
    }
}
