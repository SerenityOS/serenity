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

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JToolBar;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicSeparatorUI;


/**
 * A Basic L&amp;F implementation of ToolBarSeparatorUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Jeff Shapiro
 */

public class BasicToolBarSeparatorUI extends BasicSeparatorUI
{
    /**
     * Constructs a {@code BasicToolBarSeparatorUI}.
     */
    public BasicToolBarSeparatorUI() {}

    /**
     * Returns a new instance of {@code BasicToolBarSeparatorUI}.
     *
     * @param c a component
     * @return a new instance of {@code BasicToolBarSeparatorUI}
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new BasicToolBarSeparatorUI();
    }

    protected void installDefaults( JSeparator s )
    {
        Dimension size = ( (JToolBar.Separator)s ).getSeparatorSize();

        if ( size == null || size instanceof UIResource )
        {
            JToolBar.Separator sep = (JToolBar.Separator)s;
            size = (Dimension)(UIManager.get("ToolBar.separatorSize"));
            if (size != null) {
                if (sep.getOrientation() == JSeparator.HORIZONTAL) {
                    size = new Dimension(size.height, size.width);
                }
                sep.setSeparatorSize(size);
            }
        }
    }

    public void paint( Graphics g, JComponent c )
    {
    }

    public Dimension getPreferredSize( JComponent c )
    {
        Dimension size = ( (JToolBar.Separator)c ).getSeparatorSize();

        if ( size != null )
        {
            return size.getSize();
        }
        else
        {
            return null;
        }
    }
}
