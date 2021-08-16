/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthGraphicsUtils;
import javax.swing.plaf.synth.SynthIcon;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;

/*
 * @test
 * @bug 8081411
 * @summary Add an API for painting an icon with a SynthContext
 * @author Alexander Scherbatiy
 */
public class bug8081411 {

    private static final Color TEST_COLOR = new Color(71, 71, 72);

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(bug8081411::testSynthIcon);
    }

    private static void testSynthIcon() {

        if (!checkAndSetNimbusLookAndFeel()) {
            return;
        }

        JMenuItem menu = new JMenuItem();
        Icon subMenuIcon = UIManager.getIcon("Menu.arrowIcon");

        if (!(subMenuIcon instanceof SynthIcon)) {
            throw new RuntimeException("Icon is not a SynthIcon!");
        }

        Region region = SynthLookAndFeel.getRegion(menu);
        SynthStyle style = SynthLookAndFeel.getStyle(menu, region);
        SynthContext synthContext = new SynthContext(menu, region, style, SynthConstants.ENABLED);

        int width = SynthGraphicsUtils.getIconWidth(subMenuIcon, synthContext);
        int height = SynthGraphicsUtils.getIconHeight(subMenuIcon, synthContext);
        paintAndCheckIcon(subMenuIcon, synthContext, width, height);

        int newWidth = width * 17;
        int newHeight = height * 37;
        Icon centeredIcon = new CenteredSynthIcon((SynthIcon) subMenuIcon,
                newWidth, newHeight);
        paintAndCheckIcon(centeredIcon, synthContext, newWidth, newHeight);
    }

    private static void paintAndCheckIcon(Icon icon, SynthContext synthContext,
            int width, int height) {

        BufferedImage buffImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = buffImage.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        SynthGraphicsUtils.paintIcon(icon, synthContext, g, 0, 0, width, height);
        g.dispose();

        Color iconCenterColor = new Color(buffImage.getRGB(width / 2, height / 2));

        if (!TEST_COLOR.equals(iconCenterColor)) {
            throw new RuntimeException("Icon is painted incorrectly!");
        }
    }

    private static boolean checkAndSetNimbusLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return true;
                }
            }
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static class CenteredSynthIcon implements SynthIcon {

        private final SynthIcon icon;
        private final int width;
        private final int height;

        public CenteredSynthIcon(SynthIcon icon, int width, int height) {
            this.icon = icon;
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(SynthContext syntContext, Graphics g, int x, int y,
                int w, int h) {
            int dw = icon.getIconWidth(syntContext);
            int dh = icon.getIconHeight(syntContext);
            int dx = width - dw;
            int dy = height - dh;
            icon.paintIcon(syntContext, g, x + dx / 2, y + dy / 2,
                    dw + 2, dh + 2);
        }

        @Override
        public int getIconWidth(SynthContext sc) {
            return width;
        }

        @Override
        public int getIconHeight(SynthContext sc) {
            return height;
        }
    }
}
