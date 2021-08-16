/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.basic;


import sun.swing.DefaultLookup;
import sun.swing.UIAction;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;

import static sun.swing.SwingUtilities2.drawHLine;
import static sun.swing.SwingUtilities2.drawRect;
import static sun.swing.SwingUtilities2.drawVLine;


/**
 * Implementation of ScrollBarUI for the Basic Look and Feel
 *
 * @author Rich Schiavi
 * @author David Kloba
 * @author Hans Muller
 */
public class BasicScrollBarUI
    extends ScrollBarUI implements LayoutManager, SwingConstants
{
    private static final int POSITIVE_SCROLL = 1;
    private static final int NEGATIVE_SCROLL = -1;

    private static final int MIN_SCROLL = 2;
    private static final int MAX_SCROLL = 3;

    // NOTE: DO NOT use this field directly, SynthScrollBarUI assumes you'll
    // call getMinimumThumbSize to access it.
    /** Minimum thumb size */
    protected Dimension minimumThumbSize;
    /** Maximum thumb size */
    protected Dimension maximumThumbSize;

    /** Thumb highlight color */
    protected Color thumbHighlightColor;
    /** Thumb light shadow color */
    protected Color thumbLightShadowColor;
    /** Thumb dark shadow color */
    protected Color thumbDarkShadowColor;
    /** Thumb color */
    protected Color thumbColor;
    /** Track color */
    protected Color trackColor;
    /** Track highlight color */
    protected Color trackHighlightColor;

    /** Scrollbar */
    protected JScrollBar scrollbar;
    /** Increment button */
    protected JButton incrButton;
    /** Decrement button */
    protected JButton decrButton;
    /** Dragging */
    protected boolean isDragging;
    /** Track listener */
    protected TrackListener trackListener;
    /** Button listener */
    protected ArrowButtonListener buttonListener;
    /** Model listener */
    protected ModelListener modelListener;

    /** Thumb rectangle */
    protected Rectangle thumbRect;
    /** Track rectangle */
    protected Rectangle trackRect;

    /** Track highlight */
    protected int trackHighlight;

    /** No highlight */
    protected static final int NO_HIGHLIGHT = 0;
    /** Decrease highlight */
    protected static final int DECREASE_HIGHLIGHT = 1;
    /** Increase highlight */
    protected static final int INCREASE_HIGHLIGHT = 2;

    /** Scroll listener */
    protected ScrollListener scrollListener;
    /** Property change listener */
    protected PropertyChangeListener propertyChangeListener;
    /** Scroll timer */
    protected Timer scrollTimer;

    private static final int scrollSpeedThrottle = 60; // delay in milli seconds

    /**
     * True indicates a middle click will absolutely position the
     * scrollbar.
     */
    private boolean supportsAbsolutePositioning;

    /**
     * Hint as to what width (when vertical) or height (when horizontal)
     * should be.
     *
     * @since 1.7
     */
    protected int scrollBarWidth;

    private Handler handler;

    private boolean thumbActive;

    /**
     * Determine whether scrollbar layout should use cached value or adjusted
     * value returned by scrollbar's <code>getValue</code>.
     */
    private boolean useCachedValue = false;
    /**
     * The scrollbar value is cached to save real value if the view is adjusted.
     */
    private int scrollBarValue;

    /**
     * Distance between the increment button and the track. This may be a negative
     * number. If negative, then an overlap between the button and track will occur,
     * which is useful for shaped buttons.
     *
     * @since 1.7
     */
    protected int incrGap;

    /**
     * Distance between the decrement button and the track. This may be a negative
     * number. If negative, then an overlap between the button and track will occur,
     * which is useful for shaped buttons.
     *
     * @since 1.7
     */
    protected int decrGap;

    /**
     * Constructs a {@code BasicScrollBarUI}.
     */
    public BasicScrollBarUI() {}

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.POSITIVE_UNIT_INCREMENT));
        map.put(new Actions(Actions.POSITIVE_BLOCK_INCREMENT));
        map.put(new Actions(Actions.NEGATIVE_UNIT_INCREMENT));
        map.put(new Actions(Actions.NEGATIVE_BLOCK_INCREMENT));
        map.put(new Actions(Actions.MIN_SCROLL));
        map.put(new Actions(Actions.MAX_SCROLL));
    }

    /**
     * Creates the UI.
     * @param c the component
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)    {
        return new BasicScrollBarUI();
    }

    /**
     * Configures the scroll bar colors.
     */
    protected void configureScrollBarColors()
    {
        LookAndFeel.installColors(scrollbar, "ScrollBar.background",
                                  "ScrollBar.foreground");
        thumbHighlightColor = UIManager.getColor("ScrollBar.thumbHighlight");
        thumbLightShadowColor = UIManager.getColor("ScrollBar.thumbShadow");
        thumbDarkShadowColor = UIManager.getColor("ScrollBar.thumbDarkShadow");
        thumbColor = UIManager.getColor("ScrollBar.thumb");
        trackColor = UIManager.getColor("ScrollBar.track");
        trackHighlightColor = UIManager.getColor("ScrollBar.trackHighlight");
    }

    /**
     * Installs the UI.
     * @param c the component
     */
    public void installUI(JComponent c)   {
        scrollbar = (JScrollBar)c;
        thumbRect = new Rectangle(0, 0, 0, 0);
        trackRect = new Rectangle(0, 0, 0, 0);
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    /**
     * Uninstalls the UI.
     * @param c the component
     */
    public void uninstallUI(JComponent c) {
        scrollbar = (JScrollBar)c;
        uninstallListeners();
        uninstallDefaults();
        uninstallComponents();
        uninstallKeyboardActions();
        thumbRect = null;
        scrollbar = null;
        incrButton = null;
        decrButton = null;
    }

    /**
     * Installs the defaults.
     */
    protected void installDefaults()
    {
        scrollBarWidth = UIManager.getInt("ScrollBar.width");
        if (scrollBarWidth <= 0) {
            scrollBarWidth = 16;
        }
        minimumThumbSize = (Dimension)UIManager.get("ScrollBar.minimumThumbSize");
        maximumThumbSize = (Dimension)UIManager.get("ScrollBar.maximumThumbSize");

        Boolean absB = (Boolean)UIManager.get("ScrollBar.allowsAbsolutePositioning");
        supportsAbsolutePositioning = (absB != null) ? absB.booleanValue() :
                                      false;

        trackHighlight = NO_HIGHLIGHT;
        if (scrollbar.getLayout() == null ||
                     (scrollbar.getLayout() instanceof UIResource)) {
            scrollbar.setLayout(this);
        }
        configureScrollBarColors();
        LookAndFeel.installBorder(scrollbar, "ScrollBar.border");
        LookAndFeel.installProperty(scrollbar, "opaque", Boolean.TRUE);

        scrollBarValue = scrollbar.getValue();

        incrGap = UIManager.getInt("ScrollBar.incrementButtonGap");
        decrGap = UIManager.getInt("ScrollBar.decrementButtonGap");

        // TODO this can be removed when incrGap/decrGap become protected
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
    }

    /**
     * Installs the components.
     */
    protected void installComponents(){
        switch (scrollbar.getOrientation()) {
        case JScrollBar.VERTICAL:
            incrButton = createIncreaseButton(SOUTH);
            decrButton = createDecreaseButton(NORTH);
            break;

        case JScrollBar.HORIZONTAL:
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
                incrButton = createIncreaseButton(EAST);
                decrButton = createDecreaseButton(WEST);
            } else {
                incrButton = createIncreaseButton(WEST);
                decrButton = createDecreaseButton(EAST);
            }
            break;
        }
        scrollbar.add(incrButton);
        scrollbar.add(decrButton);
        // Force the children's enabled state to be updated.
        scrollbar.setEnabled(scrollbar.isEnabled());
    }

    /**
     * Uninstalls the components.
     */
    protected void uninstallComponents(){
        scrollbar.remove(incrButton);
        scrollbar.remove(decrButton);
    }

    /**
     * Installs the listeners.
     */
    protected void installListeners(){
        trackListener = createTrackListener();
        buttonListener = createArrowButtonListener();
        modelListener = createModelListener();
        propertyChangeListener = createPropertyChangeListener();

        scrollbar.addMouseListener(trackListener);
        scrollbar.addMouseMotionListener(trackListener);
        scrollbar.getModel().addChangeListener(modelListener);
        scrollbar.addPropertyChangeListener(propertyChangeListener);
        scrollbar.addFocusListener(getHandler());

        if (incrButton != null) {
            incrButton.addMouseListener(buttonListener);
        }
        if (decrButton != null) {
            decrButton.addMouseListener(buttonListener);
        }

        scrollListener = createScrollListener();
        scrollTimer = new Timer(scrollSpeedThrottle, scrollListener);
        scrollTimer.setInitialDelay(300);  // default InitialDelay?
    }

    /**
     * Installs the keyboard actions.
     */
    protected void installKeyboardActions(){
        LazyActionMap.installLazyActionMap(scrollbar, BasicScrollBarUI.class,
                                           "ScrollBar.actionMap");

        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        SwingUtilities.replaceUIInputMap(scrollbar, JComponent.WHEN_FOCUSED,
                                         inputMap);
        inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        SwingUtilities.replaceUIInputMap(scrollbar,
                   JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);
    }

    /**
     * Uninstalls the keyboard actions.
     */
    protected void uninstallKeyboardActions(){
        SwingUtilities.replaceUIInputMap(scrollbar, JComponent.WHEN_FOCUSED,
                                         null);
        SwingUtilities.replaceUIActionMap(scrollbar, null);
    }

    private InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_FOCUSED) {
            InputMap keyMap = (InputMap)DefaultLookup.get(
                        scrollbar, this, "ScrollBar.focusInputMap");
            InputMap rtlKeyMap;

            if (scrollbar.getComponentOrientation().isLeftToRight() ||
                ((rtlKeyMap = (InputMap)DefaultLookup.get(scrollbar, this, "ScrollBar.focusInputMap.RightToLeft")) == null)) {
                return keyMap;
            } else {
                rtlKeyMap.setParent(keyMap);
                return rtlKeyMap;
            }
        }
        else if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            InputMap keyMap = (InputMap)DefaultLookup.get(
                        scrollbar, this, "ScrollBar.ancestorInputMap");
            InputMap rtlKeyMap;

            if (scrollbar.getComponentOrientation().isLeftToRight() ||
                ((rtlKeyMap = (InputMap)DefaultLookup.get(scrollbar, this, "ScrollBar.ancestorInputMap.RightToLeft")) == null)) {
                return keyMap;
            } else {
                rtlKeyMap.setParent(keyMap);
                return rtlKeyMap;
            }
        }
        return null;
    }

    /**
     * Uninstall the listeners.
     */
    protected void uninstallListeners() {
        scrollTimer.stop();
        scrollTimer = null;

        if (decrButton != null){
            decrButton.removeMouseListener(buttonListener);
        }
        if (incrButton != null){
            incrButton.removeMouseListener(buttonListener);
        }

        scrollbar.getModel().removeChangeListener(modelListener);
        scrollbar.removeMouseListener(trackListener);
        scrollbar.removeMouseMotionListener(trackListener);
        scrollbar.removePropertyChangeListener(propertyChangeListener);
        scrollbar.removeFocusListener(getHandler());
        handler = null;
    }

    /**
     * Uninstalls the defaults.
     */
    protected void uninstallDefaults(){
        LookAndFeel.uninstallBorder(scrollbar);
        if (scrollbar.getLayout() == this) {
            scrollbar.setLayout(null);
        }
    }


    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Creates a track listener.
     * @return a track listener
     */
    protected TrackListener createTrackListener(){
        return new TrackListener();
    }

    /**
     * Creates an arrow button listener.
     * @return an arrow button   listener
     */
    protected ArrowButtonListener createArrowButtonListener(){
        return new ArrowButtonListener();
    }

    /**
     * Creates a model listener.
     * @return a model listener
     */
    protected ModelListener createModelListener(){
        return new ModelListener();
    }

    /**
     * Creates a scroll listener.
     * @return a scroll listener
     */
    protected ScrollListener createScrollListener(){
        return new ScrollListener();
    }

    /**
     * Creates a property change listener.
     * @return a property change listener
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    private void updateThumbState(int x, int y) {
        Rectangle rect = getThumbBounds();

        setThumbRollover(rect.contains(x, y));
    }

    /**
     * Sets whether or not the mouse is currently over the thumb.
     *
     * @param active True indicates the thumb is currently active.
     * @since 1.5
     */
    protected void setThumbRollover(boolean active) {
        if (thumbActive != active) {
            thumbActive = active;
            scrollbar.repaint(getThumbBounds());
        }
    }

    /**
     * Returns true if the mouse is currently over the thumb.
     *
     * @return true if the thumb is currently active
     * @since 1.5
     */
    public boolean isThumbRollover() {
        return thumbActive;
    }

    public void paint(Graphics g, JComponent c) {
        paintTrack(g, c, getTrackBounds());
        Rectangle thumbBounds = getThumbBounds();
        if (thumbBounds.intersects(g.getClipBounds())) {
            paintThumb(g, c, thumbBounds);
        }
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
    public Dimension getPreferredSize(JComponent c) {
        return (scrollbar.getOrientation() == JScrollBar.VERTICAL)
            ? new Dimension(scrollBarWidth, 48)
            : new Dimension(48, scrollBarWidth);
    }


    /**
     * @param c The JScrollBar that's delegating this method to us.
     * @return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
     * @see #getMinimumSize
     * @see #getPreferredSize
     */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Creates a decrease button.
     * @param orientation the orientation
     * @return a decrease button
     */
    protected JButton createDecreaseButton(int orientation)  {
        return new BasicArrowButton(orientation,
                                    UIManager.getColor("ScrollBar.thumb"),
                                    UIManager.getColor("ScrollBar.thumbShadow"),
                                    UIManager.getColor("ScrollBar.thumbDarkShadow"),
                                    UIManager.getColor("ScrollBar.thumbHighlight"));
    }

    /**
     * Creates an increase button.
     * @param orientation the orientation
     * @return an increase button
     */
    protected JButton createIncreaseButton(int orientation)  {
        return new BasicArrowButton(orientation,
                                    UIManager.getColor("ScrollBar.thumb"),
                                    UIManager.getColor("ScrollBar.thumbShadow"),
                                    UIManager.getColor("ScrollBar.thumbDarkShadow"),
                                    UIManager.getColor("ScrollBar.thumbHighlight"));
    }


    /**
     * Paints the decrease highlight.
     * @param g the graphics
     */
    protected void paintDecreaseHighlight(Graphics g)
    {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            //paint the distance between the start of the track and top of the thumb
            int x = insets.left;
            int y = trackRect.y;
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = thumbR.y - y;
            g.fillRect(x, y, w, h);
        } else {
            //if left-to-right, fill the area between the start of the track and
            //the left edge of the thumb. If right-to-left, fill the area between
            //the end of the thumb and end of the track.
            int x, w;
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
               x = trackRect.x;
                w = thumbR.x - x;
            } else {
                x = thumbR.x + thumbR.width;
                w = trackRect.x + trackRect.width - x;
            }
            int y = insets.top;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }


    /**
     * Paints the increase highlight.
     * @param g the graphics
     */
    protected void paintIncreaseHighlight(Graphics g)
    {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            //fill the area between the bottom of the thumb and the end of the track.
            int x = insets.left;
            int y = thumbR.y + thumbR.height;
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = trackRect.y + trackRect.height - y;
            g.fillRect(x, y, w, h);
        }
        else {
            //if left-to-right, fill the area between the right of the thumb and the
            //end of the track. If right-to-left, then fill the area to the left of
            //the thumb and the start of the track.
            int x, w;
            if (scrollbar.getComponentOrientation().isLeftToRight()) {
                x = thumbR.x + thumbR.width;
                w = trackRect.x + trackRect.width - x;
            } else {
                x = trackRect.x;
                w = thumbR.x - x;
            }
            int y = insets.top;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }


    /**
     * Paints the track.
     * @param g the graphics
     * @param c the component
     * @param trackBounds the track bounds
     */
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
    {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

        if(trackHighlight == DECREASE_HIGHLIGHT)        {
            paintDecreaseHighlight(g);
        }
        else if(trackHighlight == INCREASE_HIGHLIGHT)           {
            paintIncreaseHighlight(g);
        }
    }

    /**
     * Paints the thumb.
     * @param g the graphics
     * @param c the component
     * @param thumbBounds the thumb bounds
     */
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
    {
        if(thumbBounds.isEmpty() || !scrollbar.isEnabled())     {
            return;
        }

        int w = thumbBounds.width;
        int h = thumbBounds.height;

        g.translate(thumbBounds.x, thumbBounds.y);

        g.setColor(thumbDarkShadowColor);
        drawRect(g, 0, 0, w - 1, h - 1);
        g.setColor(thumbColor);
        g.fillRect(0, 0, w - 1, h - 1);

        g.setColor(thumbHighlightColor);
        drawVLine(g, 1, 1, h - 2);
        drawHLine(g, 2, w - 3, 1);

        g.setColor(thumbLightShadowColor);
        drawHLine(g, 2, w - 2, h - 2);
        drawVLine(g, w - 2, 1, h - 3);

        g.translate(-thumbBounds.x, -thumbBounds.y);
    }


    /**
     * Returns the smallest acceptable size for the thumb.  If the scrollbar
     * becomes so small that this size isn't available, the thumb will be
     * hidden.
     * <p>
     * <b>Warning </b>: the value returned by this method should not be
     * be modified, it's a shared static constant.
     *
     * @return The smallest acceptable size for the thumb.
     * @see #getMaximumThumbSize
     */
    protected Dimension getMinimumThumbSize() {
        return minimumThumbSize;
    }

    /**
     * Returns the largest acceptable size for the thumb.  To create a fixed
     * size thumb one make this method and <code>getMinimumThumbSize</code>
     * return the same value.
     * <p>
     * <b>Warning </b>: the value returned by this method should not be
     * be modified, it's a shared static constant.
     *
     * @return The largest acceptable size for the thumb.
     * @see #getMinimumThumbSize
     */
    protected Dimension getMaximumThumbSize()   {
        return maximumThumbSize;
    }


    /*
     * LayoutManager Implementation
     */

    public void addLayoutComponent(String name, Component child) {}
    public void removeLayoutComponent(Component child) {}

    public Dimension preferredLayoutSize(Container scrollbarContainer)  {
        return getPreferredSize((JComponent)scrollbarContainer);
    }

    public Dimension minimumLayoutSize(Container scrollbarContainer) {
        return getMinimumSize((JComponent)scrollbarContainer);
    }

    private int getValue(JScrollBar sb) {
        return (useCachedValue) ? scrollBarValue : sb.getValue();
    }

    /**
     * Laysouts a  vertical scroll bar.
     * @param sb the scroll bar
     */
    protected void layoutVScrollbar(JScrollBar sb)
    {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        /*
         * Width and left edge of the buttons and thumb.
         */
        int itemW = sbSize.width - (sbInsets.left + sbInsets.right);
        int itemX = sbInsets.left;

        /* Nominal locations of the buttons, assuming their preferred
         * size will fit.
         */
        boolean squareButtons = DefaultLookup.getBoolean(
            scrollbar, this, "ScrollBar.squareButtons", false);
        int decrButtonH = squareButtons ? itemW :
                          decrButton.getPreferredSize().height;
        int decrButtonY = sbInsets.top;

        int incrButtonH = squareButtons ? itemW :
                          incrButton.getPreferredSize().height;
        int incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);

        /* The thumb must fit within the height left over after we
         * subtract the preferredSize of the buttons and the insets
         * and the gaps
         */
        int sbInsetsH = sbInsets.top + sbInsets.bottom;
        int sbButtonsH = decrButtonH + incrButtonH;
        int gaps = decrGap + incrGap;
        float trackH = sbSize.height - (sbInsetsH + sbButtonsH) - gaps;

        /* Compute the height and origin of the thumb.   The case
         * where the thumb is at the bottom edge is handled specially
         * to avoid numerical problems in computing thumbY.  Enforce
         * the thumbs min/max dimensions.  If the thumb doesn't
         * fit in the track (trackH) we'll hide it later.
         */
        float min = sb.getMinimum();
        float extent = sb.getVisibleAmount();
        float range = sb.getMaximum() - min;
        float value = getValue(sb);

        int thumbH = (range <= 0)
            ? getMaximumThumbSize().height : (int)(trackH * (extent / range));
        thumbH = Math.max(thumbH, getMinimumThumbSize().height);
        thumbH = Math.min(thumbH, getMaximumThumbSize().height);

        int thumbY = incrButtonY - incrGap - thumbH;
        if (value < (sb.getMaximum() - sb.getVisibleAmount())) {
            float thumbRange = trackH - thumbH;
            thumbY = (int)(0.5f + (thumbRange * ((value - min) / (range - extent))));
            thumbY +=  decrButtonY + decrButtonH + decrGap;
        }

        /* If the buttons don't fit, allocate half of the available
         * space to each and move the lower one (incrButton) down.
         */
        int sbAvailButtonH = (sbSize.height - sbInsetsH);
        if (sbAvailButtonH < sbButtonsH) {
            incrButtonH = decrButtonH = sbAvailButtonH / 2;
            incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);
        }
        decrButton.setBounds(itemX, decrButtonY, itemW, decrButtonH);
        incrButton.setBounds(itemX, incrButtonY, itemW, incrButtonH);

        /* Update the trackRect field.
         */
        int itrackY = decrButtonY + decrButtonH + decrGap;
        int itrackH = incrButtonY - incrGap - itrackY;
        trackRect.setBounds(itemX, itrackY, itemW, itrackH);

        /* If the thumb isn't going to fit, zero it's bounds.  Otherwise
         * make sure it fits between the buttons.  Note that setting the
         * thumbs bounds will cause a repaint.
         */
        if(thumbH >= (int)trackH)       {
            if (UIManager.getBoolean("ScrollBar.alwaysShowThumb")) {
                // This is used primarily for GTK L&F, which expands the
                // thumb to fit the track when it would otherwise be hidden.
                setThumbBounds(itemX, itrackY, itemW, itrackH);
            } else {
                // Other L&F's simply hide the thumb in this case.
                setThumbBounds(0, 0, 0, 0);
            }
        }
        else {
            if ((thumbY + thumbH) > incrButtonY - incrGap) {
                thumbY = incrButtonY - incrGap - thumbH;
            }
            if (thumbY  < (decrButtonY + decrButtonH + decrGap)) {
                thumbY = decrButtonY + decrButtonH + decrGap + 1;
            }
            setThumbBounds(itemX, thumbY, itemW, thumbH);
        }
    }

    /**
     * Laysouts a  vertical scroll bar.
     * @param sb the scroll bar
     */
    protected void layoutHScrollbar(JScrollBar sb)
    {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        /* Height and top edge of the buttons and thumb.
         */
        int itemH = sbSize.height - (sbInsets.top + sbInsets.bottom);
        int itemY = sbInsets.top;

        boolean ltr = sb.getComponentOrientation().isLeftToRight();

        /* Nominal locations of the buttons, assuming their preferred
         * size will fit.
         */
        boolean squareButtons = DefaultLookup.getBoolean(
            scrollbar, this, "ScrollBar.squareButtons", false);
        int leftButtonW = squareButtons ? itemH :
                          decrButton.getPreferredSize().width;
        int rightButtonW = squareButtons ? itemH :
                          incrButton.getPreferredSize().width;
        if (!ltr) {
            int temp = leftButtonW;
            leftButtonW = rightButtonW;
            rightButtonW = temp;
        }
        int leftButtonX = sbInsets.left;
        int rightButtonX = sbSize.width - (sbInsets.right + rightButtonW);
        int leftGap = ltr ? decrGap : incrGap;
        int rightGap = ltr ? incrGap : decrGap;

        /* The thumb must fit within the width left over after we
         * subtract the preferredSize of the buttons and the insets
         * and the gaps
         */
        int sbInsetsW = sbInsets.left + sbInsets.right;
        int sbButtonsW = leftButtonW + rightButtonW;
        float trackW = sbSize.width - (sbInsetsW + sbButtonsW) - (leftGap + rightGap);

        /* Compute the width and origin of the thumb.  Enforce
         * the thumbs min/max dimensions.  The case where the thumb
         * is at the right edge is handled specially to avoid numerical
         * problems in computing thumbX.  If the thumb doesn't
         * fit in the track (trackH) we'll hide it later.
         */
        float min = sb.getMinimum();
        float max = sb.getMaximum();
        float extent = sb.getVisibleAmount();
        float range = max - min;
        float value = getValue(sb);

        int thumbW = (range <= 0)
            ? getMaximumThumbSize().width : (int)(trackW * (extent / range));
        thumbW = Math.max(thumbW, getMinimumThumbSize().width);
        thumbW = Math.min(thumbW, getMaximumThumbSize().width);

        int thumbX = ltr ? rightButtonX - rightGap - thumbW : leftButtonX + leftButtonW + leftGap;
        if (value < (max - sb.getVisibleAmount())) {
            float thumbRange = trackW - thumbW;
            if( ltr ) {
                thumbX = (int)(0.5f + (thumbRange * ((value - min) / (range - extent))));
            } else {
                thumbX = (int)(0.5f + (thumbRange * ((max - extent - value) / (range - extent))));
            }
            thumbX += leftButtonX + leftButtonW + leftGap;
        }

        /* If the buttons don't fit, allocate half of the available
         * space to each and move the right one over.
         */
        int sbAvailButtonW = (sbSize.width - sbInsetsW);
        if (sbAvailButtonW < sbButtonsW) {
            rightButtonW = leftButtonW = sbAvailButtonW / 2;
            rightButtonX = sbSize.width - (sbInsets.right + rightButtonW + rightGap);
        }

        (ltr ? decrButton : incrButton).setBounds(leftButtonX, itemY, leftButtonW, itemH);
        (ltr ? incrButton : decrButton).setBounds(rightButtonX, itemY, rightButtonW, itemH);

        /* Update the trackRect field.
         */
        int itrackX = leftButtonX + leftButtonW + leftGap;
        int itrackW = rightButtonX - rightGap - itrackX;
        trackRect.setBounds(itrackX, itemY, itrackW, itemH);

        /* Make sure the thumb fits between the buttons.  Note
         * that setting the thumbs bounds causes a repaint.
         */
        if (thumbW >= (int)trackW) {
            if (UIManager.getBoolean("ScrollBar.alwaysShowThumb")) {
                // This is used primarily for GTK L&F, which expands the
                // thumb to fit the track when it would otherwise be hidden.
                setThumbBounds(itrackX, itemY, itrackW, itemH);
            } else {
                // Other L&F's simply hide the thumb in this case.
                setThumbBounds(0, 0, 0, 0);
            }
        }
        else {
            if (thumbX + thumbW > rightButtonX - rightGap) {
                thumbX = rightButtonX - rightGap - thumbW;
            }
            if (thumbX  < leftButtonX + leftButtonW + leftGap) {
                thumbX = leftButtonX + leftButtonW + leftGap + 1;
            }
            setThumbBounds(thumbX, itemY, thumbW, itemH);
        }
    }

    public void layoutContainer(Container scrollbarContainer)
    {
        /* If the user is dragging the value, we'll assume that the
         * scrollbars layout is OK modulo the thumb which is being
         * handled by the dragging code.
         */
        if (isDragging) {
            return;
        }

        JScrollBar scrollbar = (JScrollBar)scrollbarContainer;
        switch (scrollbar.getOrientation()) {
        case JScrollBar.VERTICAL:
            layoutVScrollbar(scrollbar);
            break;

        case JScrollBar.HORIZONTAL:
            layoutHScrollbar(scrollbar);
            break;
        }
    }


    /**
     * Set the bounds of the thumb and force a repaint that includes
     * the old thumbBounds and the new one.
     *
     * @param x set the x location of the thumb
     * @param y set the y location of the thumb
     * @param width set the width of the thumb
     * @param height set the height of the thumb
     * @see #getThumbBounds
     */
    protected void setThumbBounds(int x, int y, int width, int height)
    {
        /* If the thumbs bounds haven't changed, we're done.
         */
        if ((thumbRect.x == x) &&
            (thumbRect.y == y) &&
            (thumbRect.width == width) &&
            (thumbRect.height == height)) {
            return;
        }

        /* Update thumbRect, and repaint the union of x,y,w,h and
         * the old thumbRect.
         */
        int minX = Math.min(x, thumbRect.x);
        int minY = Math.min(y, thumbRect.y);
        int maxX = Math.max(x + width, thumbRect.x + thumbRect.width);
        int maxY = Math.max(y + height, thumbRect.y + thumbRect.height);

        thumbRect.setBounds(x, y, width, height);
        scrollbar.repaint(minX, minY, maxX - minX, maxY - minY);

        // Once there is API to determine the mouse location this will need
        // to be changed.
        setThumbRollover(false);
    }


    /**
     * Return the current size/location of the thumb.
     * <p>
     * <b>Warning </b>: the value returned by this method should not be
     * be modified, it's a reference to the actual rectangle, not a copy.
     *
     * @return The current size/location of the thumb.
     * @see #setThumbBounds
     */
    protected Rectangle getThumbBounds() {
        return thumbRect;
    }


    /**
     * Returns the current bounds of the track, i.e. the space in between
     * the increment and decrement buttons, less the insets.  The value
     * returned by this method is updated each time the scrollbar is
     * laid out (validated).
     * <p>
     * <b>Warning </b>: the value returned by this method should not be
     * be modified, it's a reference to the actual rectangle, not a copy.
     *
     * @return the current bounds of the scrollbar track
     * @see #layoutContainer
     */
    protected Rectangle getTrackBounds() {
        return trackRect;
    }

    /*
     * Method for scrolling by a block increment.
     * Added for mouse wheel scrolling support, RFE 4202656.
     */
    static void scrollByBlock(JScrollBar scrollbar, int direction) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, and also from scrollByBlock().
            int oldValue = scrollbar.getValue();
            int blockIncrement = scrollbar.getBlockIncrement(direction);
            int delta = blockIncrement * ((direction > 0) ? +1 : -1);
            int newValue = oldValue + delta;

            // Check for overflow.
            if (delta > 0 && newValue < oldValue) {
                newValue = scrollbar.getMaximum();
            }
            else if (delta < 0 && newValue > oldValue) {
                newValue = scrollbar.getMinimum();
            }

            scrollbar.setValue(newValue);
    }

    /**
     * Scrolls by block.
     * @param direction the direction to scroll
     */
    protected void scrollByBlock(int direction)
    {
        scrollByBlock(scrollbar, direction);
            trackHighlight = direction > 0 ? INCREASE_HIGHLIGHT : DECREASE_HIGHLIGHT;
            Rectangle dirtyRect = getTrackBounds();
            scrollbar.repaint(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
    }

    /*
     * Method for scrolling by a unit increment.
     * Added for mouse wheel scrolling support, RFE 4202656.
     *
     * If limitByBlock is set to true, the scrollbar will scroll at least 1
     * unit increment, but will not scroll farther than the block increment.
     * See BasicScrollPaneUI.Handler.mouseWheelMoved().
     */
    static void scrollByUnits(JScrollBar scrollbar, int direction,
                              int units, boolean limitToBlock) {
        // This method is called from BasicScrollPaneUI to implement wheel
        // scrolling, as well as from scrollByUnit().
        int delta;
        int limit = -1;

        if (limitToBlock) {
            if (direction < 0) {
                limit = scrollbar.getValue() -
                                         scrollbar.getBlockIncrement(direction);
            }
            else {
                limit = scrollbar.getValue() +
                                         scrollbar.getBlockIncrement(direction);
            }
        }

        for (int i=0; i<units; i++) {
            if (direction > 0) {
                delta = scrollbar.getUnitIncrement(direction);
            }
            else {
                delta = -scrollbar.getUnitIncrement(direction);
            }

            int oldValue = scrollbar.getValue();
            int newValue = oldValue + delta;

            // Check for overflow.
            if (delta > 0 && newValue < oldValue) {
                newValue = scrollbar.getMaximum();
            }
            else if (delta < 0 && newValue > oldValue) {
                newValue = scrollbar.getMinimum();
            }
            if (oldValue == newValue) {
                break;
            }

            if (limitToBlock && i > 0) {
                assert limit != -1;
                if ((direction < 0 && newValue < limit) ||
                    (direction > 0 && newValue > limit)) {
                    break;
                }
            }
            scrollbar.setValue(newValue);
        }
    }

    /**
     * Scrolls by unit.
     * @param direction the direction to scroll
     */
    protected void scrollByUnit(int direction)  {
        scrollByUnits(scrollbar, direction, 1, false);
    }

    /**
     * Indicates whether the user can absolutely position the thumb with
     * a mouse gesture (usually the middle mouse button).
     *
     * @return true if a mouse gesture can absolutely position the thumb
     * @since 1.5
     */
    public boolean getSupportsAbsolutePositioning() {
        return supportsAbsolutePositioning;
    }

    /**
     * A listener to listen for model changes.
     */
    protected class ModelListener implements ChangeListener {
        /**
         * Constructs a {@code ModelListener}.
         */
        protected ModelListener() {}

        public void stateChanged(ChangeEvent e) {
            if (!useCachedValue) {
                scrollBarValue = scrollbar.getValue();
            }
            layoutContainer(scrollbar);
            useCachedValue = false;
        }
    }


    /**
     * Track mouse drags.
     */
    protected class TrackListener
        extends MouseAdapter implements MouseMotionListener
    {
        /** The offset */
        protected transient int offset;
        /** Current mouse x position */
        protected transient int currentMouseX;
        /** Current mouse y position */
        protected transient int currentMouseY;
        private transient int direction = +1;

        /**
         * Constructs a {@code TrackListener}.
         */
        protected TrackListener() {}

        /** {@inheritDoc} */
        public void mouseReleased(MouseEvent e)
        {
            if (isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
            if (SwingUtilities.isRightMouseButton(e) ||
                (!getSupportsAbsolutePositioning() &&
                 SwingUtilities.isMiddleMouseButton(e)))
                return;
            if(!scrollbar.isEnabled())
                return;

            Rectangle r = getTrackBounds();
            scrollbar.repaint(r.x, r.y, r.width, r.height);

            trackHighlight = NO_HIGHLIGHT;
            setDragging(false);
            offset = 0;
            scrollTimer.stop();
            useCachedValue = true;
            scrollbar.setValueIsAdjusting(false);
        }


        /**
         * If the mouse is pressed above the "thumb" component
         * then reduce the scrollbars value by one page ("page up"),
         * otherwise increase it by one page.  If there is no
         * thumb then page up if the mouse is in the upper half
         * of the track.
         */
        public void mousePressed(MouseEvent e)
        {
            if (SwingUtilities.isRightMouseButton(e) ||
                (!getSupportsAbsolutePositioning() &&
                 SwingUtilities.isMiddleMouseButton(e)))
                return;
            if(!scrollbar.isEnabled())
                return;

            if (!scrollbar.hasFocus() && scrollbar.isRequestFocusEnabled()) {
                scrollbar.requestFocus();
            }

            useCachedValue = true;
            scrollbar.setValueIsAdjusting(true);

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            // Clicked in the Thumb area?
            if(getThumbBounds().contains(currentMouseX, currentMouseY)) {
                switch (scrollbar.getOrientation()) {
                case JScrollBar.VERTICAL:
                    offset = currentMouseY - getThumbBounds().y;
                    break;
                case JScrollBar.HORIZONTAL:
                    offset = currentMouseX - getThumbBounds().x;
                    break;
                }
                setDragging(true);
                return;
            }
            else if (getSupportsAbsolutePositioning() &&
                     SwingUtilities.isMiddleMouseButton(e)) {
                switch (scrollbar.getOrientation()) {
                case JScrollBar.VERTICAL:
                    offset = getThumbBounds().height / 2;
                    break;
                case JScrollBar.HORIZONTAL:
                    offset = getThumbBounds().width / 2;
                    break;
                }
                setDragging(true);
                setValueFrom(e);
                return;
            }
            setDragging(false);

            Dimension sbSize = scrollbar.getSize();
            direction = +1;

            switch (scrollbar.getOrientation()) {
            case JScrollBar.VERTICAL:
                if (getThumbBounds().isEmpty()) {
                    int scrollbarCenter = sbSize.height / 2;
                    direction = (currentMouseY < scrollbarCenter) ? -1 : +1;
                } else {
                    int thumbY = getThumbBounds().y;
                    direction = (currentMouseY < thumbY) ? -1 : +1;
                }
                break;
            case JScrollBar.HORIZONTAL:
                if (getThumbBounds().isEmpty()) {
                    int scrollbarCenter = sbSize.width / 2;
                    direction = (currentMouseX < scrollbarCenter) ? -1 : +1;
                } else {
                    int thumbX = getThumbBounds().x;
                    direction = (currentMouseX < thumbX) ? -1 : +1;
                }
                if (!scrollbar.getComponentOrientation().isLeftToRight()) {
                    direction = -direction;
                }
                break;
            }
            scrollByBlock(direction);

            scrollTimer.stop();
            scrollListener.setDirection(direction);
            scrollListener.setScrollByBlock(true);
            startScrollTimerIfNecessary();
        }


        /**
         * Set the models value to the position of the thumb's top of Vertical
         * scrollbar, or the left/right of Horizontal scrollbar in
         * left-to-right/right-to-left scrollbar relative to the origin of the
         * track.
         */
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e) ||
                (!getSupportsAbsolutePositioning() &&
                 SwingUtilities.isMiddleMouseButton(e)))
                return;
            if(!scrollbar.isEnabled() || getThumbBounds().isEmpty()) {
                return;
            }
            if (isDragging) {
                setValueFrom(e);
            } else {
                currentMouseX = e.getX();
                currentMouseY = e.getY();
                updateThumbState(currentMouseX, currentMouseY);
                startScrollTimerIfNecessary();
            }
        }

        private void setValueFrom(MouseEvent e) {
            boolean active = isThumbRollover();
            BoundedRangeModel model = scrollbar.getModel();
            Rectangle thumbR = getThumbBounds();
            float trackLength;
            int thumbMin, thumbMax, thumbPos;

            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                thumbMin = trackRect.y;
                thumbMax = trackRect.y + trackRect.height - thumbR.height;
                thumbPos = Math.min(thumbMax, Math.max(thumbMin, (e.getY() - offset)));
                setThumbBounds(thumbR.x, thumbPos, thumbR.width, thumbR.height);
                trackLength = getTrackBounds().height;
            }
            else {
                thumbMin = trackRect.x;
                thumbMax = trackRect.x + trackRect.width - thumbR.width;
                thumbPos = Math.min(thumbMax, Math.max(thumbMin, (e.getX() - offset)));
                setThumbBounds(thumbPos, thumbR.y, thumbR.width, thumbR.height);
                trackLength = getTrackBounds().width;
            }

            /* Set the scrollbars value.  If the thumb has reached the end of
             * the scrollbar, then just set the value to its maximum.  Otherwise
             * compute the value as accurately as possible.
             */
            if (thumbPos == thumbMax) {
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL ||
                    scrollbar.getComponentOrientation().isLeftToRight()) {
                    scrollbar.setValue(model.getMaximum() - model.getExtent());
                } else {
                    scrollbar.setValue(model.getMinimum());
                }
            }
            else {
                float valueMax = model.getMaximum() - model.getExtent();
                float valueRange = valueMax - model.getMinimum();
                float thumbValue = thumbPos - thumbMin;
                float thumbRange = thumbMax - thumbMin;
                int value;
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL ||
                    scrollbar.getComponentOrientation().isLeftToRight()) {
                    value = (int)(0.5 + ((thumbValue / thumbRange) * valueRange));
                } else {
                    value = (int)(0.5 + (((thumbMax - thumbPos) / thumbRange) * valueRange));
                }

                useCachedValue = true;
                scrollBarValue = value + model.getMinimum();
                scrollbar.setValue(adjustValueIfNecessary(scrollBarValue));
            }
            setThumbRollover(active);
        }

        private int adjustValueIfNecessary(int value) {
            if (scrollbar.getParent() instanceof JScrollPane) {
                JScrollPane scrollpane = (JScrollPane)scrollbar.getParent();
                JViewport viewport = scrollpane.getViewport();
                Component view = viewport.getView();
                if (view instanceof JList) {
                    JList<?> list = (JList)view;
                    if (DefaultLookup.getBoolean(list, list.getUI(),
                                                 "List.lockToPositionOnScroll", false)) {
                        int adjustedValue = value;
                        int mode = list.getLayoutOrientation();
                        int orientation = scrollbar.getOrientation();
                        if (orientation == JScrollBar.VERTICAL && mode == JList.VERTICAL) {
                            int index = list.locationToIndex(new Point(0, value));
                            Rectangle rect = list.getCellBounds(index, index);
                            if (rect != null) {
                                adjustedValue = rect.y;
                            }
                        }
                        if (orientation == JScrollBar.HORIZONTAL &&
                            (mode == JList.VERTICAL_WRAP || mode == JList.HORIZONTAL_WRAP)) {
                            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                                int index = list.locationToIndex(new Point(value, 0));
                                Rectangle rect = list.getCellBounds(index, index);
                                if (rect != null) {
                                    adjustedValue = rect.x;
                                }
                            }
                            else {
                                Point loc = new Point(value, 0);
                                int extent = viewport.getExtentSize().width;
                                loc.x += extent - 1;
                                int index = list.locationToIndex(loc);
                                Rectangle rect = list.getCellBounds(index, index);
                                if (rect != null) {
                                    adjustedValue = rect.x + rect.width - extent;
                                }
                            }
                        }
                        value = adjustedValue;

                    }
                }
            }
            return value;
        }

        private void startScrollTimerIfNecessary() {
            if (scrollTimer.isRunning()) {
                return;
            }

            Rectangle tb = getThumbBounds();

            switch (scrollbar.getOrientation()) {
            case JScrollBar.VERTICAL:
                if (direction > 0) {
                    if (tb.y + tb.height < trackListener.currentMouseY) {
                        scrollTimer.start();
                    }
                } else if (tb.y > trackListener.currentMouseY) {
                    scrollTimer.start();
                }
                break;
            case JScrollBar.HORIZONTAL:
                if ((direction > 0 && isMouseAfterThumb())
                        || (direction < 0 && isMouseBeforeThumb())) {

                    scrollTimer.start();
                }
                break;
            }
        }

        /** {@inheritDoc} */
        public void mouseMoved(MouseEvent e) {
            if (!isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
        }

        /**
         * Invoked when the mouse exits the scrollbar.
         *
         * @param e MouseEvent further describing the event
         * @since 1.5
         */
        public void mouseExited(MouseEvent e) {
            if (!isDragging) {
                setThumbRollover(false);
            }
        }
    }


    /**
     * Listener for cursor keys.
     */
    protected class ArrowButtonListener extends MouseAdapter
    {
        // Because we are handling both mousePressed and Actions
        // we need to make sure we don't fire under both conditions.
        // (keyfocus on scrollbars causes action without mousePress
        boolean handledEvent;

        /**
         * Constructs an {@code ArrowButtonListener}.
         */
        protected ArrowButtonListener() {}

        public void mousePressed(MouseEvent e)          {
            if(!scrollbar.isEnabled()) { return; }
            // not an unmodified left mouse button
            //if(e.getModifiers() != InputEvent.BUTTON1_MASK) {return; }
            if( ! SwingUtilities.isLeftMouseButton(e)) { return; }

            int direction = (e.getSource() == incrButton) ? 1 : -1;

            scrollByUnit(direction);
            scrollTimer.stop();
            scrollListener.setDirection(direction);
            scrollListener.setScrollByBlock(false);
            scrollTimer.start();

            handledEvent = true;
            if (!scrollbar.hasFocus() && scrollbar.isRequestFocusEnabled()) {
                scrollbar.requestFocus();
            }
        }

        public void mouseReleased(MouseEvent e)         {
            scrollTimer.stop();
            handledEvent = false;
            scrollbar.setValueIsAdjusting(false);
        }
    }


    /**
     * Listener for scrolling events initiated in the
     * <code>ScrollPane</code>.
     */
    protected class ScrollListener implements ActionListener
    {
        int direction = +1;
        boolean useBlockIncrement;

        /** Constructs a {@code ScrollListener}. */
        public ScrollListener() {
            direction = +1;
            useBlockIncrement = false;
        }

        /**
         * Constructs a {@code ScrollListener}.
         * @param dir direction
         * @param block use block increment
         */
        public ScrollListener(int dir, boolean block)   {
            direction = dir;
            useBlockIncrement = block;
        }

        /**
         * Sets the direction.
         * @param direction the new direction
         */
        public void setDirection(int direction) { this.direction = direction; }
        /**
         * Sets the scrolling by block
         * @param block whether or not to scroll by block
         */
        public void setScrollByBlock(boolean block) { this.useBlockIncrement = block; }

        /** {@inheritDoc} */
        public void actionPerformed(ActionEvent e) {
            if(useBlockIncrement)       {
                scrollByBlock(direction);
                // Stop scrolling if the thumb catches up with the mouse
                if(scrollbar.getOrientation() == JScrollBar.VERTICAL)   {
                    if(direction > 0)   {
                        if(getThumbBounds().y + getThumbBounds().height
                                >= trackListener.currentMouseY)
                                    ((Timer)e.getSource()).stop();
                    } else if(getThumbBounds().y <= trackListener.currentMouseY)        {
                        ((Timer)e.getSource()).stop();
                    }
                } else {
                    if ((direction > 0 && !isMouseAfterThumb())
                           || (direction < 0 && !isMouseBeforeThumb())) {

                       ((Timer)e.getSource()).stop();
                    }
                }
            } else {
                scrollByUnit(direction);
            }

            if(direction > 0
                && scrollbar.getValue()+scrollbar.getVisibleAmount()
                        >= scrollbar.getMaximum())
                ((Timer)e.getSource()).stop();
            else if(direction < 0
                && scrollbar.getValue() <= scrollbar.getMinimum())
                ((Timer)e.getSource()).stop();
        }
    }

    private boolean isMouseLeftOfThumb() {
        return trackListener.currentMouseX < getThumbBounds().x;
    }

    private boolean isMouseRightOfThumb() {
        Rectangle tb = getThumbBounds();
        return trackListener.currentMouseX > tb.x + tb.width;
    }

    private boolean isMouseBeforeThumb() {
        return scrollbar.getComponentOrientation().isLeftToRight()
            ? isMouseLeftOfThumb()
            : isMouseRightOfThumb();
    }

    private boolean isMouseAfterThumb() {
        return scrollbar.getComponentOrientation().isLeftToRight()
            ? isMouseRightOfThumb()
            : isMouseLeftOfThumb();
    }

    private void updateButtonDirections() {
        int orient = scrollbar.getOrientation();
        if (scrollbar.getComponentOrientation().isLeftToRight()) {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton)incrButton).setDirection(
                        orient == HORIZONTAL? EAST : SOUTH);
            }
            if (decrButton instanceof BasicArrowButton) {
                ((BasicArrowButton)decrButton).setDirection(
                        orient == HORIZONTAL? WEST : NORTH);
            }
        }
        else {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton)incrButton).setDirection(
                        orient == HORIZONTAL? WEST : SOUTH);
            }
            if (decrButton instanceof BasicArrowButton) {
                ((BasicArrowButton)decrButton).setDirection(
                        orient == HORIZONTAL ? EAST : NORTH);
            }
        }
    }

    private void setDragging(boolean dragging) {
        this.isDragging = dragging;
        scrollbar.repaint(getThumbBounds());
    }


    /** Property change handler */
    public class PropertyChangeHandler implements PropertyChangeListener
    {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /** {@inheritDoc} */
        public void propertyChange(PropertyChangeEvent e) {
            getHandler().propertyChange(e);
        }
    }


    /**
     * Used for scrolling the scrollbar.
     */
    private static class Actions extends UIAction {
        private static final String POSITIVE_UNIT_INCREMENT =
                                    "positiveUnitIncrement";
        private static final String POSITIVE_BLOCK_INCREMENT =
                                    "positiveBlockIncrement";
        private static final String NEGATIVE_UNIT_INCREMENT =
                                    "negativeUnitIncrement";
        private static final String NEGATIVE_BLOCK_INCREMENT =
                                    "negativeBlockIncrement";
        private static final String MIN_SCROLL = "minScroll";
        private static final String MAX_SCROLL = "maxScroll";

        Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            JScrollBar scrollBar = (JScrollBar)e.getSource();
            String key = getName();
            if (key == POSITIVE_UNIT_INCREMENT) {
                scroll(scrollBar, POSITIVE_SCROLL, false);
            }
            else if (key == POSITIVE_BLOCK_INCREMENT) {
                scroll(scrollBar, POSITIVE_SCROLL, true);
            }
            else if (key == NEGATIVE_UNIT_INCREMENT) {
                scroll(scrollBar, NEGATIVE_SCROLL, false);
            }
            else if (key == NEGATIVE_BLOCK_INCREMENT) {
                scroll(scrollBar, NEGATIVE_SCROLL, true);
            }
            else if (key == MIN_SCROLL) {
                scroll(scrollBar, BasicScrollBarUI.MIN_SCROLL, true);
            }
            else if (key == MAX_SCROLL) {
                scroll(scrollBar, BasicScrollBarUI.MAX_SCROLL, true);
            }
        }
        private void scroll(JScrollBar scrollBar, int dir, boolean block) {

            if (dir == NEGATIVE_SCROLL || dir == POSITIVE_SCROLL) {
                int amount;
                // Don't use the BasicScrollBarUI.scrollByXXX methods as we
                // don't want to use an invokeLater to reset the trackHighlight
                // via an invokeLater
                if (block) {
                    if (dir == NEGATIVE_SCROLL) {
                        amount = -1 * scrollBar.getBlockIncrement(-1);
                    }
                    else {
                        amount = scrollBar.getBlockIncrement(1);
                    }
                }
                else {
                    if (dir == NEGATIVE_SCROLL) {
                        amount = -1 * scrollBar.getUnitIncrement(-1);
                    }
                    else {
                        amount = scrollBar.getUnitIncrement(1);
                    }
                }
                scrollBar.setValue(scrollBar.getValue() + amount);
            }
            else if (dir == BasicScrollBarUI.MIN_SCROLL) {
                scrollBar.setValue(scrollBar.getMinimum());
            }
            else if (dir == BasicScrollBarUI.MAX_SCROLL) {
                scrollBar.setValue(scrollBar.getMaximum());
            }
        }
    }


    //
    // EventHandler
    //
    private class Handler implements FocusListener, PropertyChangeListener {
        //
        // FocusListener
        //
        public void focusGained(FocusEvent e) {
            scrollbar.repaint();
        }

        public void focusLost(FocusEvent e) {
            scrollbar.repaint();
        }


        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if ("model" == propertyName) {
                BoundedRangeModel oldModel = (BoundedRangeModel)e.getOldValue();
                BoundedRangeModel newModel = (BoundedRangeModel)e.getNewValue();
                oldModel.removeChangeListener(modelListener);
                newModel.addChangeListener(modelListener);
                scrollBarValue = scrollbar.getValue();
                scrollbar.repaint();
                scrollbar.revalidate();
            } else if ("orientation" == propertyName) {
                updateButtonDirections();
            } else if ("componentOrientation" == propertyName) {
                updateButtonDirections();
                InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
                SwingUtilities.replaceUIInputMap(scrollbar, JComponent.WHEN_FOCUSED, inputMap);
            }
        }
    }
}
