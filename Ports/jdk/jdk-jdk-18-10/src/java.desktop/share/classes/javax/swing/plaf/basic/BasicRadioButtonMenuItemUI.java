/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.border.*;

/**
 * BasicRadioButtonMenuItem implementation
 *
 * @author Georges Saab
 * @author David Karlton
 */
public class BasicRadioButtonMenuItemUI extends BasicMenuItemUI
{
    /**
     * Constructs a {@code BasicRadioButtonMenuItemUI}.
     */
    public BasicRadioButtonMenuItemUI() {}

    /**
     * Returns a new instance of {@code BasicRadioButtonMenuItemUI}.
     *
     * @param b a component
     * @return a new instance of {@code BasicRadioButtonMenuItemUI}
     */
    public static ComponentUI createUI(JComponent b) {
        return new BasicRadioButtonMenuItemUI();
    }

    protected String getPropertyPrefix() {
        return "RadioButtonMenuItem";
    }

    /**
     * Invoked when mouse event occurs.
     *
     * @param item a menu item
     * @param e a mouse event
     * @param path an array of {@code MenuElement}
     * @param manager an instance of {@code MenuSelectionManager}
     */
    public void processMouseEvent(JMenuItem item,MouseEvent e,MenuElement[] path,MenuSelectionManager manager) {
        Point p = e.getPoint();
        if(p.x >= 0 && p.x < item.getWidth() &&
           p.y >= 0 && p.y < item.getHeight()) {
            if(e.getID() == MouseEvent.MOUSE_RELEASED) {
                manager.clearSelectedPath();
                item.doClick(0);
                item.setArmed(false);
            } else
                manager.setSelectedPath(path);
        } else if(item.getModel().isArmed()) {
            MenuElement[] newPath = new MenuElement[path.length-1];
            int i,c;
            for(i=0,c=path.length-1;i<c;i++)
                newPath[i] = path[i];
            manager.setSelectedPath(newPath);
        }
    }
}
