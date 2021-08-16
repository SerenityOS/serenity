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
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.plaf.*;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;
import apple.laf.JRSUIState.ScrollBarState;

import com.apple.laf.AquaUtils.RecyclableSingleton;

public class AquaScrollBarUI extends ScrollBarUI {
    private static final int kInitialDelay = 300;
    private static final int kNormalDelay = 100;

    // when we make small and mini scrollbars, this will no longer be a constant
    static final int MIN_ARROW_COLLAPSE_SIZE = 64;

    // tracking state
    protected boolean fIsDragging;
    protected Timer fScrollTimer;
    protected ScrollListener fScrollListener;
    protected TrackListener fTrackListener;
    protected Hit fTrackHighlight = Hit.NONE;
    protected Hit fMousePart = Hit.NONE; // Which arrow (if any) we moused pressed down in (used by arrow drag tracking)

    protected JScrollBar fScrollBar;
    protected ModelListener fModelListener;
    protected PropertyChangeListener fPropertyChangeListener;

    protected final AquaPainter<ScrollBarState> painter = AquaPainter.create(JRSUIStateFactory.getScrollBar());

    // Create PLAF
    public static ComponentUI createUI(final JComponent c) {
        return new AquaScrollBarUI();
    }

    public AquaScrollBarUI() { }

    public void installUI(final JComponent c) {
        fScrollBar = (JScrollBar)c;
        installListeners();
        configureScrollBarColors();
    }

    public void uninstallUI(final JComponent c) {
        uninstallListeners();
        fScrollBar = null;
    }

    protected void configureScrollBarColors() {
        LookAndFeel.installColors(fScrollBar, "ScrollBar.background", "ScrollBar.foreground");
    }

    protected TrackListener createTrackListener() {
        return new TrackListener();
    }

    protected ScrollListener createScrollListener() {
        return new ScrollListener();
    }

    protected void installListeners() {
        fTrackListener = createTrackListener();
        fModelListener = createModelListener();
        fPropertyChangeListener = createPropertyChangeListener();
        fScrollBar.addMouseListener(fTrackListener);
        fScrollBar.addMouseMotionListener(fTrackListener);
        fScrollBar.getModel().addChangeListener(fModelListener);
        fScrollBar.addPropertyChangeListener(fPropertyChangeListener);
        fScrollListener = createScrollListener();
        fScrollTimer = new Timer(kNormalDelay, fScrollListener);
        fScrollTimer.setInitialDelay(kInitialDelay); // default InitialDelay?
    }

