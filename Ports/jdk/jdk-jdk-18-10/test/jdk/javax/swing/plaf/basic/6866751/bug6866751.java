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
 * @bug 6866751
 * @summary J2SE_Swing_Reg: the caret disappears when moving to the end of the line.
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import java.awt.*;

public class bug6866751 {
    private static JFrame frame;
    private static JTextArea area;

    public static void main(String[] args) throws Exception {
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
                public void run() {
                    int width = area.getWidth();
                    double caretX =
                            area.getCaret().getMagicCaretPosition().getX();
                    if (width < caretX + 1) {
                        throw new RuntimeException(
                                "Width of the area (" + width +
                                        ") is less than caret x-position " +
                                        caretX + 1);
                    }
                    area.putClientProperty("caretWidth", 10);
                    frame.pack();
                }
            });
            new Robot().waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    int width = area.getWidth();
                    double caretX =
                            area.getCaret().getMagicCaretPosition().getX();
                    if (width < caretX + 10) {
                        throw new RuntimeException(
                                "Width of the area (" + width +
                                        ") is less  than caret x-position " +
                                        caretX + 10);
                    }
                }
            });
            System.out.println("ok");
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (frame != null) { frame.dispose(); }
                }
            });
        }
    }

    static void setup(JFrame frame) {
        area = new JTextArea();
        frame.getContentPane().add(new JScrollPane(area));
        area.setText(
                "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm");
        area.getCaret().setDot(area.getText().length() + 1);

        frame.setSize(300, 200);
        frame.setVisible(true);

        area.requestFocus();

    }

}
