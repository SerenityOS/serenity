/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.AffineTransform;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.View;

import sun.swing.SwingUtilities2;
import apple.laf.*;
import apple.laf.JRSUIConstants.*;

public class AquaTabbedPaneUI extends AquaTabbedPaneCopyFromBasicUI {
    private static final int kSmallTabHeight = 20; // height of a small tab
    private static final int kLargeTabHeight = 23; // height of a large tab
    private static final int kMaxIconSize = kLargeTabHeight - 7;

    private static final double kNinetyDegrees = (Math.PI / 2.0); // used for rotation

    protected final Insets currentContentDrawingInsets = new Insets(0, 0, 0, 0);
    protected final Insets currentContentBorderInsets = new Insets(0, 0, 0, 0);
    protected final Insets contentDrawingInsets = new Insets(0, 0, 0, 0);

    protected int pressedTab = -3; // -2 is right scroller, -1 is left scroller
    protected boolean popupSelectionChanged;

    protected Boolean isDefaultFocusReceiver = null;
    protected boolean hasAvoidedFirstFocus = false;

    // Create PLAF
    public static ComponentUI createUI(final JComponent c) {
        return new AquaTabbedPaneUI();
    }

    protected final AquaTabbedPaneTabState visibleTabState = new AquaTabbedPaneTabState(this);
    protected final AquaPainter<JRSUIState> painter = AquaPainter.create(JRSUIStateFactory.getTab());

    public AquaTabbedPaneUI() { }

    protected void installListeners() {
        super.installListeners();

        // We're not just a mouseListener, we're a mouseMotionListener
        if (mouseListener != null) {
            tabPane.addMouseMotionListener((MouseMotionListener)mouseListener);
        }
    }

    protected void installDefaults() {
        super.installDefaults();

        if (tabPane.getFont() instanceof UIResource) {
            final Boolean b = (Boolean)UIManager.get("TabbedPane.useSmallLayout");
            if (b != null && b == Boolean.TRUE) {
                tabPane.setFont(UIManager.getFont("TabbedPane.smallFont"));
                painter.state.set(Size.SMALL);
            }
        }

        contentDrawingInsets.set(0, 11, 13, 10);
        tabPane.setOpaque(false);
    }

    protected void assureRectsCreated(final int tabCount) {
        visibleTabState.init(tabCount);
        super.assureRectsCreated(tabCount);
    }

    @Override
    protected void uninstallListeners() {
        // We're not just a mouseListener, we're a mouseMotionListener
        if (mouseListener instanceof  MouseHandler) {
            final MouseHandler mh = (MouseHandler) mouseListener;
            mh.dispose();
            tabPane.removeMouseMotionListener(mh);
        }
        super.uninstallListeners();
    }

    protected void uninstallDefaults() {
        contentDrawingInsets.set(0, 0, 0, 0);
    }

    protected MouseListener createMouseListener() {
        return new MouseHandler();
    }

    protected FocusListener createFocusListener() {
        return new FocusHandler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new TabbedPanePropertyChangeHandler();
    }

    protected LayoutManager createLayoutManager() {
        return new AquaTruncatingTabbedPaneLayout();
    }

    protected boolean shouldRepaintSelectedTabOnMouseDown() {
        return false;
    }

    // Paint Methods
    // Cache for performance
    final Rectangle fContentRect = new Rectangle();
    final Rectangle fIconRect = new Rectangle();
    final Rectangle fTextRect = new Rectangle();

    // UI Rendering
    public void paint(final Graphics g, final JComponent c) {
        painter.state.set(getDirection());

        final int tabPlacement = tabPane.getTabPlacement();
        final int selectedIndex = tabPane.getSelectedIndex();
        paintContentBorder(g, tabPlacement, selectedIndex);

        // we want to call ensureCurrentLayout, but it's private
        ensureCurrentLayout();
        final Rectangle clipRect = g.getClipBounds();

        final boolean active = tabPane.isEnabled();
        final boolean frameActive = AquaFocusHandler.isActive(tabPane);
        final boolean isLeftToRight = tabPane.getComponentOrientation().isLeftToRight() || tabPlacement == LEFT || tabPlacement == RIGHT;

        // Paint tabRuns of tabs from back to front
        if (visibleTabState.needsScrollTabs()) {
            paintScrollingTabs(g, clipRect, tabPlacement, selectedIndex, active, frameActive, isLeftToRight);
            return;
        }

        // old way
        paintAllTabs(g, clipRect, tabPlacement, selectedIndex, active, frameActive, isLeftToRight);
    }

    protected void paintAllTabs(final Graphics g, final Rectangle clipRect, final int tabPlacement, final int selectedIndex, final boolean active, final boolean frameActive, final boolean isLeftToRight) {
        boolean drawSelectedLast = false;
        for (int i = 0; i < rects.length; i++) {
            if (i == selectedIndex) {
                drawSelectedLast = true;
            } else {
                if (rects[i].intersects(clipRect)) {
                    paintTabNormal(g, tabPlacement, i, active, frameActive, isLeftToRight);
                }
            }
        }

        // paint the selected tab last.
        if (drawSelectedLast && rects[selectedIndex].intersects(clipRect)) {
            paintTabNormal(g, tabPlacement, selectedIndex, active, frameActive, isLeftToRight);
        }
    }