    protected void uninstallListeners() {
        fScrollTimer.stop();
        fScrollTimer = null;
        fScrollBar.getModel().removeChangeListener(fModelListener);
        fScrollBar.removeMouseListener(fTrackListener);
        fScrollBar.removeMouseMotionListener(fTrackListener);
        fScrollBar.removePropertyChangeListener(fPropertyChangeListener);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    protected ModelListener createModelListener() {
        return new ModelListener();
    }

    protected void syncState(final JComponent c) {
        final ScrollBarState scrollBarState = painter.state;
        scrollBarState.set(isHorizontal() ? Orientation.HORIZONTAL : Orientation.VERTICAL);

        final float trackExtent = fScrollBar.getMaximum() - fScrollBar.getMinimum() - fScrollBar.getModel().getExtent();
        if (trackExtent <= 0.0f) {
            scrollBarState.set(NothingToScroll.YES);
            return;
        }

        final ScrollBarPart pressedPart = getPressedPart();
        scrollBarState.set(pressedPart);
        scrollBarState.set(getState(c, pressedPart));
        scrollBarState.set(NothingToScroll.NO);
        scrollBarState.setValue((fScrollBar.getValue() - fScrollBar.getMinimum()) / trackExtent);
        scrollBarState.setThumbStart(getThumbStart());
        scrollBarState.setThumbPercent(getThumbPercent());
        scrollBarState.set(shouldShowArrows() ? ShowArrows.YES : ShowArrows.NO);
    }

    public void paint(final Graphics g, final JComponent c) {
        syncState(c);
        painter.paint(g, c, 0, 0, fScrollBar.getWidth(), fScrollBar.getHeight());
    }

    protected State getState(final JComponent c, final ScrollBarPart pressedPart) {
        if (!AquaFocusHandler.isActive(c)) return State.INACTIVE;
        if (!c.isEnabled()) return State.INACTIVE;
        if (pressedPart != ScrollBarPart.NONE) return State.PRESSED;
        return State.ACTIVE;
    }

    private static final RecyclableSingleton<Map<Hit, ScrollBarPart>> hitToPressedPartMap = new RecyclableSingleton<Map<Hit,ScrollBarPart>>(){
        @Override
        protected Map<Hit, ScrollBarPart> getInstance() {
            final Map<Hit, ScrollBarPart> map = new HashMap<Hit, ScrollBarPart>(7);
            map.put(ScrollBarHit.ARROW_MAX, ScrollBarPart.ARROW_MAX);
            map.put(ScrollBarHit.ARROW_MIN, ScrollBarPart.ARROW_MIN);
            map.put(ScrollBarHit.ARROW_MAX_INSIDE, ScrollBarPart.ARROW_MAX_INSIDE);
            map.put(ScrollBarHit.ARROW_MIN_INSIDE, ScrollBarPart.ARROW_MIN_INSIDE);
            map.put(ScrollBarHit.TRACK_MAX, ScrollBarPart.TRACK_MAX);
            map.put(ScrollBarHit.TRACK_MIN, ScrollBarPart.TRACK_MIN);
            map.put(ScrollBarHit.THUMB, ScrollBarPart.THUMB);
            return map;
        }
    };
    protected ScrollBarPart getPressedPart() {
        if (!fTrackListener.fInArrows || !fTrackListener.fStillInArrow) return ScrollBarPart.NONE;
        final ScrollBarPart pressedPart = hitToPressedPartMap.get().get(fMousePart);
        if (pressedPart == null) return ScrollBarPart.NONE;
        return pressedPart;
    }

    protected boolean shouldShowArrows() {
        return MIN_ARROW_COLLAPSE_SIZE < (isHorizontal() ? fScrollBar.getWidth() : fScrollBar.getHeight());
    }

    // Layout Methods
    // Layout is controlled by the user in the Appearance Control Panel
    // Theme will redraw correctly for the current layout
    public void layoutContainer(final Container fScrollBarContainer) {
        fScrollBar.repaint();
        fScrollBar.revalidate();
    }

    protected Rectangle getTrackBounds() {
        return new Rectangle(0, 0, fScrollBar.getWidth(), fScrollBar.getHeight());
    }

    protected Rectangle getDragBounds() {
        return new Rectangle(0, 0, fScrollBar.getWidth(), fScrollBar.getHeight());
    }

    protected void startTimer(final boolean initial) {
        fScrollTimer.setInitialDelay(initial ? kInitialDelay : kNormalDelay); // default InitialDelay?
        fScrollTimer.start();
    }

    protected void scrollByBlock(final int direction) {
        synchronized(fScrollBar) {
            final int oldValue = fScrollBar.getValue();
            final int blockIncrement = fScrollBar.getBlockIncrement(direction);
            final int delta = blockIncrement * ((direction > 0) ? +1 : -1);

            fScrollBar.setValue(oldValue + delta);
            fTrackHighlight = direction > 0 ? ScrollBarHit.TRACK_MAX : ScrollBarHit.TRACK_MIN;
            fScrollBar.repaint();
            fScrollListener.setDirection(direction);
            fScrollListener.setScrollByBlock(true);
        }
    }

    protected void scrollByUnit(final int direction) {
        synchronized(fScrollBar) {
            int delta = fScrollBar.getUnitIncrement(direction);
            if (direction <= 0) delta = -delta;

            fScrollBar.setValue(delta + fScrollBar.getValue());
            fScrollBar.repaint();
            fScrollListener.setDirection(direction);
            fScrollListener.setScrollByBlock(false);
        }
    }

    protected Hit getPartHit(final int x, final int y) {
        syncState(fScrollBar);
        return JRSUIUtils.HitDetection.getHitForPoint(painter.getControl(), 0, 0, fScrollBar.getWidth(), fScrollBar.getHeight(), x, y);
    }

    protected class PropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent e) {
            final String propertyName = e.getPropertyName();

