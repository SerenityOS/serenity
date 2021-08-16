/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */


package com.sun.java.swing.ui;

import java.awt.*;
import javax.swing.ImageIcon;

public class SplashScreen extends Window
{

    public SplashScreen(Frame f)
    {
        super(f);
        setBackground(Color.white);
        java.net.URL url = getClass().getResource("/images/SplashScreen.jpg");
        if(url != null)
        {
            screen = new ImageIcon(url);
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(screen.getImage(), 0);
            try
            {
                mt.waitForAll();
            }
            catch(Exception ex) { }
        }
    }

    public void setVisible(boolean val)
    {
        if(screen == null)
            return;
        if(val)
        {
            setSize(screen.getIconWidth(), screen.getIconHeight());
            setLocation(-500, -500);
            super.setVisible(true);
            Dimension d = getToolkit().getScreenSize();
            Insets i = getInsets();
            int w = screen.getIconWidth() + i.left + i.right;
            int h = screen.getIconHeight() + i.top + i.bottom;
            setSize(w, h);
            setLocation(d.width / 2 - w / 2, d.height / 2 - h / 2);
        } else
        {
            super.setVisible(false);
        }
    }

    public void paint(Graphics g)
    {
        if(screen != null)
        {
            Dimension d = getSize();
            g.setColor(Color.black);
            g.drawRect(0, 0, d.width - 1, d.height - 1);
            g.drawImage(screen.getImage(), 1, 1, this);
        }
    }

    private ImageIcon screen;
}
