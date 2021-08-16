/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;

import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaUtils.*;

public abstract class AquaButtonBorder extends AquaBorder implements Border, UIResource {
    private static final RecyclableSingleton<Dynamic> fDynamic = new RecyclableSingletonFromDefaultConstructor<Dynamic>(Dynamic.class);
    public static AquaButtonBorder getDynamicButtonBorder() {
        return fDynamic.get();
    }

    private static final RecyclableSingleton<Toggle> fToggle = new RecyclableSingletonFromDefaultConstructor<Toggle>(Toggle.class);
    public static AquaButtonBorder getToggleButtonBorder() {
        return fToggle.get();
    }

    private static final RecyclableSingleton<Toolbar> fToolBar = new RecyclableSingletonFromDefaultConstructor<Toolbar>(Toolbar.class);
    public static Border getToolBarButtonBorder() {
        return fToolBar.get();
    }

    private static final RecyclableSingleton<Named> fBevel = new RecyclableSingleton<Named>() {
        protected Named getInstance() {
            return new Named(Widget.BUTTON_BEVEL, new SizeDescriptor(new SizeVariant().alterMargins(2, 4, 2, 4)));
        }
    };
    public static AquaButtonBorder getBevelButtonBorder() {
        return fBevel.get();
    }

    public AquaButtonBorder(final SizeDescriptor sizeDescriptor) {
        super(sizeDescriptor);
    }

    public AquaButtonBorder(final AquaButtonBorder other) {
        super(other);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        // for now we don't paint a border. We let the button paint it since there
        // needs to be a strict ordering for aqua components.
        //paintButton(c, g, x, y, width, height);
    }

    public void paintButton(final Component c, final Graphics g, int x, int y, int width, int height) {
        final AbstractButton b = (AbstractButton)c;
        final ButtonModel model = b.getModel();

        final State state = getButtonState(b, model);
        painter.state.set(state);
        painter.state.set((state != State.DISABLED && state != State.INACTIVE) && b.isFocusPainted() && isFocused(b) ? Focused.YES : Focused.NO);

        // Full border size of the component.
        // g.setColor(new Color(0, 255, 0, 70));
        // g.drawRect(x, y, width - 1, height - 1);

        final Insets subInsets = sizeVariant.insets;
        x += subInsets.left;
        y += subInsets.top;
        width -= (subInsets.left + subInsets.right);
        height -= (subInsets.top + subInsets.bottom);

        // Where the native border should start to paint.
        // g.setColor(new Color(255, 0, 255, 70));
        // g.drawRect(x, y, width - 1, height - 1);

        doButtonPaint(b, model, g, x, y, width, height);
    }

    protected void doButtonPaint(final AbstractButton b, final ButtonModel model, final Graphics g, final int x, final int y, final int width, final int height) {
        painter.paint(g, b, x, y, width, height);
    }

    protected State getButtonState(final AbstractButton b, final ButtonModel model) {
        if (!b.isEnabled()) return State.DISABLED;

        // The default button shouldn't draw its color when the window is inactive.
        // Changed for <rdar://problem/3614421>: Aqua LAF Buttons are incorrectly drawn disabled
        // all we need to do is make sure we aren't the default button any more and that
        // we aren't active, but we still are enabled if the button is enabled.
        // if we set dimmed we would appear disabled despite being enabled and click through
        // works so this now matches the text drawing and most importantly the HIG
        if (!AquaFocusHandler.isActive(b)) return State.INACTIVE;

        if (model.isArmed() && model.isPressed()) return State.PRESSED;
        if (model.isSelected() && isSelectionPressing()) return State.PRESSED;
        if ((b instanceof JButton) && ((JButton)b).isDefaultButton()) return State.PULSED;

        return State.ACTIVE;
    }

    protected boolean isSelectionPressing() {
        return true;
    }

    public boolean hasSmallerInsets(final JComponent c) {
        final Insets inset = c.getInsets();
        final Insets margin = sizeVariant.margins;

        if (margin.equals(inset)) return false;

        return (
            (inset.top < margin.top) ||
            (inset.left < margin.left) ||
            (inset.right < margin.right) ||
            (inset.bottom < margin.bottom)
        );
    }

    /**
     * Returns the insets of the border.
     * @param c the component for which this border insets value applies
     */
    public Insets getBorderInsets(final Component c) {
        if (c == null || !(c instanceof AbstractButton)) return new Insets(0, 0, 0, 0);

        Insets margin = ((AbstractButton)c).getMargin();
        margin = (margin == null) ? new InsetsUIResource(0, 0, 0, 0) : (Insets)margin.clone();

        margin.top += sizeVariant.margins.top;
        margin.bottom += sizeVariant.margins.bottom;
        margin.left += sizeVariant.margins.left;
        margin.right += sizeVariant.margins.right;

        return margin;
    }

    public Insets getContentInsets(final AbstractButton b, final int w, final int h) {
        return null;
    }

    public void alterPreferredSize(final Dimension d) {
        if (sizeVariant.h > 0 && sizeVariant.h > d.height) d.height = sizeVariant.h;
        if (sizeVariant.w > 0 && sizeVariant.w > d.width) d.width = sizeVariant.w;
    }

    /**
     * Returns whether or not the border is opaque.  If the border
     * is opaque, it is responsible for filling in it's own
     * background when painting.
     */
    public boolean isBorderOpaque() {
        return false;
    }

    static class SizeConstants {
        protected static final int fNormalButtonHeight = 29;
        protected static final int fNormalMinButtonWidth = 40;
        protected static final int fSquareButtonHeightThreshold = 23;
        protected static final int fSquareButtonWidthThreshold = 16;
    }

