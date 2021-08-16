/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * @test
 * @bug 8152159
 * @summary LabelUI is not updated for TitledBorder
 * @run main/othervm TitledBorderLabelUITest LAF
 * @run main/othervm TitledBorderLabelUITest LabelUI
 */

public class TitledBorderLabelUITest {

    private static final int SIZE = 50;
    private static boolean useLAF;

    public static void main(String[] args) throws Exception {
        useLAF = "LAF".equals(args[0]);
        SwingUtilities.invokeAndWait(TitledBorderLabelUITest::createAndShowGUI);
    }

    private static void createAndShowGUI() {

        try {
            UIManager.setLookAndFeel(new TestLookAndFeel());

            JLabel label = new JLabel("Test Label");
            label.setSize(SIZE, SIZE);
            TitledBorder border = new TitledBorder("ABCDEF");
            label.setBorder(new TitledBorder(border));

            if (useLAF) {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } else {
                UIManager.getDefaults().put("LabelUI", MetalLabelUI.class.getName());
            }

            SwingUtilities.updateComponentTreeUI(label);

            paintToImage(label);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void paintToImage(JComponent comp) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        comp.paint(g);
        g.dispose();
    }

    public static class TestLookAndFeel extends MetalLookAndFeel {

        @Override
        protected void initClassDefaults(UIDefaults table) {
            super.initClassDefaults(table);
            table.put("LabelUI", TestLabelUI.class.getName());
        }
    }

    public static class TestLabelUI extends MetalLabelUI implements UIResource {

        public static ComponentUI createUI(JComponent c) {
            return new TestLabelUI();
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            super.paint(g, c);
            throw new RuntimeException("New LabelUI is not installed!");
        }
    }
}