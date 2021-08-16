/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

import apple.laf.*;
import apple.laf.JRSUIUtils.NineSliceMetricsProvider;
import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaImageFactory.NineSliceMetrics;
import com.apple.laf.AquaUtils.RecyclableSingleton;

public class AquaSliderUI extends BasicSliderUI implements Sizeable {
//    static final Dimension roundThumbSize = new Dimension(21 + 4, 21 + 4); // +2px on both sides for focus fuzz
//    static final Dimension pointingThumbSize = new Dimension(19 + 4, 22 + 4);

    private static final RecyclableSingleton<SizeDescriptor> roundThumbDescriptor = new RecyclableSingleton<SizeDescriptor>() {
        protected SizeDescriptor getInstance() {
            return new SizeDescriptor(new SizeVariant(25, 25)) {
                public SizeVariant deriveSmall(final SizeVariant v) {
                    return super.deriveSmall(v.alterMinSize(-2, -2));
                }
                public SizeVariant deriveMini(final SizeVariant v) {
                    return super.deriveMini(v.alterMinSize(-2, -2));
                }
            };
        }
    };
    private static final RecyclableSingleton<SizeDescriptor> pointingThumbDescriptor = new RecyclableSingleton<SizeDescriptor>() {
        protected SizeDescriptor getInstance() {
            return new SizeDescriptor(new SizeVariant(23, 26)) {
                public SizeVariant deriveSmall(final SizeVariant v) {
                    return super.deriveSmall(v.alterMinSize(-2, -2));
                }
                public SizeVariant deriveMini(final SizeVariant v) {
                    return super.deriveMini(v.alterMinSize(-2, -2));
                }
            };
        }
    };

    static final AquaPainter<JRSUIState> trackPainter = AquaPainter.create(JRSUIStateFactory.getSliderTrack(), new NineSliceMetricsProvider() {
        @Override
        public NineSliceMetrics getNineSliceMetricsForState(JRSUIState state) {
            if (state.is(Orientation.VERTICAL)) {
                return new NineSliceMetrics(5, 7, 0, 0, 3, 3, true, false, true);
            }
            return new NineSliceMetrics(7, 5, 3, 3, 0, 0, true, true, false);
        }
    });
    final AquaPainter<JRSUIState> thumbPainter = AquaPainter.create(JRSUIStateFactory.getSliderThumb());

    protected Color tickColor;
    protected Color disabledTickColor;

    protected transient boolean fIsDragging = false;

    // From AppearanceManager doc
    static final int kTickWidth = 3;
    static final int kTickLength = 8;

    // Create PLAF
    public static ComponentUI createUI(final JComponent c) {
        return new AquaSliderUI((JSlider)c);
    }

    public AquaSliderUI(final JSlider b) {
        super(b);
    }

    public void installUI(final JComponent c) {
        super.installUI(c);

        LookAndFeel.installProperty(slider, "opaque", Boolean.FALSE);
        tickColor = UIManager.getColor("Slider.tickColor");
    }

    protected BasicSliderUI.TrackListener createTrackListener(final JSlider s) {
        return new TrackListener();
    }

    protected void installListeners(final JSlider s) {
        super.installListeners(s);
        AquaFocusHandler.install(s);
        AquaUtilControlSize.addSizePropertyListener(s);
    }

    protected void uninstallListeners(final JSlider s) {
        AquaUtilControlSize.removeSizePropertyListener(s);
        AquaFocusHandler.uninstall(s);
        super.uninstallListeners(s);
    }

    public void applySizeFor(final JComponent c, final Size size) {
        thumbPainter.state.set(size);
        trackPainter.state.set(size);
    }

