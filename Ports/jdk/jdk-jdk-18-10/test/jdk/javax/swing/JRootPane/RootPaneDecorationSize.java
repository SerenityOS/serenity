/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @bug 4916923
 * @key headful
 * @summary MetalRootLayout does not correctly calculate minimumsize
 */
public final class RootPaneDecorationSize {

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            EventQueue.invokeAndWait(() -> setLookAndFeel(laf));
            EventQueue.invokeAndWait(RootPaneDecorationSize::test);
        }
    }

    private static void test() {
        JFrame frame = new JFrame();
        Dimension size;
        Dimension min;
        Dimension pref;
        try {
            // undecorated frame and decorated root pane usually used together
            frame.setUndecorated(true);
            frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            // customize the current L&F (mimic custom L&F)
            JLayeredPane layeredPane = frame.getRootPane().getLayeredPane();
            for (Component comp : layeredPane.getComponents()) {
                comp.setMinimumSize(new Dimension(1000, 10));
                comp.setMaximumSize(new Dimension(1000, 10));
                comp.setPreferredSize(new Dimension(1000, 10));
            }
            frame.pack();
            size = frame.getSize();
            min = frame.getMinimumSize();
            pref = frame.getPreferredSize();
        } finally {
            frame.dispose();
        }
        System.err.println("\tsize = " + size);
        System.err.println("\tminimumSize = " + min);
        System.err.println("\tpreferredSize = " + pref);

        // We cannot predict which size will be used by the current L&F
        // but based on customization above the height < 1000 and width > 1000
        if (size.height > 1000 || min.height > 1000 || pref.height > 1000) {
            throw new RuntimeException("The height too big");
        }
        if (size.width < 1000 || min.width < 1000 || pref.width < 1000) {
            throw new RuntimeException("The width too small");
        }
    }

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            System.err.println("LookAndFeel: " + laf.getClassName());
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored){
            System.err.println("Unsupported LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