    protected void paintScrollingTabs(final Graphics g, final Rectangle clipRect, final int tabPlacement, final int selectedIndex, final boolean active, final boolean frameActive, final boolean isLeftToRight) {
//        final Graphics g2 = g.create();
//        g2.setColor(Color.cyan);
//        Rectangle r = new Rectangle();
//        for (int i = 0; i < visibleTabState.getTotal(); i++) {
//            r.add(rects[visibleTabState.getIndex(i)]);
//        }
//        g2.fillRect(r.x, r.y, r.width, r.height);
//        g2.dispose();
//        System.out.println(r);

        // for each visible tab, except the selected one
        for (int i = 0; i < visibleTabState.getTotal(); i++) {
            final int realIndex = visibleTabState.getIndex(i);
            if (realIndex != selectedIndex) {
                if (rects[realIndex].intersects(clipRect)) {
                    paintTabNormal(g, tabPlacement, realIndex, active, frameActive, isLeftToRight);
                }
            }
        }

        final Rectangle leftScrollTabRect = visibleTabState.getLeftScrollTabRect();
        if (visibleTabState.needsLeftScrollTab() && leftScrollTabRect.intersects(clipRect)) {
            paintTabNormalFromRect(g, tabPlacement, leftScrollTabRect, -2, fIconRect, fTextRect, visibleTabState.needsLeftScrollTab(), frameActive, isLeftToRight);
        }

        final Rectangle rightScrollTabRect = visibleTabState.getRightScrollTabRect();
        if (visibleTabState.needsRightScrollTab() && rightScrollTabRect.intersects(clipRect)) {
            paintTabNormalFromRect(g, tabPlacement, rightScrollTabRect, -1, fIconRect, fTextRect, visibleTabState.needsRightScrollTab(), frameActive, isLeftToRight);
        }

        if (selectedIndex >= 0) { // && rects[selectedIndex].intersects(clipRect)) {
            paintTabNormal(g, tabPlacement, selectedIndex, active, frameActive, isLeftToRight);
        }
    }

    private static boolean isScrollTabIndex(final int index) {
        return index == -1 || index == -2;
    }

    protected static void transposeRect(final Rectangle r) {
        int temp = r.y;
        r.y = r.x;
        r.x = temp;
        temp = r.width;
        r.width = r.height;
        r.height = temp;
    }

    protected int getTabLabelShiftX(final int tabPlacement, final int tabIndex, final boolean isSelected) {
        final Rectangle tabRect = (tabIndex >= 0 ? rects[tabIndex] : visibleTabState.getRightScrollTabRect());
        int nudge = 0;
        switch (tabPlacement) {
            case LEFT:
            case RIGHT:
                nudge = tabRect.height % 2;
                break;
            case BOTTOM:
            case TOP:
            default:
                nudge = tabRect.width % 2;
        }
        return nudge;
    }

    protected int getTabLabelShiftY(final int tabPlacement, final int tabIndex, final boolean isSelected) {
        switch (tabPlacement) {
            case RIGHT:
            case LEFT:
            case BOTTOM:
                return -1;
            case TOP:
            default:
                return 0;
        }
    }

    protected Icon getIconForScrollTab(final int tabPlacement, final int tabIndex, final boolean enabled) {
        boolean shouldFlip = !AquaUtils.isLeftToRight(tabPane);
        if (tabPlacement == RIGHT) shouldFlip = false;
        if (tabPlacement == LEFT) shouldFlip = true;

        int direction = tabIndex == -1 ? EAST : WEST;
        if (shouldFlip) {
            if (direction == EAST) {
                direction = WEST;
            } else if (direction == WEST) {
                direction = EAST;
            }
        }

        if (enabled) return AquaImageFactory.getArrowIconForDirection(direction);

        final Image icon = AquaImageFactory.getArrowImageForDirection(direction);
        return new ImageIcon(AquaUtils.generateDisabledImage(icon));
    }

    protected void paintContents(final Graphics g, final int tabPlacement, final int tabIndex, final Rectangle tabRect, final Rectangle iconRect, final Rectangle textRect, final boolean isSelected) {
        final Shape temp = g.getClip();
        g.clipRect(fContentRect.x, fContentRect.y, fContentRect.width, fContentRect.height);

        final Component component;
        final String title;
        final Icon icon;
        if (isScrollTabIndex(tabIndex)) {
            component = null;
            title = null;
            icon = getIconForScrollTab(tabPlacement, tabIndex, true);
        } else {
            component = getTabComponentAt(tabIndex);
            if (component == null) {
                title = tabPane.getTitleAt(tabIndex);
                icon = getIconForTab(tabIndex);
            } else {
                title = null;
                icon = null;
            }
        }

        final boolean isVertical = tabPlacement == RIGHT || tabPlacement == LEFT;
        if (isVertical) {
            transposeRect(fContentRect);
        }

        final Font font = tabPane.getFont();
        final FontMetrics metrics = g.getFontMetrics(font);

        // our scrolling tabs
        layoutLabel(tabPlacement, metrics, tabIndex < 0 ? 0 : tabIndex, title, icon, fContentRect, iconRect, textRect, false); // Never give it "isSelected" - ApprMgr handles this
        if (isVertical) {
            transposeRect(fContentRect);
            transposeRect(iconRect);
            transposeRect(textRect);
        }

        // from super.paintText - its normal text painting is totally wrong for the Mac
        if (!(g instanceof Graphics2D)) {
            g.setClip(temp);
            return;
        }
        final Graphics2D g2d = (Graphics2D) g;

        AffineTransform savedAT = null;
        if (isVertical) {
            savedAT = g2d.getTransform();
            rotateGraphics(g2d, tabRect, textRect, iconRect, tabPlacement);
        }

        // not for the scrolling tabs
        if (component == null && tabIndex >= 0) {
            String clippedTitle = SwingUtilities2.clipStringIfNecessary(tabPane, metrics,
                    title, textRect.width);
            paintTitle(g2d, font, metrics, textRect, tabIndex, clippedTitle);
        }

        if (icon != null) {
            paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
        }

        if (savedAT != null) {
            g2d.setTransform(savedAT);
        }

        g.setClip(temp);
    }

