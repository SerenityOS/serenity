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

package javax.swing.colorchooser;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import sun.swing.SwingUtilities2;


/**
 * The standard preview panel for the color chooser.
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
 * @author Steve Wilson
 * @see JColorChooser
 */
@SuppressWarnings("serial") // Same-version serialization only
class DefaultPreviewPanel extends JPanel {

    private int squareSize = 25;
    private int squareGap = 5;
    private int innerGap = 5;


    private int textGap = 5;
    private Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
    private String sampleText;

    private int swatchWidth = 50;

    private Color oldColor = null;

    private JColorChooser getColorChooser() {
        return (JColorChooser)SwingUtilities.getAncestorOfClass(
                                   JColorChooser.class, this);
    }

    public Dimension getPreferredSize() {
        JComponent host = getColorChooser();
        if (host == null) {
            host = this;
        }
        FontMetrics fm = host.getFontMetrics(getFont());

        int ascent = fm.getAscent();
        int height = fm.getHeight();
        int width = SwingUtilities2.stringWidth(host, fm, getSampleText());

        int y = height*3 + textGap*3;
        int x = squareSize * 3 + squareGap*2 + swatchWidth + width + textGap*3;
        return new Dimension( x,y );
    }

    public void paintComponent(Graphics g) {
        if (oldColor == null)
            oldColor = getForeground();

        g.setColor(getBackground());
        g.fillRect(0,0,getWidth(),getHeight());

        if (this.getComponentOrientation().isLeftToRight()) {
            int squareWidth = paintSquares(g, 0);
            int textWidth = paintText(g, squareWidth);
            paintSwatch(g, squareWidth + textWidth);
        } else {
            int swatchWidth = paintSwatch(g, 0);
            int textWidth = paintText(g, swatchWidth);
            paintSquares(g , swatchWidth + textWidth);

        }
    }

    private int paintSwatch(Graphics g, int offsetX) {
        int swatchX = offsetX;
        g.setColor(oldColor);
        g.fillRect(swatchX, 0, swatchWidth, (squareSize) + (squareGap/2));
        g.setColor(getForeground());
        g.fillRect(swatchX, (squareSize) + (squareGap/2), swatchWidth, (squareSize) + (squareGap/2) );
        return (swatchX+swatchWidth);
    }

    private int paintText(Graphics g, int offsetX) {
        g.setFont(getFont());
        JComponent host = getColorChooser();
        if (host == null) {
            host = this;
        }
        FontMetrics fm = SwingUtilities2.getFontMetrics(host, g);

        int ascent = fm.getAscent();
        int height = fm.getHeight();
        int width = SwingUtilities2.stringWidth(host, fm, getSampleText());

        int textXOffset = offsetX + textGap;

        Color color = getForeground();

        g.setColor(color);

        SwingUtilities2.drawString(host, g, getSampleText(),textXOffset+(textGap/2),
                                   ascent+2);

        g.fillRect(textXOffset,
                   ( height) + textGap,
                   width + (textGap),
                   height +2);

        g.setColor(Color.black);
        SwingUtilities2.drawString(host, g, getSampleText(),
                     textXOffset+(textGap/2),
                     height+ascent+textGap+2);


        g.setColor(Color.white);

        g.fillRect(textXOffset,
                   ( height + textGap) * 2,
                   width + (textGap),
                   height +2);

        g.setColor(color);
        SwingUtilities2.drawString(host, g, getSampleText(),
                     textXOffset+(textGap/2),
                     ((height+textGap) * 2)+ascent+2);

        return width + textGap*3;

    }

    private int paintSquares(Graphics g, int offsetX) {

        int squareXOffset = offsetX;
        Color color = getForeground();

        g.setColor(Color.white);
        g.fillRect(squareXOffset,0,squareSize,squareSize);
        g.setColor(color);
        g.fillRect(squareXOffset+innerGap,
                   innerGap,
                   squareSize - (innerGap*2),
                   squareSize - (innerGap*2));
        g.setColor(Color.white);
        g.fillRect(squareXOffset+innerGap*2,
                   innerGap*2,
                   squareSize - (innerGap*4),
                   squareSize - (innerGap*4));

        g.setColor(color);
        g.fillRect(squareXOffset,squareSize+squareGap,squareSize,squareSize);

        g.translate(squareSize+squareGap, 0);
        g.setColor(Color.black);
        g.fillRect(squareXOffset,0,squareSize,squareSize);
        g.setColor(color);
        g.fillRect(squareXOffset+innerGap,
                   innerGap,
                   squareSize - (innerGap*2),
                   squareSize - (innerGap*2));
        g.setColor(Color.white);
        g.fillRect(squareXOffset+innerGap*2,
                   innerGap*2,
                   squareSize - (innerGap*4),
                   squareSize - (innerGap*4));
        g.translate(-(squareSize+squareGap), 0);

        g.translate(squareSize+squareGap, squareSize+squareGap);
        g.setColor(Color.white);
        g.fillRect(squareXOffset,0,squareSize,squareSize);
        g.setColor(color);
        g.fillRect(squareXOffset+innerGap,
                   innerGap,
                   squareSize - (innerGap*2),
                   squareSize - (innerGap*2));
        g.translate(-(squareSize+squareGap), -(squareSize+squareGap));



        g.translate((squareSize+squareGap)*2, 0);
        g.setColor(Color.white);
        g.fillRect(squareXOffset,0,squareSize,squareSize);
        g.setColor(color);
        g.fillRect(squareXOffset+innerGap,
                   innerGap,
                   squareSize - (innerGap*2),
                   squareSize - (innerGap*2));
        g.setColor(Color.black);
        g.fillRect(squareXOffset+innerGap*2,
                   innerGap*2,
                   squareSize - (innerGap*4),
                   squareSize - (innerGap*4));
        g.translate(-((squareSize+squareGap)*2), 0);

        g.translate((squareSize+squareGap)*2, (squareSize+squareGap));
        g.setColor(Color.black);
        g.fillRect(squareXOffset,0,squareSize,squareSize);
        g.setColor(color);
        g.fillRect(squareXOffset+innerGap,
                   innerGap,
                   squareSize - (innerGap*2),
                   squareSize - (innerGap*2));
        g.translate(-((squareSize+squareGap)*2), -(squareSize+squareGap));

        return (squareSize*3+squareGap*2);

    }

    private String getSampleText() {
        if (this.sampleText == null) {
            this.sampleText = UIManager.getString("ColorChooser.sampleText", getLocale());
        }
        return this.sampleText;
    }
}
