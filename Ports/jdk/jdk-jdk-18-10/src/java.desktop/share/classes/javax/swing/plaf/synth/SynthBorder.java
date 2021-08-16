/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.*;
import javax.swing.plaf.UIResource;

/**
 * SynthBorder is a border that delegates to a Painter. The Insets
 * are determined at construction time.
 *
 * @author Scott Violet
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class SynthBorder extends AbstractBorder implements UIResource {
    private SynthUI ui;
    private Insets insets;

    SynthBorder(SynthUI ui, Insets insets) {
        this.ui = ui;
        this.insets = insets;
    }

    SynthBorder(SynthUI ui) {
        this(ui, null);
    }

    public void paintBorder(Component c, Graphics g, int x, int y,
                            int width, int height) {
        JComponent jc = (JComponent)c;
        SynthContext context = ui.getContext(jc);
        SynthStyle style = context.getStyle();
        if (style == null) {
            assert false: "SynthBorder is being used outside after the UI " +
                          "has been uninstalled";
            return;
        }
        ui.paintBorder(context, g, x, y, width, height);
    }

    /**
     * Reinitializes the insets parameter with this Border's current Insets.
     * @param c the component for which this border insets value applies
     * @param insets the object to be reinitialized
     * @return the <code>insets</code> object
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        if (this.insets != null) {
            if (insets == null) {
                insets = new Insets(this.insets.top, this.insets.left,
                                  this.insets.bottom, this.insets.right);
            }
            else {
                insets.top    = this.insets.top;
                insets.bottom = this.insets.bottom;
                insets.left   = this.insets.left;
                insets.right  = this.insets.right;
            }
        }
        else if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        else {
            insets.top = insets.bottom = insets.left = insets.right = 0;
        }
        if (c instanceof JComponent) {
            Region region = Region.getRegion((JComponent)c);
            Insets margin = null;
            if ((region == Region.ARROW_BUTTON || region == Region.BUTTON ||
                 region == Region.CHECK_BOX ||
                 region == Region.CHECK_BOX_MENU_ITEM ||
                 region == Region.MENU || region == Region.MENU_ITEM ||
                 region == Region.RADIO_BUTTON ||
                 region == Region.RADIO_BUTTON_MENU_ITEM ||
                 region == Region.TOGGLE_BUTTON) &&
                       (c instanceof AbstractButton)) {
                margin = ((AbstractButton)c).getMargin();
            }
            else if ((region == Region.EDITOR_PANE ||
                      region == Region.FORMATTED_TEXT_FIELD ||
                      region == Region.PASSWORD_FIELD ||
                      region == Region.TEXT_AREA ||
                      region == Region.TEXT_FIELD ||
                      region == Region.TEXT_PANE) &&
                        (c instanceof JTextComponent)) {
                margin = ((JTextComponent)c).getMargin();
            }
            else if (region == Region.TOOL_BAR && (c instanceof JToolBar)) {
                margin = ((JToolBar)c).getMargin();
            }
            else if (region == Region.MENU_BAR && (c instanceof JMenuBar)) {
                margin = ((JMenuBar)c).getMargin();
            }
            if (margin != null) {
                insets.top += margin.top;
                insets.bottom += margin.bottom;
                insets.left += margin.left;
                insets.right += margin.right;
            }
        }
        return insets;
    }

    /**
     * This default implementation returns false.
     * @return false
     */
    public boolean isBorderOpaque() {
        return false;
    }
}