    protected void paintTitle(final Graphics2D g2d, final Font font, final FontMetrics metrics, final Rectangle textRect, final int tabIndex, final String title) {
        final View v = getTextViewForTab(tabIndex);
        if (v != null) {
            v.paint(g2d, textRect);
            return;
        }

        if (title == null) return;

        final Color color = tabPane.getForegroundAt(tabIndex);
        if (color instanceof UIResource) {
            // sja fix getTheme().setThemeTextColor(g, isSelected, isPressed && tracking, tabPane.isEnabledAt(tabIndex));
            if (tabPane.isEnabledAt(tabIndex)) {
                g2d.setColor(Color.black);
            } else {
                g2d.setColor(Color.gray);
            }
        } else {
            g2d.setColor(color);
        }

        g2d.setFont(font);
        SwingUtilities2.drawString(tabPane, g2d, title, textRect.x, textRect.y + metrics.getAscent());
    }

    protected void rotateGraphics(final Graphics2D g2d, final Rectangle tabRect, final Rectangle textRect, final Rectangle iconRect, final int tabPlacement) {
        int yDiff = 0; // textRect.y - tabRect.y;
        int xDiff = 0; // (tabRect.x+tabRect.width) - (textRect.x+textRect.width);
        int yIconDiff = 0; // iconRect.y - tabRect.y;
        int xIconDiff = 0; // (tabRect.x+tabRect.width) - (iconRect.x + iconRect.width);

        final double rotateAmount = (tabPlacement == LEFT ? -kNinetyDegrees : kNinetyDegrees);
        g2d.transform(AffineTransform.getRotateInstance(rotateAmount, tabRect.x, tabRect.y));

        // x and y diffs are named weirdly.
        // I will rename them, but what they mean now is
        // original x offset which will be used to adjust the y coordinate for the
        // rotated context
        if (tabPlacement == LEFT) {
            g2d.translate(-tabRect.height - 1, 1);
            xDiff = textRect.x - tabRect.x;
            yDiff = tabRect.height + tabRect.y - (textRect.y + textRect.height);
            xIconDiff = iconRect.x - tabRect.x;
            yIconDiff = tabRect.height + tabRect.y - (iconRect.y + iconRect.height);
        } else {
            g2d.translate(0, -tabRect.width - 1);
            yDiff = textRect.y - tabRect.y;
            xDiff = (tabRect.x + tabRect.width) - (textRect.x + textRect.width);
            yIconDiff = iconRect.y - tabRect.y;
            xIconDiff = (tabRect.x + tabRect.width) - (iconRect.x + iconRect.width);
        }

        // rotation changes needed for the rendering
        // we are rotating so we can't just use the rects wholesale.
        textRect.x = tabRect.x + yDiff;
        textRect.y = tabRect.y + xDiff;

        int tempVal = textRect.height;
        textRect.height = textRect.width;
        textRect.width = tempVal;
    // g.setColor(Color.red);
    // g.drawLine(textRect.x, textRect.y, textRect.x+textRect.height, textRect.y+textRect.width);
    // g.drawLine(textRect.x+textRect.height, textRect.y, textRect.x, textRect.y+textRect.width);

        iconRect.x = tabRect.x + yIconDiff;
        iconRect.y = tabRect.y + xIconDiff;

        tempVal = iconRect.height;
        iconRect.height = iconRect.width;
        iconRect.width = tempVal;
    }

    protected void paintTabNormal(final Graphics g, final int tabPlacement, final int tabIndex, final boolean active, final boolean frameActive, final boolean isLeftToRight) {
        paintTabNormalFromRect(g, tabPlacement, rects[tabIndex], tabIndex, fIconRect, fTextRect, active, frameActive, isLeftToRight);
    }

    protected void paintTabNormalFromRect(final Graphics g,
                                          final int tabPlacement,
                                          final Rectangle tabRect,
                                          final int nonRectIndex,
                                          final Rectangle iconRect,
                                          final Rectangle textRect,
                                          final boolean active,
                                          final boolean frameActive,
                                          final boolean isLeftToRight) {
        final int selectedIndex = tabPane.getSelectedIndex();
        final boolean isSelected = selectedIndex == nonRectIndex;

        paintCUITab(g, tabPlacement, tabRect, isSelected, frameActive, isLeftToRight, nonRectIndex);

        textRect.setBounds(tabRect);
        fContentRect.setBounds(tabRect);
        paintContents(g, tabPlacement, nonRectIndex, tabRect, iconRect, textRect, isSelected);
    }

    protected void paintCUITab(final Graphics g, final int tabPlacement,
                               final Rectangle tabRect,
                               final boolean isSelected,
                               final boolean frameActive,
                               final boolean isLeftToRight,
                               final int nonRectIndex) {
        final int tabCount = tabPane.getTabCount();

        final boolean needsLeftScrollTab = visibleTabState.needsLeftScrollTab();
        final boolean needsRightScrollTab = visibleTabState.needsRightScrollTab();

        // first or last
        boolean first = nonRectIndex == 0;
        boolean last = nonRectIndex == tabCount - 1;
        if (needsLeftScrollTab || needsRightScrollTab) {
            if (nonRectIndex == -1) {
                first = false;
                last = true;
            } else if (nonRectIndex == -2) {
                first = true;
                last = false;
            } else {
                if (needsLeftScrollTab) first = false;
                if (needsRightScrollTab) last = false;
            }
        }

        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            final boolean tempSwap = last;
            last = first;
            first = tempSwap;
        }

        final State state = getState(nonRectIndex, frameActive, isSelected);
        painter.state.set(state);
        painter.state.set(isSelected || (state == State.INACTIVE && frameActive) ? BooleanValue.YES : BooleanValue.NO);
        painter.state.set(getSegmentPosition(first, last, isLeftToRight));
        final int selectedIndex = tabPane.getSelectedIndex();
        painter.state.set(getSegmentTrailingSeparator(nonRectIndex, selectedIndex, isLeftToRight));
        painter.state.set(getSegmentLeadingSeparator(nonRectIndex, selectedIndex, isLeftToRight));
        painter.state.set(tabPane.hasFocus() && isSelected ? Focused.YES : Focused.NO);
        painter.paint(g, tabPane, tabRect.x, tabRect.y, tabRect.width, tabRect.height);

