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
 * @bug 6587742
 * @summary filling half of a JSlider's track is no longer optional
 * @author Pavel Porvatov
 * @run applet/manual=done bug6587742.html
 */

import javax.swing.*;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class bug6587742 extends JApplet {
    public void init() {
        TestPanel panel = new TestPanel();

        setContentPane(panel);
    }

    private class TestPanel extends JPanel {
        private final JComboBox cbThemes = new JComboBox();

        private TestPanel() {
            // Fill cbThemes
            cbThemes.addItem(new OceanTheme());
            cbThemes.addItem(new DefaultMetalTheme());

            cbThemes.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    MetalTheme theme = (MetalTheme) cbThemes.getSelectedItem();

                    if (theme != null) {
                        MetalLookAndFeel.setCurrentTheme(theme);

                        // re-install the Metal Look and Feel
                        try {
                            UIManager.setLookAndFeel(new MetalLookAndFeel());
                        } catch (UnsupportedLookAndFeelException e1) {
                            JOptionPane.showMessageDialog(TestPanel.this, "Can't change theme: " + e1.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);

                            return;
                        }

                        SwingUtilities.updateComponentTreeUI(bug6587742.this);
                    }
                }
            });

            JPanel pnVertical = new JPanel();

            pnVertical.setLayout(new BoxLayout(pnVertical, BoxLayout.Y_AXIS));

            for (int i = 0; i < 12; i++) {
                int filled = i >> 2;

                pnVertical.add(createSlider(false, filled > 1 ? null : Boolean.valueOf(filled == 1), (i & 2) == 0,
                        (i & 1) != 0));
            }

            JPanel pnHorizontal = new JPanel();

            pnHorizontal.setLayout(new BoxLayout(pnHorizontal, BoxLayout.X_AXIS));

            for (int i = 0; i < 12; i++) {
                int filled = i >> 2;

                pnHorizontal.add(createSlider(true, filled > 1 ? null : Boolean.valueOf(filled == 1), (i & 2) == 0,
                        (i & 1) != 0));
            }

            JTabbedPane tpSliders = new JTabbedPane();

            tpSliders.add("Vertical sliders", pnVertical);
            tpSliders.add("Horizontal sliders", pnHorizontal);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            add(new JLabel("Select theme:"));
            add(cbThemes);
            add(tpSliders);
        }
    }

    private static JSlider createSlider(boolean vertical, Boolean filled, boolean enabled, boolean inverted) {
        JSlider result = new JSlider(vertical ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL, 0, 100, 50);

        result.setMajorTickSpacing(20);
        result.setMinorTickSpacing(5);
        result.setPaintTicks(true);
        result.setPaintLabels(true);
        result.setEnabled(enabled);

        if (filled != null) {
            result.putClientProperty("JSlider.isFilled", filled);
        }

        result.setInverted(inverted);
        result.setToolTipText("<html>vertical = " + vertical + "<br>enabled = " + enabled + "<br>filled = " + filled +
                "<br>inverted = " + inverted + "</html>");

        return result;
    }
}
