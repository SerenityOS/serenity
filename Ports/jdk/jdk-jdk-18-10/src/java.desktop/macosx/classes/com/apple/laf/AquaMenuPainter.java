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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import sun.swing.SwingUtilities2;

import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaIcon.InvertableIcon;
import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

/**
 * AquaMenuPainter, implements paintMenuItem to avoid code duplication
 *
 * BasicMenuItemUI didn't factor out the various parts of the Menu, and
 * we subclass it and its subclasses BasicMenuUI
 * Our classes need an implementation of paintMenuItem
 * that allows them to paint their own backgrounds
 */

public class AquaMenuPainter {
    // Glyph statics:
    // ASCII character codes
    static final byte
        kShiftGlyph = 0x05,
        kOptionGlyph = 0x07,
        kControlGlyph = 0x06,
        kPencilGlyph = 0x0F,
        kCommandMark = 0x11;

    // Unicode character codes
    static final char
        kUBlackDiamond = 0x25C6,
        kUCheckMark = 0x2713,
        kUControlGlyph = 0x2303,
        kUOptionGlyph = 0x2325,
        kUEnterGlyph = 0x2324,
        kUCommandGlyph = 0x2318,
        kULeftDeleteGlyph = 0x232B,
        kURightDeleteGlyph = 0x2326,
        kUShiftGlyph = 0x21E7,
        kUCapsLockGlyph = 0x21EA;

    static final int ALT_GRAPH_MASK = 1 << 5; // New to Java2
    @SuppressWarnings("deprecation")
    static final int sUnsupportedModifiersMask =
            ~(InputEvent.CTRL_MASK | InputEvent.ALT_MASK | InputEvent.SHIFT_MASK
                    | InputEvent.META_MASK | ALT_GRAPH_MASK);

    interface Client {
        public void paintBackground(Graphics g, JComponent c, int menuWidth, int menuHeight);
    }

    // Return a string with the proper modifier glyphs
    static String getKeyModifiersText(final int modifiers, final boolean isLeftToRight) {
        return getKeyModifiersUnicode(modifiers, isLeftToRight);
    }

