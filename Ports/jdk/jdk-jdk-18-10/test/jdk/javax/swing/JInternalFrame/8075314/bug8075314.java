/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8075314
 * @summary All the InternalFrames will be maximized after maximizing only one
 * of the InternalFrame with the special options "-client -Xmixed
 * -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel".
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import java.beans.PropertyVetoException;

public class bug8075314 {


    private static JFrame frame;
    private static JInternalFrame frame1;
    private static JInternalFrame frame2;

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager
                .getInstalledLookAndFeels()) {
            UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame = new JFrame();
                        frame.setUndecorated(true);
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        setup(frame);
                    }
                });

                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            frame1.setMaximum(true);
                            frame1.setClosed(true);
                            if (frame2.isMaximum()) {
                                throw new RuntimeException(
                                        "Frame2 is maximized!");
                            }
                        } catch (PropertyVetoException e) {
                            throw new RuntimeException(e);
                        }

                    }
                });
            } finally {
                if (frame != null) { frame.dispose(); }
            }
        }
        System.out.println("ok");
    }

    private static void setup(JFrame frame) {
        JDesktopPane desktop = new JDesktopPane();
        frame.setContentPane(desktop);

        frame1 = new JInternalFrame("1", true, true, true, true);
        frame1.setBounds(40, 40, 300, 200);
        frame1.setVisible(true);
        desktop.add(frame1);
        frame2 = new JInternalFrame("2", true, true, true, true);
        frame2.setBounds(20, 20, 300, 200);
        frame2.setVisible(true);
        desktop.add(frame2);

        frame.setSize(500, 400);
        frame.setVisible(true);
    }
}
