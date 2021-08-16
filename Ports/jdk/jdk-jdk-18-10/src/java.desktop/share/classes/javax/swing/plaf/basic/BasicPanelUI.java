/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import java.awt.*;
import java.awt.event.*;


/**
 * BasicPanel implementation
 *
 * @author Steve Wilson
 */
public class BasicPanelUI extends PanelUI {

    // Shared UI object
    private static PanelUI panelUI;

    /**
     * Constructs a {@code BasicPanelUI}.
     */
    public BasicPanelUI() {}

    /**
     * Returns an instance of {@code BasicPanelUI}.
     *
     * @param c a component
     * @return an instance of {@code BasicPanelUI}
     */
    public static ComponentUI createUI(JComponent c) {
        if(panelUI == null) {
            panelUI = new BasicPanelUI();
        }
        return panelUI;
    }

    public void installUI(JComponent c) {
        JPanel p = (JPanel)c;
        super.installUI(p);
        installDefaults(p);
    }

    public void uninstallUI(JComponent c) {
        JPanel p = (JPanel)c;
        uninstallDefaults(p);
        super.uninstallUI(c);
    }

    /**
     * Method for installing panel properties.
     *
     * @param p an instance of {@code JPanel}
     */
    protected void installDefaults(JPanel p) {
        LookAndFeel.installColorsAndFont(p,
                                         "Panel.background",
                                         "Panel.foreground",
                                         "Panel.font");
        LookAndFeel.installBorder(p,"Panel.border");
        LookAndFeel.installProperty(p, "opaque", Boolean.TRUE);
    }

    /**
     * Method for uninstalling panel properties.
     *
     * @param p an instance of {@code JPanel}
     */
    protected void uninstallDefaults(JPanel p) {
        LookAndFeel.uninstallBorder(p);
    }


    /**
     * Returns the baseline.
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        Border border = c.getBorder();
        if (border instanceof AbstractBorder) {
            return ((AbstractBorder)border).getBaseline(c, width, height);
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.
     *
     * @throws NullPointerException {@inheritDoc}
     * @see javax.swing.JComponent#getBaseline(int, int)
     * @since 1.6
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(
            JComponent c) {
        super.getBaselineResizeBehavior(c);
        Border border = c.getBorder();
        if (border instanceof AbstractBorder) {
            return ((AbstractBorder)border).getBaselineResizeBehavior(c);
        }
        return Component.BaselineResizeBehavior.OTHER;
    }
}
