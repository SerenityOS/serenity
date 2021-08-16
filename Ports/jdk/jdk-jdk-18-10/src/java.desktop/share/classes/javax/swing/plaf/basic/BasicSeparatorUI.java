/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;


/**
 * A Basic L&amp;F implementation of SeparatorUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Georges Saab
 * @author Jeff Shapiro
 */

public class BasicSeparatorUI extends SeparatorUI
{
    /**
     * The color of the shadow.
     */
    protected Color shadow;

    /**
     * The color of the highlighting.
     */
    protected Color highlight;

    /**
     * Constructs a {@code BasicSeparatorUI}.
     */
    public BasicSeparatorUI() {}

    /**
     * Returns a new instance of {@code BasicSeparatorUI}.
     *
     * @param c a component
     * @return a new instance of {@code BasicSeparatorUI}
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new BasicSeparatorUI();
    }

    public void installUI( JComponent c )
    {
        installDefaults( (JSeparator)c );
        installListeners( (JSeparator)c );
    }

    public void uninstallUI(JComponent c)
    {
        uninstallDefaults( (JSeparator)c );
        uninstallListeners( (JSeparator)c );
    }

    /**
     * Installs default properties.
     *
     * @param s an instance of {@code JSeparator}
     */
    protected void installDefaults( JSeparator s )
    {
        LookAndFeel.installColors(s, "Separator.background", "Separator.foreground");
        LookAndFeel.installProperty(s, "opaque", Boolean.FALSE);
    }

    /**
     * Uninstalls default properties.
     *
     * @param s an instance of {@code JSeparator}
     */
    protected void uninstallDefaults( JSeparator s )
    {
    }

    /**
     * Registers listeners.
     *
     * @param s an instance of {@code JSeparator}
     */
    protected void installListeners( JSeparator s )
    {
    }

    /**
     * Unregisters listeners.
     *
     * @param s an instance of {@code JSeparator}
     */
    protected void uninstallListeners( JSeparator s )
    {
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

    public Dimension getMinimumSize( JComponent c ) { return null; }
    public Dimension getMaximumSize( JComponent c ) { return null; }
}