    // Return a string with the proper modifier glyphs
    @SuppressWarnings("deprecation")
    private static String getKeyModifiersUnicode(final int modifiers, final boolean isLeftToRight) {
        final StringBuilder buf = new StringBuilder(2);
        // Order (from StandardMenuDef.c): control, option(alt), shift, cmd
        // reverse for right-to-left
        //$ check for substitute key glyphs for localization
        if (isLeftToRight) {
            if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                buf.append(kUControlGlyph);
            }
            if ((modifiers & (InputEvent.ALT_MASK | ALT_GRAPH_MASK)) != 0) {
                buf.append(kUOptionGlyph);
            }
            if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
                buf.append(kUShiftGlyph);
            }
            if ((modifiers & InputEvent.META_MASK) != 0) {
                buf.append(kUCommandGlyph);
            }
        } else {
            if ((modifiers & InputEvent.META_MASK) != 0) {
                buf.append(kUCommandGlyph);
            }
            if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
                buf.append(kUShiftGlyph);
            }
            if ((modifiers & (InputEvent.ALT_MASK | ALT_GRAPH_MASK)) != 0) {
                buf.append(kUOptionGlyph);
            }
            if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                buf.append(kUControlGlyph);
            }
        }
        return buf.toString();
    }

    private static final RecyclableSingleton<AquaMenuPainter> sPainter = new RecyclableSingletonFromDefaultConstructor<AquaMenuPainter>(AquaMenuPainter.class);
    static AquaMenuPainter instance() {
        return sPainter.get();
    }

    static final int defaultMenuItemGap = 2;
    static final int kAcceleratorArrowSpace = 16; // Accel space doesn't overlap arrow space, even though items can't have both

    static class RecyclableBorder extends RecyclableSingleton<Border> {
        final String borderName;
        RecyclableBorder(final String borderName) { this.borderName = borderName; }
        protected Border getInstance() { return UIManager.getBorder(borderName); }
    }

    private static final RecyclableBorder menuBarPainter = new RecyclableBorder("MenuBar.backgroundPainter");
    private static final RecyclableBorder selectedMenuBarItemPainter = new RecyclableBorder("MenuBar.selectedBackgroundPainter");
    private static final RecyclableBorder selectedMenuItemPainter = new RecyclableBorder("MenuItem.selectedBackgroundPainter");

    public void paintMenuBarBackground(final Graphics g, final int width, final int height, final JComponent c) {
        g.setColor(c == null ? Color.white : c.getBackground());
        g.fillRect(0, 0, width, height);
        menuBarPainter.get().paintBorder(null, g, 0, 0, width, height);
    }

    public void paintSelectedMenuTitleBackground(final Graphics g, final int width, final int height) {
        selectedMenuBarItemPainter.get().paintBorder(null, g, -1, 0, width + 2, height);
    }

    public void paintSelectedMenuItemBackground(final Graphics g, final int width, final int height) {
        selectedMenuItemPainter.get().paintBorder(null, g, 0, 0, width, height);
    }

    protected void paintMenuItem(final Client client, final Graphics g, final JComponent c, final Icon checkIcon, final Icon arrowIcon, final Color background, final Color foreground, final Color disabledForeground, final Color selectionForeground, final int defaultTextIconGap, final Font acceleratorFont) {
        final JMenuItem b = (JMenuItem)c;
        final ButtonModel model = b.getModel();

//        Dimension size = b.getSize();
        final int menuWidth = b.getWidth();
        final int menuHeight = b.getHeight();
        final Insets i = c.getInsets();

        Rectangle viewRect = new Rectangle(0, 0, menuWidth, menuHeight);

        viewRect.x += i.left;
        viewRect.y += i.top;
        viewRect.width -= (i.right + viewRect.x);
        viewRect.height -= (i.bottom + viewRect.y);

        final Font holdf = g.getFont();
        final Color holdc = g.getColor();
        final Font f = c.getFont();
        g.setFont(f);
        final FontMetrics fm = g.getFontMetrics(f);

        final FontMetrics fmAccel = g.getFontMetrics(acceleratorFont);

        // Paint background (doesn't touch the Graphics object's color)
        if (c.isOpaque()) {
            client.paintBackground(g, c, menuWidth, menuHeight);
        }

        // get Accelerator text
        final KeyStroke accelerator = b.getAccelerator();
        String modifiersString = "", keyString = "";
        final boolean leftToRight = AquaUtils.isLeftToRight(c);
        if (accelerator != null) {
            final int modifiers = accelerator.getModifiers();
            if (modifiers > 0) {
                modifiersString = getKeyModifiersText(modifiers, leftToRight);
            }
            final int keyCode = accelerator.getKeyCode();
            if (keyCode != 0) {
                keyString = KeyEvent.getKeyText(keyCode);
            } else {
                keyString += accelerator.getKeyChar();
            }
        }

        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        Rectangle acceleratorRect = new Rectangle();
        Rectangle checkIconRect = new Rectangle();
        Rectangle arrowIconRect = new Rectangle();

        // layout the text and icon
        final String text = layoutMenuItem(b, fm, b.getText(), fmAccel, keyString, modifiersString, b.getIcon(), checkIcon, arrowIcon, b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect, textRect, acceleratorRect, checkIconRect, arrowIconRect, b.getText() == null ? 0 : defaultTextIconGap, defaultTextIconGap);

        // if this is in a AquaScreenMenuBar that's attached to a DialogPeer
        // the native menu will be disabled, though the awt Menu won't know about it
        // so the JPopupMenu will not have visibility set and the items should draw disabled
        // If it's not on a JPopupMenu then it should just use the model's enable state
        final Container parent = b.getParent();
        final boolean parentIsMenuBar = parent instanceof JMenuBar;

        Container ancestor = parent;
        while (ancestor != null && !(ancestor instanceof JPopupMenu)) ancestor = ancestor.getParent();

        boolean isEnabled = model.isEnabled() && (ancestor == null || ancestor.isVisible());

        // Set the accel/normal text color
        boolean isSelected = false;
        if (!isEnabled) {
            // *** paint the text disabled
            g.setColor(disabledForeground);
        } else {
            // *** paint the text normally
            if (model.isArmed() || (c instanceof JMenu && model.isSelected())) {
                g.setColor(selectionForeground);
                isSelected = true;
            } else {
                g.setColor(parentIsMenuBar ? parent.getForeground() : b.getForeground()); // Which is either MenuItem.foreground or the user's choice
            }
        }

        // We want to paint the icon after the text color is set since some icon painting depends on the correct
        // graphics color being set
        // See <rdar://problem/3792383> Menu icons missing in Java2D's Lines.Joins demo
        // Paint the Icon
        if (b.getIcon() != null) {
            paintIcon(g, b, iconRect, isEnabled);
        }

        // Paint the Check using the current text color
        if (checkIcon != null) {
            paintCheck(g, b, checkIcon, checkIconRect);
        }

        // Draw the accelerator first in case the HTML renderer changes the color
        if (keyString != null && !keyString.isEmpty()) {
            final int yAccel = acceleratorRect.y + fm.getAscent();
            if (modifiersString.isEmpty()) {
                // just draw the keyString
                SwingUtilities2.drawString(c, g, keyString, acceleratorRect.x, yAccel);
            } else {
                final int modifiers = accelerator.getModifiers();
                int underlinedChar = 0;
                if ((modifiers & ALT_GRAPH_MASK) > 0) underlinedChar = kUOptionGlyph; // This is a Java2 thing, we won't be getting kOptionGlyph
                // The keyStrings should all line up, so always adjust the width by the same amount
                // (if they're multi-char, they won't line up but at least they won't be cut off)
                final int emWidth = Math.max(fm.charWidth('M'), SwingUtilities.computeStringWidth(fm, keyString));

                if (leftToRight) {
                    g.setFont(acceleratorFont);
                    drawString(g, c, modifiersString, underlinedChar, acceleratorRect.x, yAccel, isEnabled, isSelected);
                    g.setFont(f);
                    SwingUtilities2.drawString(c, g, keyString, acceleratorRect.x + acceleratorRect.width - emWidth, yAccel);
                } else {
                    final int xAccel = acceleratorRect.x + emWidth;
                    g.setFont(acceleratorFont);
                    drawString(g, c, modifiersString, underlinedChar, xAccel, yAccel, isEnabled, isSelected);
                    g.setFont(f);
                    SwingUtilities2.drawString(c, g, keyString, xAccel - fm.stringWidth(keyString), yAccel);
                }
            }
        }

        // Draw the Text
        if (text != null && !text.isEmpty()) {
            final View v = (View)c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                v.paint(g, textRect);
            } else {
                final int mnemonic = (AquaMnemonicHandler.isMnemonicHidden() ? -1 : model.getMnemonic());
                drawString(g, c, text, mnemonic, textRect.x, textRect.y + fm.getAscent(), isEnabled, isSelected);
            }
        }

        // Paint the Arrow
        if (arrowIcon != null) {
            paintArrow(g, b, model, arrowIcon, arrowIconRect);
        }

        g.setColor(holdc);
        g.setFont(holdf);
    }

    // All this had to be copied from BasicMenuItemUI, just to get the right keyModifiersText fn
    // and a few Mac tweaks
    protected Dimension getPreferredMenuItemSize(final JComponent c, final Icon checkIcon, final Icon arrowIcon, final int defaultTextIconGap, final Font acceleratorFont) {
        final JMenuItem b = (JMenuItem)c;
        final Icon icon = b.getIcon();
        final String text = b.getText();
        final KeyStroke accelerator = b.getAccelerator();
        String keyString = "", modifiersString = "";

        if (accelerator != null) {
            final int modifiers = accelerator.getModifiers();
            if (modifiers > 0) {
                modifiersString = getKeyModifiersText(modifiers, true); // doesn't matter, this is just for metrics
            }
            final int keyCode = accelerator.getKeyCode();
            if (keyCode != 0) {
                keyString = KeyEvent.getKeyText(keyCode);
            } else {
                keyString += accelerator.getKeyChar();
            }
        }

        final Font font = b.getFont();
        final FontMetrics fm = b.getFontMetrics(font);
        final FontMetrics fmAccel = b.getFontMetrics(acceleratorFont);

        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();
        Rectangle acceleratorRect = new Rectangle();
        Rectangle checkIconRect = new Rectangle();
        Rectangle arrowIconRect = new Rectangle();
        Rectangle viewRect = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);

        layoutMenuItem(b, fm, text, fmAccel, keyString, modifiersString, icon, checkIcon, arrowIcon, b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect, textRect, acceleratorRect, checkIconRect, arrowIconRect, text == null ? 0 : defaultTextIconGap, defaultTextIconGap);
        // find the union of the icon and text rects
        Rectangle r = new Rectangle();
        r.setBounds(textRect);
        r = SwingUtilities.computeUnion(iconRect.x, iconRect.y, iconRect.width, iconRect.height, r);
        //   r = iconRect.union(textRect);

        // Add in the accelerator
        boolean acceleratorTextIsEmpty = (keyString == null) || keyString.isEmpty();

        if (!acceleratorTextIsEmpty) {
            r.width += acceleratorRect.width;
        }

        if (!isTopLevelMenu(b)) {
            // Add in the checkIcon
            r.width += checkIconRect.width;
            r.width += defaultTextIconGap;

            // Add in the arrowIcon space
            r.width += defaultTextIconGap;
            r.width += arrowIconRect.width;
        }

        final Insets insets = b.getInsets();
        if (insets != null) {
            r.width += insets.left + insets.right;
            r.height += insets.top + insets.bottom;
        }

        // Tweak for Mac
        r.width += 4 + defaultTextIconGap;
        r.height = Math.max(r.height, 18);

        return r.getSize();
    }

    protected void paintCheck(final Graphics g, final JMenuItem item, Icon checkIcon, Rectangle checkIconRect) {
        if (isTopLevelMenu(item) || !item.isSelected()) return;

        if (item.isArmed() && checkIcon instanceof InvertableIcon) {
            ((InvertableIcon)checkIcon).getInvertedIcon().paintIcon(item, g, checkIconRect.x, checkIconRect.y);
        } else {
            checkIcon.paintIcon(item, g, checkIconRect.x, checkIconRect.y);
        }
    }

    protected void paintIcon(final Graphics g, final JMenuItem c, final Rectangle localIconRect, boolean isEnabled) {
        final ButtonModel model = c.getModel();
        Icon icon;
        if (!isEnabled) {
            icon = c.getDisabledIcon();
        } else if (model.isPressed() && model.isArmed()) {
            icon = c.getPressedIcon();
            if (icon == null) {
                // Use default icon
                icon = c.getIcon();
            }
        } else {
            icon = c.getIcon();
        }

        if (icon != null) icon.paintIcon(c, g, localIconRect.x, localIconRect.y);
    }

    protected void paintArrow(Graphics g, JMenuItem c, ButtonModel model, Icon arrowIcon, Rectangle arrowIconRect) {
        if (isTopLevelMenu(c)) return;

        if (c instanceof JMenu && (model.isArmed() || model.isSelected()) && arrowIcon instanceof InvertableIcon) {
            ((InvertableIcon)arrowIcon).getInvertedIcon().paintIcon(c, g, arrowIconRect.x, arrowIconRect.y);
        } else {
            arrowIcon.paintIcon(c, g, arrowIconRect.x, arrowIconRect.y);
        }
    }

    /** Draw a string with the graphics g at location (x,y) just like g.drawString() would.
     *  The first occurrence of underlineChar in text will be underlined. The matching is
     *  not case sensitive.
     */
    public void drawString(final Graphics g, final JComponent c, final String text, final int underlinedChar, final int x, final int y, final boolean isEnabled, final boolean isSelected) {
        char lc, uc;
        int index = -1, lci, uci;

        if (underlinedChar != '\0') {
            uc = Character.toUpperCase((char)underlinedChar);
            lc = Character.toLowerCase((char)underlinedChar);

            uci = text.indexOf(uc);
            lci = text.indexOf(lc);

            if (uci == -1) index = lci;
            else if (lci == -1) index = uci;
            else index = (lci < uci) ? lci : uci;
        }

        SwingUtilities2.drawStringUnderlineCharAt(c, g, text, index, x, y);
    }

    /*
     * Returns false if the component is a JMenu and it is a top
     * level menu (on the menubar).
     */
    private static boolean isTopLevelMenu(final JMenuItem menuItem) {
        return (menuItem instanceof JMenu) && (((JMenu)menuItem).isTopLevelMenu());
    }

    private String layoutMenuItem(final JMenuItem menuItem, final FontMetrics fm, final String text, final FontMetrics fmAccel, String keyString, final String modifiersString, final Icon icon, final Icon checkIcon, final Icon arrowIcon, final int verticalAlignment, final int horizontalAlignment, final int verticalTextPosition, final int horizontalTextPosition, final Rectangle viewR, final Rectangle iconR, final Rectangle textR, final Rectangle acceleratorR, final Rectangle checkIconR, final Rectangle arrowIconR, final int textIconGap, final int menuItemGap) {
        // Force it to do "LEFT", then flip the rects if we're right-to-left
        SwingUtilities.layoutCompoundLabel(menuItem, fm, text, icon, verticalAlignment, SwingConstants.LEFT, verticalTextPosition, horizontalTextPosition, viewR, iconR, textR, textIconGap);

        final boolean acceleratorTextIsEmpty = (keyString == null) || keyString.isEmpty();

        if (acceleratorTextIsEmpty) {
            acceleratorR.width = acceleratorR.height = 0;
            keyString = "";
        } else {
            // Accel space doesn't overlap arrow space, even though items can't have both
            acceleratorR.width = SwingUtilities.computeStringWidth(fmAccel, modifiersString);
            // The keyStrings should all line up, so always adjust the width by the same amount
            // (if they're multi-char, they won't line up but at least they won't be cut off)
            acceleratorR.width += Math.max(fm.charWidth('M'), SwingUtilities.computeStringWidth(fm, keyString));
            acceleratorR.height = fmAccel.getHeight();
        }

        /* Initialize the checkIcon bounds rectangle checkIconR.
         */

        final boolean isTopLevelMenu = isTopLevelMenu(menuItem);
        if (!isTopLevelMenu) {
            if (checkIcon != null) {
                checkIconR.width = checkIcon.getIconWidth();
                checkIconR.height = checkIcon.getIconHeight();
            } else {
                checkIconR.width = checkIconR.height = 16;
            }

            /* Initialize the arrowIcon bounds rectangle arrowIconR.
             */

            if (arrowIcon != null) {
                arrowIconR.width = arrowIcon.getIconWidth();
                arrowIconR.height = arrowIcon.getIconHeight();
            } else {
                arrowIconR.width = arrowIconR.height = 16;
            }

            textR.x += 12;
            iconR.x += 12;
        }

        final Rectangle labelR = iconR.union(textR);

        // Position the Accelerator text rect
        // Menu shortcut text *ought* to have the letters left-justified - look at a menu with an "M" in it
        acceleratorR.x += (viewR.width - arrowIconR.width - acceleratorR.width);
        acceleratorR.y = viewR.y + (viewR.height / 2) - (acceleratorR.height / 2);

        if (!isTopLevelMenu) {
            //    if ( GetSysDirection() < 0 ) hierRect.right = hierRect.left + w + 4;
            //    else hierRect.left = hierRect.right - w - 4;
            arrowIconR.x = (viewR.width - arrowIconR.width) + 1;
            arrowIconR.y = viewR.y + (labelR.height / 2) - (arrowIconR.height / 2) + 1;

            checkIconR.y = viewR.y + (labelR.height / 2) - (checkIconR.height / 2);
            checkIconR.x = 5;

            textR.width += 8;
        }

        /*System.out.println("Layout: " +horizontalAlignment+ " v=" +viewR+"  c="+checkIconR+" i="+
         iconR+" t="+textR+" acc="+acceleratorR+" a="+arrowIconR);*/

        if (!AquaUtils.isLeftToRight(menuItem)) {
            // Flip the rectangles so that instead of [check][icon][text][accel/arrow] it's [accel/arrow][text][icon][check]
            final int w = viewR.width;
            checkIconR.x = w - (checkIconR.x + checkIconR.width);
            iconR.x = w - (iconR.x + iconR.width);
            textR.x = w - (textR.x + textR.width);
            acceleratorR.x = w - (acceleratorR.x + acceleratorR.width);
            arrowIconR.x = w - (arrowIconR.x + arrowIconR.width);
        }
        textR.x += menuItemGap;
        iconR.x += menuItemGap;

        return text;
    }

    public static Border getMenuBarPainter() {
        final AquaBorder border = new AquaBorder.Default();
        border.painter.state.set(Widget.MENU_BAR);
        return border;
    }

    public static Border getSelectedMenuBarItemPainter() {
        final AquaBorder border = new AquaBorder.Default();
        border.painter.state.set(Widget.MENU_TITLE);
        border.painter.state.set(State.PRESSED);
        return border;
    }

    public static Border getSelectedMenuItemPainter() {
        final AquaBorder border = new AquaBorder.Default();
        border.painter.state.set(Widget.MENU_ITEM);
        border.painter.state.set(State.PRESSED);
        return border;
    }
}
