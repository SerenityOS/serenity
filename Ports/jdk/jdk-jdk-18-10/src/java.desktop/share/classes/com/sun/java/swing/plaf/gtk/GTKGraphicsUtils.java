/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.java.swing.plaf.gtk;

import javax.swing.*;
import javax.swing.plaf.synth.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * @author Joshua Outwater
 */
class GTKGraphicsUtils extends SynthGraphicsUtils {
    public void paintText(SynthContext context, Graphics g, String text,
                          int x, int y, int mnemonicIndex) {
        if (text == null || text.length() <= 0) {
            // We don't need to paint empty strings
            return;
        }

        if (context.getRegion() == Region.INTERNAL_FRAME_TITLE_PANE) {
            // Metacity handles painting of text on internal frame title,
            // ignore this.
            return;
        }
        int componentState = context.getComponentState();

        String themeName = GTKLookAndFeel.getGtkThemeName();
        if (themeName != null && themeName.startsWith("blueprint") &&
            shouldShadowText(context.getRegion(), componentState)) {

            g.setColor(Color.BLACK);
            super.paintText(context, g, text, x+1, y+1, mnemonicIndex);
            g.setColor(Color.WHITE);
        }

        super.paintText(context, g, text, x, y, mnemonicIndex);
    }

    /**
     * Paints text at the specified location. This will not attempt to
     * render the text as html nor will it offset by the insets of the
     * component.
     *
     * @param context SynthContext
     * @param g Graphics used to render string in.
     * @param text Text to render
     * @param bounds Bounds of the text to be drawn.
     * @param mnemonicIndex Index to draw string at.
     */
    public void paintText(SynthContext context, Graphics g, String text,
                          Rectangle bounds, int mnemonicIndex) {
        if (text == null || text.length() <= 0) {
            // We don't need to paint empty strings
            return;
        }

        Region id = context.getRegion();
        if ((id == Region.RADIO_BUTTON ||
             id == Region.CHECK_BOX ||
             id == Region.TABBED_PANE_TAB) &&
            (context.getComponentState() & SynthConstants.FOCUSED) != 0)
        {
            JComponent source = context.getComponent();
            if (!(source instanceof AbstractButton) ||
                ((AbstractButton)source).isFocusPainted()) {

                // The "bounds" parameter encompasses only the actual text;
                // when drawing the focus, we need to expand that bounding
                // box by "focus-line-width" plus "focus-padding".  Note that
                // the layout process for these components will have already
                // taken these values into account, so there should always
                // be enough space allocated for drawing the focus indicator.
                int synthState = context.getComponentState();
                GTKStyle style = (GTKStyle)context.getStyle();
                int focusSize =
                    style.getClassSpecificIntValue(context,
                                                   "focus-line-width", 1);
                int focusPad =
                    style.getClassSpecificIntValue(context,
                                                   "focus-padding", 1);
                int totalFocus = focusSize + focusPad;
                int x = bounds.x - totalFocus;
                int y = bounds.y - totalFocus;
                int w = bounds.width  + (2 * totalFocus);
                int h = bounds.height + (2 * totalFocus);

                Color color = g.getColor();
                GTKPainter.INSTANCE.paintFocus(context, g, id,
                                               synthState, "checkbutton",
                                               x, y, w, h);
                g.setColor(color);
            }
        }
        super.paintText(context, g, text, bounds, mnemonicIndex);
    }

    private static boolean shouldShadowText(Region id, int state) {
        int gtkState = GTKLookAndFeel.synthStateToGTKState(id, state);
        return((gtkState == SynthConstants.MOUSE_OVER) &&
               (id == Region.MENU ||
                id == Region.MENU_ITEM ||
                id == Region.CHECK_BOX_MENU_ITEM ||
                id == Region.RADIO_BUTTON_MENU_ITEM));
    }
}