            if ("model".equals(propertyName)) {
                final BoundedRangeModel oldModel = (BoundedRangeModel)e.getOldValue();
                final BoundedRangeModel newModel = (BoundedRangeModel)e.getNewValue();
                oldModel.removeChangeListener(fModelListener);
                newModel.addChangeListener(fModelListener);
                fScrollBar.repaint();
                fScrollBar.revalidate();
            } else if (AquaFocusHandler.FRAME_ACTIVE_PROPERTY.equals(propertyName)) {
                fScrollBar.repaint();
            }
        }
    }

    protected class ModelListener implements ChangeListener {
        public void stateChanged(final ChangeEvent e) {
            layoutContainer(fScrollBar);
        }
    }

    // Track mouse drags.
    protected class TrackListener extends MouseAdapter implements MouseMotionListener {
        protected transient int fCurrentMouseX, fCurrentMouseY;
        protected transient boolean fInArrows; // are we currently tracking arrows?
        protected transient boolean fStillInArrow = false; // Whether mouse is in an arrow during arrow tracking
        protected transient boolean fStillInTrack = false; // Whether mouse is in the track during pageup/down tracking
        protected transient int fFirstMouseX, fFirstMouseY, fFirstValue; // Values for getValueFromOffset

        public void mouseReleased(final MouseEvent e) {
            if (!fScrollBar.isEnabled()) return;
            if (fInArrows) {
                mouseReleasedInArrows(e);
            } else {
                mouseReleasedInTrack(e);
            }

            fInArrows = false;
            fStillInArrow = false;
            fStillInTrack = false;

            fScrollBar.repaint();
            fScrollBar.revalidate();
        }

        public void mousePressed(final MouseEvent e) {
            if (!fScrollBar.isEnabled()) return;

            final Hit part = getPartHit(e.getX(), e.getY());
            fInArrows = HitUtil.isArrow(part);
            if (fInArrows) {
                mousePressedInArrows(e, part);
            } else {
                if (part == Hit.NONE) {
                    fTrackHighlight = Hit.NONE;
                } else {
                    mousePressedInTrack(e, part);
                }
            }
        }

        public void mouseDragged(final MouseEvent e) {
            if (!fScrollBar.isEnabled()) return;

            if (fInArrows) {
                mouseDraggedInArrows(e);
            } else if (fIsDragging) {
                mouseDraggedInTrack(e);
            } else {
                // In pageup/down zones

                // check that thumb has not been scrolled under the mouse cursor
                final Hit previousPart = getPartHit(fCurrentMouseX, fCurrentMouseY);
                if (!HitUtil.isTrack(previousPart)) {
                    fStillInTrack = false;
                }

                fCurrentMouseX = e.getX();
                fCurrentMouseY = e.getY();

                final Hit part = getPartHit(e.getX(), e.getY());
                final boolean temp = HitUtil.isTrack(part);
                if (temp == fStillInTrack) return;

                fStillInTrack = temp;
                if (!fStillInTrack) {
                    fScrollTimer.stop();
                } else {
                    fScrollListener.actionPerformed(new ActionEvent(fScrollTimer, 0, ""));
                    startTimer(false);
                }
            }
        }

        int getValueFromOffset(final int xOffset, final int yOffset, final int firstValue) {
            final boolean isHoriz = isHorizontal();

            // find the amount of pixels we've moved x & y (we only care about one)
            final int offsetWeCareAbout = isHoriz ? xOffset : yOffset;

            // now based on that floating point percentage compute the real scroller value.
            final int visibleAmt = fScrollBar.getVisibleAmount();
            final int max = fScrollBar.getMaximum();
            final int min = fScrollBar.getMinimum();
            final int extent = max - min;

            // ask native to tell us what the new float that is a ratio of how much scrollable area
            // we have moved (not the thumb area, just the scrollable). If the
            // scroller goes 0-100 with a visible area of 20 we are getting a ratio of the
            // remaining 80.
            syncState(fScrollBar);
            final double offsetChange = JRSUIUtils.ScrollBar.getNativeOffsetChange(painter.getControl(), 0, 0, fScrollBar.getWidth(), fScrollBar.getHeight(), offsetWeCareAbout, visibleAmt, extent);

            // the scrollable area is the extent - visible amount;
            final int scrollableArea = extent - visibleAmt;

            final int changeByValue = (int)(offsetChange * scrollableArea);
            int newValue = firstValue + changeByValue;
            newValue = Math.max(min, newValue);
            newValue = Math.min((max - visibleAmt), newValue);
            return newValue;
        }

        /**
         * Arrow Listeners
         */
        // Because we are handling both mousePressed and Actions
        // we need to make sure we don't fire under both conditions.
        // (keyfocus on scrollbars causes action without mousePress
        void mousePressedInArrows(final MouseEvent e, final Hit part) {
            final int direction = HitUtil.isIncrement(part) ? 1 : -1;

            fStillInArrow = true;
            scrollByUnit(direction);
            fScrollTimer.stop();
            fScrollListener.setDirection(direction);
            fScrollListener.setScrollByBlock(false);

            fMousePart = part;
            startTimer(true);
        }

        void mouseReleasedInArrows(final MouseEvent e) {
            fScrollTimer.stop();
            fMousePart = Hit.NONE;
            fScrollBar.setValueIsAdjusting(false);
        }

        void mouseDraggedInArrows(final MouseEvent e) {
            final Hit whichPart = getPartHit(e.getX(), e.getY());

            if ((fMousePart == whichPart) && fStillInArrow) return; // Nothing has changed, so return

            if (fMousePart != whichPart && !HitUtil.isArrow(whichPart)) {
                // The mouse is not over the arrow we mouse pressed in, so stop the timer and mark as
                // not being in the arrow
                fScrollTimer.stop();
                fStillInArrow = false;
                fScrollBar.repaint();
            } else {
                // We are in the arrow we mouse pressed down in originally, but the timer was stopped so we need
                // to start it up again.
                fMousePart = whichPart;
                fScrollListener.setDirection(HitUtil.isIncrement(whichPart) ? 1 : -1);
                fStillInArrow = true;
                fScrollListener.actionPerformed(new ActionEvent(fScrollTimer, 0, ""));
                startTimer(false);
            }

            fScrollBar.repaint();
        }

        void mouseReleasedInTrack(final MouseEvent e) {
            if (fTrackHighlight != Hit.NONE) {
                fScrollBar.repaint();
            }

            fTrackHighlight = Hit.NONE;
            fIsDragging = false;
            fScrollTimer.stop();
            fScrollBar.setValueIsAdjusting(false);
        }

        /**
         * Adjust the fScrollBars value based on the result of hitTestTrack
         */
        void mousePressedInTrack(final MouseEvent e, final Hit part) {
            fScrollBar.setValueIsAdjusting(true);

            // If option-click, toggle scroll-to-here
            boolean shouldScrollToHere = (part != ScrollBarHit.THUMB) && JRSUIUtils.ScrollBar.useScrollToClick();
            if (e.isAltDown()) shouldScrollToHere = !shouldScrollToHere;

            // pretend the mouse was dragged from a point in the current thumb to the current mouse point in one big jump
            if (shouldScrollToHere) {
                final Point p = getScrollToHereStartPoint(e.getX(), e.getY());
                fFirstMouseX = p.x;
                fFirstMouseY = p.y;
                fFirstValue = fScrollBar.getValue();
                moveToMouse(e);

                // OK, now we're in the thumb - any subsequent dragging should move it
                fTrackHighlight = ScrollBarHit.THUMB;
                fIsDragging = true;
                return;
            }

            fCurrentMouseX = e.getX();
            fCurrentMouseY = e.getY();

            int direction = 0;
            if (part == ScrollBarHit.TRACK_MIN) {
                fTrackHighlight = ScrollBarHit.TRACK_MIN;
                direction = -1;
            } else if (part == ScrollBarHit.TRACK_MAX) {
                fTrackHighlight = ScrollBarHit.TRACK_MAX;
                direction = 1;
            } else {
                fFirstValue = fScrollBar.getValue();
                fFirstMouseX = fCurrentMouseX;
                fFirstMouseY = fCurrentMouseY;
                fTrackHighlight = ScrollBarHit.THUMB;
                fIsDragging = true;
                return;
            }

            fIsDragging = false;
            fStillInTrack = true;

            scrollByBlock(direction);
            // Check the new location of the thumb
            // stop scrolling if the thumb is under the mouse??

            final Hit newPart = getPartHit(fCurrentMouseX, fCurrentMouseY);
            if (newPart == ScrollBarHit.TRACK_MIN || newPart == ScrollBarHit.TRACK_MAX) {
                fScrollTimer.stop();
                fScrollListener.setDirection(((newPart == ScrollBarHit.TRACK_MAX) ? 1 : -1));
                fScrollListener.setScrollByBlock(true);
                startTimer(true);
            }
        }

        /**
         * Set the models value to the position of the top/left
         * of the thumb relative to the origin of the track.
         */
        void mouseDraggedInTrack(final MouseEvent e) {
            moveToMouse(e);
        }

        // For normal mouse dragging or click-to-here
        // fCurrentMouseX, fCurrentMouseY, and fFirstValue must be set
        void moveToMouse(final MouseEvent e) {
            fCurrentMouseX = e.getX();
            fCurrentMouseY = e.getY();

            final int oldValue = fScrollBar.getValue();
            final int newValue = getValueFromOffset(fCurrentMouseX - fFirstMouseX, fCurrentMouseY - fFirstMouseY, fFirstValue);
            if (newValue == oldValue) return;

            fScrollBar.setValue(newValue);
            final Rectangle dirtyRect = getTrackBounds();
            fScrollBar.repaint(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
        }
    }

    /**
     * Listener for scrolling events initiated in the ScrollPane.
     */
    protected class ScrollListener implements ActionListener {
        boolean fUseBlockIncrement;
        int fDirection = 1;

        void setDirection(final int direction) {
            this.fDirection = direction;
        }

        void setScrollByBlock(final boolean block) {
            this.fUseBlockIncrement = block;
        }

        public void actionPerformed(final ActionEvent e) {
            if (fUseBlockIncrement) {
                Hit newPart = getPartHit(fTrackListener.fCurrentMouseX, fTrackListener.fCurrentMouseY);

                if (newPart == ScrollBarHit.TRACK_MIN || newPart == ScrollBarHit.TRACK_MAX) {
                    final int newDirection = (newPart == ScrollBarHit.TRACK_MAX ? 1 : -1);
                    if (fDirection != newDirection) {
                        fDirection = newDirection;
                    }
                }

                scrollByBlock(fDirection);
                newPart = getPartHit(fTrackListener.fCurrentMouseX, fTrackListener.fCurrentMouseY);

                if (newPart == ScrollBarHit.THUMB) {
                    ((Timer)e.getSource()).stop();
                }
            } else {
                scrollByUnit(fDirection);
            }

            if (fDirection > 0 && fScrollBar.getValue() + fScrollBar.getVisibleAmount() >= fScrollBar.getMaximum()) {
                ((Timer)e.getSource()).stop();
            } else if (fDirection < 0 && fScrollBar.getValue() <= fScrollBar.getMinimum()) {
                ((Timer)e.getSource()).stop();
            }
        }
    }

    float getThumbStart() {
        final int max = fScrollBar.getMaximum();
        final int min = fScrollBar.getMinimum();
        final int extent = max - min;
        if (extent <= 0) return 0f;

        return (float)(fScrollBar.getValue() - fScrollBar.getMinimum()) / (float)extent;
    }

    float getThumbPercent() {
        final int visible = fScrollBar.getVisibleAmount();
        final int max = fScrollBar.getMaximum();
        final int min = fScrollBar.getMinimum();
        final int extent = max - min;
        if (extent <= 0) return 0f;

        return (float)visible / (float)extent;
    }

    /**
     * A scrollbar's preferred width is 16 by a reasonable size to hold
     * the arrows
     *
     * @param c The JScrollBar that's delegating this method to us.
     * @return The preferred size of a Basic JScrollBar.
     * @see #getMaximumSize
     * @see #getMinimumSize
     */
    public Dimension getPreferredSize(final JComponent c) {
        return isHorizontal() ? new Dimension(96, 15) : new Dimension(15, 96);
    }

    public Dimension getMinimumSize(final JComponent c) {
        return isHorizontal() ? new Dimension(54, 15) : new Dimension(15, 54);
    }

    public Dimension getMaximumSize(final JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    boolean isHorizontal() {
        return fScrollBar.getOrientation() == Adjustable.HORIZONTAL;
    }

    // only do scroll-to-here for page up and page down regions, when the option key is pressed
    // This gets the point where the mouse would have been clicked in the current thumb
    // so we can pretend the mouse was dragged to the current mouse point in one big jump
    Point getScrollToHereStartPoint(final int clickPosX, final int clickPosY) {
        // prepare the track rectangle and limit rectangle so we can do our calculations
        final Rectangle limitRect = getDragBounds(); // GetThemeTrackDragRect

        // determine the bounding rectangle for our thumb region
        syncState(fScrollBar);
        double[] rect = new double[4];
        JRSUIUtils.ScrollBar.getPartBounds(rect, painter.getControl(), 0, 0, fScrollBar.getWidth(), fScrollBar.getHeight(), ScrollBarPart.THUMB);
        final Rectangle r = new Rectangle((int)rect[0], (int)rect[1], (int)rect[2], (int)rect[3]);

        // figure out the scroll-to-here start location based on our orientation, the
        // click position, and where it must be in the thumb to travel to the endpoints
        // properly.
        final Point startPoint = new Point(clickPosX, clickPosY);

        if (isHorizontal()) {
            final int halfWidth = r.width / 2;
            final int limitRectRight = limitRect.x + limitRect.width;

            if (clickPosX + halfWidth > limitRectRight) {
                // Up against right edge
                startPoint.x = r.x + r.width - limitRectRight - clickPosX - 1;
            } else if (clickPosX - halfWidth < limitRect.x) {
                // Up against left edge
                startPoint.x = r.x + clickPosX - limitRect.x;
            } else {
                // Center the thumb
                startPoint.x = r.x + halfWidth;
            }

            // Pretend clicked in middle of indicator vertically
            startPoint.y = (r.y + r.height) / 2;
            return startPoint;
        }

        final int halfHeight = r.height / 2;
        final int limitRectBottom = limitRect.y + limitRect.height;

        if (clickPosY + halfHeight > limitRectBottom) {
            // Up against bottom edge
            startPoint.y = r.y + r.height - limitRectBottom - clickPosY - 1;
        } else if (clickPosY - halfHeight < limitRect.y) {
            // Up against top edge
            startPoint.y = r.y + clickPosY - limitRect.y;
        } else {
            // Center the thumb
            startPoint.y = r.y + halfHeight;
        }

        // Pretend clicked in middle of indicator horizontally
        startPoint.x = (r.x + r.width) / 2;

        return startPoint;
    }

    static class HitUtil {
        static boolean isIncrement(final Hit hit) {
            return (hit == ScrollBarHit.ARROW_MAX) || (hit == ScrollBarHit.ARROW_MAX_INSIDE);
        }

        static boolean isDecrement(final Hit hit) {
            return (hit == ScrollBarHit.ARROW_MIN) || (hit == ScrollBarHit.ARROW_MIN_INSIDE);
        }

        static boolean isArrow(final Hit hit) {
            return isIncrement(hit) || isDecrement(hit);
        }

        static boolean isTrack(final Hit hit) {
            return (hit == ScrollBarHit.TRACK_MAX) || (hit == ScrollBarHit.TRACK_MIN);
        }
    }
}
