/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6524424
 * @requires (os.family == "windows")
 * @summary JSlider Clicking In Tracks Behavior Inconsistent For Different Tick Spacings
 * @author Pavel Porvatov
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 * @run applet/manual=done bug6524424.html
 */

import java.awt.*;
import javax.swing.*;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class bug6524424 extends JApplet {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new WindowsLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();

            return;
        }

        TestPanel panel = new TestPanel();

        JFrame frame = new JFrame();

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }

    public void init() {
        TestPanel panel = new TestPanel();

        setContentPane(panel);
    }

    private static class TestPanel extends JPanel {

        private TestPanel() {
            super(new GridBagLayout());

            JSlider slider1 = createSlider(1, 2);
            JSlider slider2 = createSlider(2, 4);
            JSlider slider3 = createSlider(3, 6);

            addComponent(this, slider1);
            addComponent(this, slider2);
            addComponent(this, slider3);
        }

        private JSlider createSlider(int tickMinor, int tickMajor) {
            JSlider result = new JSlider();

            result.setPaintLabels(true);
            result.setPaintTicks(true);
            result.setSnapToTicks(true);
            result.setMinimum(0);
            result.setMaximum(12);
            result.setMinorTickSpacing(tickMinor);
            result.setMajorTickSpacing(tickMajor);

            return result;
        }
    }

    private static void addComponent(JPanel panel, Component component) {
        panel.add(component, new GridBagConstraints(0,
                panel.getComponentCount(), 1, 1,
                1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
    }
}