    // Paint Methods
    public void paint(final Graphics g, final JComponent c) {
        // We have to override paint of BasicSliderUI because we need slight differences.
        // We don't paint focus the same way - it is part of the thumb.
        // We also need to repaint the whole track when the thumb moves.
        recalculateIfInsetsChanged();
        final Rectangle clip = g.getClipBounds();

        final Orientation orientation = slider.getOrientation() == SwingConstants.HORIZONTAL ? Orientation.HORIZONTAL : Orientation.VERTICAL;
        final State state = getState();

        if (slider.getPaintTrack()) {
            // This is needed for when this is used as a renderer. It is the same as BasicSliderUI.java
            // and is missing from our reimplementation.
            //
            // <rdar://problem/3721898> JSlider in TreeCellRenderer component not painted properly.
            //
            final boolean trackIntersectsClip = clip.intersects(trackRect);
            if (!trackIntersectsClip) {
                calculateGeometry();
            }

            if (trackIntersectsClip || clip.intersects(thumbRect)) paintTrack(g, c, orientation, state);
        }

        if (slider.getPaintTicks() && clip.intersects(tickRect)) {
            paintTicks(g);
        }

        if (slider.getPaintLabels() && clip.intersects(labelRect)) {
            paintLabels(g);
        }

        if (clip.intersects(thumbRect)) {
            paintThumb(g, c, orientation, state);
        }
    }

    // Paints track and thumb
    public void paintTrack(final Graphics g, final JComponent c, final Orientation orientation, final State state) {
        trackPainter.state.set(orientation);
        trackPainter.state.set(state);

        // for debugging
        //g.setColor(Color.green);
        //g.drawRect(trackRect.x, trackRect.y, trackRect.width - 1, trackRect.height - 1);
        trackPainter.paint(g, c, trackRect.x, trackRect.y, trackRect.width, trackRect.height);
    }

    // Paints thumb only
    public void paintThumb(final Graphics g, final JComponent c, final Orientation orientation, final State state) {
        thumbPainter.state.set(orientation);
        thumbPainter.state.set(state);
        thumbPainter.state.set(slider.hasFocus() ? Focused.YES : Focused.NO);
        thumbPainter.state.set(getDirection(orientation));

        // for debugging
        //g.setColor(Color.blue);
        //g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);
        thumbPainter.paint(g, c, thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
    }

    Direction getDirection(final Orientation orientation) {
        if (shouldUseArrowThumb()) {
            return orientation == Orientation.HORIZONTAL ? Direction.DOWN : Direction.RIGHT;
        }

        return Direction.NONE;
    }

    State getState() {
        if (!slider.isEnabled()) {
            return State.DISABLED;
        }

        if (fIsDragging) {
            return State.PRESSED;
        }

        if (!AquaFocusHandler.isActive(slider)) {
            return State.INACTIVE;
        }

        return State.ACTIVE;
    }

    public void paintTicks(final Graphics g) {
        if (slider.isEnabled()) {
            g.setColor(tickColor);
        } else {
            if (disabledTickColor == null) {
                disabledTickColor = new Color(tickColor.getRed(), tickColor.getGreen(), tickColor.getBlue(), tickColor.getAlpha() / 2);
            }
            g.setColor(disabledTickColor);
        }

        super.paintTicks(g);
    }

    // Layout Methods

    // Used lots
    protected void calculateThumbLocation() {
        super.calculateThumbLocation();

        if (shouldUseArrowThumb()) {
            final boolean isHorizonatal = slider.getOrientation() == SwingConstants.HORIZONTAL;
            final Size size = AquaUtilControlSize.getUserSizeFrom(slider);

            if (size == Size.REGULAR) {
                if (isHorizonatal) thumbRect.y += 3; else thumbRect.x += 2; return;
            }

            if (size == Size.SMALL) {
                if (isHorizonatal) thumbRect.y += 2; else thumbRect.x += 2; return;
            }

            if (size == Size.MINI) {
                if (isHorizonatal) thumbRect.y += 1; return;
            }
        }
    }

    // Only called from calculateGeometry
    protected void calculateThumbSize() {
        final SizeDescriptor descriptor = shouldUseArrowThumb() ? pointingThumbDescriptor.get() : roundThumbDescriptor.get();
        final SizeVariant variant = descriptor.get(slider);

        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            thumbRect.setSize(variant.w, variant.h);
        } else {
            thumbRect.setSize(variant.h, variant.w);
        }
    }

