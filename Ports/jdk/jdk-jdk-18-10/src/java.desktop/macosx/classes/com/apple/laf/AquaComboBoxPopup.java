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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboPopup;

import sun.lwawt.macosx.CPlatformWindow;

@SuppressWarnings("serial") // Superclass is not serializable across versions
final class AquaComboBoxPopup extends BasicComboPopup {
    static final int FOCUS_RING_PAD_LEFT = 6;
    static final int FOCUS_RING_PAD_RIGHT = 6;
    static final int FOCUS_RING_PAD_BOTTOM = 5;

    protected Component topStrut;
    protected Component bottomStrut;
    protected boolean isPopDown = false;

    public AquaComboBoxPopup(final JComboBox<Object> cBox) {
        super(cBox);
    }

    @Override
    protected void configurePopup() {
        super.configurePopup();

        setBorderPainted(false);
        setBorder(null);
        updateContents(false);

        // TODO: CPlatformWindow?
        putClientProperty(CPlatformWindow.WINDOW_FADE_OUT, Integer.valueOf(150));
    }

    public void updateContents(final boolean remove) {
        // for more background on this issue, see AquaMenuBorder.getBorderInsets()

        isPopDown = isPopdown();
        if (isPopDown) {
            if (remove) {
                if (topStrut != null) {
                    this.remove(topStrut);
                }
                if (bottomStrut != null) {
                    this.remove(bottomStrut);
                }
            } else {
                add(scroller);
            }
        } else {
            if (topStrut == null) {
                topStrut = Box.createVerticalStrut(4);
                bottomStrut = Box.createVerticalStrut(4);
            }

            if (remove) remove(scroller);

            this.add(topStrut);
            this.add(scroller);
            this.add(bottomStrut);
        }
    }

    protected Dimension getBestPopupSizeForRowCount(final int maxRowCount) {
        final int currentElementCount = comboBox.getModel().getSize();
        final int rowCount = Math.min(maxRowCount, currentElementCount);

        final Dimension popupSize = new Dimension();
        final ListCellRenderer<Object> renderer = list.getCellRenderer();

        for (int i = 0; i < rowCount; i++) {
            final Object value = list.getModel().getElementAt(i);
            final Component c = renderer.getListCellRendererComponent(list, value, i, false, false);

            final Dimension prefSize = c.getPreferredSize();
            popupSize.height += prefSize.height;
            popupSize.width = Math.max(prefSize.width, popupSize.width);
        }

        popupSize.width += 10;

        return popupSize;
    }

    protected boolean shouldScroll() {
        return comboBox.getItemCount() > comboBox.getMaximumRowCount();
    }

    protected boolean isPopdown() {
        return shouldScroll() || AquaComboBoxUI.isPopdown(comboBox);
    }

    @Override
    public void show() {
        final int startItemCount = comboBox.getItemCount();

        final Rectangle popupBounds = adjustPopupAndGetBounds();
        if (popupBounds == null) return; // null means don't show

        comboBox.firePopupMenuWillBecomeVisible();
        show(comboBox, popupBounds.x, popupBounds.y);

        // hack for <rdar://problem/4905531> JComboBox does not fire popupWillBecomeVisible if item count is 0
        final int afterShowItemCount = comboBox.getItemCount();
        if (afterShowItemCount == 0) {
            hide();
            return;
        }

        if (startItemCount != afterShowItemCount) {
            final Rectangle newBounds = adjustPopupAndGetBounds();
            list.setSize(newBounds.width, newBounds.height);
            pack();

            final Point newLoc = comboBox.getLocationOnScreen();
            setLocation(newLoc.x + newBounds.x, newLoc.y + newBounds.y);
        }
        // end hack

        list.requestFocusInWindow();
    }

    @Override
    @SuppressWarnings("serial") // anonymous class
    protected JList<Object> createList() {
        return new JList<Object>(comboBox.getModel()) {
            @Override
            @SuppressWarnings("deprecation")
            public void processMouseEvent(MouseEvent e) {
                if (e.isMetaDown()) {
                    e = new MouseEvent((Component) e.getSource(), e.getID(),
                                       e.getWhen(),
                                       e.getModifiers() ^ InputEvent.META_MASK,
                                       e.getX(), e.getY(), e.getXOnScreen(),
                                       e.getYOnScreen(), e.getClickCount(),
                                       e.isPopupTrigger(), MouseEvent.NOBUTTON);
                }
                super.processMouseEvent(e);
            }
        };
    }

