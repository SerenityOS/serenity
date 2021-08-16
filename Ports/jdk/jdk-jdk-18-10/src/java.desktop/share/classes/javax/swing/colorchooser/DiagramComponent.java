/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.colorchooser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

@SuppressWarnings("serial") // Superclass is not serializable across versions
final class DiagramComponent extends JComponent implements MouseListener, MouseMotionListener {

    private final ColorPanel panel;
    private final boolean diagram;

    private final Insets insets = new Insets(0, 0, 0, 0);

    private int width;
    private int height;

    private int[] array;
    private BufferedImage image;

    DiagramComponent(ColorPanel panel, boolean diagram) {
        this.panel = panel;
        this.diagram = diagram;
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        getInsets(this.insets);
        this.width = getWidth() - this.insets.left - this.insets.right;
        this.height = getHeight() - this.insets.top - this.insets.bottom;
        if ((this.width <= 0) || (this.height <= 0))
            return;
        boolean update = (this.image == null)
                || (this.width != this.image.getWidth())
                || (this.height != this.image.getHeight());
        if (update) {
            int size = this.width * this.height;
            if ((this.array == null) || (this.array.length < size)) {
                this.array = new int[size];
            }
            this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        }
        {
            float dx = 1.0f / (float) (this.width - 1);
            float dy = 1.0f / (float) (this.height - 1);

            int offset = 0;
            float y = 0.0f;
            for (int h = 0; h < this.height; h++, y += dy) {
                if (this.diagram) {
                    float x = 0.0f;
                    for (int w = 0; w < this.width; w++, x += dx, offset++) {
                        this.array[offset] = this.panel.getColor(x, y);
                    }
                }
                else {
                    int color = this.panel.getColor(y);
                    for (int w = 0; w < this.width; w++, offset++) {
                        this.array[offset] = color;
                    }
                }
            }
        }
        this.image.setRGB(0, 0, this.width, this.height, this.array, 0, this.width);
        g.drawImage(this.image, this.insets.left, this.insets.top, this.width, this.height, this);
        if (isEnabled()) {
            this.width--;
            this.height--;
            g.setXORMode(Color.WHITE);
            g.setColor(Color.BLACK);
            if (this.diagram) {
                int x = getValue(this.panel.getValueX(), this.insets.left, this.width);
                int y = getValue(this.panel.getValueY(), this.insets.top, this.height);
                g.drawLine(x - 8, y, x + 8, y);
                g.drawLine(x, y - 8, x, y + 8);
            }
            else {
                int z = getValue(this.panel.getValueZ(), this.insets.top, this.height);
                g.drawLine(this.insets.left, z, this.insets.left + this.width, z);
            }
            g.setPaintMode();
        }
    }

    public void mousePressed(MouseEvent event) {
        mouseDragged(event);
    }

    public void mouseReleased(MouseEvent event) {
    }

    public void mouseClicked(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mouseMoved(MouseEvent event) {
    }

    public void mouseDragged(MouseEvent event) {
        if (isEnabled()) {
            float y = getValue(event.getY(), this.insets.top, this.height);
            if (this.diagram) {
                float x = getValue(event.getX(), this.insets.left, this.width);
                this.panel.setValue(x, y);
            }
            else {
                this.panel.setValue(y);
            }
        }
    }

    private static int getValue(float value, int min, int max) {
        return min + (int) (value * (float) (max));
    }

    private static float getValue(int value, int min, int max) {
        if (min < value) {
            value -= min;
            return (value < max)
                    ? (float) value / (float) max
                    : 1.0f;
        }
        return 0.0f;
    }
}