    protected boolean shouldUseArrowThumb() {
        if (slider.getPaintTicks() || slider.getPaintLabels()) return true;

        final Object shouldPaintArrowThumbProperty = slider.getClientProperty("Slider.paintThumbArrowShape");
        if (shouldPaintArrowThumbProperty != null && shouldPaintArrowThumbProperty instanceof Boolean) {
            return ((Boolean)shouldPaintArrowThumbProperty).booleanValue();
        }

        return false;
    }

    protected void calculateTickRect() {
        // super assumes tickRect ends align with trackRect ends.
        // Ours need to inset by trackBuffer
        // Ours also needs to be *inside* trackRect
        final int tickLength = slider.getPaintTicks() ? getTickLength() : 0;
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            tickRect.height = tickLength;
            tickRect.x = trackRect.x + trackBuffer;
            tickRect.y = trackRect.y + trackRect.height - (tickRect.height / 2);
            tickRect.width = trackRect.width - (trackBuffer * 2);
        } else {
            tickRect.width = tickLength;
            tickRect.x = trackRect.x + trackRect.width - (tickRect.width / 2);
            tickRect.y = trackRect.y + trackBuffer;
            tickRect.height = trackRect.height - (trackBuffer * 2);
        }
    }

    // Basic's preferred size doesn't allow for our focus ring, throwing off things like SwingSet2
    public Dimension getPreferredHorizontalSize() {
        return new Dimension(190, 21);
    }

    public Dimension getPreferredVerticalSize() {
        return new Dimension(21, 190);
    }

    protected ChangeListener createChangeListener(final JSlider s) {
        return new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (fIsDragging) return;
                calculateThumbLocation();
                slider.repaint();
            }
        };
    }

    // This is copied almost verbatim from superclass, except we changed things to use fIsDragging
    // instead of isDragging since isDragging was a private member.
    class TrackListener extends javax.swing.plaf.basic.BasicSliderUI.TrackListener {
        protected transient int offset;
        protected transient int currentMouseX = -1, currentMouseY = -1;

        public void mouseReleased(final MouseEvent e) {
            if (!slider.isEnabled()) return;

            currentMouseX = -1;
            currentMouseY = -1;

            offset = 0;
            scrollTimer.stop();

            // This is the way we have to determine snap-to-ticks.  It's hard to explain
            // but since ChangeEvents don't give us any idea what has changed we don't
            // have a way to stop the thumb bounds from being recalculated.  Recalculating
            // the thumb bounds moves the thumb over the current value (i.e., snapping
            // to the ticks).
            if (slider.getSnapToTicks() /*|| slider.getSnapToValue()*/) {
                fIsDragging = false;
                slider.setValueIsAdjusting(false);
            } else {
                slider.setValueIsAdjusting(false);
                fIsDragging = false;
            }

            slider.repaint();
        }

        public void mousePressed(final MouseEvent e) {
            if (!slider.isEnabled()) return;

            // We should recalculate geometry just before
            // calculation of the thumb movement direction.
            // It is important for the case, when JSlider
            // is a cell editor in JTable. See 6348946.
            calculateGeometry();

            final boolean firstClick = (currentMouseX == -1) && (currentMouseY == -1);

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (slider.isRequestFocusEnabled()) {
                slider.requestFocus();
            }

            boolean isMouseEventInThumb = thumbRect.contains(currentMouseX, currentMouseY);

            // we don't want to move the thumb if we just clicked on the edge of the thumb
            if (!firstClick || !isMouseEventInThumb) {
                slider.setValueIsAdjusting(true);

                switch (slider.getOrientation()) {
                    case SwingConstants.VERTICAL:
                        slider.setValue(valueForYPosition(currentMouseY));
                        break;
                    case SwingConstants.HORIZONTAL:
                        slider.setValue(valueForXPosition(currentMouseX));
                        break;
                }

                slider.setValueIsAdjusting(false);

                isMouseEventInThumb = true; // since we just moved it in there
            }

            // Clicked in the Thumb area?
            if (isMouseEventInThumb) {
                switch (slider.getOrientation()) {
                    case SwingConstants.VERTICAL:
                        offset = currentMouseY - thumbRect.y;
                        break;
                    case SwingConstants.HORIZONTAL:
                        offset = currentMouseX - thumbRect.x;
                        break;
                }

                fIsDragging = true;
                return;
            }

            fIsDragging = false;
        }

        public boolean shouldScroll(final int direction) {
            final Rectangle r = thumbRect;
            if (slider.getOrientation() == SwingConstants.VERTICAL) {
                if (drawInverted() ? direction < 0 : direction > 0) {
                    if (r.y + r.height <= currentMouseY) return false;
                } else {
                    if (r.y >= currentMouseY) return false;
                }
            } else {
                if (drawInverted() ? direction < 0 : direction > 0) {
                    if (r.x + r.width >= currentMouseX) return false;
                } else {
                    if (r.x <= currentMouseX) return false;
                }
            }

            if (direction > 0 && slider.getValue() + slider.getExtent() >= slider.getMaximum()) {
                return false;
            }

            if (direction < 0 && slider.getValue() <= slider.getMinimum()) {
                return false;
            }

            return true;
        }

        /**
         * Set the models value to the position of the top/left
         * of the thumb relative to the origin of the track.
         */
        public void mouseDragged(final MouseEvent e) {
            int thumbMiddle = 0;

            if (!slider.isEnabled()) return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (!fIsDragging) return;

            slider.setValueIsAdjusting(true);

            switch (slider.getOrientation()) {
                case SwingConstants.VERTICAL:
                    final int halfThumbHeight = thumbRect.height / 2;
                    int thumbTop = e.getY() - offset;
                    int trackTop = trackRect.y;
                    int trackBottom = trackRect.y + (trackRect.height - 1);
                    final int vMax = yPositionForValue(slider.getMaximum() - slider.getExtent());

                    if (drawInverted()) {
                        trackBottom = vMax;
                    } else {
                        trackTop = vMax;
                    }
                    thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                    thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

                    setThumbLocation(thumbRect.x, thumbTop);

                    thumbMiddle = thumbTop + halfThumbHeight;
                    slider.setValue(valueForYPosition(thumbMiddle));
                    break;
                case SwingConstants.HORIZONTAL:
                    final int halfThumbWidth = thumbRect.width / 2;
                    int thumbLeft = e.getX() - offset;
                    int trackLeft = trackRect.x;
                    int trackRight = trackRect.x + (trackRect.width - 1);
                    final int hMax = xPositionForValue(slider.getMaximum() - slider.getExtent());

                    if (drawInverted()) {
                        trackLeft = hMax;
                    } else {
                        trackRight = hMax;
                    }
                    thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                    thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

                    setThumbLocation(thumbLeft, thumbRect.y);

                    thumbMiddle = thumbLeft + halfThumbWidth;
                    slider.setValue(valueForXPosition(thumbMiddle));
                    break;
                default:
                    return;
            }

            // enable live snap-to-ticks <rdar://problem/3165310>
            if (slider.getSnapToTicks()) {
                calculateThumbLocation();
                setThumbLocation(thumbRect.x, thumbRect.y); // need to call to refresh the repaint region
            }
        }

        public void mouseMoved(final MouseEvent e) { }
    }

    // Super handles snap-to-ticks by recalculating the thumb rect in the TrackListener
    // See setThumbLocation for why that doesn't work
    int getScale() {
        if (!slider.getSnapToTicks()) return 1;
        int scale = slider.getMinorTickSpacing();
            if (scale < 1) scale = slider.getMajorTickSpacing();
        if (scale < 1) return 1;
        return scale;
    }
}
