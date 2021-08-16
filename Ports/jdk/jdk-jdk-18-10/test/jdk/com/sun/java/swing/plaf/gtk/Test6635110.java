/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6635110
   @key headful
   @summary GTK icons should not throw NPE when called by non-GTK UI
   @author Peter Zhelezniakov
   @run main Test6635110
*/

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.plaf.basic.*;


public class Test6635110 implements Runnable {

    static final int WIDTH = 160;
    static final int HEIGHT = 80;
    final BufferedImage IMAGE =
            new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

    @Override public void run() {
        JMenu menu = new JMenu("menu");
        menu.setUI(new BasicMenuUI());
        paint(menu);

        JToolBar tb = new JToolBar();
        tb.setFloatable(true);
        tb.setUI(new BasicToolBarUI());
        paint(tb);
    }

    void paint(Component c) {
        c.setSize(WIDTH, HEIGHT);
        c.paint(IMAGE.getGraphics());
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            System.out.println("GTKLookAndFeel cannot be set, skipping this test");
            return;
        }
        SwingUtilities.invokeAndWait(new Test6635110());
    }
}
