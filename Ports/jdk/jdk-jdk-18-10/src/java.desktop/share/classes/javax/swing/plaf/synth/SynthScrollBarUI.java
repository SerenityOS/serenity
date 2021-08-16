/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.beans.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;


/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JScrollBar}.
 *
 * @author Scott Violet
 * @since 1.7
 */
public class SynthScrollBarUI extends BasicScrollBarUI
                              implements PropertyChangeListener, SynthUI {

    private SynthStyle style;
    private SynthStyle thumbStyle;
    private SynthStyle trackStyle;

    private boolean validMinimumThumbSize;

    /**
     *
     * Constructs a {@code SynthScrollBarUI}.
     */
    public SynthScrollBarUI() {}

    /**
     * Returns a UI.
     * @return a UI
     * @param c a component
     */
    public static ComponentUI createUI(JComponent c)    {
        return new SynthScrollBarUI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        super.installDefaults();
        trackHighlight = NO_HIGHLIGHT;
        if (scrollbar.getLayout() == null ||
                     (scrollbar.getLayout() instanceof UIResource)) {
            scrollbar.setLayout(this);
        }
        configureScrollBarColors();
        updateStyle(scrollbar);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureScrollBarColors() {
    }

    private void updateStyle(JScrollBar c) {
        SynthStyle oldStyle = style;
        SynthContext context = getContext(c, ENABLED);
        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            scrollBarWidth = style.getInt(context,"ScrollBar.thumbHeight", 14);
            minimumThumbSize = (Dimension)style.get(context,
                                                "ScrollBar.minimumThumbSize");
            if (minimumThumbSize == null) {
                minimumThumbSize = new Dimension();
                validMinimumThumbSize = false;
            }
            else {
                validMinimumThumbSize = true;
            }
            maximumThumbSize = (Dimension)style.get(context,
                        "ScrollBar.maximumThumbSize");
            if (maximumThumbSize == null) {
                maximumThumbSize = new Dimension(4096, 4097);
            }

            incrGap = style.getInt(context, "ScrollBar.incrementButtonGap", 0);
            decrGap = style.getInt(context, "ScrollBar.decrementButtonGap", 0);

            // handle scaling for sizeVarients for special case components. The
            // key "JComponent.sizeVariant" scales for large/small/mini
            // components are based on Apples LAF
            String scaleKey = (String)scrollbar.getClientProperty(
                    "JComponent.sizeVariant");
            if (scaleKey != null){
                if ("large".equals(scaleKey)){
                    scrollBarWidth *= 1.15;
                    incrGap *= 1.15;
                    decrGap *= 1.15;
                } else if ("small".equals(scaleKey)){
                    scrollBarWidth *= 0.857;
                    incrGap *= 0.857;
                    decrGap *= 0.857;
                } else if ("mini".equals(scaleKey)){
                    scrollBarWidth *= 0.714;
                    incrGap *= 0.714;
                    decrGap *= 0.714;
                }
            }

            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }

        context = getContext(c, Region.SCROLL_BAR_TRACK, ENABLED);
        trackStyle = SynthLookAndFeel.updateStyle(context, this);

        context = getContext(c, Region.SCROLL_BAR_THUMB, ENABLED);
        thumbStyle = SynthLookAndFeel.updateStyle(context, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        scrollbar.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults(){
        SynthContext context = getContext(scrollbar, ENABLED);
        style.uninstallDefaults(context);
        style = null;

        context = getContext(scrollbar, Region.SCROLL_BAR_TRACK, ENABLED);
        trackStyle.uninstallDefaults(context);
        trackStyle = null;

        context = getContext(scrollbar, Region.SCROLL_BAR_THUMB, ENABLED);
        thumbStyle.uninstallDefaults(context);
        thumbStyle = null;

        super.uninstallDefaults();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.getComponentState(c));
    }

    private SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private SynthContext getContext(JComponent c, Region region) {
        return getContext(c, region, getComponentState(c, region));
    }

    private SynthContext getContext(JComponent c, Region region, int state) {
        SynthStyle style = trackStyle;

        if (region == Region.SCROLL_BAR_THUMB) {
            style = thumbStyle;
        }
        return SynthContext.getContext(c, region, style, state);
    }

    private int getComponentState(JComponent c, Region region) {
        if (region == Region.SCROLL_BAR_THUMB && c.isEnabled()) {
            if (isDragging) {
                return PRESSED;
            } else if (isThumbRollover()) {
                return MOUSE_OVER;
            }
        }
        return SynthLookAndFeel.getComponentState(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getSupportsAbsolutePositioning() {
        SynthContext context = getContext(scrollbar);
        boolean value = style.getBoolean(context,
                      "ScrollBar.allowsAbsolutePositioning", false);
        return value;
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
        context.getPainter().paintScrollBarBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight(),
                          scrollbar.getOrientation());
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
        SynthContext subcontext = getContext(scrollbar,
                                             Region.SCROLL_BAR_TRACK);
        paintTrack(subcontext, g, getTrackBounds());

        subcontext = getContext(scrollbar, Region.SCROLL_BAR_THUMB);
        paintThumb(subcontext, g, getThumbBounds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintScrollBarBorder(context, g, x, y, w, h,
                                                  scrollbar.getOrientation());
    }

    /**
     * Paints the scrollbar track.
     *
     * @param context context for the component being painted
     * @param g {@code Graphics} object used for painting
     * @param trackBounds bounding box for the track
     */
    protected void paintTrack(SynthContext context, Graphics g,
                              Rectangle trackBounds) {
        SynthLookAndFeel.updateSubregion(context, g, trackBounds);
        context.getPainter().paintScrollBarTrackBackground(context, g, trackBounds.x,
                        trackBounds.y, trackBounds.width, trackBounds.height,
                        scrollbar.getOrientation());
        context.getPainter().paintScrollBarTrackBorder(context, g, trackBounds.x,
                        trackBounds.y, trackBounds.width, trackBounds.height,
                        scrollbar.getOrientation());
    }

    /**
     * Paints the scrollbar thumb.
     *
     * @param context context for the component being painted
     * @param g {@code Graphics} object used for painting
     * @param thumbBounds bounding box for the thumb
     */
    protected void paintThumb(SynthContext context, Graphics g,
                              Rectangle thumbBounds) {
        SynthLookAndFeel.updateSubregion(context, g, thumbBounds);
        int orientation = scrollbar.getOrientation();
        context.getPainter().paintScrollBarThumbBackground(context, g, thumbBounds.x,
                        thumbBounds.y, thumbBounds.width, thumbBounds.height,
                        orientation);
        context.getPainter().paintScrollBarThumbBorder(context, g, thumbBounds.x,
                        thumbBounds.y, thumbBounds.width, thumbBounds.height,
                        orientation);
    }

    /**
     * A vertical scrollbar's preferred width is the maximum of
     * preferred widths of the (non <code>null</code>)
     * increment/decrement buttons,
     * and the minimum width of the thumb. The preferred height is the
     * sum of the preferred heights of the same parts.  The basis for
     * the preferred size of a horizontal scrollbar is similar.
     * <p>
     * The <code>preferredSize</code> is only computed once, subsequent
     * calls to this method just return a cached size.
     *
     * @param c the <code>JScrollBar</code> that's delegating this method to us
     * @return the preferred size of a Basic JScrollBar
     * @see #getMaximumSize
     * @see #getMinimumSize
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        return (scrollbar.getOrientation() == JScrollBar.VERTICAL)
            ? new Dimension(scrollBarWidth + insets.left + insets.right, 48)
            : new Dimension(48, scrollBarWidth + insets.top + insets.bottom);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Dimension getMinimumThumbSize() {
        if (!validMinimumThumbSize) {
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                minimumThumbSize.width = scrollBarWidth;
                minimumThumbSize.height = 7;
            } else {
                minimumThumbSize.width = 7;
                minimumThumbSize.height = scrollBarWidth;
            }
        }
        return minimumThumbSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JButton createDecreaseButton(int orientation)  {
        @SuppressWarnings("serial") // anonymous class
        SynthArrowButton synthArrowButton = new SynthArrowButton(orientation) {
            @Override
            public boolean contains(int x, int y) {
                if (decrGap < 0) { //there is an overlap between the track and button
                    int width = getWidth();
                    int height = getHeight();
                    if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                        //adjust the height by decrGap
                        //Note: decrGap is negative!
                        height += decrGap;
                    } else {
                        //adjust the width by decrGap
                        //Note: decrGap is negative!
                        width += decrGap;
                    }
                    return (x >= 0) && (x < width) && (y >= 0) && (y < height);
                }
                return super.contains(x, y);
            }
        };
        synthArrowButton.setName("ScrollBar.button");
        return synthArrowButton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JButton createIncreaseButton(int orientation)  {
        @SuppressWarnings("serial") // anonymous class
        SynthArrowButton synthArrowButton = new SynthArrowButton(orientation) {
            @Override
            public boolean contains(int x, int y) {
                if (incrGap < 0) { //there is an overlap between the track and button
                    int width = getWidth();
                    int height = getHeight();
                    if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                        //adjust the height and y by incrGap
                        //Note: incrGap is negative!
                        height += incrGap;
                        y += incrGap;
                    } else {
                        //adjust the width and x by incrGap
                        //Note: incrGap is negative!
                        width += incrGap;
                        x += incrGap;
                    }
                    return (x >= 0) && (x < width) && (y >= 0) && (y < height);
                }
                return super.contains(x, y);
            }
        };
        synthArrowButton.setName("ScrollBar.button");
        return synthArrowButton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setThumbRollover(boolean active) {
        if (isThumbRollover() != active) {
            scrollbar.repaint(getThumbBounds());
            super.setThumbRollover(active);
        }
    }

    private void updateButtonDirections() {
        int orient = scrollbar.getOrientation();
        if (scrollbar.getComponentOrientation().isLeftToRight()) {
            ((SynthArrowButton)incrButton).setDirection(
                        orient == HORIZONTAL? EAST : SOUTH);
            ((SynthArrowButton)decrButton).setDirection(
                        orient == HORIZONTAL? WEST : NORTH);
        }
        else {
            ((SynthArrowButton)incrButton).setDirection(
                        orient == HORIZONTAL? WEST : SOUTH);
            ((SynthArrowButton)decrButton).setDirection(
                        orient == HORIZONTAL ? EAST : NORTH);
        }
    }

    //
    // PropertyChangeListener
    //
    public void propertyChange(PropertyChangeEvent e) {
        String propertyName = e.getPropertyName();

        if (SynthLookAndFeel.shouldUpdateStyle(e)) {
            updateStyle((JScrollBar)e.getSource());
        }

        if ("orientation" == propertyName) {
            updateButtonDirections();
        }
        else if ("componentOrientation" == propertyName) {
            updateButtonDirections();
        }
    }
}
