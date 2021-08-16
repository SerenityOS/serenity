/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.plaf.*;


/**
 * A Metal L&amp;F implementation of PopupMenuSeparatorUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Jeff Shapiro
 */

public class MetalPopupMenuSeparatorUI extends MetalSeparatorUI
{
    /**
     * Constructs a {@code MetalPopupMenuSeparatorUI}.
     */
    public MetalPopupMenuSeparatorUI() {}

    /**
     * Constructs a new {@code MetalPopupMenuSeparatorUI} instance.
     *
     * @param c a component
     * @return a new {@code MetalPopupMenuSeparatorUI} instance
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new MetalPopupMenuSeparatorUI();
    }

    public void paint( Graphics g, JComponent c )
    {
        Dimension s = c.getSize();

        g.setColor( c.getForeground() );
        g.drawLine( 0, 1, s.width, 1 );

        g.setColor( c.getBackground() );
        g.drawLine( 0, 2, s.width, 2 );
        g.drawLine( 0, 0, 0, 0 );
        g.drawLine( 0, 3, 0, 3 );
    }

    public Dimension getPreferredSize( JComponent c )
    {
        return new Dimension( 0, 4 );
    }
}
