/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
 */

/* @test
   @bug 6726866 8186617
   @summary Repainting artifacts when resizing or dragging JInternalFrames in
            non-opaque toplevel
   @run applet/manual=yesno bug6726866.html
*/

import java.awt.Color;
import java.awt.Window;

import javax.swing.JApplet;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

public class bug6726866 extends JApplet {

    public void init() {
        JFrame frame = new JFrame("bug6726866");
        frame.setUndecorated(true);
        setWindowNonOpaque(frame);

        JDesktopPane desktop = new JDesktopPane();
        desktop.setBackground(Color.GREEN);
        JInternalFrame iFrame = new JInternalFrame("Test", true, true, true, true);
        iFrame.add(new JLabel("internal Frame"));
        iFrame.setBounds(10, 10, 300, 200);
        iFrame.setVisible(true);
        desktop.add(iFrame);
        frame.add(desktop);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.toFront();
    }

    public static void setWindowNonOpaque(Window window) {
        Color bg = window.getBackground();
        if (bg == null) {
            bg = new Color(0, 0, 0, 0);
        }
        window.setBackground(
                new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0));
    }
}
