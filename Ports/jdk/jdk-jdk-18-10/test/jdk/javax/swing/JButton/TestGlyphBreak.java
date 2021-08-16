/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191428
 * @summary  Verifies if text view is not borken into multiple lines
 * @key headful
 * @run main/othervm -Dsun.java2d.uiScale=1.2 TestGlyphBreak
 */

import java.awt.FontMetrics;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class TestGlyphBreak {

    static JFrame f;
    static int btnHeight;
    static FontMetrics fm;

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            String str = "<html><font size=2 color=red><bold>Three!</font></html>";
            JButton b = new JButton();
            b.setText(str);

            f = new JFrame();
            f.add(b);
            f.pack();
            f.setVisible(true);
            btnHeight = b.getHeight();
            fm = b.getFontMetrics(b.getFont());

        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
        }
        SwingUtilities.invokeAndWait(() -> f.dispose());
        System.out.println("metrics getHeight " + fm.getHeight() +
                             " button height " + btnHeight);

        // Check if text is broken into 2 lines, in which case button height
        // will be twice the string height
        if (btnHeight > 2*fm.getHeight()) {
            throw new RuntimeException("TextView is broken into different lines");
        }
    }
}
