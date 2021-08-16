/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.beans.PropertyVetoException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

import sun.swing.SwingUtilities2;

import apple.laf.*;
import apple.laf.JRSUIConstants.*;
import apple.laf.JRSUIState.TitleBarHeightState;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaInternalFrameBorderMetrics;
import java.awt.geom.AffineTransform;

public class AquaInternalFrameBorder implements Border, UIResource {
    private static final int kCloseButton = 0;
    private static final int kIconButton = 1;
    private static final int kGrowButton = 2;

    private static final int sMaxIconWidth = 15;
    private static final int sMaxIconHeight = sMaxIconWidth;
    private static final int sAfterButtonPad = 11;
    private static final int sAfterIconPad = 5;
    private static final int sRightSideTitleClip = 0;

    private static final int kContentTester = 100; // For getting region insets

    private static final RecyclableSingleton<AquaInternalFrameBorder> documentWindowFrame = new RecyclableSingleton<AquaInternalFrameBorder>() {
        protected AquaInternalFrameBorder getInstance() {
            return new AquaInternalFrameBorder(WindowType.DOCUMENT);
        }
    };
    protected static AquaInternalFrameBorder window() {
        return documentWindowFrame.get();
    }

    private static final RecyclableSingleton<AquaInternalFrameBorder> utilityWindowFrame = new RecyclableSingleton<AquaInternalFrameBorder>() {
        protected AquaInternalFrameBorder getInstance() {
            return new AquaInternalFrameBorder(WindowType.UTILITY);
        }
    };
    protected static AquaInternalFrameBorder utility() {
        return utilityWindowFrame.get();
    }

    private static final RecyclableSingleton<AquaInternalFrameBorder> dialogWindowFrame = new RecyclableSingleton<AquaInternalFrameBorder>() {
        protected AquaInternalFrameBorder getInstance() {
            return new AquaInternalFrameBorder(WindowType.DOCUMENT);
        }
    };
    protected static AquaInternalFrameBorder dialog() {
        return dialogWindowFrame.get();
    }

    private final AquaInternalFrameBorderMetrics metrics;

    private final int fThisButtonSpan;
    private final int fThisLeftSideTotal;

    private final boolean fIsUtility;

    // Instance variables
    private final WindowType fWindowKind; // Which kind of window to draw
    private Insets fBorderInsets; // Cached insets object

    private Color selectedTextColor;
    private Color notSelectedTextColor;

    private Rectangle fInBounds; // Cached bounds rect object

    protected final AquaPainter<TitleBarHeightState> titleBarPainter = AquaPainter.create(JRSUIStateFactory.getTitleBar());
    protected final AquaPainter<JRSUIState> widgetPainter = AquaPainter.create(JRSUIState.getInstance());

    protected AquaInternalFrameBorder(final WindowType kind) {
        fWindowKind = kind;

        titleBarPainter.state.set(WindowClipCorners.YES);
        if (fWindowKind == WindowType.UTILITY) {
            fIsUtility = true;
            metrics = AquaInternalFrameBorderMetrics.getMetrics(true);

            widgetPainter.state.set(WindowType.UTILITY);
            titleBarPainter.state.set(WindowType.UTILITY);
        } else {
            fIsUtility = false;
            metrics = AquaInternalFrameBorderMetrics.getMetrics(false);

            widgetPainter.state.set(WindowType.DOCUMENT);
            titleBarPainter.state.set(WindowType.DOCUMENT);
        }
        titleBarPainter.state.setValue(metrics.titleBarHeight);
        titleBarPainter.state.set(WindowTitleBarSeparator.YES);
        widgetPainter.state.set(AlignmentVertical.CENTER);

        fThisButtonSpan = (metrics.buttonWidth * 3) + (metrics.buttonPadding * 2);
        fThisLeftSideTotal = metrics.leftSidePadding + fThisButtonSpan + sAfterButtonPad;
    }

    public void setColors(final Color inSelectedTextColor, final Color inNotSelectedTextColor) {
        selectedTextColor = inSelectedTextColor;
        notSelectedTextColor = inNotSelectedTextColor;
    }

    // Utility to lazy-init and fill in fInBounds
    protected void setInBounds(final int x, final int y, final int w, final int h) {
        if (fInBounds == null) fInBounds = new Rectangle();

        fInBounds.x = x;
        fInBounds.y = y;
        fInBounds.width = w;
        fInBounds.height = h;
    }

    // Border interface
    public boolean isBorderOpaque() {
        return false;
    }

