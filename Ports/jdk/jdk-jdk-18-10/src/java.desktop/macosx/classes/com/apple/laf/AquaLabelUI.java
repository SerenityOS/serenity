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

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import sun.swing.SwingUtilities2;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

public class AquaLabelUI extends BasicLabelUI {
    private static final RecyclableSingleton<AquaLabelUI> aquaLabelUI = new RecyclableSingletonFromDefaultConstructor<AquaLabelUI>(AquaLabelUI.class);

    public static ComponentUI createUI(final JComponent c) {
        return aquaLabelUI.get();
    }

    protected void installListeners(final JLabel c) {
        super.installListeners(c);
        AquaUtilControlSize.addSizePropertyListener(c);
    }

    protected void uninstallListeners(final JLabel c) {
        AquaUtilControlSize.removeSizePropertyListener(c);
        super.uninstallListeners(c);
    }

    protected void paintEnabledText(final JLabel l, final Graphics g, final String s, final int textX, final int textY) {
        int mnemIndex = l.getDisplayedMnemonicIndex();
        if (AquaMnemonicHandler.isMnemonicHidden()) {
            mnemIndex = -1;
        }

        g.setColor(l.getForeground());
        SwingUtilities2.drawStringUnderlineCharAt(l, g, s, mnemIndex, textX, textY);
    }

    /**
     * Paint clippedText at textX, textY with background.lighter() and then
     * shifted down and to the right by one pixel with background.darker().
     *
     * @see #paint
     * @see #paintEnabledText
     */
    protected void paintDisabledText(final JLabel l, final Graphics g, final String s, final int textX, final int textY) {
        int accChar = l.getDisplayedMnemonicIndex();
        if (AquaMnemonicHandler.isMnemonicHidden()) {
            accChar = -1;
        }

        final Color background = l.getBackground();

        // if our background is still something we set then we can use our happy background color.
        if (background instanceof UIResource) {
            g.setColor(getDisabledLabelColor(l));
            SwingUtilities2.drawStringUnderlineCharAt(l, g, s, accChar, textX, textY);
        } else {
            super.paintDisabledText(l, g, s, textX, textY);
        }
    }

    static final String DISABLED_COLOR_KEY = "Label.disabledForegroundColor";
    protected Color getDisabledLabelColor(final JLabel label) {
        final Color fg = label.getForeground();

        final Object colorProperty = label.getClientProperty(DISABLED_COLOR_KEY);
        if (colorProperty instanceof Color) {
            final Color disabledColor = (Color)colorProperty;
            if ((fg.getRGB() << 8) == (disabledColor.getRGB() << 8)) return disabledColor;
        }

        final Color newDisabledColor = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), fg.getAlpha() / 2);
        label.putClientProperty(DISABLED_COLOR_KEY, newDisabledColor);
        return newDisabledColor;
    }
}