        if (isScrollTabIndex(nonRectIndex)) return;

        final Color color = tabPane.getBackgroundAt(nonRectIndex);
        if (color == null || (color instanceof UIResource)) return;

        if (!isLeftToRight && (tabPlacement == TOP || tabPlacement == BOTTOM)) {
            final boolean tempSwap = last;
            last = first;
            first = tempSwap;
        }

        fillTabWithBackground(g, tabRect, tabPlacement, first, last, color);
    }

    protected Direction getDirection() {
        switch (tabPane.getTabPlacement()) {
            case SwingConstants.BOTTOM: return Direction.SOUTH;
            case SwingConstants.LEFT: return Direction.WEST;
            case SwingConstants.RIGHT: return Direction.EAST;
        }
        return Direction.NORTH;
    }

    protected static SegmentPosition getSegmentPosition(final boolean first, final boolean last, final boolean isLeftToRight) {
        if (first && last) return SegmentPosition.ONLY;
        if (first) return isLeftToRight ? SegmentPosition.FIRST : SegmentPosition.LAST;
        if (last) return isLeftToRight ? SegmentPosition.LAST : SegmentPosition.FIRST;
        return SegmentPosition.MIDDLE;
    }

    protected SegmentTrailingSeparator getSegmentTrailingSeparator(final int index, final int selectedIndex, final boolean isLeftToRight) {
        return SegmentTrailingSeparator.YES;
    }

    protected SegmentLeadingSeparator getSegmentLeadingSeparator(final int index, final int selectedIndex, final boolean isLeftToRight) {
        return SegmentLeadingSeparator.NO;
    }

    protected boolean isTabBeforeSelectedTab(final int index, final int selectedIndex, final boolean isLeftToRight) {
        if (index == -2 && visibleTabState.getIndex(0) == selectedIndex) return true;
        int indexBeforeSelectedIndex = isLeftToRight ? selectedIndex - 1 : selectedIndex + 1;
        return index == indexBeforeSelectedIndex ? true : false;
    }

    protected State getState(final int index, final boolean frameActive, final boolean isSelected) {
        if (!frameActive) return State.INACTIVE;
        if (!tabPane.isEnabled()) return State.DISABLED;
        if (JRSUIUtils.TabbedPane.useLegacyTabs()) {
            if (isSelected) return State.PRESSED;
            if (pressedTab == index) return State.INACTIVE;
        } else {
            if (isSelected) return State.ACTIVE;
            if (pressedTab == index) return State.PRESSED;
        }
        return State.ACTIVE;
    }

    /**
     * This routine adjusts the background fill rect so it just fits inside a tab, allowing for
     * whether we're talking about a first tab or last tab.  NOTE that this code is very much
     * Aqua 2 dependent!
     */
    static class AlterRects {
        Rectangle standard, first, last;
        AlterRects(final int x, final int y, final int w, final int h) { standard = new Rectangle(x, y, w, h); }
        AlterRects start(final int x, final int y, final int w, final int h) { first = new Rectangle(x, y, w, h); return this; }
        AlterRects end(final int x, final int y, final int w, final int h) { last = new Rectangle(x, y, w, h); return this; }

        static Rectangle alter(final Rectangle r, final Rectangle o) {
            // r = new Rectangle(r);
            r.x += o.x;
            r.y += o.y;
            r.width += o.width;
            r.height += o.height;
            return r;
        }
    }

    static AlterRects[] alterRects = new AlterRects[5];

    protected static AlterRects getAlterationFor(final int tabPlacement) {
        if (alterRects[tabPlacement] != null) return alterRects[tabPlacement];

        switch (tabPlacement) {
            case LEFT: return alterRects[LEFT] = new AlterRects(2, 0, -4, 1).start(0, 0, 0, -4).end(0, 3, 0, -3);
            case RIGHT: return alterRects[RIGHT] = new AlterRects(1, 0, -4, 1).start(0, 0, 0, -4).end(0, 3, 0, -3);
            case BOTTOM: return alterRects[BOTTOM] = new AlterRects(0, 1, 0, -4).start(3, 0, -3, 0).end(0, 0, -3, 0);
            case TOP:
            default: return alterRects[TOP] = new AlterRects(0, 2, 0, -4).start(3, 0, -3, 0).end(0, 0, -3, 0);
        }
    }

    protected void fillTabWithBackground(final Graphics g, final Rectangle rect, final int tabPlacement, final boolean first, final boolean last, final Color color) {
        final Rectangle fillRect = new Rectangle(rect);

        final AlterRects alteration = getAlterationFor(tabPlacement);
        AlterRects.alter(fillRect, alteration.standard);
        if (first) AlterRects.alter(fillRect, alteration.first);
        if (last) AlterRects.alter(fillRect, alteration.last);

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * 0.25)));
        g.fillRoundRect(fillRect.x, fillRect.y, fillRect.width, fillRect.height, 3, 1);
    }

    protected Insets getContentBorderInsets(final int tabPlacement) {
        final Insets draw = getContentDrawingInsets(tabPlacement); // will be rotated

        rotateInsets(contentBorderInsets, currentContentBorderInsets, tabPlacement);

        currentContentBorderInsets.left += draw.left;
        currentContentBorderInsets.right += draw.right;
        currentContentBorderInsets.top += draw.top;
        currentContentBorderInsets.bottom += draw.bottom;

        return currentContentBorderInsets;
    }

    protected static void rotateInsets(final Insets topInsets, final Insets targetInsets, final int targetPlacement) {
        switch (targetPlacement) {
            case LEFT:
                targetInsets.top = topInsets.left;
                targetInsets.left = topInsets.top;
                targetInsets.bottom = topInsets.right;
                targetInsets.right = topInsets.bottom;
                break;
            case BOTTOM:
                targetInsets.top = topInsets.bottom;
                targetInsets.left = topInsets.left;
                targetInsets.bottom = topInsets.top;
                targetInsets.right = topInsets.right;
                break;
            case RIGHT:
                targetInsets.top = topInsets.right;
                targetInsets.left = topInsets.bottom;
                targetInsets.bottom = topInsets.left;
                targetInsets.right = topInsets.top;
                break;
            case TOP:
            default:
                targetInsets.top = topInsets.top;
                targetInsets.left = topInsets.left;
                targetInsets.bottom = topInsets.bottom;
                targetInsets.right = topInsets.right;
        }
    }

    protected Insets getContentDrawingInsets(final int tabPlacement) {
        rotateInsets(contentDrawingInsets, currentContentDrawingInsets, tabPlacement);
        return currentContentDrawingInsets;
    }

    protected Icon getIconForTab(final int tabIndex) {
        final Icon mainIcon = super.getIconForTab(tabIndex);
        if (mainIcon == null) return null;

        final int iconHeight = mainIcon.getIconHeight();
        if (iconHeight <= kMaxIconSize) return mainIcon;
        final float ratio = (float)kMaxIconSize / (float)iconHeight;

        final int iconWidth = mainIcon.getIconWidth();
        return new AquaIcon.CachingScalingIcon((int)(iconWidth * ratio), kMaxIconSize) {
            Image createImage() {
                return AquaIcon.getImageForIcon(mainIcon);
            }
        };
    }

    private static final int TAB_BORDER_INSET = 9;
    protected void paintContentBorder(final Graphics g, final int tabPlacement, final int selectedIndex) {
        final int width = tabPane.getWidth();
        final int height = tabPane.getHeight();
        final Insets insets = tabPane.getInsets();

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        switch (tabPlacement) {
            case TOP:
                y += TAB_BORDER_INSET;
                h -= TAB_BORDER_INSET;
                break;
            case BOTTOM:
                h -= TAB_BORDER_INSET;// - 2;
                break;
            case LEFT:
                x += TAB_BORDER_INSET;// - 5;
                w -= TAB_BORDER_INSET;// + 1;
                break;
            case RIGHT:
                w -= TAB_BORDER_INSET;// + 1;
                break;
        }

        if (tabPane.isOpaque()) {
            g.setColor(tabPane.getBackground());
            g.fillRect(0, 0, width, height);
        }

        AquaGroupBorder.getTabbedPaneGroupBorder().paintBorder(tabPane, g, x, y, w, h);
    }

    // see paintContentBorder
    protected void repaintContentBorderEdge() {
        final int width = tabPane.getWidth();
        final int height = tabPane.getHeight();
        final Insets insets = tabPane.getInsets();
        final int tabPlacement = tabPane.getTabPlacement();
        final Insets localContentBorderInsets = getContentBorderInsets(tabPlacement);

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        switch (tabPlacement) {
            case LEFT:
                x += calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                w = localContentBorderInsets.left;
                break;
            case RIGHT:
                w = localContentBorderInsets.right;
                break;
            case BOTTOM:
                h = localContentBorderInsets.bottom;
                break;
            case TOP:
            default:
                y += calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                h = localContentBorderInsets.top;
        }
        tabPane.repaint(x, y, w, h);
    }

    public boolean isTabVisible(final int index) {
        if (index == -1 || index == -2) return true;
        for (int i = 0; i < visibleTabState.getTotal(); i++) {
            if (visibleTabState.getIndex(i) == index) return true;
        }
        return false;
    }

    /**
     * Returns the bounds of the specified tab index.  The bounds are
     * with respect to the JTabbedPane's coordinate space.  If the tab at this
     * index is not currently visible in the UI, then returns null.
     */
    @Override
    public Rectangle getTabBounds(final JTabbedPane pane, final int i) {
        if (visibleTabState.needsScrollTabs()
                && (visibleTabState.isBefore(i) || visibleTabState.isAfter(i))) {
            return null;
        }
        return super.getTabBounds(pane, i);
    }

    /**
     * Returns the tab index which intersects the specified point
     * in the JTabbedPane's coordinate space.
     */
    public int tabForCoordinate(final JTabbedPane pane, final int x, final int y) {
        ensureCurrentLayout();
        final Point p = new Point(x, y);
        if (visibleTabState.needsScrollTabs()) {
            for (int i = 0; i < visibleTabState.getTotal(); i++) {
                final int realOffset = visibleTabState.getIndex(i);
                if (rects[realOffset].contains(p.x, p.y)) return realOffset;
            }
            if (visibleTabState.getRightScrollTabRect().contains(p.x, p.y)) return -1; //tabPane.getTabCount();
        } else {
            //old way
            final int tabCount = tabPane.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                if (rects[i].contains(p.x, p.y)) return i;
            }
        }
        return -1;
    }

    protected Insets getTabInsets(final int tabPlacement, final int tabIndex) {
        switch (tabPlacement) {
            case LEFT: return UIManager.getInsets("TabbedPane.leftTabInsets");
            case RIGHT: return UIManager.getInsets("TabbedPane.rightTabInsets");
        }
        return tabInsets;
    }

    // This is the preferred size - the layout manager will ignore if it has to
    protected int calculateTabHeight(final int tabPlacement, final int tabIndex, final int fontHeight) {
        // Constrain to what the Mac allows
        final int result = super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);

        // force tabs to have a max height for aqua
        if (result <= kSmallTabHeight) return kSmallTabHeight;
        return kLargeTabHeight;
    }

    // JBuilder requested this - it's against HI, but then so are multiple rows
    protected boolean shouldRotateTabRuns(final int tabPlacement) {
        return false;
    }

    protected class TabbedPanePropertyChangeHandler extends PropertyChangeHandler {
        public void propertyChange(final PropertyChangeEvent e) {
            final String prop = e.getPropertyName();

            if (!AquaFocusHandler.FRAME_ACTIVE_PROPERTY.equals(prop)) {
                super.propertyChange(e);
                return;
            }

            final JTabbedPane comp = (JTabbedPane)e.getSource();
            comp.repaint();

            // Repaint the "front" tab and the border
            final int selected = tabPane.getSelectedIndex();
            final Rectangle[] theRects = rects;
            if (selected >= 0 && selected < theRects.length) comp.repaint(theRects[selected]);
            repaintContentBorderEdge();
        }
    }

    protected ChangeListener createChangeListener() {
        return new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (!isTabVisible(tabPane.getSelectedIndex())) popupSelectionChanged = true;
                tabPane.revalidate();
                tabPane.repaint();
            }
        };
    }

    protected class FocusHandler extends FocusAdapter {
        Rectangle sWorkingRect = new Rectangle();

        public void focusGained(final FocusEvent e) {
            if (isDefaultFocusReceiver(tabPane) && !hasAvoidedFirstFocus) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
                hasAvoidedFirstFocus = true;
            }
            adjustPaintingRectForFocusRing(e);
        }

        public void focusLost(final FocusEvent e) {
            adjustPaintingRectForFocusRing(e);
        }

        void adjustPaintingRectForFocusRing(final FocusEvent e) {
            final JTabbedPane pane = (JTabbedPane)e.getSource();
            final int tabCount = pane.getTabCount();
            final int selectedIndex = pane.getSelectedIndex();

            if (selectedIndex != -1 && tabCount > 0 && tabCount == rects.length) {
                sWorkingRect.setBounds(rects[selectedIndex]);
                sWorkingRect.grow(4, 4);
                pane.repaint(sWorkingRect);
            }
        }

        boolean isDefaultFocusReceiver(final JComponent component) {
            if (isDefaultFocusReceiver == null) {
                Component defaultFocusReceiver = KeyboardFocusManager.getCurrentKeyboardFocusManager().getDefaultFocusTraversalPolicy().getDefaultComponent(getTopLevelFocusCycleRootAncestor(component));
                isDefaultFocusReceiver = defaultFocusReceiver != null && defaultFocusReceiver.equals(component);
            }
            return isDefaultFocusReceiver.booleanValue();
        }

        Container getTopLevelFocusCycleRootAncestor(Container container) {
            Container ancestor;
            while ((ancestor = container.getFocusCycleRootAncestor()) != null) {
                container = ancestor;
            }
            return container;
        }
    }

    class MouseHandler extends MouseInputAdapter implements ActionListener {

        int trackingTab = -3;
        private final Timer popupTimer = new Timer(500, this);

        MouseHandler() {
            popupTimer.setRepeats(false);
        }

        void dispose (){
            popupTimer.removeActionListener(this);
            popupTimer.stop();
        }

        public void mousePressed(final MouseEvent e) {
            final JTabbedPane pane = (JTabbedPane)e.getSource();
            if (!pane.isEnabled()) {
                trackingTab = -3;
                return;
            }

            final Point p = e.getPoint();
            trackingTab = getCurrentTab(pane, p);
            if (trackingTab == -3 || (!shouldRepaintSelectedTabOnMouseDown() && trackingTab == pane.getSelectedIndex())) {
                trackingTab = -3;
                return;
            }

            if (trackingTab < 0 && trackingTab > -3) {
                popupTimer.start();
            }

            pressedTab = trackingTab;
            repaint(pane, pressedTab);
        }

        public void mouseDragged(final MouseEvent e) {
            if (trackingTab < -2) return;

            final JTabbedPane pane = (JTabbedPane)e.getSource();
            final int currentTab = getCurrentTab(pane, e.getPoint());

            if (currentTab != trackingTab) {
                pressedTab = -3;
            } else {
                pressedTab = trackingTab;
            }

            if (trackingTab < 0 && trackingTab > -3) {
                popupTimer.start();
            }

            repaint(pane, trackingTab);
        }

        public void mouseReleased(final MouseEvent e) {
            if (trackingTab < -2) return;

            popupTimer.stop();

            final JTabbedPane pane = (JTabbedPane)e.getSource();
            final Point p = e.getPoint();
            final int currentTab = getCurrentTab(pane, p);

            if (trackingTab == -1 && currentTab == -1) {
                pane.setSelectedIndex(pane.getSelectedIndex() + 1);
            }

            if (trackingTab == -2 && currentTab == -2) {
                pane.setSelectedIndex(pane.getSelectedIndex() - 1);
            }

            if (trackingTab >= 0 && currentTab == trackingTab) {
                pane.setSelectedIndex(trackingTab);
            }

            repaint(pane, trackingTab);

            pressedTab = -3;
            trackingTab = -3;
        }

        public void actionPerformed(final ActionEvent e) {
            if (trackingTab != pressedTab) {
                return;
            }

            if (trackingTab == -1) {
                showFullPopup(false);
                trackingTab = -3;
            }

            if (trackingTab == -2) {
                showFullPopup(true);
                trackingTab = -3;
            }
        }

        int getCurrentTab(final JTabbedPane pane, final Point p) {
            final int tabIndex = tabForCoordinate(pane, p.x, p.y);
            if (tabIndex >= 0 && pane.isEnabledAt(tabIndex)) return tabIndex;

            if (visibleTabState.needsLeftScrollTab() && visibleTabState.getLeftScrollTabRect().contains(p)) return -2;
            if (visibleTabState.needsRightScrollTab() && visibleTabState.getRightScrollTabRect().contains(p)) return -1;

            return -3;
        }

        void repaint(final JTabbedPane pane, final int tab) {
            switch (tab) {
                case -1:
                    pane.repaint(visibleTabState.getRightScrollTabRect());
                    return;
                case -2:
                    pane.repaint(visibleTabState.getLeftScrollTabRect());
                    return;
                default:
                    if (trackingTab >= 0) pane.repaint(rects[trackingTab]);
                    return;
            }
        }

        void showFullPopup(final boolean firstTab) {
            final JPopupMenu popup = new JPopupMenu();

            for (int i = 0; i < tabPane.getTabCount(); i++) {
                if (firstTab ? visibleTabState.isBefore(i) : visibleTabState.isAfter(i)) {
                    popup.add(createMenuItem(i));
                }
            }

            if (firstTab) {
                final Rectangle leftScrollTabRect = visibleTabState.getLeftScrollTabRect();
                final Dimension popupRect = popup.getPreferredSize();
                popup.show(tabPane, leftScrollTabRect.x - popupRect.width, leftScrollTabRect.y + 7);
            } else {
                final Rectangle rightScrollTabRect = visibleTabState.getRightScrollTabRect();
                popup.show(tabPane, rightScrollTabRect.x + rightScrollTabRect.width, rightScrollTabRect.y + 7);
            }

            popup.addPopupMenuListener(new PopupMenuListener() {
                public void popupMenuCanceled(final PopupMenuEvent e) { }
                public void popupMenuWillBecomeVisible(final PopupMenuEvent e) { }

                public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                    pressedTab = -3;
                    tabPane.repaint(visibleTabState.getLeftScrollTabRect());
                    tabPane.repaint(visibleTabState.getRightScrollTabRect());
                }
            });
        }

        JMenuItem createMenuItem(final int i) {
            final Component component = getTabComponentAt(i);
            final JMenuItem menuItem;
            if (component == null) {
                menuItem = new JMenuItem(tabPane.getTitleAt(i), tabPane.getIconAt(i));
            } else {
                @SuppressWarnings("serial") // anonymous class
                JMenuItem tmp = new JMenuItem() {
                    public void paintComponent(final Graphics g) {
                        super.paintComponent(g);
                        final Dimension size = component.getSize();
                        component.setSize(getSize());
                        component.validate();
                        component.paint(g);
                        component.setSize(size);
                    }

                    public Dimension getPreferredSize() {
                        return component.getPreferredSize();
                    }
                };
                menuItem = tmp;
            }

            final Color background = tabPane.getBackgroundAt(i);
            if (!(background instanceof UIResource)) {
                menuItem.setBackground(background);
            }

            menuItem.setForeground(tabPane.getForegroundAt(i));
            // for <rdar://problem/3520267> make sure to disable items that are disabled in the tab.
            if (!tabPane.isEnabledAt(i)) menuItem.setEnabled(false);

            final int fOffset = i;
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent ae) {
                    boolean visible = isTabVisible(fOffset);
                    tabPane.setSelectedIndex(fOffset);
                    if (!visible) {
                        popupSelectionChanged = true;
                        tabPane.invalidate();
                        tabPane.repaint();
                    }
                }
            });

            return menuItem;
        }
    }

    protected class AquaTruncatingTabbedPaneLayout extends AquaTabbedPaneCopyFromBasicUI.TabbedPaneLayout {
        // fix for Radar #3346131
        protected int preferredTabAreaWidth(final int tabPlacement, final int height) {
            // Our superclass wants to stack tabs, but we rotate them,
            // so when tabs are on the left or right we know that
            // our width is actually the "height" of a tab which is then
            // rotated.
            if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
                return super.preferredTabAreaHeight(tabPlacement, height);
            }

            return super.preferredTabAreaWidth(tabPlacement, height);
        }

        protected int preferredTabAreaHeight(final int tabPlacement, final int width) {
            if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
                return super.preferredTabAreaWidth(tabPlacement, width);
            }

            return super.preferredTabAreaHeight(tabPlacement, width);
        }

        protected void calculateTabRects(final int tabPlacement, final int tabCount) {
            if (tabCount <= 0) return;

            superCalculateTabRects(tabPlacement, tabCount); // does most of the hard work

            // If they haven't been padded (which they only do when there are multiple rows) we should center them
            if (rects.length <= 0) return;

            visibleTabState.alignRectsRunFor(rects, tabPane.getSize(), tabPlacement, AquaUtils.isLeftToRight(tabPane));
        }

        protected void padTabRun(final int tabPlacement, final int start, final int end, final int max) {
            if (tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM) {
                super.padTabRun(tabPlacement, start, end, max);
                return;
            }

            final Rectangle lastRect = rects[end];
            final int runHeight = (lastRect.y + lastRect.height) - rects[start].y;
            final int deltaHeight = max - (lastRect.y + lastRect.height);
            final float factor = (float)deltaHeight / (float)runHeight;
            for (int i = start; i <= end; i++) {
                final Rectangle pastRect = rects[i];
                if (i > start) {
                    pastRect.y = rects[i - 1].y + rects[i - 1].height;
                }
                pastRect.height += Math.round(pastRect.height * factor);
            }
            lastRect.height = max - lastRect.y;
        }

        /**
         * This is a massive routine and I left it like this because the bulk of the code comes
         * from the BasicTabbedPaneUI class. Here is what it does:
         * 1. Calculate rects for the tabs - we have to play tricks here because our right and left tabs
         *    should get widths calculated the same way as top and bottom, but they will be rotated so the
         *    calculated width is stored as the rect height.
         * 2. Decide if we can fit all the tabs.
         * 3. When we cannot fit all the tabs we create a tab popup, and then layout the new tabs until
         *    we can't fit them anymore. Laying them out is a matter of adding them into the visible list
         *    and shifting them horizontally to the correct location.
         */
        protected synchronized void superCalculateTabRects(final int tabPlacement, final int tabCount) {
            final Dimension size = tabPane.getSize();
            final Insets insets = tabPane.getInsets();
            final Insets localTabAreaInsets = getTabAreaInsets(tabPlacement);

            // Calculate bounds within which a tab run must fit
            final int returnAt;
            final int x, y;
            switch (tabPlacement) {
                case SwingConstants.LEFT:
                    maxTabWidth = calculateMaxTabHeight(tabPlacement);
                    x = insets.left + localTabAreaInsets.left;
                    y = insets.top + localTabAreaInsets.top;
                    returnAt = size.height - (insets.bottom + localTabAreaInsets.bottom);
                    break;
                case SwingConstants.RIGHT:
                    maxTabWidth = calculateMaxTabHeight(tabPlacement);
                    x = size.width - insets.right - localTabAreaInsets.right - maxTabWidth - 1;
                    y = insets.top + localTabAreaInsets.top;
                    returnAt = size.height - (insets.bottom + localTabAreaInsets.bottom);
                    break;
                case SwingConstants.BOTTOM:
                    maxTabHeight = calculateMaxTabHeight(tabPlacement);
                    x = insets.left + localTabAreaInsets.left;
                    y = size.height - insets.bottom - localTabAreaInsets.bottom - maxTabHeight;
                    returnAt = size.width - (insets.right + localTabAreaInsets.right);
                    break;
                case SwingConstants.TOP:
                default:
                    maxTabHeight = calculateMaxTabHeight(tabPlacement);
                    x = insets.left + localTabAreaInsets.left;
                    y = insets.top + localTabAreaInsets.top;
                    returnAt = size.width - (insets.right + localTabAreaInsets.right);
                    break;
            }

            tabRunOverlay = getTabRunOverlay(tabPlacement);

            runCount = 0;
            selectedRun = 0;

            if (tabCount == 0) return;

            final FontMetrics metrics = getFontMetrics();
            final boolean verticalTabRuns = (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT);
            final int selectedIndex = tabPane.getSelectedIndex();

            // calculate all the widths
            // if they all fit we are done, if not
            // we have to do the dance of figuring out which ones to show.
            visibleTabState.setNeedsScrollers(false);
            for (int i = 0; i < tabCount; i++) {
                final Rectangle rect = rects[i];

                if (verticalTabRuns) {
                    calculateVerticalTabRunRect(rect, metrics, tabPlacement, returnAt, i, x, y);

                    // test if we need to scroll!
                    if (rect.y + rect.height > returnAt) {
                        visibleTabState.setNeedsScrollers(true);
                    }
                } else {
                    calculateHorizontalTabRunRect(rect, metrics, tabPlacement, returnAt, i, x, y);

                    // test if we need to scroll!
                    if (rect.x + rect.width > returnAt) {
                        visibleTabState.setNeedsScrollers(true);
                    }
                }
            }

            visibleTabState.relayoutForScrolling(rects, x, y, returnAt, selectedIndex, verticalTabRuns, tabCount, AquaUtils.isLeftToRight(tabPane));
            // Pad the selected tab so that it appears raised in front

            // if right to left and tab placement on the top or
            // the bottom, flip x positions and adjust by widths
            if (!AquaUtils.isLeftToRight(tabPane) && !verticalTabRuns) {
                final int rightMargin = size.width - (insets.right + localTabAreaInsets.right);
                for (int i = 0; i < tabCount; i++) {
                    rects[i].x = rightMargin - rects[i].x - rects[i].width;
                }
            }
        }

        private void calculateHorizontalTabRunRect(final Rectangle rect, final FontMetrics metrics, final int tabPlacement, final int returnAt, final int i, final int x, final int y) {
            // Tabs on TOP or BOTTOM....
            if (i > 0) {
                rect.x = rects[i - 1].x + rects[i - 1].width;
            } else {
                tabRuns[0] = 0;
                runCount = 1;
                maxTabWidth = 0;
                rect.x = x;
            }

            rect.width = calculateTabWidth(tabPlacement, i, metrics);
            maxTabWidth = Math.max(maxTabWidth, rect.width);

            rect.y = y;
            rect.height = maxTabHeight;
        }

        private void calculateVerticalTabRunRect(final Rectangle rect, final FontMetrics metrics, final int tabPlacement, final int returnAt, final int i, final int x, final int y) {
            // Tabs on LEFT or RIGHT...
            if (i > 0) {
                rect.y = rects[i - 1].y + rects[i - 1].height;
            } else {
                tabRuns[0] = 0;
                runCount = 1;
                maxTabHeight = 0;
                rect.y = y;
            }

            rect.height = calculateTabWidth(tabPlacement, i, metrics);
            maxTabHeight = Math.max(maxTabHeight, rect.height);

            rect.x = x;
            rect.width = maxTabWidth;
        }

        protected void layoutTabComponents() {
            final Container tabContainer = getTabContainer();
            if (tabContainer == null) return;

            final int placement = tabPane.getTabPlacement();
            final Rectangle rect = new Rectangle();
            final Point delta = new Point(-tabContainer.getX(), -tabContainer.getY());

            for (int i = 0; i < tabPane.getTabCount(); i++) {
                final Component c = getTabComponentAt(i);
                if (c == null) continue;

                getTabBounds(i, rect);
                final Insets insets = getTabInsets(tabPane.getTabPlacement(), i);
                final boolean isSeleceted = i == tabPane.getSelectedIndex();

                if (placement == SwingConstants.TOP || placement == SwingConstants.BOTTOM) {
                    rect.x += insets.left + delta.x + getTabLabelShiftX(placement, i, isSeleceted);
                    rect.y += insets.top + delta.y + getTabLabelShiftY(placement, i, isSeleceted) + 1;
                    rect.width -= insets.left + insets.right;
                    rect.height -= insets.top + insets.bottom - 1;
                } else {
                    rect.x += insets.top + delta.x + getTabLabelShiftY(placement, i, isSeleceted) + (placement == SwingConstants.LEFT ? 2 : 1);
                    rect.y += insets.left + delta.y + getTabLabelShiftX(placement, i, isSeleceted);
                    rect.width -= insets.top + insets.bottom - 1;
                    rect.height -= insets.left + insets.right;
                }

                c.setBounds(rect);
            }
        }
    }
}
