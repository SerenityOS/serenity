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

package javax.swing.colorchooser;

import java.awt.*;
import java.io.*;


/**
  * Center-positioning layout manager.
  * @author Tom Santos
  * @author Steve Wilson
  */
@SuppressWarnings("serial") // Same-version serialization only
class CenterLayout implements LayoutManager, Serializable {
    public void addLayoutComponent(String name, Component comp) { }
    public void removeLayoutComponent(Component comp) { }

    public Dimension preferredLayoutSize( Container container ) {
        Component c = container.getComponent( 0 );
        if ( c != null ) {
            Dimension size = c.getPreferredSize();
            Insets insets = container.getInsets();
            size.width += insets.left + insets.right;
            size.height += insets.top + insets.bottom;
            return size;
        }
        else {
            return new Dimension( 0, 0 );
        }
    }

    public Dimension minimumLayoutSize(Container cont) {
        return preferredLayoutSize(cont);
    }

    public void layoutContainer(Container container) {
        try {
           Component c = container.getComponent( 0 );

           c.setSize( c.getPreferredSize() );
           Dimension size = c.getSize();
           Dimension containerSize = container.getSize();
           Insets containerInsets = container.getInsets();
           containerSize.width -= containerInsets.left + containerInsets.right;
           containerSize.height -= containerInsets.top + containerInsets.bottom;
           int componentLeft = (containerSize.width / 2) - (size.width / 2);
           int componentTop = (containerSize.height / 2) - (size.height / 2);
           componentLeft += containerInsets.left;
           componentTop += containerInsets.top;

            c.setBounds( componentLeft, componentTop, size.width, size.height );
         }
         catch( Exception e ) {
         }
    }
}