    // Border interface
    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
        // For expanded InternalFrames, the frame & component are the same object
        paintBorder((JInternalFrame)c, c, g, x, y, w, h);
    }

    protected void paintTitleContents(final Graphics g, final JInternalFrame frame, final int x, final int y, final int w, final int h) {
        final boolean isSelected = frame.isSelected();
        final Font f = g.getFont();

        g.setFont(metrics.font);

        // Center text vertically.
        final FontMetrics fm = g.getFontMetrics();
        final int baseline = (metrics.titleBarHeight + fm.getAscent() - fm.getLeading() - fm.getDescent()) / 2;

        // max button is the rightmost so use it
        final int usedWidth = fThisLeftSideTotal + sRightSideTitleClip;
        int iconWidth = getIconWidth(frame);
        if (iconWidth > 0) iconWidth += sAfterIconPad;

        final int totalWidth = w;

        // window title looks like: | 0 0 0(sAfterButtonPad)IconWidth Title(right pad) |
        final int availTextWidth = totalWidth - usedWidth - iconWidth - sAfterButtonPad;

        final String title = frame.getTitle();

        String text = title;
        int totalTextWidth = 0;

        int startXPosition = fThisLeftSideTotal;
        boolean wasTextShortened = false;
        // shorten the string to fit in the
        if (text != null && !text.isEmpty()) {
            totalTextWidth = SwingUtilities.computeStringWidth(fm, text);
            final String clipString = "\u2026";
            if (totalTextWidth > availTextWidth) {
                wasTextShortened = true;
                totalTextWidth = SwingUtilities.computeStringWidth(fm, clipString);
                int nChars;
                for (nChars = 0; nChars < text.length(); nChars++) {
                    final int nextCharWidth = fm.charWidth(text.charAt(nChars));
                    if ((totalTextWidth + nextCharWidth) > availTextWidth) {
                        break;
                    }
                    totalTextWidth += nextCharWidth;
                }
                text = text.substring(0, nChars) + clipString;
            }

            if (!wasTextShortened) {
                // center it!
                startXPosition = (totalWidth - (totalTextWidth + iconWidth)) / 2;
                if (startXPosition < fThisLeftSideTotal) {
                    startXPosition = fThisLeftSideTotal;
                }
            }

            if (isSelected || fIsUtility) {
                g.setColor(Color.lightGray);
            } else {
                g.setColor(Color.white);
            }
            SwingUtilities2.drawString(frame, g, text, x + startXPosition + iconWidth, y + baseline + 1);

            if (isSelected || fIsUtility) {
                g.setColor(selectedTextColor);
            } else {
                g.setColor(notSelectedTextColor);
            }

            SwingUtilities2.drawString(frame, g, text, x + startXPosition + iconWidth, y + baseline);
            g.setFont(f);
        }

        // sja fix x & y
        final int iconYPostion = (metrics.titleBarHeight - getIconHeight(frame)) / 2;
        paintTitleIcon(g, frame, x + startXPosition, y + iconYPostion);
    }

    public int getWhichButtonHit(final JInternalFrame frame, final int x, final int y) {
        int buttonHit = -1;

        final Insets i = frame.getInsets();
        int startX = i.left + metrics.leftSidePadding - 1;
        if (isInsideYButtonArea(i, y) && x >= startX) {
            if (x <= (startX + metrics.buttonWidth)) {
                if (frame.isClosable()) {
                    buttonHit = kCloseButton;
                }
            } else {
                startX += metrics.buttonWidth + metrics.buttonPadding;
                if (x >= startX && x <= (startX + metrics.buttonWidth)) {
                    if (frame.isIconifiable()) {
                        buttonHit = kIconButton;
                    }
                } else {
                    startX += metrics.buttonWidth + metrics.buttonPadding;
                    if (x >= startX && x <= (startX + metrics.buttonWidth)) {
                        if (frame.isMaximizable()) {
                            buttonHit = kGrowButton;
                        }
                    }
                }
            }
        }

        return buttonHit;
    }

    public void doButtonAction(final JInternalFrame frame, final int whichButton) {
        switch (whichButton) {
            case kCloseButton:
                frame.doDefaultCloseAction();
                break;

            case kIconButton:
                if (frame.isIconifiable()) {
                    if (!frame.isIcon()) {
                        try {
                            frame.setIcon(true);
                        } catch(final PropertyVetoException e1) {}
                    } else {
                        try {
                            frame.setIcon(false);
                        } catch(final PropertyVetoException e1) {}
                    }
                }
                break;

            case kGrowButton:
                if (frame.isMaximizable()) {
                    if (!frame.isMaximum()) {
                        try {
                            frame.setMaximum(true);
                        } catch(final PropertyVetoException e5) {}
                    } else {
                        try {
                            frame.setMaximum(false);
                        } catch(final PropertyVetoException e6) {}
                    }
                }
                break;

            default:
                System.err.println("AquaInternalFrameBorder should never get here!!!!");
                Thread.dumpStack();
                break;
        }
    }

    public boolean isInsideYButtonArea(final Insets i, final int y) {
        final int startY = (i.top - metrics.titleBarHeight / 2) - (metrics.buttonHeight / 2) - 1;
        final int endY = startY + metrics.buttonHeight;
        return y >= startY && y <= endY;
    }

    public boolean getWithinRolloverArea(final Insets i, final int x, final int y) {
        final int startX = i.left + metrics.leftSidePadding;
        final int endX = startX + fThisButtonSpan;
        return isInsideYButtonArea(i, y) && x >= startX && x <= endX;
    }

    protected void paintTitleIcon(final Graphics g, final JInternalFrame frame,
            final int x, final int y) {

        Icon icon = frame.getFrameIcon();
        if (icon == null) {
            icon = UIManager.getIcon("InternalFrame.icon");
        }

        if (icon == null) {
            return;
        }

        if (icon.getIconWidth() > sMaxIconWidth
                || icon.getIconHeight() > sMaxIconHeight) {
            final Graphics2D g2 = (Graphics2D) g;
            final AffineTransform savedAT = g2.getTransform();
            double xScaleFactor = (double) sMaxIconWidth / icon.getIconWidth();
            double yScaleFactor = (double) sMaxIconHeight / icon.getIconHeight();

            //Coordinates are after a translation hence relative origin shifts
            g2.translate(x, y);

            //scaling factor is needed to scale while maintaining aspect ratio
            double scaleMaintainAspectRatio = Math.min(xScaleFactor, yScaleFactor);

            //minimum value is taken to set to a maximum Icon Dimension
            g2.scale(scaleMaintainAspectRatio, scaleMaintainAspectRatio);

            icon.paintIcon(frame, g2, 0, 0);
            g2.setTransform(savedAT);

        } else {
            icon.paintIcon(frame, g, x, y);
        }
    }

    protected int getIconWidth(final JInternalFrame frame) {
        int width = 0;

        Icon icon = frame.getFrameIcon();
        if (icon == null) {
            icon = UIManager.getIcon("InternalFrame.icon");
        }
        if (icon != null) {
            width = Math.min(icon.getIconWidth(), sMaxIconWidth);
        }

        return width;
    }

    protected int getIconHeight(final JInternalFrame frame) {
        int height = 0;

        Icon icon = frame.getFrameIcon();
        if (icon == null) {
            icon = UIManager.getIcon("InternalFrame.icon");
        }
        if (icon != null) {
            height = Math.min(icon.getIconHeight(), sMaxIconHeight);
        }

        return height;
    }

    public void drawWindowTitle(final Graphics g, final JInternalFrame frame, final int inX, final int inY, final int inW, final int inH) {
        final int x = inX;
        final int y = inY;
        final int w = inW;
        int h = inH;

        h = metrics.titleBarHeight + inH;

        // paint the background
        titleBarPainter.state.set(frame.isSelected() ? State.ACTIVE : State.INACTIVE);
        titleBarPainter.paint(g, frame, x, y, w, h);

        // now the title and the icon
        paintTitleContents(g, frame, x, y, w, h);

        // finally the widgets
        drawAllWidgets(g, frame); // rollover is last attribute
    }

    // Component could be a JInternalFrame or a JDesktopIcon
    void paintBorder(final JInternalFrame frame, final Component c, final Graphics g, final int x, final int y, final int w, final int h) {
        if (fBorderInsets == null) getBorderInsets(c);
        // Set the contentRect - inset by border size
        setInBounds(x + fBorderInsets.left, y + fBorderInsets.top, w - (fBorderInsets.right + fBorderInsets.left), h - (fBorderInsets.top + fBorderInsets.bottom));

        // Set parameters
        setMetrics(frame, c);

        // Draw the frame
        drawWindowTitle(g, frame, x, y, w, h);
    }

    // defaults to false
    boolean isDirty(final JInternalFrame frame) {
        final Object dirty = frame.getClientProperty("windowModified");
        if (dirty == null || dirty == Boolean.FALSE) return false;
        return true;
    }

    // Border interface
    public Insets getBorderInsets(final Component c) {
        if (fBorderInsets == null) fBorderInsets = new Insets(0, 0, 0, 0);

        // Paranoia check
        if (!(c instanceof JInternalFrame)) return fBorderInsets;

        final JInternalFrame frame = (JInternalFrame)c;

        // Set the contentRect to an arbitrary value (in case the current real one is too small)
        setInBounds(0, 0, kContentTester, kContentTester);

        // Set parameters
        setMetrics(frame, c);

        fBorderInsets.left = 0;
        fBorderInsets.top = metrics.titleBarHeight;
        fBorderInsets.right = 0;
        fBorderInsets.bottom = 0;

        return fBorderInsets;
    }

    public void repaintButtonArea(final JInternalFrame frame) {
        final Insets i = frame.getInsets();
        final int x = i.left + metrics.leftSidePadding;
        final int y = i.top - metrics.titleBarHeight + 1;
        frame.repaint(x, y, fThisButtonSpan, metrics.titleBarHeight - 2);
    }

    // Draw all the widgets this frame supports
    void drawAllWidgets(final Graphics g, final JInternalFrame frame) {
        int x = metrics.leftSidePadding;
        int y = (metrics.titleBarHeight - metrics.buttonHeight) / 2 - metrics.titleBarHeight;

        final Insets insets = frame.getInsets();
        x += insets.left;
        y += insets.top + metrics.downShift;

        final AquaInternalFrameUI ui = (AquaInternalFrameUI)frame.getUI();
        final int buttonPressedIndex = ui.getWhichButtonPressed();
        final boolean overButton = ui.getMouseOverPressedButton();
        final boolean rollover = ui.getRollover();

        final boolean frameSelected = frame.isSelected() || fIsUtility;
        final boolean generalActive = rollover || frameSelected;

        final boolean dirty = isDirty(frame);

        paintButton(g, frame, x, y, kCloseButton, buttonPressedIndex, overButton, frame.isClosable(), generalActive, rollover, dirty);

        x += metrics.buttonPadding + metrics.buttonWidth;
        paintButton(g, frame, x, y, kIconButton, buttonPressedIndex, overButton, frame.isIconifiable(), generalActive, rollover, false);

        x += metrics.buttonPadding + metrics.buttonWidth;
        paintButton(g, frame, x, y, kGrowButton, buttonPressedIndex, overButton, frame.isMaximizable(), generalActive, rollover, false);
    }

    public void paintButton(final Graphics g, final JInternalFrame frame, final int x, final int y, final int buttonType, final int buttonPressedIndex, final boolean overButton, final boolean enabled, final boolean active, final boolean anyRollover, final boolean dirty) {
        widgetPainter.state.set(getWidget(frame, buttonType));
        widgetPainter.state.set(getState(buttonPressedIndex == buttonType && overButton, anyRollover, active, enabled));
        widgetPainter.state.set(dirty ? BooleanValue.YES : BooleanValue.NO);
        widgetPainter.paint(g, frame, x, y, metrics.buttonWidth, metrics.buttonHeight);
    }

    static Widget getWidget(final JInternalFrame frame, final int buttonType) {
        switch (buttonType) {
            case kIconButton: return Widget.TITLE_BAR_COLLAPSE_BOX;
            case kGrowButton: return Widget.TITLE_BAR_ZOOM_BOX;
        }

        return Widget.TITLE_BAR_CLOSE_BOX;
    }

    static State getState(final boolean pressed, final boolean rollover, final boolean active, final boolean enabled) {
        if (!enabled) return State.DISABLED;
        if (!active) return State.INACTIVE;
        if (pressed) return State.PRESSED;
        if (rollover) return State.ROLLOVER;
        return State.ACTIVE;
    }

    protected void setMetrics(final JInternalFrame frame, final Component window) {
        final String title = frame.getTitle();
        final FontMetrics fm = frame.getFontMetrics(UIManager.getFont("InternalFrame.titleFont"));
        int titleWidth = 0;
        int titleHeight = fm.getAscent();
        if (title != null) {
            titleWidth = SwingUtilities.computeStringWidth(fm, title);
        }
        // Icon space
        final Icon icon = frame.getFrameIcon();
        if (icon != null) {
            titleWidth += icon.getIconWidth();
            titleHeight = Math.max(titleHeight, icon.getIconHeight());
        }
    }

    protected int getTitleHeight() {
        return metrics.titleBarHeight;
    }
}
