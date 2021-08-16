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

package javax.swing.plaf.metal;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicSeparatorUI;


/**
 * A Metal L&amp;F implementation of SeparatorUI.  This implementation
 * is a "combined" view/controller.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Jeff Shapiro
 */
@SuppressWarnings("serial") // Same-version serialization only
public class MetalSeparatorUI extends BasicSeparatorUI
{
    /**
     * Constructs a {@code MetalSeparatorUI}.
     */
    public MetalSeparatorUI() {}

    /**
     * Constructs a new {@code MetalSeparatorUI} instance.
     *
     * @param c a component
     * @return a new {@code MetalSeparatorUI} instance.
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new MetalSeparatorUI();
    }

    protected void installDefaults( JSeparator s )
    {
        LookAndFeel.installColors( s, "Separator.background", "Separator.foreground" );
    }

    public void paint( Graphics g, JComponent c )
    {
        Dimension s = c.getSize();

        if ( ((JSeparator)c).getOrientation() == JSeparator.VERTICAL )
        {
          g.setColor( c.getForeground() );
          g.drawLine( 0, 0, 0, s.height );

          g.setColor( c.getBackground() );
          g.drawLine( 1, 0, 1, s.height );
        }
        else  // HORIZONTAL
        {
          g.setColor( c.getForeground() );
          g.drawLine( 0, 0, s.width, 0 );

          g.setColor( c.getBackground() );
          g.drawLine( 0, 1, s.width, 1 );
        }
    }

    public Dimension getPreferredSize( JComponent c )
    {
        if ( ((JSeparator)c).getOrientation() == JSeparator.VERTICAL )
            return new Dimension( 2, 0 );
        else
            return new Dimension( 0, 2 );
    }
}
