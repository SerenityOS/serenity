/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.AffineTransform;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import sun.swing.SwingUtilities2;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JProgressBar}.
 *
 * @author Joshua Outwater
 * @since 1.7
 */
public class SynthProgressBarUI extends BasicProgressBarUI
                                implements SynthUI, PropertyChangeListener {
    private SynthStyle style;
    private int progressPadding;
    private boolean rotateText; // added for Nimbus LAF
    private boolean paintOutsideClip;
    private boolean tileWhenIndeterminate; //whether to tile indeterminate painting
    private int tileWidth; //the width of each tile
    private Dimension minBarSize; // minimal visible bar size
    private int glowWidth; // Glow around the bar foreground

    /**
     *
     * Constructs a {@code SynthProgressBarUI}.
     */
    public SynthProgressBarUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param x component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent x) {
        return new SynthProgressBarUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        progressBar.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        progressBar.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        updateStyle(progressBar);
    }

    private void updateStyle(JProgressBar c) {
        SynthContext context = getContext(c, ENABLED);
        SynthStyle oldStyle = style;
        style = SynthLookAndFeel.updateStyle(context, this);
        setCellLength(style.getInt(context, "ProgressBar.cellLength", 1));
        setCellSpacing(style.getInt(context, "ProgressBar.cellSpacing", 0));
        progressPadding = style.getInt(context,
                "ProgressBar.progressPadding", 0);
        paintOutsideClip = style.getBoolean(context,
                "ProgressBar.paintOutsideClip", false);
        rotateText = style.getBoolean(context,
                "ProgressBar.rotateText", false);
        tileWhenIndeterminate = style.getBoolean(context, "ProgressBar.tileWhenIndeterminate", false);
        tileWidth = style.getInt(context, "ProgressBar.tileWidth", 15);
        // handle scaling for sizeVarients for special case components. The
        // key "JComponent.sizeVariant" scales for large/small/mini
        // components are based on Apples LAF
        String scaleKey = (String)progressBar.getClientProperty(
                "JComponent.sizeVariant");
        if (scaleKey != null){
            if ("large".equals(scaleKey)){
                tileWidth *= 1.15;
            } else if ("small".equals(scaleKey)){
                tileWidth *= 0.857;
            } else if ("mini".equals(scaleKey)){
                tileWidth *= 0.784;
            }
        }
        minBarSize = (Dimension)style.get(context, "ProgressBar.minBarSize");
        glowWidth = style.getInt(context, "ProgressBar.glowWidth", 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(progressBar, ENABLED);

        style.uninstallDefaults(context);
        style = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private int getComponentState(JComponent c) {
        return SynthLookAndFeel.getComponentState(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (progressBar.isStringPainted() &&
                progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            SynthContext context = getContext(c);
            Font font = context.getStyle().getFont(context);
            FontMetrics metrics = progressBar.getFontMetrics(font);
            return (height - metrics.getAscent() - metrics.getDescent()) / 2 +
                    metrics.getAscent();
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Rectangle getBox(Rectangle r) {
        if (tileWhenIndeterminate) {
            return SwingUtilities.calculateInnerArea(progressBar, r);
        } else {
            return super.getBox(r);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setAnimationIndex(int newValue) {
        if (paintOutsideClip) {
            if (getAnimationIndex() == newValue) {
                return;
            }
            super.setAnimationIndex(newValue);
            progressBar.repaint();
        } else {
            super.setAnimationIndex(newValue);
        }
    }

    /**
     * Notifies this UI delegate to repaint the specified component.
     * This method paints the component background, then calls
     * the {@link #paint(SynthContext,Graphics)} method.
     *
     * <p>In general, this method does not need to be overridden by subclasses.
     * All Look and Feel rendering code should reside in the {@code paint} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        SynthLookAndFeel.update(context, g);
        context.getPainter().paintProgressBarBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight(),
                          progressBar.getOrientation());
        paint(context, g);
    }

    /**
     * Paints the specified component according to the Look and Feel.
     * <p>This method is not used by Synth Look and Feel.
     * Painting is handled by the {@link #paint(SynthContext,Graphics)} method.
     *
     * @param g the {@code Graphics} object used for painting
     * @param c the component being painted
     * @see #paint(SynthContext,Graphics)
     */
    @Override
    public void paint(Graphics g, JComponent c) {
        SynthContext context = getContext(c);

        paint(context, g);
    }

    /**
     * Paints the specified component.
     *
     * @param context context for the component being painted
     * @param g the {@code Graphics} object used for painting
     * @see #update(Graphics,JComponent)
     */
    protected void paint(SynthContext context, Graphics g) {
        JProgressBar pBar = (JProgressBar)context.getComponent();
        int x = 0, y = 0, width = 0, height = 0;
        if (!pBar.isIndeterminate()) {
            Insets pBarInsets = pBar.getInsets();
            double percentComplete = pBar.getPercentComplete();
            if (percentComplete != 0.0) {
                if (pBar.getOrientation() == JProgressBar.HORIZONTAL) {
                    x = pBarInsets.left + progressPadding;
                    y = pBarInsets.top + progressPadding;
                    width = (int)(percentComplete * (pBar.getWidth()
                            - (pBarInsets.left + progressPadding
                             + pBarInsets.right + progressPadding)));
                    height = pBar.getHeight()
                            - (pBarInsets.top + progressPadding
                             + pBarInsets.bottom + progressPadding);

                    if (!SynthLookAndFeel.isLeftToRight(pBar)) {
                        x = pBar.getWidth() - pBarInsets.right - width
                                - progressPadding - glowWidth;
                    }
                } else {  // JProgressBar.VERTICAL
                    x = pBarInsets.left + progressPadding;
                    width = pBar.getWidth()
                            - (pBarInsets.left + progressPadding
                            + pBarInsets.right + progressPadding);
                    height = (int)(percentComplete * (pBar.getHeight()
                            - (pBarInsets.top + progressPadding
                             + pBarInsets.bottom + progressPadding)));
                    y = pBar.getHeight() - pBarInsets.bottom - height
                            - progressPadding;

                    if (SynthLookAndFeel.isLeftToRight(pBar)) {
                        y -= glowWidth;
                    }
                }
            }
        } else {
            boxRect = getBox(boxRect);
            x = boxRect.x + progressPadding;
            y = boxRect.y + progressPadding;
            width = boxRect.width - progressPadding - progressPadding;
            height = boxRect.height - progressPadding - progressPadding;
        }

        //if tiling and indeterminate, then paint the progress bar foreground a
        //bit wider than it should be. Shift as needed to ensure that there is
        //an animated effect
        if (tileWhenIndeterminate && pBar.isIndeterminate()) {
            double percentComplete = (double)getAnimationIndex() / (double)getFrameCount();
            int offset = (int)(percentComplete * tileWidth);
            Shape clip = g.getClip();
            g.clipRect(x, y, width, height);
            if (pBar.getOrientation() == JProgressBar.HORIZONTAL) {
                //paint each tile horizontally
                for (int i=x-tileWidth+offset; i<=width; i+=tileWidth) {
                    context.getPainter().paintProgressBarForeground(
                            context, g, i, y, tileWidth, height, pBar.getOrientation());
                }
            } else { //JProgressBar.VERTICAL
                //paint each tile vertically
                for (int i=y-offset; i<height+tileWidth; i+=tileWidth) {
                    context.getPainter().paintProgressBarForeground(
                            context, g, x, i, width, tileWidth, pBar.getOrientation());
                }
            }
            g.setClip(clip);
        } else {
            if (minBarSize == null || (width >= minBarSize.width
                    && height >= minBarSize.height)) {
                context.getPainter().paintProgressBarForeground(context, g,
                        x, y, width, height, pBar.getOrientation());
            }
        }

        if (pBar.isStringPainted()) {
            paintText(context, g, pBar.getString());
        }
    }

    /**
     * Paints the component's text.
     *
     * @param context context for the component being painted
     * @param g {@code Graphics} object used for painting
     * @param title the text to paint
     */
    protected void paintText(SynthContext context, Graphics g, String title) {
        if (progressBar.isStringPainted()) {
            SynthStyle style = context.getStyle();
            Font font = style.getFont(context);
            FontMetrics fm = SwingUtilities2.getFontMetrics(
                    progressBar, g, font);
            int strLength = style.getGraphicsUtils(context).
                computeStringWidth(context, font, fm, title);
            Rectangle bounds = progressBar.getBounds();

            if (rotateText &&
                    progressBar.getOrientation() == JProgressBar.VERTICAL){
                Graphics2D g2 = (Graphics2D)g;
                // Calculate the position for the text.
                Point textPos;
                AffineTransform rotation;
                if (progressBar.getComponentOrientation().isLeftToRight()){
                    rotation = AffineTransform.getRotateInstance(-Math.PI/2);
                    textPos = new Point(
                        (bounds.width+fm.getAscent()-fm.getDescent())/2,
                           (bounds.height+strLength)/2);
                } else {
                    rotation = AffineTransform.getRotateInstance(Math.PI/2);
                    textPos = new Point(
                        (bounds.width-fm.getAscent()+fm.getDescent())/2,
                           (bounds.height-strLength)/2);
                }

                // Progress bar isn't wide enough for the font.  Don't paint it.
                if (textPos.x < 0) {
                    return;
                }

                // Paint the text.
                font = font.deriveFont(rotation);
                g2.setFont(font);
                g2.setColor(style.getColor(context, ColorType.TEXT_FOREGROUND));
                style.getGraphicsUtils(context).paintText(context, g, title,
                                                     textPos.x, textPos.y, -1);
            } else {
                // Calculate the bounds for the text.
                Rectangle textRect = new Rectangle(
                    (bounds.width / 2) - (strLength / 2),
                    (bounds.height -
                        (fm.getAscent() + fm.getDescent())) / 2,
                    0, 0);

                // Progress bar isn't tall enough for the font.  Don't paint it.
                if (textRect.y < 0) {
                    return;
                }

                // Paint the text.
                g.setColor(style.getColor(context, ColorType.TEXT_FOREGROUND));
                g.setFont(font);
                style.getGraphicsUtils(context).paintText(context, g, title,
                                                     textRect.x, textRect.y, -1);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintProgressBarBorder(context, g, x, y, w, h,
                                                    progressBar.getOrientation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (SynthLookAndFeel.shouldUpdateStyle(e) ||
                "indeterminate".equals(e.getPropertyName())) {
            updateStyle((JProgressBar)e.getSource());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension size = null;
        Insets border = progressBar.getInsets();
        FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        String progString = progressBar.getString();
        int stringHeight = fontSizer.getHeight() + fontSizer.getDescent();

        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            size = new Dimension(getPreferredInnerHorizontal());
            if (progressBar.isStringPainted()) {
                // adjust the height if necessary to make room for the string
                if (stringHeight > size.height) {
                    size.height = stringHeight;
                }

                // adjust the width if necessary to make room for the string
                int stringWidth = SwingUtilities2.stringWidth(
                                       progressBar, fontSizer, progString);
                if (stringWidth > size.width) {
                    size.width = stringWidth;
                }
            }
        } else {
            size = new Dimension(getPreferredInnerVertical());
            if (progressBar.isStringPainted()) {
                // make sure the width is big enough for the string
                if (stringHeight > size.width) {
                    size.width = stringHeight;
                }

                // make sure the height is big enough for the string
                int stringWidth = SwingUtilities2.stringWidth(
                                       progressBar, fontSizer, progString);
                if (stringWidth > size.height) {
                    size.height = stringWidth;
                }
            }
        }

        // handle scaling for sizeVarients for special case components. The
        // key "JComponent.sizeVariant" scales for large/small/mini
        // components are based on Apples LAF
        String scaleKey = (String)progressBar.getClientProperty(
                "JComponent.sizeVariant");
        if (scaleKey != null){
            if ("large".equals(scaleKey)){
                size.width *= 1.15f;
                size.height *= 1.15f;
            } else if ("small".equals(scaleKey)){
                size.width *= 0.90f;
                size.height *= 0.90f;
            } else if ("mini".equals(scaleKey)){
                size.width *= 0.784f;
                size.height *= 0.784f;
            }
        }

        size.width += border.left + border.right;
        size.height += border.top + border.bottom;

        return size;
    }
}