    public static class Dynamic extends AquaButtonBorder {
        final Insets ALTERNATE_PUSH_INSETS = new Insets(3, 12, 5, 12);
        final Insets ALTERNATE_BEVEL_INSETS = new Insets(0, 5, 0, 5);
        final Insets ALTERNATE_SQUARE_INSETS = new Insets(0, 2, 0, 2);
        public Dynamic() {
            super(new SizeDescriptor(new SizeVariant(75, 29).alterMargins(3, 20, 5, 20)) {
                public SizeVariant deriveSmall(final SizeVariant v) {
                    return super.deriveSmall(v.alterMinSize(0, -2).alterMargins(0, -3, 0, -3).alterInsets(-3, -3, -4, -3));
                }
                public SizeVariant deriveMini(final SizeVariant v) {
                    return super.deriveMini(v.alterMinSize(0, -2).alterMargins(0, -3, 0, -3).alterInsets(-3, -3, -1, -3));
                }
            });
        }

        public Dynamic(final Dynamic other) {
            super(other);
        }

        protected State getButtonState(final AbstractButton b, final ButtonModel model) {
            final State state = super.getButtonState(b, model);
            painter.state.set(state == State.PULSED ? Animating.YES : Animating.NO);
            return state;
        }

        public Insets getContentInsets(final AbstractButton b, final int width, final int height) {
            final Size size = AquaUtilControlSize.getUserSizeFrom(b);
            final Widget style = getStyleForSize(b, size, width, height);

            if (style == Widget.BUTTON_PUSH) {
                return ALTERNATE_PUSH_INSETS;
            }
            if (style == Widget.BUTTON_BEVEL_ROUND) {
                return ALTERNATE_BEVEL_INSETS;
            }
            if (style == Widget.BUTTON_BEVEL) {
                return ALTERNATE_SQUARE_INSETS;
            }

            return null;
        }

        protected void doButtonPaint(final AbstractButton b, final ButtonModel model, final Graphics g, int x, int y, int width, int height) {
            final Size size = AquaUtilControlSize.getUserSizeFrom(b);
            painter.state.set(size);

            final Widget style = getStyleForSize(b, size, width, height);
            painter.state.set(style);

            // custom adjusting
            if (style == Widget.BUTTON_PUSH && y % 2 == 0) {
                if (size == Size.REGULAR) { y += 1; height -= 1; }
                if (size == Size.MINI) { height -= 1; x += 4; width -= 8; }
            }

            super.doButtonPaint(b, model, g, x, y, width, height);
        }

        protected Widget getStyleForSize(final AbstractButton b, final Size size, final int width, final int height) {
            if (size != null && size != Size.REGULAR) {
                return Widget.BUTTON_PUSH;
            }

            if (height < SizeConstants.fSquareButtonHeightThreshold || width < SizeConstants.fSquareButtonWidthThreshold) {
                return Widget.BUTTON_BEVEL;
            }

            if (height <= SizeConstants.fNormalButtonHeight + 3 && width < SizeConstants.fNormalMinButtonWidth) {
                return Widget.BUTTON_BEVEL;
            }

            if ((height > SizeConstants.fNormalButtonHeight + 3) || (b.getIcon() != null) || hasSmallerInsets(b)){
                return Widget.BUTTON_BEVEL_ROUND;
            }

            return Widget.BUTTON_PUSH;
        }
    }

    public static class Toggle extends AquaButtonBorder {
        public Toggle() {
            super(new SizeDescriptor(new SizeVariant().alterMargins(6, 6, 6, 6)));
        }

        public Toggle(final Toggle other) {
            super(other);
        }

        protected void doButtonPaint(final AbstractButton b, final ButtonModel model, final Graphics g, final int x, final int y, final int width, final int height) {
            if (height < SizeConstants.fSquareButtonHeightThreshold || width < SizeConstants.fSquareButtonWidthThreshold) {
                painter.state.set(Widget.BUTTON_BEVEL);
                super.doButtonPaint(b, model, g, x, y, width, height);
                return;
            }

            painter.state.set(Widget.BUTTON_BEVEL_ROUND);
            super.doButtonPaint(b, model, g, x, y + 1, width, height - 1);
        }
    }

    public static class Named extends AquaButtonBorder {
        public Named(final Widget widget, final SizeDescriptor sizeDescriptor) {
            super(sizeDescriptor);
            painter.state.set(widget);
        }

        // called by reflection
        public Named(final Named sizeDescriptor) {
            super(sizeDescriptor);
        }

        protected void doButtonPaint(final AbstractButton b, final ButtonModel model, final Graphics g, final int x, final int y, final int width, final int height) {
            painter.state.set(model.isSelected() ? BooleanValue.YES : BooleanValue.NO);
            super.doButtonPaint(b, model, g, x, y, width, height);
        }
    }

    public static class Toolbar extends AquaButtonBorder {
        public Toolbar() {
            super(new SizeDescriptor(new SizeVariant().alterMargins(5, 5, 5, 5)));
            painter.state.set(Widget.TOOLBAR_ITEM_WELL);
        }

        public Toolbar(final Toolbar other) {
            super(other);
        }

        protected void doButtonPaint(final AbstractButton b, final ButtonModel model, final Graphics g, final int x, final int y, final int w, final int h) {
            if (!model.isSelected()) return; // only paint when the toolbar button is selected
            super.doButtonPaint(b, model, g, x, y, w, h);
        }
    }
}
