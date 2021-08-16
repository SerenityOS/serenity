/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;

import sun.swing.SwingUtilities2;

/**
 * Provides the Synth L&amp;F UI delegate for
 * {@link javax.swing.JTabbedPane}.
 *
 * <p>Looks up the {@code selectedTabPadInsets} property from the Style,
 * which represents additional insets for the selected tab.
 *
 * @author Scott Violet
 * @since 1.7
 */
public class SynthTabbedPaneUI extends BasicTabbedPaneUI
                               implements PropertyChangeListener, SynthUI {

    /**
     * <p>If non-zero, tabOverlap indicates the amount that the tab bounds
     * should be altered such that they would overlap with a tab on either the
     * leading or trailing end of a run (ie: in TOP, this would be on the left
     * or right).</p>

     * <p>A positive overlap indicates that tabs should overlap right/down,
     * while a negative overlap indicates tha tabs should overlap left/up.</p>
     *
     * <p>When tabOverlap is specified, it both changes the x position and width
     * of the tab if in TOP or BOTTOM placement, and changes the y position and
     * height if in LEFT or RIGHT placement.</p>
     *
     * <p>This is done for the following reason. Consider a run of 10 tabs.
     * There are 9 gaps between these tabs. If you specified a tabOverlap of
     * "-1", then each of the tabs "x" values will be shifted left. This leaves
     * 9 pixels of space to the right of the right-most tab unpainted. So, each
     * tab's width is also extended by 1 pixel to make up the difference.</p>
     *
     * <p>This property respects the RTL component orientation.</p>
     */
    private int tabOverlap = 0;

    /**
     * When a tabbed pane has multiple rows of tabs, this indicates whether
     * the tabs in the upper row(s) should extend to the base of the tab area,
     * or whether they should remain at their normal tab height. This does not
     * affect the bounds of the tabs, only the bounds of area painted by the
     * tabs. The text position does not change. The result is that the bottom
     * border of the upper row of tabs becomes fully obscured by the lower tabs,
     * resulting in a cleaner look.
     */
    private boolean extendTabsToBase = false;

    private SynthContext tabAreaContext;
    private SynthContext tabContext;
    private SynthContext tabContentContext;

    private SynthStyle style;
    private SynthStyle tabStyle;
    private SynthStyle tabAreaStyle;
    private SynthStyle tabContentStyle;

    private Rectangle textRect = new Rectangle();
    private Rectangle iconRect = new Rectangle();

    private Rectangle tabAreaBounds = new Rectangle();

    //added for the Nimbus look and feel, where the tab area is painted differently depending on the
    //state for the selected tab
    private boolean tabAreaStatesMatchSelectedTab = false;
    //added for the Nimbus LAF to ensure that the labels don't move whether the tab is selected or not
    private boolean nudgeSelectedLabel = true;

    private boolean selectedTabIsPressed = false;

    /**
     *
     * Constructs a {@code SynthTabbedPaneUI}.
     */
    public SynthTabbedPaneUI() {}

    /**
     * Creates a new UI object for the given component.
     *
     * @param c component to create UI object for
     * @return the UI object
     */
    public static ComponentUI createUI(JComponent c) {
        return new SynthTabbedPaneUI();
    }

     private boolean scrollableTabLayoutEnabled() {
        return (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installDefaults() {
        updateStyle(tabPane);
    }

    private void updateStyle(JTabbedPane c) {
        SynthContext context = getContext(c, ENABLED);
        SynthStyle oldStyle = style;
        style = SynthLookAndFeel.updateStyle(context, this);
        // Add properties other than JComponent colors, Borders and
        // opacity settings here:
        if (style != oldStyle) {
            tabRunOverlay =
                style.getInt(context, "TabbedPane.tabRunOverlay", 0);
            tabOverlap = style.getInt(context, "TabbedPane.tabOverlap", 0);
            extendTabsToBase = style.getBoolean(context,
                    "TabbedPane.extendTabsToBase", false);
            textIconGap = style.getInt(context, "TabbedPane.textIconGap", 0);
            selectedTabPadInsets = (Insets)style.get(context,
                "TabbedPane.selectedTabPadInsets");
            if (selectedTabPadInsets == null) {
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }
            tabAreaStatesMatchSelectedTab = style.getBoolean(context,
                    "TabbedPane.tabAreaStatesMatchSelectedTab", false);
            nudgeSelectedLabel = style.getBoolean(context,
                    "TabbedPane.nudgeSelectedLabel", true);
            if (oldStyle != null) {
                uninstallKeyboardActions();
                installKeyboardActions();
            }
        }

        tabContext = getContext(c, Region.TABBED_PANE_TAB, ENABLED);
        this.tabStyle = SynthLookAndFeel.updateStyle(tabContext, this);
        tabInsets = tabStyle.getInsets(tabContext, null);


        tabAreaContext = getContext(c, Region.TABBED_PANE_TAB_AREA, ENABLED);
        this.tabAreaStyle = SynthLookAndFeel.updateStyle(tabAreaContext, this);
        tabAreaInsets = tabAreaStyle.getInsets(tabAreaContext, null);


        tabContentContext = getContext(c, Region.TABBED_PANE_CONTENT, ENABLED);
        this.tabContentStyle = SynthLookAndFeel.updateStyle(tabContentContext,
                                                            this);
        contentBorderInsets =
            tabContentStyle.getInsets(tabContentContext, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void installListeners() {
        super.installListeners();
        tabPane.addPropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallListeners() {
        super.uninstallListeners();
        tabPane.removePropertyChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void uninstallDefaults() {
        SynthContext context = getContext(tabPane, ENABLED);
        style.uninstallDefaults(context);
        style = null;

        tabStyle.uninstallDefaults(tabContext);
        tabContext = null;
        tabStyle = null;

        tabAreaStyle.uninstallDefaults(tabAreaContext);
        tabAreaContext = null;
        tabAreaStyle = null;

        tabContentStyle.uninstallDefaults(tabContentContext);
        tabContentContext = null;
        tabContentStyle = null;
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

    private SynthContext getContext(JComponent c, Region subregion, int state){
        SynthStyle style = null;

        if (subregion == Region.TABBED_PANE_TAB) {
            style = tabStyle;
        }
        else if (subregion == Region.TABBED_PANE_TAB_AREA) {
            style = tabAreaStyle;
        }
        else if (subregion == Region.TABBED_PANE_CONTENT) {
            style = tabContentStyle;
        }
        return SynthContext.getContext(c, subregion, style, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JButton createScrollButton(int direction) {
        // added for Nimbus LAF so that it can use the basic arrow buttons
        // UIManager is queried directly here because this is called before
        // updateStyle is called so the style can not be queried directly
        if (UIManager.getBoolean("TabbedPane.useBasicArrows")) {
            JButton btn = super.createScrollButton(direction);
            btn.setBorder(BorderFactory.createEmptyBorder());
            return btn;
        }
        return new SynthScrollableTabButton(direction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (SynthLookAndFeel.shouldUpdateStyle(e)) {
            updateStyle(tabPane);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to keep track of whether the selected tab is also pressed.
     */
    @Override
    protected MouseListener createMouseListener() {
        final MouseListener delegate = super.createMouseListener();
        final MouseMotionListener delegate2 = (MouseMotionListener)delegate;
        return new MouseListener() {
            public void mouseClicked(MouseEvent e) { delegate.mouseClicked(e); }
            public void mouseEntered(MouseEvent e) { delegate.mouseEntered(e); }
            public void mouseExited(MouseEvent e) { delegate.mouseExited(e); }

            public void mousePressed(MouseEvent e) {
                if (!tabPane.isEnabled()) {
                    return;
                }

                int tabIndex = tabForCoordinate(tabPane, e.getX(), e.getY());
                if (tabIndex >= 0 && tabPane.isEnabledAt(tabIndex)) {
                    if (tabIndex == tabPane.getSelectedIndex()) {
                        // Clicking on selected tab
                        selectedTabIsPressed = true;
                        //TODO need to just repaint the tab area!
                        tabPane.repaint();
                    }
                }

                //forward the event (this will set the selected index, or none at all
                delegate.mousePressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (selectedTabIsPressed) {
                    selectedTabIsPressed = false;
                    //TODO need to just repaint the tab area!
                    tabPane.repaint();
                }
                //forward the event
                delegate.mouseReleased(e);

                //hack: The super method *should* be setting the mouse-over property correctly
                //here, but it doesn't. That is, when the mouse is released, whatever tab is below the
                //released mouse should be in rollover state. But, if you select a tab and don't
                //move the mouse, this doesn't happen. Hence, forwarding the event.
                delegate2.mouseMoved(e);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getTabLabelShiftX(int tabPlacement, int tabIndex, boolean isSelected) {
        if (nudgeSelectedLabel) {
            return super.getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        } else {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected) {
        if (nudgeSelectedLabel) {
            return super.getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        } else {
            return 0;
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
        context.getPainter().paintTabbedPaneBackground(context,
                          g, 0, 0, c.getWidth(), c.getHeight());
        paint(context, g);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getBaseline(int tab) {
        if (tabPane.getTabComponentAt(tab) != null ||
                getTextViewForTab(tab) != null) {
            return super.getBaseline(tab);
        }
        String title = tabPane.getTitleAt(tab);
        Font font = tabContext.getStyle().getFont(tabContext);
        FontMetrics metrics = getFontMetrics(font);
        Icon icon = getIconForTab(tab);
        textRect.setBounds(0, 0, 0, 0);
        iconRect.setBounds(0, 0, 0, 0);
        calcRect.setBounds(0, 0, Short.MAX_VALUE, maxTabHeight);
        tabContext.getStyle().getGraphicsUtils(tabContext).layoutText(
                tabContext, metrics, title, icon, SwingUtilities.CENTER,
                SwingUtilities.CENTER, SwingUtilities.LEADING,
                SwingUtilities.CENTER, calcRect,
                iconRect, textRect, textIconGap);
        return textRect.y + metrics.getAscent() + getBaselineOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintTabbedPaneBorder(context, g, x, y, w, h);
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
        int selectedIndex = tabPane.getSelectedIndex();
        int tabPlacement = tabPane.getTabPlacement();

        ensureCurrentLayout();

        // Paint tab area
        // If scrollable tabs are enabled, the tab area will be
        // painted by the scrollable tab panel instead.
        //
        if (!scrollableTabLayoutEnabled()) { // WRAP_TAB_LAYOUT
            Insets insets = tabPane.getInsets();
            int x = insets.left;
            int y = insets.top;
            int width = tabPane.getWidth() - insets.left - insets.right;
            int height = tabPane.getHeight() - insets.top - insets.bottom;
            int size;
            switch(tabPlacement) {
            case LEFT:
                width = calculateTabAreaWidth(tabPlacement, runCount,
                                              maxTabWidth);
                break;
            case RIGHT:
                size = calculateTabAreaWidth(tabPlacement, runCount,
                                             maxTabWidth);
                x = x + width - size;
                width = size;
                break;
            case BOTTOM:
                size = calculateTabAreaHeight(tabPlacement, runCount,
                                              maxTabHeight);
                y = y + height - size;
                height = size;
                break;
            case TOP:
            default:
                height = calculateTabAreaHeight(tabPlacement, runCount,
                                                maxTabHeight);
            }

            tabAreaBounds.setBounds(x, y, width, height);

            if (g.getClipBounds().intersects(tabAreaBounds)) {
                paintTabArea(tabAreaContext, g, tabPlacement,
                         selectedIndex, tabAreaBounds);
            }
        }

        // Paint content border
        paintContentBorder(tabContentContext, g, tabPlacement, selectedIndex);
    }

    protected void paintTabArea(Graphics g, int tabPlacement,
                                int selectedIndex) {
        // This can be invoked from ScrollabeTabPanel
        Insets insets = tabPane.getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = tabPane.getWidth() - insets.left - insets.right;
        int height = tabPane.getHeight() - insets.top - insets.bottom;

        paintTabArea(tabAreaContext, g, tabPlacement, selectedIndex,
                     new Rectangle(x, y, width, height));
    }

    private void paintTabArea(SynthContext ss, Graphics g,
                                int tabPlacement, int selectedIndex,
                                Rectangle tabAreaBounds) {
        Rectangle clipRect = g.getClipBounds();

        //if the tab area's states should match that of the selected tab, then
        //first update the selected tab's states, then set the state
        //for the tab area to match
        //otherwise, restore the tab area's state to ENABLED (which is the
        //only supported state otherwise).
        if (tabAreaStatesMatchSelectedTab && selectedIndex >= 0) {
            updateTabContext(selectedIndex, true, selectedTabIsPressed,
                              (getRolloverTab() == selectedIndex),
                              (getFocusIndex() == selectedIndex));
            ss.setComponentState(tabContext.getComponentState());
        } else {
            ss.setComponentState(SynthConstants.ENABLED);
        }

        // Paint the tab area.
        SynthLookAndFeel.updateSubregion(ss, g, tabAreaBounds);
        ss.getPainter().paintTabbedPaneTabAreaBackground(ss, g,
             tabAreaBounds.x, tabAreaBounds.y, tabAreaBounds.width,
             tabAreaBounds.height, tabPlacement);
        ss.getPainter().paintTabbedPaneTabAreaBorder(ss, g, tabAreaBounds.x,
             tabAreaBounds.y, tabAreaBounds.width, tabAreaBounds.height,
             tabPlacement);

        int tabCount = tabPane.getTabCount();

        iconRect.setBounds(0, 0, 0, 0);
        textRect.setBounds(0, 0, 0, 0);

        // Paint tabRuns of tabs from back to front
        for (int i = runCount - 1; i >= 0; i--) {
            int start = tabRuns[i];
            int next = tabRuns[(i == runCount - 1)? 0 : i + 1];
            int end = (next != 0? next - 1: tabCount - 1);
            for (int j = start; j <= end; j++) {
                if (rects[j].intersects(clipRect) && selectedIndex != j) {
                    paintTab(tabContext, g, tabPlacement, rects, j, iconRect,
                             textRect);
                }
            }
        }

        if (selectedIndex >= 0) {
            if (rects[selectedIndex].intersects(clipRect)) {
                paintTab(tabContext, g, tabPlacement, rects, selectedIndex,
                         iconRect, textRect);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setRolloverTab(int index) {
        int oldRolloverTab = getRolloverTab();
        super.setRolloverTab(index);

        Rectangle r = null;

        if (oldRolloverTab != index && tabAreaStatesMatchSelectedTab) {
            //TODO need to just repaint the tab area!
            tabPane.repaint();
        } else {
            if ((oldRolloverTab >= 0) && (oldRolloverTab < tabPane.getTabCount())) {
                r = getTabBounds(tabPane, oldRolloverTab);
                if (r != null) {
                    tabPane.repaint(r);
                }
            }

            if (index >= 0) {
                r = getTabBounds(tabPane, index);
                if (r != null) {
                    tabPane.repaint(r);
                }
            }
        }
    }

    private void paintTab(SynthContext ss, Graphics g,
                            int tabPlacement, Rectangle[] rects, int tabIndex,
                            Rectangle iconRect, Rectangle textRect) {
        Rectangle tabRect = rects[tabIndex];
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == tabIndex;
        updateTabContext(tabIndex, isSelected, isSelected && selectedTabIsPressed,
                            (getRolloverTab() == tabIndex),
                            (getFocusIndex() == tabIndex));

        SynthLookAndFeel.updateSubregion(ss, g, tabRect);
        int x = tabRect.x;
        int y = tabRect.y;
        int height = tabRect.height;
        int width = tabRect.width;
        int placement = tabPane.getTabPlacement();
        if (extendTabsToBase && runCount > 1) {
            //paint this tab such that its edge closest to the base is equal to
            //edge of the selected tab closest to the base. In terms of the TOP
            //tab placement, this will cause the bottom of each tab to be
            //painted even with the bottom of the selected tab. This is because
            //in each tab placement (TOP, LEFT, BOTTOM, RIGHT) the selected tab
            //is closest to the base.
            if (selectedIndex >= 0) {
                Rectangle r = rects[selectedIndex];
                switch (placement) {
                    case TOP:
                        int bottomY = r.y + r.height;
                        height = bottomY - tabRect.y;
                        break;
                    case LEFT:
                        int rightX = r.x + r.width;
                        width = rightX - tabRect.x;
                        break;
                    case BOTTOM:
                        int topY = r.y;
                        height = (tabRect.y + tabRect.height) - topY;
                        y = topY;
                        break;
                    case RIGHT:
                        int leftX = r.x;
                        width = (tabRect.x + tabRect.width) - leftX;
                        x = leftX;
                        break;
                }
            }
        }
        tabContext.getPainter().paintTabbedPaneTabBackground(tabContext, g,
                x, y, width, height, tabIndex, placement);
        tabContext.getPainter().paintTabbedPaneTabBorder(tabContext, g,
                x, y, width, height, tabIndex, placement);

        if (tabPane.getTabComponentAt(tabIndex) == null) {
            String title = tabPane.getTitleAt(tabIndex);
            String clippedTitle = title;
            Font font = ss.getStyle().getFont(ss);
            FontMetrics metrics = SwingUtilities2.getFontMetrics(tabPane, g, font);
            Icon icon = getIconForTab(tabIndex);

            layoutLabel(ss, tabPlacement, metrics, tabIndex, title, icon,
                    tabRect, iconRect, textRect, isSelected);
            clippedTitle = SwingUtilities2.clipStringIfNecessary(null, metrics,
                           title, textRect.width);
            paintText(ss, g, tabPlacement, font, metrics,
                    tabIndex, clippedTitle, textRect, isSelected);

            paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        }
    }

    private void layoutLabel(SynthContext ss, int tabPlacement,
                               FontMetrics metrics, int tabIndex,
                               String title, Icon icon,
                               Rectangle tabRect, Rectangle iconRect,
                               Rectangle textRect, boolean isSelected ) {
        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            tabPane.putClientProperty("html", v);
        }

        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

        ss.getStyle().getGraphicsUtils(ss).layoutText(ss, metrics, title,
                         icon, SwingUtilities.CENTER, SwingUtilities.CENTER,
                         SwingUtilities.LEADING, SwingUtilities.CENTER,
                         tabRect, iconRect, textRect, textIconGap);

        tabPane.putClientProperty("html", null);

        int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
        int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;
    }

    private void paintText(SynthContext ss,
                             Graphics g, int tabPlacement,
                             Font font, FontMetrics metrics, int tabIndex,
                             String title, Rectangle textRect,
                             boolean isSelected) {
        g.setFont(font);

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
            // html
            v.paint(g, textRect);
        } else {
            // plain text
            int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);

            g.setColor(ss.getStyle().getColor(ss, ColorType.TEXT_FOREGROUND));
            ss.getStyle().getGraphicsUtils(ss).paintText(ss, g, title,
                                  textRect, mnemIndex);
        }
    }


    private void paintContentBorder(SynthContext ss, Graphics g,
                                      int tabPlacement, int selectedIndex) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        switch(tabPlacement) {
          case LEFT:
              x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              w -= (x - insets.left);
              break;
          case RIGHT:
              w -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
              break;
          case BOTTOM:
              h -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              break;
          case TOP:
          default:
              y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
              h -= (y - insets.top);
        }
        SynthLookAndFeel.updateSubregion(ss, g, new Rectangle(x, y, w, h));
        ss.getPainter().paintTabbedPaneContentBackground(ss, g, x, y,
                                                           w, h);
        ss.getPainter().paintTabbedPaneContentBorder(ss, g, x, y, w, h);
    }

    private void ensureCurrentLayout() {
        if (!tabPane.isValid()) {
            tabPane.validate();
        }
        /* If tabPane doesn't have a peer yet, the validate() call will
         * silently fail.  We handle that by forcing a layout if tabPane
         * is still invalid.  See bug 4237677.
         */
        if (!tabPane.isValid()) {
            TabbedPaneLayout layout = (TabbedPaneLayout)tabPane.getLayout();
            layout.calculateLayoutInfo();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int calculateMaxTabHeight(int tabPlacement) {
        FontMetrics metrics = getFontMetrics(tabContext.getStyle().getFont(
                                             tabContext));
        int tabCount = tabPane.getTabCount();
        int result = 0;
        int fontHeight = metrics.getHeight();
        for(int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabHeight(tabPlacement, i, fontHeight), result);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex,
                                    FontMetrics metrics) {
        Icon icon = getIconForTab(tabIndex);
        Insets tabInsets = getTabInsets(tabPlacement, tabIndex);
        int width = tabInsets.left + tabInsets.right;
        Component tabComponent = tabPane.getTabComponentAt(tabIndex);
        if (tabComponent != null) {
            width += tabComponent.getPreferredSize().width;
        } else {
            if (icon != null) {
                width += icon.getIconWidth() + textIconGap;
            }
            View v = getTextViewForTab(tabIndex);
            if (v != null) {
                // html
                width += (int) v.getPreferredSpan(View.X_AXIS);
            } else {
                // plain text
                String title = tabPane.getTitleAt(tabIndex);
                width += tabContext.getStyle().getGraphicsUtils(tabContext).
                        computeStringWidth(tabContext, metrics.getFont(),
                                metrics, title);
            }
        }
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int calculateMaxTabWidth(int tabPlacement) {
        FontMetrics metrics = getFontMetrics(tabContext.getStyle().getFont(
                                     tabContext));
        int tabCount = tabPane.getTabCount();
        int result = 0;
        for(int i = 0; i < tabCount; i++) {
            result = Math.max(calculateTabWidth(tabPlacement, i, metrics),
                              result);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Insets getTabInsets(int tabPlacement, int tabIndex) {
        updateTabContext(tabIndex, false, false, false,
                          (getFocusIndex() == tabIndex));
        return tabInsets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FontMetrics getFontMetrics() {
        return getFontMetrics(tabContext.getStyle().getFont(tabContext));
    }

    private FontMetrics getFontMetrics(Font font) {
        return tabPane.getFontMetrics(font);
    }

    private void updateTabContext(int index, boolean selected,
                                  boolean isMouseDown, boolean isMouseOver, boolean hasFocus) {
        int state = 0;
        if (!tabPane.isEnabled() || !tabPane.isEnabledAt(index)) {
            state |= SynthConstants.DISABLED;
            if (selected) {
                state |= SynthConstants.SELECTED;
            }
        }
        else if (selected) {
            state |= (SynthConstants.ENABLED | SynthConstants.SELECTED);
            if (isMouseOver && UIManager.getBoolean("TabbedPane.isTabRollover")) {
                state |= SynthConstants.MOUSE_OVER;
            }
        }
        else if (isMouseOver) {
            state |= (SynthConstants.ENABLED | SynthConstants.MOUSE_OVER);
        }
        else {
            state = SynthLookAndFeel.getComponentState(tabPane);
            state &= ~SynthConstants.FOCUSED; // don't use tabbedpane focus state
        }
        if (hasFocus && tabPane.hasFocus()) {
            state |= SynthConstants.FOCUSED; // individual tab has focus
        }
        if (isMouseDown) {
            state |= SynthConstants.PRESSED;
        }

        tabContext.setComponentState(state);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to create a TabbedPaneLayout subclass which takes into
     * account tabOverlap.
     */
    @Override
    protected LayoutManager createLayoutManager() {
        if (tabPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            return super.createLayoutManager();
        } else { /* WRAP_TAB_LAYOUT */
            return new TabbedPaneLayout() {
                @Override
                public void calculateLayoutInfo() {
                    super.calculateLayoutInfo();
                    //shift all the tabs, if necessary
                    if (tabOverlap != 0) {
                        int tabCount = tabPane.getTabCount();
                        //left-to-right/right-to-left only affects layout
                        //when placement is TOP or BOTTOM
                        boolean ltr = tabPane.getComponentOrientation().isLeftToRight();
                        for (int i = runCount - 1; i >= 0; i--) {
                            int start = tabRuns[i];
                            int next = tabRuns[(i == runCount - 1)? 0 : i + 1];
                            int end = (next != 0? next - 1: tabCount - 1);
                            for (int j = start+1; j <= end; j++) {
                                // xshift and yshift represent the amount &
                                // direction to shift the tab in their
                                // respective axis.
                                int xshift = 0;
                                int yshift = 0;
                                // configure xshift and y shift based on tab
                                // position and ltr/rtl
                                switch (tabPane.getTabPlacement()) {
                                    case JTabbedPane.TOP:
                                    case JTabbedPane.BOTTOM:
                                        xshift = ltr ? tabOverlap : -tabOverlap;
                                        break;
                                    case JTabbedPane.LEFT:
                                    case JTabbedPane.RIGHT:
                                        yshift = tabOverlap;
                                        break;
                                    default: //do nothing
                                }
                                rects[j].x += xshift;
                                rects[j].y += yshift;
                                rects[j].width += Math.abs(xshift);
                                rects[j].height += Math.abs(yshift);
                            }
                        }
                    }
                }
            };
        }
    }

    /**
     * A subclass of {@code SynthArrowButton} that implements
     * {@code UIResource}.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class SynthScrollableTabButton extends SynthArrowButton implements
            UIResource {
        public SynthScrollableTabButton(int direction) {
            super(direction);
            setName("TabbedPane.button");
        }
    }
}
