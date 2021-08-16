/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6742358
 * @summary MetalSliderUI paint wrong vertical disabled filled JSlider for DefaultMetalTheme
 * @author Pavel Porvatov
 * @run applet/manual=done bug6742358.html
 */

import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class bug6742358 extends JApplet {
    public static void main(String[] args) {
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());

        JFrame frame = new JFrame();

        frame.setContentPane(new TestPanel());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    public void init() {
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());

        TestPanel panel = new TestPanel();

        setContentPane(panel);
    }

    private static class TestPanel extends JPanel {

        private TestPanel() {
            JPanel pnVertical = new JPanel();

            pnVertical.setLayout(new BoxLayout(pnVertical, BoxLayout.Y_AXIS));

            for (int i = 0; i < 8; i++) {
                pnVertical.add(createSlider(false, (i & 4) == 0, (i & 2) == 0, (i & 1) == 0));
            }

            JPanel pnHorizontal = new JPanel();

            pnHorizontal.setLayout(new BoxLayout(pnHorizontal, BoxLayout.X_AXIS));

            for (int i = 0; i < 8; i++) {
                pnHorizontal.add(createSlider(true, (i & 4) == 0, (i & 2) == 0, (i & 1) == 0));
            }

            add(pnHorizontal);
            add(pnVertical);
        }
    }

    private static JSlider createSlider(boolean vertical, boolean enabled, boolean filled, boolean inverted) {
        JSlider result = new JSlider(vertical ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL, 0, 10, 5);

        result.setEnabled(enabled);
        result.putClientProperty("JSlider.isFilled", filled);
        result.setInverted(inverted);
        result.setToolTipText("<html>vertical = " + vertical + "<br>enabled = " + enabled + "<br>filled = " + filled +
                "<br>inverted = " + inverted + "</html>");

        return result;
    }
}
