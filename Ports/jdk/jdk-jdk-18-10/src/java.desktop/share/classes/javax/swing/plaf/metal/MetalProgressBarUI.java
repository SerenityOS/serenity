/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.awt.*;

/**
 * The Metal implementation of ProgressBarUI.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Michael C. Albers
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalProgressBarUI extends BasicProgressBarUI {

    private Rectangle innards;
    private Rectangle box;

    /**
     * Constructs a {@code MetalProgressBarUI}.
     */
    public MetalProgressBarUI() {}

    /**
     * Constructs an instance of {@code MetalProgressBarUI}.
     *
     * @param c a component
     * @return an instance of {@code MetalProgressBarUI}
     */
    public static ComponentUI createUI(JComponent c) {
        return new MetalProgressBarUI();
    }

    /**
     * Draws a bit of special highlighting on the progress bar.
     * The core painting is deferred to the BasicProgressBar's
     * <code>paintDeterminate</code> method.
     * @since 1.4
     */
    public void paintDeterminate(Graphics g, JComponent c) {
        super.paintDeterminate(g,c);

        if (!(g instanceof Graphics2D)) {
            return;
        }

        if (progressBar.isBorderPainted()) {
            Insets b = progressBar.getInsets(); // area for border
            int barRectWidth = progressBar.getWidth() - (b.left + b.right);
            int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
            int amountFull = getAmountFull(b, barRectWidth, barRectHeight);
            boolean isLeftToRight = MetalUtils.isLeftToRight(c);
            int startX, startY, endX, endY;

            // The progress bar border is painted according to a light source.
            // This light source is stationary and does not change when the
            // component orientation changes.
            startX = b.left;
            startY = b.top;
            endX = b.left + barRectWidth - 1;
            endY = b.top + barRectHeight - 1;

            Graphics2D g2 = (Graphics2D)g;
            g2.setStroke(new BasicStroke(1.f));

            if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
                // Draw light line lengthwise across the progress bar.
                g2.setColor(MetalLookAndFeel.getControlShadow());
                g2.drawLine(startX, startY, endX, startY);

                if (amountFull > 0) {
                    // Draw darker lengthwise line over filled area.
                    g2.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());

                    if (isLeftToRight) {
                        g2.drawLine(startX, startY,
                                startX + amountFull - 1, startY);
                    } else {
                        g2.drawLine(endX, startY,
                                endX - amountFull + 1, startY);
                        if (progressBar.getPercentComplete() != 1.f) {
                            g2.setColor(MetalLookAndFeel.getControlShadow());
                        }
                    }
                }
                // Draw a line across the width.  The color is determined by
                // the code above.
                g2.drawLine(startX, startY, startX, endY);

            } else { // VERTICAL
                // Draw light line lengthwise across the progress bar.
                g2.setColor(MetalLookAndFeel.getControlShadow());
                g2.drawLine(startX, startY, startX, endY);

                if (amountFull > 0) {
                    // Draw darker lengthwise line over filled area.
                    g2.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
                    g2.drawLine(startX, endY,
                            startX, endY - amountFull + 1);
                }
                // Draw a line across the width.  The color is determined by
                // the code above.
                g2.setColor(MetalLookAndFeel.getControlShadow());

                if (progressBar.getPercentComplete() == 1.f) {
                    g2.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
                }
                g2.drawLine(startX, startY, endX, startY);
            }
        }
    }

    /**
     * Draws a bit of special highlighting on the progress bar
     * and bouncing box.
     * The core painting is deferred to the BasicProgressBar's
     * <code>paintIndeterminate</code> method.
     * @since 1.4
     */
    public void paintIndeterminate(Graphics g, JComponent c) {
        super.paintIndeterminate(g, c);

        if (!progressBar.isBorderPainted() || (!(g instanceof Graphics2D))) {
            return;
        }

        Insets b = progressBar.getInsets(); // area for border
        int barRectWidth = progressBar.getWidth() - (b.left + b.right);
        int barRectHeight = progressBar.getHeight() - (b.top + b.bottom);
        int amountFull = getAmountFull(b, barRectWidth, barRectHeight);
        boolean isLeftToRight = MetalUtils.isLeftToRight(c);
        int startX, startY, endX, endY;
        Rectangle box = null;
        box = getBox(box);

        // The progress bar border is painted according to a light source.
        // This light source is stationary and does not change when the
        // component orientation changes.
        startX = b.left;
        startY = b.top;
        endX = b.left + barRectWidth - 1;
        endY = b.top + barRectHeight - 1;

        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(1.f));

        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            // Draw light line lengthwise across the progress bar.
            g2.setColor(MetalLookAndFeel.getControlShadow());
            g2.drawLine(startX, startY, endX, startY);
            g2.drawLine(startX, startY, startX, endY);

            // Draw darker lengthwise line over filled area.
            g2.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g2.drawLine(box.x, startY, box.x + box.width - 1, startY);

        } else { // VERTICAL
            // Draw light line lengthwise across the progress bar.
            g2.setColor(MetalLookAndFeel.getControlShadow());
            g2.drawLine(startX, startY, startX, endY);
            g2.drawLine(startX, startY, endX, startY);

            // Draw darker lengthwise line over filled area.
            g2.setColor(MetalLookAndFeel.getPrimaryControlDarkShadow());
            g2.drawLine(startX, box.y, startX, box.y + box.height - 1);
        }
    }
}