    protected Rectangle adjustPopupAndGetBounds() {
        if (isPopDown != isPopdown()) {
            updateContents(true);
        }

        final Dimension popupSize = getBestPopupSizeForRowCount(comboBox.getMaximumRowCount());
        final Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height, popupSize.width, popupSize.height);
        if (popupBounds == null) return null; // returning null means don't show anything

        final Dimension realPopupSize = popupBounds.getSize();
        scroller.setMaximumSize(realPopupSize);
        scroller.setPreferredSize(realPopupSize);
        scroller.setMinimumSize(realPopupSize);
        list.invalidate();

        final int selectedIndex = comboBox.getSelectedIndex();
        if (selectedIndex == -1) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(selectedIndex);
        }
        list.ensureIndexIsVisible(list.getSelectedIndex());

        return popupBounds;
    }

    // Get the bounds of the screen where the menu should appear
    // p is the origin of the combo box in screen bounds
    Rectangle getBestScreenBounds(final Point p) {
        //System.err.println("GetBestScreenBounds p: "+ p.x + ", " + p.y);
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();
        for (final GraphicsDevice gd : gs) {
            final GraphicsConfiguration[] gc = gd.getConfigurations();
            for (final GraphicsConfiguration element0 : gc) {
                final Rectangle gcBounds = element0.getBounds();
                if (gcBounds.contains(p)) {
                    return getAvailableScreenArea(gcBounds, element0);
                }
            }
        }

        // Hmm.  Origin's off screen, but is any part on?
        final Rectangle comboBoxBounds = comboBox.getBounds();
        comboBoxBounds.setLocation(p);
        for (final GraphicsDevice gd : gs) {
            final GraphicsConfiguration[] gc = gd.getConfigurations();
            for (final GraphicsConfiguration element0 : gc) {
                final Rectangle gcBounds = element0.getBounds();
                if (gcBounds.intersects(comboBoxBounds)) {
                    return getAvailableScreenArea(gcBounds, element0);
                }
            }
        }

        return null;
    }

    private Rectangle getAvailableScreenArea(Rectangle bounds,
                                             GraphicsConfiguration gc) {
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                             bounds.width - insets.left - insets.right,
                             bounds.height - insets.top - insets.bottom);
    }

    private int getComboBoxEdge(int py, boolean bottom) {
        int offset = bottom ? 9 : -9;
        // if py is less than new y we have a clipped combo, so leave it alone.
        return Math.min((py / 2) + offset, py);
    }

    @Override
    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        final int itemCount = comboBox.getModel().getSize();
        final boolean isPopdown = isPopdown();
        final boolean isTableCellEditor = AquaComboBoxUI.isTableCellEditor(comboBox);
        if (isPopdown && !isTableCellEditor) {
            // place the popup just below the button, which is
            // near the center of a large combo box
            py = getComboBoxEdge(py, true);
        }

        // px & py are relative to the combo box

        // **** Common calculation - applies to the scrolling and menu-style ****
        final Point p = new Point(0, 0);
        SwingUtilities.convertPointToScreen(p, comboBox);
        //System.err.println("First Converting from point to screen: 0,0 is now " + p.x + ", " + p.y);
        final Rectangle scrBounds = getBestScreenBounds(p);
        //System.err.println("BestScreenBounds is " + scrBounds);

        // If the combo box is totally off screen, do whatever super does
        if (scrBounds == null) return super.computePopupBounds(px, py, pw, ph);

        // line up with the bottom of the text field/button (or top, if we have to go above it)
        // and left edge if left-to-right, right edge if right-to-left
        final Insets comboBoxInsets = comboBox.getInsets();
        final Rectangle comboBoxBounds = comboBox.getBounds();

        if (shouldScroll()) {
            pw += 15;
        }

        if (isPopdown) {
            pw += 4;
        }

        // the popup should be wide enough for the items but not wider than the screen it's on
        final int minWidth = comboBoxBounds.width - (comboBoxInsets.left + comboBoxInsets.right);
        pw = Math.max(minWidth, pw);

        final boolean leftToRight = AquaUtils.isLeftToRight(comboBox);
        if (leftToRight) {
            px += comboBoxInsets.left;
            if (!isPopDown) px -= FOCUS_RING_PAD_LEFT;
        } else {
            px = comboBoxBounds.width - pw - comboBoxInsets.right;
            if (!isPopDown) px += FOCUS_RING_PAD_RIGHT;
        }
        py -= (comboBoxInsets.bottom); //sja fix was +kInset

        // Make sure it's all on the screen - shift it by the amount it's off
        p.x += px;
        p.y += py; // Screen location of px & py
        if (p.x < scrBounds.x) {
            px = px + (scrBounds.x - p.x);
        }
        if (p.y < scrBounds.y) {
            py = py + (scrBounds.y - p.y);
        }

        final Point top = new Point(0, 0);
        SwingUtilities.convertPointFromScreen(top, comboBox);
        //System.err.println("Converting from point to screen: 0,0 is now " + top.x + ", " + top.y);

        // Since the popup is at zero in this coord space, the maxWidth == the X coord of the screen right edge
        // (it might be wider than the screen, if the combo is off the left edge)
        final int maxWidth = Math.min(scrBounds.width, top.x + scrBounds.x + scrBounds.width) - 2; // subtract some buffer space

        pw = Math.min(maxWidth, pw);
        if (pw < minWidth) {
            px -= (minWidth - pw);
            pw = minWidth;
        }

        // this is a popup window, and will continue calculations below
        if (!isPopdown) {
            // popup windows are slightly inset from the combo end-cap
            pw -= 6;
            return computePopupBoundsForMenu(px, py, pw, ph, itemCount, scrBounds);
        }

        // don't attempt to inset table cell editors
        if (!isTableCellEditor) {
            pw -= (FOCUS_RING_PAD_LEFT + FOCUS_RING_PAD_RIGHT);
            if (leftToRight) {
                px += FOCUS_RING_PAD_LEFT;
            }
        }

        final Rectangle r = new Rectangle(px, py, pw, ph);
        if (r.y + r.height < top.y + scrBounds.y + scrBounds.height) {
            return r;
        }
        // Check whether it goes below the bottom of the screen, if so flip it
        int newY = getComboBoxEdge(comboBoxBounds.height, false) - ph - comboBoxInsets.top;
        if (newY > top.y + scrBounds.y) {
            return new Rectangle(px, newY, r.width, r.height);
        } else {
            // There are no place at top, move popup to the center of the screen
            r.y = top.y + scrBounds.y + Math.max(0, (scrBounds.height - ph) / 2 );
            r.height = Math.min(scrBounds.height, ph);
        }
        return r;
    }

    // The one to use when itemCount <= maxRowCount.  Size never adjusts for arrows
    // We want it positioned so the selected item is right above the combo box
    protected Rectangle computePopupBoundsForMenu(final int px, final int py,
                                                  final int pw, final int ph,
                                                  final int itemCount,
                                                  final Rectangle scrBounds) {
        //System.err.println("computePopupBoundsForMenu: " + px + "," + py + " " +  pw + "," + ph);
        //System.err.println("itemCount: " +itemCount +" src: "+ scrBounds);
        int elementSize = 0; //kDefaultItemSize;
        if (list != null && itemCount > 0) {
            final Rectangle cellBounds = list.getCellBounds(0, 0);
            if (cellBounds != null) elementSize = cellBounds.height;
        }

        int offsetIndex = comboBox.getSelectedIndex();
        if (offsetIndex < 0) offsetIndex = 0;
        list.setSelectedIndex(offsetIndex);

        final int selectedLocation = elementSize * offsetIndex;

        final Point top = new Point(0, scrBounds.y);
        final Point bottom = new Point(0, scrBounds.y + scrBounds.height - 20); // Allow some slack
        SwingUtilities.convertPointFromScreen(top, comboBox);
        SwingUtilities.convertPointFromScreen(bottom, comboBox);

        final Rectangle popupBounds = new Rectangle(px, py, pw, ph);// Relative to comboBox

        final int theRest = ph - selectedLocation;

        // If the popup fits on the screen and the selection appears under the mouse w/o scrolling, cool!
        // If the popup won't fit on the screen, adjust its position but not its size
        // and rewrite this to support arrows - JLists always move the contents so they all show

        // Test to see if it extends off the screen
        final boolean extendsOffscreenAtTop = selectedLocation > -top.y;
        final boolean extendsOffscreenAtBottom = theRest > bottom.y;

        if (extendsOffscreenAtTop) {
            popupBounds.y = top.y + 1;
            // Round it so the selection lines up with the combobox
            popupBounds.y = (popupBounds.y / elementSize) * elementSize;
        } else if (extendsOffscreenAtBottom) {
            // Provide blank space at top for off-screen stuff to scroll into
            popupBounds.y = bottom.y - popupBounds.height; // popupBounds.height has already been adjusted to fit
        } else { // fits - position it so the selectedLocation is under the mouse
            popupBounds.y = -selectedLocation;
        }

        // Center the selected item on the combobox
        final int height = comboBox.getHeight();
        final Insets insets = comboBox.getInsets();
        final int buttonSize = height - (insets.top + insets.bottom);
        final int diff = (buttonSize - elementSize) / 2 + insets.top;
        popupBounds.y += diff - FOCUS_RING_PAD_BOTTOM;

        return popupBounds;
    }
}
